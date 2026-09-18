#!/usr/bin/env bash
set -eu -o pipefail

REPO_ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_DIR="/werkstatt"
REQUIRED_DIRS=(src logs appbuilder build)
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
LOG_PREFIX="[setup_host]"

log() {
  printf '%s %s\n' "$LOG_PREFIX" "$*"
}

require_command() {
  command -v "$1" >/dev/null 2>&1
}

detect_architecture() {
  local arch
  arch="$(uname -m 2>/dev/null || printf 'unknown')"
  case "$arch" in
    aarch64|arm64)
      printf '%s\n' "arm64"
      ;;
    x86_64|amd64)
      printf '%s\n' "amd64"
      ;;
    *)
      printf '%s\n' "$arch"
      ;;
  esac
}

detect_java_home() {
  local candidates=(
    "${JAVA_HOME:-}"
    "/usr/lib/jvm/java-21-openjdk-amd64"
    "/usr/lib/jvm/java-21-openjdk"
    "/usr/lib/jvm/default-java"
    "/data/data/com.termux/files/usr/lib/jvm/java-17-openjdk"
    "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk"
  )

  for candidate in "${candidates[@]}"; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  if require_command java; then
    local real_java
    real_java="$(readlink -f "$(command -v java)")"
    printf '%s\n' "$(dirname "$(dirname "$real_java")")"
    return 0
  fi

  return 1
}

ensure_system_tools() {
  if require_command apt-get; then
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq
    apt-get install -y --no-install-recommends curl wget unzip ca-certificates git jq openjdk-21-jdk libstdc++6 zlib1g libc6 >/dev/null 2>&1 || true
  elif require_command pkg; then
    pkg update -y >/dev/null 2>&1 || true
    pkg install -y git curl wget unzip openssl ca-certificates openjdk-21 >/dev/null 2>&1 || true
  fi
}

