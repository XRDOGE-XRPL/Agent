#!/usr/bin/env bash
set -eu

REPO_ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_DIR="/werkstatt"
REQUIRED_DIRS=(src logs appbuilder build)

log() {
  printf '%s\n' "$*"
}

ensure_workspace() {
  mkdir -p "$WORKSPACE_DIR"
  for dir in "${REQUIRED_DIRS[@]}"; do
    mkdir -p "$WORKSPACE_DIR/$dir"
  done

  mkdir -p "$WORKSPACE_DIR/logs"
  touch "$WORKSPACE_DIR/logs/agent.log" "$WORKSPACE_DIR/logs/terminal_exec.log"
}

ensure_gradle_hardening() {
  local props_file="$REPO_ROOT/gradle.properties"
  local init_file="$HOME/.gradle/init.gradle"

  if [ -f "$props_file" ]; then
    if grep -q '^android.aapt2.daemon.enabled=' "$props_file"; then
      sed -i 's|^android.aapt2.daemon.enabled=.*|android.aapt2.daemon.enabled=false|' "$props_file"
    else
      printf '\nandroid.aapt2.daemon.enabled=false\n' >> "$props_file"
    fi

    if grep -q '^android.aapt2FromMavenOverride=' "$props_file"; then
      sed -i 's|^android.aapt2FromMavenOverride=.*|android.aapt2FromMavenOverride=/opt/android-sdk/build-tools/34.0.0/aapt2|' "$props_file"
    else
      printf 'android.aapt2FromMavenOverride=/opt/android-sdk/build-tools/34.0.0/aapt2\n' >> "$props_file"
    fi
  else
    cat > "$props_file" <<'EOF'
org.gradle.jvmargs=-Xmx2g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
org.gradle.parallel=false
org.gradle.caching=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
android.defaults.buildfeatures.resvalues=true
android.aapt2.daemon.enabled=false
android.aapt2FromMavenOverride=/opt/android-sdk/build-tools/34.0.0/aapt2
EOF
  fi

  if [ -f "$init_file" ]; then
    sed -i '/AarResourcesCompilerTransform/d' "$init_file" 2>/dev/null || true
  fi

  local local_props="$REPO_ROOT/local.properties"
  mkdir -p "$(dirname "$local_props")"
  if [ ! -f "$local_props" ]; then
    printf 'sdk.dir=/opt/android-sdk\ncmake.dir=/usr\n' > "$local_props"
  else
    if ! grep -q '^sdk.dir=' "$local_props"; then
      printf 'sdk.dir=/opt/android-sdk\n' >> "$local_props"
    else
      sed -i 's|^sdk.dir=.*|sdk.dir=/opt/android-sdk|' "$local_props"
    fi
    if ! grep -q '^cmake.dir=' "$local_props"; then
      printf 'cmake.dir=/usr\n' >> "$local_props"
    else
      sed -i 's|^cmake.dir=.*|cmake.dir=/usr|' "$local_props"
    fi
  fi
}

write_json_file() {
  local path="$1"
  local json_text="$2"
  printf '%s\n' "$json_text" > "$path"

  python3 - "$path" <<'PY'
import json, sys
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as fh:
    json.load(fh)
PY
}

ensure_metadata() {
  local timestamp
  timestamp="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

  local manifest_json
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

  local state_json
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

  local memory_json
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

fix_permissions() {
  chmod +x "$REPO_ROOT/gradlew"
  chmod +x "$REPO_ROOT/build_apk.sh"
  find "$REPO_ROOT" -type f -name '*.sh' -exec chmod +x {} +
}

main() {
  ensure_workspace
  ensure_gradle_hardening
  ensure_metadata
  fix_permissions

  log "Host workspace initialized at $WORKSPACE_DIR"
  log "Required directories: ${REQUIRED_DIRS[*]}"
  log "Gradle AAPT2 hardening applied for /opt/android-sdk/build-tools/34.0.0/aapt2"
  log "Gradle and build scripts marked executable."
  log "Next step: cd $REPO_ROOT && ./gradlew clean assembleDebug --no-daemon"
}

main "$@"
