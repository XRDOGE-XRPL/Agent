#!/usr/bin/env python3
"""Intelligenter Bootstrap für Termux-/Android-Laufzeitumgebungen.

Das Skript erkennt automatisch, ob es in einer Termux-Umgebung läuft, prüft die
wesentlichen Pakete und Konfigurationspfade und richtet die minimale Runtime ein,
ohne dass der Nutzer manuell jede Abhängigkeit auflösen muss.
"""

from __future__ import annotations

import argparse
import json
import os
import platform
import shlex
import shutil
import socket
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Dict, Iterable, List, Optional

DEFAULT_REQUIRED_PACKAGES = [
    "git",
    "cmake",
    "clang",
    "python",
    "make",
    "curl",
    "wget",
    "openssl",
    "termux-api",
    "nodejs",
    "jq",
]

DEFAULT_CONFIG = {
    "termux": False,
    "workspace": "~/workspace",
    "ollama_url": "http://127.0.0.1:11434",
    "model": "qwen2.5-coder",
    "port": 5050,
    "socket_host": "127.0.0.1",
    "timeout_seconds": 120,
    "offline_mode": False,
    "runtime_dir": "~/.agent",
    "log_dir": "~/.agent/logs",
}


def detect_termux() -> bool:
    prefix = os.environ.get("PREFIX", "")
    if "termux" in prefix.lower() or "com.termux" in prefix:
        return True
    if os.path.exists("/data/data/com.termux"):
        return True
    if shutil.which("termux-info") is not None:
        return True
    release = platform.uname().release.lower()
    return "termux" in release or "android" in release and os.environ.get("TERMUX_VERSION") is not None


def detect_architecture() -> str:
    machine = platform.machine() or platform.processor() or "unknown"
    return machine.lower()


def detect_memory_mb() -> Optional[int]:
    try:
        with open("/proc/meminfo", "r", encoding="utf-8") as handle:
            for line in handle:
                if line.startswith("MemTotal:"):
                    parts = line.split()
                    if len(parts) >= 3:
                        return int(int(parts[1]) / 1024)
        return None
    except OSError:
        return None


def detect_java_home() -> Optional[str]:
    candidates = [
        os.environ.get("JAVA_HOME"),
        "/usr/lib/jvm/java-21-openjdk-amd64",
        "/usr/lib/jvm/java-21-openjdk-arm64",
        "/usr/lib/jvm/java-21-openjdk",
        "/usr/lib/jvm/java-17-openjdk-amd64",
        "/usr/lib/jvm/java-17-openjdk-arm64",
        "/usr/lib/jvm/default-java",
        "/usr/lib/jvm/default",
    ]
    for candidate in candidates:
        if not candidate:
            continue
        java_bin = Path(candidate) / "bin" / ("java.exe" if os.name == "nt" else "java")
        if java_bin.exists() and java_bin.is_file():
            return str(Path(candidate).resolve())

    java_cmd = shutil.which("java")
    if java_cmd:
        resolved = Path(java_cmd).resolve()
        if resolved.name == "java":
            java_home = resolved.parent.parent
            if java_home.exists():
                return str(java_home)

    return None