resolve_android_aapt2() {
  local sdk_root="${ANDROID_SDK_ROOT:-/opt/android-sdk}"
  local candidate candidates=()

  if [ -d "$sdk_root/build-tools" ]; then
    while IFS= read -r candidate; do
      candidates+=("$candidate")
    done < <(find "$sdk_root/build-tools" -path '*/aapt2' -type f 2>/dev/null | sort)
  fi

  if [ -x "$sdk_root/build-tools/37.0.0/aapt2" ]; then
    printf '%s\n' "$sdk_root/build-tools/37.0.0/aapt2"
    return 0
  fi

  if [ -x "$sdk_root/build-tools/34.0.0/aapt2" ]; then
    printf '%s\n' "$sdk_root/build-tools/34.0.0/aapt2"
    return 0
  fi

  for candidate in "${candidates[@]}"; do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

ensure_java() {
  local java_home
  java_home="$(detect_java_home || true)"

  if [ -n "$java_home" ] && [ -x "$java_home/bin/java" ]; then
    export JAVA_HOME="$java_home"
    export PATH="$JAVA_HOME/bin:$PATH"
    return 0
  fi

  log "No usable JDK found. Install JDK 21 and rerun the setup script."
  return 1
}

ensure_workspace() {
  mkdir -p "$WORKSPACE_DIR"
  for dir in "${REQUIRED_DIRS[@]}"; do
    mkdir -p "$WORKSPACE_DIR/$dir"
  done

  mkdir -p "$WORKSPACE_DIR/logs"
  touch "$WORKSPACE_DIR/logs/agent.log" "$WORKSPACE_DIR/logs/terminal_exec.log"
}

write_json_file() {
  local path="$1"
  local json_text="$2"
  printf '%s\n' "$json_text" > "$path"

  python3 - "$path" <<'PY'
import json, sys
with open(sys.argv[1], 'r', encoding='utf-8') as fh:
    json.load(fh)
PY
}

ensure_metadata() {
  local timestamp
  timestamp="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

  local manifest_json state_json memory_json
  manifest_json=$(cat <<EOF
{
  "name": "Autonomer Entwicklungsagent",
  "workspace": "$WORKSPACE_DIR",
  "requiredDirectories": ["src", "logs", "appbuilder", "build"],
  "status": "initialized",
  "lastUpdated": "$timestamp",
  "agent": {
    "provider": "local",
    "model": "qwen2.5-coder",
    "iterations": 8,
    "ttlMinutes": 30
  }
}
EOF
)

  state_json=$(cat <<EOF
{
  "workspace": "$WORKSPACE_DIR",
  "agentStatus": "ready",
  "lastUpdated": "$timestamp",
  "provider": "local",
  "model": "qwen2.5-coder",
  "iterations": 8,
  "ttlMinutes": 30,
  "buildStatus": "ready",
  "lastTerminalCommand": null,
  "lastTerminalExit": null,
  "lastTerminalOutput": null,
  "memoryEntries": 0
}
EOF
)

  memory_json=$(cat <<EOF
{
  "workspace": "$WORKSPACE_DIR",
  "status": "ready",
  "provider": "local",
  "model": "qwen2.5-coder",
  "iterations": 8,
  "ttlMinutes": 30,
  "entries": {},
  "updatedAt": "$timestamp"
}
EOF
)

  if [ ! -f "$WORKSPACE_DIR/manifest.json" ] || ! python3 - "$WORKSPACE_DIR/manifest.json" >/dev/null 2>&1 <<'PY'
import json, sys
try:
    with open(sys.argv[1], 'r', encoding='utf-8') as fh:
        json.load(fh)
    raise SystemExit(0)
except Exception:
    raise SystemExit(1)
PY
  then
    write_json_file "$WORKSPACE_DIR/manifest.json" "$manifest_json"
  fi

  if [ ! -f "$WORKSPACE_DIR/state.json" ] || ! python3 - "$WORKSPACE_DIR/state.json" >/dev/null 2>&1 <<'PY'
import json, sys
try:
    with open(sys.argv[1], 'r', encoding='utf-8') as fh:
        json.load(fh)
    raise SystemExit(0)
except Exception:
    raise SystemExit(1)
PY
  then
    write_json_file "$WORKSPACE_DIR/state.json" "$state_json"
  fi

  if [ ! -f "$WORKSPACE_DIR/memory.json" ] || ! python3 - "$WORKSPACE_DIR/memory.json" >/dev/null 2>&1 <<'PY'
import json, sys
try:
    with open(sys.argv[1], 'r', encoding='utf-8') as fh:
        json.load(fh)
    raise SystemExit(0)
except Exception:
    raise SystemExit(1)
PY
  then
    write_json_file "$WORKSPACE_DIR/memory.json" "$memory_json"
  fi
}

ensure_gradle_hardening() {
  local props_file="$REPO_ROOT/gradle.properties"
  local local_props="$REPO_ROOT/local.properties"
  local aapt2_override
  local arch

  mkdir -p "$REPO_ROOT"
  arch="$(detect_architecture)"
  aapt2_override="$(resolve_android_aapt2 || true)"

  if [ ! -f "$props_file" ]; then
    touch "$props_file"
  fi

  if grep -q '^org.gradle.daemon=' "$props_file"; then
    sed -i 's|^org.gradle.daemon=.*|org.gradle.daemon=false|' "$props_file"
  else
    printf '\norg.gradle.daemon=false\n' >> "$props_file"
  fi

  if grep -q '^org.gradle.parallel=' "$props_file"; then
    sed -i 's|^org.gradle.parallel=.*|org.gradle.parallel=false|' "$props_file"
  else
    printf 'org.gradle.parallel=false\n' >> "$props_file"
  fi

  if grep -q '^org.gradle.workers.max=' "$props_file"; then
    sed -i 's|^org.gradle.workers.max=.*|org.gradle.workers.max=1|' "$props_file"
  else
    printf 'org.gradle.workers.max=1\n' >> "$props_file"
  fi

  if grep -q '^org.gradle.jvmargs=' "$props_file"; then
    sed -i 's|^org.gradle.jvmargs=.*|org.gradle.jvmargs=-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false|' "$props_file"
  else
    printf 'org.gradle.jvmargs=-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false\n' >> "$props_file"
  fi

  if grep -q '^android.aapt2.daemon.enabled=' "$props_file"; then
    sed -i 's|^android.aapt2.daemon.enabled=.*|android.aapt2.daemon.enabled=false|' "$props_file"
  else
    printf '\nandroid.aapt2.daemon.enabled=false\n' >> "$props_file"
  fi

  if [ -n "$aapt2_override" ] && [ -x "$aapt2_override" ] && [ "$arch" != "amd64" ]; then
    if grep -q '^android.aapt2FromMavenOverride=' "$props_file"; then
      sed -i "s|^android.aapt2FromMavenOverride=.*|android.aapt2FromMavenOverride=$aapt2_override|" "$props_file"
    else
      printf "android.aapt2FromMavenOverride=%s\n" "$aapt2_override" >> "$props_file"
    fi
  else
    if grep -q '^android.aapt2FromMavenOverride=' "$props_file"; then
      sed -i '/^android.aapt2FromMavenOverride=/d' "$props_file"
    fi
    log "No valid local aapt2 binary found for ${arch}; Maven AAPT2 fallback unset to avoid broken overrides."
  fi

  if ! grep -q '^android.useAndroidX=' "$props_file"; then
    printf 'android.useAndroidX=true\n' >> "$props_file"
  fi

  if ! grep -q '^android.enableJetifier=' "$props_file"; then
    printf 'android.enableJetifier=true\n' >> "$props_file"
  fi

  if ! grep -q '^android.nonTransitiveRClass=' "$props_file"; then
    printf 'android.nonTransitiveRClass=true\n' >> "$props_file"
  fi

  if [ -f "$local_props" ]; then
    sed -i 's|^sdk.dir=.*|sdk.dir=/opt/android-sdk|' "$local_props"
    sed -i 's|^cmake.dir=.*|cmake.dir=/usr|' "$local_props"
  else
    printf 'sdk.dir=/opt/android-sdk\ncmake.dir=/usr\n' > "$local_props"
  fi
}

ensure_android_sdk() {
  mkdir -p "$ANDROID_SDK_ROOT"

  if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    log "Android SDK tools already available at $ANDROID_SDK_ROOT"
    return 0
  fi

  if ! require_command curl && ! require_command wget; then
    log "curl/wget missing. Cannot fetch Android command line tools."
    return 1
  fi

  local archive_path="/tmp/cmdline-tools.zip"
  if require_command curl; then
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$archive_path" || \
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip" -o "$archive_path"
  else
    wget -O "$archive_path" "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" || \
    wget -O "$archive_path" "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
  fi

  mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
  unzip -o "$archive_path" -d "$ANDROID_SDK_ROOT/cmdline-tools" >/dev/null

  if [ -d "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" ]; then
    mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  elif [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools/latest"
  fi

  if [ -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    yes | "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.1.12297006" >/dev/null
  fi

  export ANDROID_HOME="$ANDROID_SDK_ROOT"
  export ANDROID_SDK_ROOT="$ANDROID_SDK_ROOT"
}

fix_permissions() {
  chmod +x "$REPO_ROOT/gradlew"
  chmod +x "$REPO_ROOT/build_apk.sh"
  find "$REPO_ROOT" -type f -name '*.sh' -exec chmod +x {} +
}

main() {
  ensure_system_tools
  ensure_java
  ensure_workspace
  ensure_gradle_hardening
  ensure_metadata
  ensure_android_sdk || true
  fix_permissions

  export ANDROID_HOME="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
  export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT}"
  export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools"

  local aapt2_override
  local arch
  arch="$(detect_architecture)"
  aapt2_override="$(resolve_android_aapt2 || true)"

  log "Host architecture: $arch"
  log "Host workspace initialized at $WORKSPACE_DIR"
  log "Required directories: ${REQUIRED_DIRS[*]}"
  log "JAVA_HOME=$JAVA_HOME"
  log "ANDROID_HOME=$ANDROID_HOME"
  if [ -n "$aapt2_override" ] && [ -x "$aapt2_override" ]; then
    log "Gradle AAPT2 hardening applied for $aapt2_override"
  else
    log "Gradle AAPT2 hardening active, but no valid local aapt2 override was found."
  fi
  log "Gradle and build scripts marked executable."
  log "Next step: cd $REPO_ROOT && ./gradlew clean assembleDebug --no-daemon"
}

main "$@"
