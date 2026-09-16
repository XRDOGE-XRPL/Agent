#!/usr/bin/env python3
"""Lokaler Agentenlauf für Termux, Windows und Linux (Ollama + Dateisystem + Git)."""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
from pathlib import Path

try:
    from termux_bootstrap import bootstrap_runtime
except ImportError:  # pragma: no cover
    bootstrap_runtime = None

SYSTEM_PROMPT = """Du bist ein autonomer Entwicklungsagent. Antworte AUSSCHLIESSLICH mit JSON.
Schema:
{"schritte":[{"aktion":"schreiben|loeschen|lesen|bauen|testen|git_status|fertig|abbrechen",
"pfad":"relativer/pfad","inhalt":"dateiinhalt","begruendung":"kurz"}]}
Nur relative Pfade, keine Platzhalter, Begründungen auf Deutsch.
"""


def ollama_anfragen(url: str, modell: str, prompt: str) -> str:
    koerper = json.dumps({"model": modell, "prompt": prompt, "stream": False}).encode("utf-8")
    anfrage = urllib.request.Request(
        url.rstrip("/") + "/api/generate",
        data=koerper,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(anfrage, timeout=120) as antwort:
        daten = json.loads(antwort.read().decode("utf-8"))
    return str(daten.get("response", ""))


def ausfuehren(verzeichnis: Path, kommando: str) -> tuple[int, str]:
    fertig = subprocess.run(
        kommando,
        cwd=str(verzeichnis),
        shell=True,
        capture_output=True,
        text=True,
    )
    return fertig.returncode, (fertig.stdout or "") + (fertig.stderr or "")


def pfad_sicher(wurzel: Path, ziel: Path) -> bool:
    try:
        if any(teil == ".." for teil in Path(ziel).parts):
            return False
        basis = wurzel.resolve()
        kandidat = ziel.resolve(strict=False) if ziel.is_absolute() else (basis / ziel).resolve(strict=False)
        relativ = kandidat.relative_to(basis)
        return all(teil != ".." for teil in relativ.parts)
    except (ValueError, RuntimeError):
        return False


def bauen(verzeichnis: Path) -> tuple[int, str]:
    if (verzeichnis / "CMakeLists.txt").exists():
        build = verzeichnis / "build-lokal"
        build.mkdir(exist_ok=True)
        code, text = ausfuehren(verzeichnis, f'cmake -S "{verzeichnis}" -B "{build}"')
        if code != 0:
            return code, text
        code, text2 = ausfuehren(build, "cmake --build .")
        text += text2
        if code != 0:
            return code, text
        code, text3 = ausfuehren(build, "ctest --output-on-failure")
        return code, text + text3
    if (verzeichnis / "build.gradle.kts").exists() or (verzeichnis / "build.gradle").exists():
        wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
        if not (verzeichnis / ("gradlew.bat" if os.name == "nt" else "gradlew")).exists():
            wrapper = "gradle"
        return ausfuehren(verzeichnis, f"{wrapper} test --no-daemon")
    return 1, "Kein CMake- oder Gradle-Projekt gefunden."


def schleife(args: argparse.Namespace) -> int:
    wurzel = Path(args.arbeitsverzeichnis).resolve()
    wurzel.mkdir(parents=True, exist_ok=True)
    letzte_fehler = ""
    letzter_ok = False
    for iteration in range(1, args.max_iterationen + 1):
        print(f"[INFO] Iteration {iteration}/{args.max_iterationen}", file=sys.stderr)
        dateien = []
        for pfad in wurzel.rglob("*"):
            if any(teil in {".git", "build", "build-lokal", ".gradle"} for teil in pfad.parts):
                continue
            if pfad.is_file():
                dateien.append(str(pfad.relative_to(wurzel)))
        prompt = (
            f"{SYSTEM_PROMPT}\nIteration: {iteration}\nAufgabe: {args.aufgabe}\n"
            f"Dateien: {dateien[:200]}\nLetzte Fehler:\n{letzte_fehler}\n"
        )
        if args.offline:
            antwort = '{"schritte":[{"aktion":"git_status","begruendung":"Offline-Modus: keine Ollama-Anfrage; lokale Prüfung wird verwendet."}]}'
        else:
            try:
                antwort = ollama_anfragen(args.ollama_url, args.modell, prompt)
            except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as ex:
                print(f"[FEHLER] Ollama: {ex}", file=sys.stderr)
                return 2
        start = antwort.find("{")
        ende = antwort.rfind("}")
        if start < 0 or ende <= start:
            letzte_fehler = "Kein JSON in der Antwort"
            continue
        try:
            objekt = json.loads(antwort[start : ende + 1])
        except json.JSONDecodeError:
            letzte_fehler = "JSON ungültig"
            continue
        schritte = objekt.get("schritte") or [objekt]
        fertig = False
        for schritt in schritte:
            aktion = schritt.get("aktion", "")
            rel = schritt.get("pfad", "")
            print(f"[INFO] Aktion {aktion} {rel}", file=sys.stderr)
            if aktion == "schreiben":
                ziel = (wurzel / rel).resolve()
                if not pfad_sicher(wurzel, ziel):
                    letzte_fehler = f"Unsicherer Pfad: {rel}"
                    continue
                ziel.parent.mkdir(parents=True, exist_ok=True)
                ziel.write_text(schritt.get("inhalt", ""), encoding="utf-8")
            elif aktion == "loeschen":
                ziel = (wurzel / rel).resolve()
                if pfad_sicher(wurzel, ziel) and ziel.exists():
                    if ziel.is_dir():
                        os.rmdir(ziel)
                    else:
                        ziel.unlink()
            elif aktion in {"bauen", "testen"}:
                code, text = bauen(wurzel)
                letzter_ok = code == 0
                letzte_fehler = "" if letzter_ok else text
                print(text)
            elif aktion == "git_status":
                _, text = ausfuehren(wurzel, "git status --porcelain=v1 -uall")
                print(text)
            elif aktion == "fertig":
                fertig = True
        if fertig and letzter_ok:
            print("ERFOLG:", schritt.get("begruendung", "fertig"))
            return 0
    print("FEHLGESCHLAGEN: Maximale Iterationen erreicht", file=sys.stderr)
    return 2


def main() -> int:
    parser = argparse.ArgumentParser(description="Lokaler Agentenlauf (Termux/Ollama)")
    parser.add_argument("--arbeitsverzeichnis", required=True)
    parser.add_argument("--aufgabe", required=True)
    parser.add_argument("--ollama-url", default="http://127.0.0.1:11434")
    parser.add_argument("--modell", default="llama3.2")
    parser.add_argument("--max-iterationen", type=int, default=8)
    parser.add_argument("--offline", action="store_true", help="skip Ollama requests and operate in local-safe offline mode")
    parser.add_argument("--bootstrap", action="store_true", help="run Termux bootstrap before the agent starts")
    args = parser.parse_args()
    if bootstrap_runtime is not None and (args.bootstrap or "TERMUX_VERSION" in os.environ or "com.termux" in os.environ.get("PREFIX", "")):
        try:
            bootstrap_runtime(install_missing=True, install_ollama=True, workspace=args.arbeitsverzeichnis, ollama_url=args.ollama_url, model=args.modell, quiet=True)
        except Exception as ex:
            print(f"[WARN] Bootstrap konnte nicht vollständig ausgeführt werden: {ex}", file=sys.stderr)
    return schleife(args)


if __name__ == "__main__":
    sys.exit(main())