def run_command(command: List[str], allow_failure: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(command, capture_output=True, text=True, check=False)


def is_package_installed(name: str) -> bool:
    if shutil.which("pkg") is not None:
        result = run_command(["pkg", "list-installed"], allow_failure=True)
        if result.returncode == 0:
            return name in result.stdout.lower()
    if shutil.which("dpkg") is not None:
        result = run_command(["dpkg", "-s", name], allow_failure=True)
        if result.returncode == 0:
            return True
    if shutil.which("apt") is not None:
        result = run_command(["apt", "list", "--installed"], allow_failure=True)
        if result.returncode == 0:
            return name in result.stdout.lower()
    return shutil.which(name) is not None


def ensure_directory(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def install_missing_packages(packages: Iterable[str]) -> List[str]:
    missing = [pkg for pkg in packages if not is_package_installed(pkg)]
    if not missing:
        return []
    pkg_manager = shutil.which("pkg")
    if pkg_manager is None:
        return missing
    result = run_command([pkg_manager, "install", "-y", *missing], allow_failure=True)
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip() or "pkg install failed")
    return missing


def ensure_python_venv(venv_dir: Path) -> Optional[Path]:
    if not venv_dir.exists():
        python_cmd = shutil.which("python3") or shutil.which("python")
        if python_cmd is None:
            return None
        result = run_command([python_cmd, "-m", "venv", str(venv_dir)], allow_failure=True)
        if result.returncode != 0:
            raise RuntimeError(result.stderr.strip() or result.stdout.strip() or "venv creation failed")
    venv_bin = venv_dir / ("Scripts" if os.name == "nt" else "bin")
    python_path = venv_bin / ("python.exe" if os.name == "nt" else "python")
    if not python_path.exists():
        return None
    return python_path


def ensure_ollama() -> Dict[str, object]:
    if shutil.which("ollama") is not None:
        return {"installed": True, "status": "ready"}

    pkg_manager = shutil.which("pkg")
    if pkg_manager is not None:
        result = run_command([pkg_manager, "install", "-y", "ollama"], allow_failure=True)
        if result.returncode == 0:
            return {"installed": True, "status": "installed"}
        return {"installed": False, "status": "unavailable", "error": result.stderr.strip() or result.stdout.strip()}

    return {"installed": False, "status": "unavailable", "error": "ollama not installed and no pkg manager available"}


def default_runtime_dir() -> Path:
    return Path.home() / ".agent"


def ensure_runtime_config(path: Path, workspace: str, ollama_url: str, model: str) -> Dict[str, object]:
    config = {
        **DEFAULT_CONFIG,
        "termux": detect_termux(),
        "workspace": workspace,
        "ollama_url": ollama_url,
        "model": model,
        "runtime_dir": str(default_runtime_dir()),
        "log_dir": str(default_runtime_dir() / "logs"),
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(config, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return config


def check_ollama_port(url: str) -> Dict[str, object]:
    try:
        from urllib.parse import urlparse

        parsed = urlparse(url)
        host = parsed.hostname or "127.0.0.1"
        port = parsed.port or 11434
        with socket.create_connection((host, port), timeout=2):
            pass
        try:
            request = urllib.request.Request(url.rstrip("/") + "/api/tags", method="GET", headers={"Accept": "application/json"})
            with urllib.request.urlopen(request, timeout=5) as response:
                payload = response.read().decode("utf-8", errors="replace")
                return {"reachable": True, "host": host, "port": port, "status": "ready", "response_preview": payload[:200]}
        except (urllib.error.URLError, TimeoutError, ValueError) as exc:
            return {"reachable": True, "host": host, "port": port, "status": "port_open_but_unresponsive", "error": str(exc)}
    except OSError as exc:
        return {"reachable": False, "host": "127.0.0.1", "port": 11434, "status": "down", "error": str(exc)}


def is_proot_available() -> bool:
    return shutil.which("proot") is not None or shutil.which("proot-distro") is not None


def detect_proot_root() -> Optional[str]:
    candidates = [
        "/data/data/com.termux/files/usr/bin/proot-distro",
        "/data/data/com.termux/files/usr/bin/proot",
        "/usr/bin/proot",
        "/usr/local/bin/proot",
    ]
    for candidate in candidates:
        if os.path.exists(candidate):
            return candidate
    return None


def run_host_command(command: str) -> Dict[str, str]:
    result = subprocess.run(command, shell=True, executable="/bin/bash", capture_output=True, text=True, check=False)
    return {
        "returncode": str(result.returncode),
        "stdout": result.stdout.strip(),
        "stderr": result.stderr.strip(),
    }


def build_proot_bootstrap_script(repo_url: str, repo_path: str = "/root/Agent", sdk_root: str = "/opt/android-sdk") -> str:
    java_home = detect_java_home() or "/usr/lib/jvm/java-21-openjdk-amd64"
    script = f"""
set -e
export REPO_PATH={repo_path}
export REPO_URL={repo_url}
export SDK_ROOT={sdk_root}

if ! command -v proot >/dev/null 2>&1 && ! command -v proot-distro >/dev/null 2>&1; then
  pkg update -y
  pkg install -y git wget curl unzip proot proot-distro
fi

if ! proot-distro list 2>/dev/null | grep -qi debian; then
  proot-distro install debian
fi

proot-distro login debian --user root -- bash -lc '
  set -e
  export DEBIAN_FRONTEND=noninteractive
  export JAVA_HOME={java_home}
  export PATH=$JAVA_HOME/bin:$PATH
  export ANDROID_SDK_ROOT={sdk_root}
  export ANDROID_HOME={sdk_root}

  apt-get update
  apt-get install -y --no-install-recommends \
    git wget curl unzip ca-certificates \
    openjdk-21-jdk build-essential cmake \
    python3 python-is-python3 ninja-build

  mkdir -p {sdk_root}
  cd /tmp
  wget -O commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip || \
    curl -L -o commandlinetools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -o commandlinetools.zip -d {sdk_root}
  mkdir -p {sdk_root}/cmdline-tools/latest
  if [ -d {sdk_root}/cmdline-tools ]; then
    find {sdk_root}/cmdline-tools -mindepth 1 -maxdepth 1 -exec mv {{}} {sdk_root}/cmdline-tools/latest/ \\; 2>/dev/null || true
  fi

  yes | {sdk_root}/cmdline-tools/latest/bin/sdkmanager --sdk_root={sdk_root} "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.1.12297006"
  printf '%s\\n%s\\n' "sdk.dir={sdk_root}" "cmake.dir=/usr" > /tmp/android-startup.env
  echo "Startup setup complete. This script prepares the Proot Debian + Android SDK toolchain only; it does not build the project."
  echo "To continue manually, clone or open the repo and run the project-specific build steps from inside the Proot environment."
'
"""
    return script


def apply_termux_proot_android_build(repo_url: str = "https://github.com/XRDOGE-XRPL/Agent.git", repo_path: str = "/root/Agent", sdk_root: str = "/opt/android-sdk", quiet: bool = False) -> Dict[str, object]:
    report: Dict[str, object] = {
        "termux": detect_termux(),
        "proot_available": is_proot_available(),
        "proot_root": detect_proot_root(),
        "repo_url": repo_url,
        "repo_path": repo_path,
        "sdk_root": sdk_root,
        "status": "not_run",
        "warnings": [],
    }

    if not detect_termux():
        report["status"] = "safe-mode"
        report["warnings"].append("Not running in Termux; Proot bootstrap is skipped.")
        return report

    script = build_proot_bootstrap_script(repo_url=repo_url, repo_path=repo_path, sdk_root=sdk_root)
    if not quiet:
        print("[termux-bootstrap] provisioning Proot Debian + Android toolchain...")
    result = run_host_command(script)
    report["command_result"] = result
    report["status"] = "success" if int(result["returncode"]) == 0 else "failed"
    if int(result["returncode"]) != 0:
        report["warnings"].append("Proot bootstrap failed. See stderr output for details.")
    return report


def bootstrap_runtime(
    install_missing: bool = True,
    install_ollama: bool = True,
    workspace: Optional[str] = None,
    ollama_url: str = "http://127.0.0.1:11434",
    model: str = "qwen2.5-coder",
    quiet: bool = False,
) -> Dict[str, object]:
    termux_mode = detect_termux()
    runtime_dir = default_runtime_dir()
    config_path = runtime_dir / "config.json"
    status_path = runtime_dir / "status.json"
    log_dir = runtime_dir / "logs"

    ensure_directory(runtime_dir)
    ensure_directory(log_dir)

    if workspace is None:
        workspace = str(Path.home() / "workspace")

    report: Dict[str, object] = {
        "termux": termux_mode,
        "platform": platform.platform(),
        "arch": detect_architecture(),
        "memory_mb": detect_memory_mb(),
        "workspace": workspace,
        "config_path": str(config_path),
        "status": "ready",
        "package_install_attempted": False,
        "packages_missing": [],
        "warnings": [],
        "health": {"runtime_ready": False, "ollama_ready": False, "python_ready": False, "socket_ready": False},
    }

    if not termux_mode:
        report["status"] = "safe-mode"
        report["warnings"].append("Not running in Termux. Runtime stays in safe mode and only local checks are performed.")
        ensure_runtime_config(config_path, workspace, ollama_url, model)
        report["health"]["runtime_ready"] = True
        report["health"]["python_ready"] = shutil.which("python3") is not None or shutil.which("python") is not None
        report["health"]["ollama_ready"] = False
        report["health"]["socket_ready"] = False
        status_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        return report

    package_list = list(DEFAULT_REQUIRED_PACKAGES)
    report["packages_missing"] = [pkg for pkg in package_list if not is_package_installed(pkg)]

    if install_missing and report["packages_missing"]:
        report["package_install_attempted"] = True
        try:
            installed = install_missing_packages(report["packages_missing"])
            report["packages_missing"] = installed
        except RuntimeError as exc:
            report["status"] = "warning"
            report["warnings"].append(str(exc))

    if install_ollama:
        ollama_status = ensure_ollama()
        report["ollama"] = ollama_status
        if not ollama_status.get("installed", False):
            report["status"] = "warning"
            report["warnings"].append("Ollama could not be installed automatically; falling back to offline-safe checks.")

    venv_dir = runtime_dir / "venv"
    try:
        venv_python = ensure_python_venv(venv_dir)
        report["venv_python"] = str(venv_python) if venv_python else None
    except RuntimeError as exc:
        report["status"] = "warning"
        report["warnings"].append(str(exc))

    config = ensure_runtime_config(config_path, workspace, ollama_url, model)
    report["config"] = config
    report["log_dir"] = str(log_dir)

    venv_python = None
    if runtime_dir.exists():
        venv_path = runtime_dir / "venv" / ("Scripts" if os.name == "nt" else "bin") / ("python.exe" if os.name == "nt" else "python")
        report["health"]["python_ready"] = venv_path.exists() or shutil.which("python3") is not None or shutil.which("python") is not None

    ollama_state = check_ollama_port(ollama_url)
    report["health"]["ollama_ready"] = bool(ollama_state.get("reachable"))
    report["health"]["socket_ready"] = report["health"]["ollama_ready"]
    report["health"]["runtime_ready"] = bool(report["health"]["python_ready"]) and report["health"]["socket_ready"]
    report["ollama_probe"] = ollama_state

    if report["health"]["runtime_ready"]:
        report["status"] = "ready"
    elif report["status"] == "ready":
        report["status"] = "warning"
        report["warnings"].append("Runtime was initialized, but a required health check is still not ready.")

    status_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    if not quiet:
        print(f"[termux-bootstrap] termux={termux_mode} status={report['status']}")
        print(f"[termux-bootstrap] runtime_dir={runtime_dir}")
        print(f"[termux-bootstrap] workspace={workspace}")
        print(f"[termux-bootstrap] health={json.dumps(report['health'], ensure_ascii=False)}")
        if report["warnings"]:
            for warning in report["warnings"]:
                print(f"[termux-bootstrap] warning: {warning}")

    return report


def main() -> int:
    parser = argparse.ArgumentParser(description="Bootstrap a Termux runtime for the autonomous agent.")
    parser.add_argument("--bootstrap", action="store_true", help="check dependencies and auto-install missing packages")
    parser.add_argument("--check", action="store_true", help="run a status check without installing packages")
    parser.add_argument("--workspace", default=str(Path.home() / "workspace"), help="workspace directory used by the agent")
    parser.add_argument("--ollama-url", default="http://127.0.0.1:11434", help="Ollama URL for the local runtime")
    parser.add_argument("--model", default="qwen2.5-coder", help="default model name")
    parser.add_argument("--json", action="store_true", help="print compact JSON report")
    parser.add_argument("--quiet", action="store_true", help="reduce console output")
    parser.add_argument("--healthcheck", action="store_true", help="run the status/health validation without changing the environment")
    parser.add_argument("--native-build", action="store_true", help="run the native Termux build flow directly on Android using JAVA_HOME/ANDROID_HOME and low-resource Gradle settings")
    parser.add_argument("--proot-android-setup", "--proot-android-build", dest="proot_android_build", action="store_true", help="provision the Proot Debian environment and Android SDK toolchain (startup setup only; no project build step)")
    parser.add_argument("--repo-url", default="https://github.com/XRDOGE-XRPL/Agent.git", help="GitHub repository URL to clone into the Proot Debian environment")
    parser.add_argument("--repo-path", default="/root/Agent", help="Target repo path inside the Proot Debian environment")
    parser.add_argument("--sdk-root", default="/opt/android-sdk", help="Android SDK path inside the Proot Debian environment")
    args = parser.parse_args()

    if args.native_build:
        native_script = Path(__file__).resolve().parent.parent / "termux_native_setup.sh"
        if not native_script.exists():
            print(f"[termux-bootstrap] Native setup script not found: {native_script}", file=sys.stderr)
            return 1
        return subprocess.run(["bash", str(native_script)], check=False).returncode

    if args.proot_android_build:
        report = apply_termux_proot_android_build(repo_url=args.repo_url, repo_path=args.repo_path, sdk_root=args.sdk_root, quiet=args.quiet)
        if args.json:
            print(json.dumps(report, ensure_ascii=False))
        return 0

    if args.healthcheck:
        report = bootstrap_runtime(install_missing=False, install_ollama=False, workspace=args.workspace, ollama_url=args.ollama_url, model=args.model, quiet=args.quiet)
        if args.json:
            print(json.dumps(report, ensure_ascii=False))
        return 0

    if args.check and not args.bootstrap:
        report = bootstrap_runtime(install_missing=False, install_ollama=False, workspace=args.workspace, ollama_url=args.ollama_url, model=args.model, quiet=args.quiet)
        if args.json:
            print(json.dumps(report, ensure_ascii=False))
        return 0

    if args.bootstrap:
        report = bootstrap_runtime(install_missing=True, install_ollama=True, workspace=args.workspace, ollama_url=args.ollama_url, model=args.model, quiet=args.quiet)
        if args.json:
            print(json.dumps(report, ensure_ascii=False))
        return 0

    if not any((args.check, args.bootstrap, args.healthcheck, args.proot_android_build)):
        report = bootstrap_runtime(install_missing=True, install_ollama=True, workspace=args.workspace, ollama_url=args.ollama_url, model=args.model, quiet=args.quiet)
        if args.json:
            print(json.dumps(report, ensure_ascii=False))
        return 0

    return 0


if __name__ == "__main__":
    sys.exit(main())
