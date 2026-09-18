#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TERMUX_HOME="${HOME:-/data/data/com.termux/files/home}"
ANDROID_HOME="${ANDROID_HOME:-${TERMUX_HOME}/Android/Sdk}"
TERMUX_PREFIX="${PREFIX:-}"
ARCH="$(uname -m 2>/dev/null || printf 'unknown')"

log() {
  printf '%s\n' "$*"
}

require_command() {
  command -v "$1" >/dev/null 2>&1
}

detect_java_home() {
  for candidate in \
    "${JAVA_HOME:-}" \
    /data/data/com.termux/files/usr/lib/jvm/java-21-openjdk \
    /data/data/com.termux/files/usr/lib/jvm/java-17-openjdk \
    /usr/lib/jvm/java-21-openjdk \
    /usr/lib/jvm/java-21-openjdk-arm64 \
    /usr/lib/jvm/java-17-openjdk \
    /usr/lib/jvm/default-java; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  if require_command java; then
    local resolved
    resolved="$(readlink -f "$(command -v java)" 2>/dev/null || true)"
    if [ -n "$resolved" ] && [ -x "${resolved%/bin/java}/bin/java" ]; then
      printf '%s\n' "${resolved%/bin/java}"
      return 0
    fi
  fi
  return 1
}

install_termux_packages() {
  if ! require_command pkg; then
    log "Termux pkg tool not found. Native Termux build is unavailable here."
    return 1
  fi

  pkg update -y >/dev/null 2>&1 || true
  pkg install -y \
    git curl wget unzip ca-certificates \
    openjdk-21 cmake ninja make clang \
    libstdc++ zlib >/dev/null 2>&1 || true
}

ensure_sdkmanager() {
  local sdk_root="${ANDROID_HOME}"
  local cmd_tools="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
  local archive

  if [ -x "$cmd_tools" ]; then
    return 0
  fi

  mkdir -p "$sdk_root"
  archive="/tmp/cmdline-tools.zip"
  if require_command curl; then
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$archive" || \
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip" -o "$archive" || \
    return 1
  elif require_command wget; then
    wget -O "$archive" "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" || \
    wget -O "$archive" "https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip" || \
    return 1
  else
    return 1
  fi

  mkdir -p "$sdk_root/cmdline-tools"
  unzip -o "$archive" -d "$sdk_root/cmdline-tools" >/dev/null 2>&1 || true
  if [ -d "$sdk_root/cmdline-tools/cmdline-tools" ]; then
    rm -rf "$sdk_root/cmdline-tools/latest"
    mv "$sdk_root/cmdline-tools/cmdline-tools" "$sdk_root/cmdline-tools/latest"
  elif [ ! -d "$sdk_root/cmdline-tools/latest" ]; then
    mkdir -p "$sdk_root/cmdline-tools/latest"
  fi

  if [ -x "$cmd_tools" ]; then
    return 0
  fi

  return 1
}

ensure_host_env() {
  local java_home
  java_home="$(detect_java_home || true)"
  if [ -z "$java_home" ]; then
    log "No usable JDK found. Install openjdk-21 and rerun this script."
    exit 1
  fi

  export JAVA_HOME="$java_home"
  export PATH="$JAVA_HOME/bin:$PATH"
  export ANDROID_HOME
  export ANDROID_SDK_ROOT="${ANDROID_HOME}"
  export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
}

guard_against_bad_toolchain_architecture() {
  case "$ARCH" in
    aarch64|arm64)
      local ndk_root="${ANDROID_HOME}/ndk"
      if [ -d "$ndk_root" ]; then
        while IFS= read -r mismatch; do
          if [ -n "$mismatch" ]; then
            echo "Architektur-Mismatch erkannt: Android NDK verwendet im Host-Setup \"$mismatch\" (linux-x86_64) auf einem ARM64-System." >&2
            echo "Das führt bei clang++ zu 'Illegal instruction'. Setze das Host-NDK auf das native ARM64-Paket oder verwende den System-Compiler explizit." >&2
            exit 1
          fi
        done < <(find "$ndk_root" -path '*/toolchains/llvm/prebuilt/linux-x86_64' -type d 2>/dev/null)
      fi

      if [ -n "${CC:-}" ] && printf '%s' "$CC" | grep -Eqi 'qemu-bin|linux-x86_64'; then
        echo "Stale CC path detected: $CC" >&2
        unset CC
      fi
      if [ -n "${CXX:-}" ] && printf '%s' "$CXX" | grep -Eqi 'qemu-bin|linux-x86_64'; then
        echo "Stale CXX path detected: $CXX" >&2
        unset CXX
      fi
      ;;
  esac
}

ensure_local_properties() {
  local props_file="$REPO_ROOT/local.properties"
  if [ -f "$props_file" ]; then
    if grep -q '^sdk.dir=' "$props_file"; then
      sed -i "s#^sdk.dir=.*#sdk.dir=${ANDROID_HOME}#" "$props_file"
    else
      printf 'sdk.dir=%s\n' "$ANDROID_HOME" >> "$props_file"
    fi
    if grep -q '^cmake.dir=' "$props_file"; then
      sed -i 's#^cmake.dir=.*#cmake.dir=/usr#' "$props_file"
    else
      printf 'cmake.dir=/usr\n' >> "$props_file"
    fi
  else
    printf 'sdk.dir=%s\ncmake.dir=/usr\n' "$ANDROID_HOME" > "$props_file"
  fi
}

ensure_gradle_hardening() {
  local props_file="$REPO_ROOT/gradle.properties"
  local aapt2_override=""

  if [ -x "$ANDROID_HOME/build-tools/37.0.0/aapt2" ]; then
    aapt2_override="$ANDROID_HOME/build-tools/37.0.0/aapt2"
  elif [ -x "$ANDROID_HOME/build-tools/34.0.0/aapt2" ]; then
    aapt2_override="$ANDROID_HOME/build-tools/34.0.0/aapt2"
  fi

  touch "$props_file"

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

  if [ -n "$aapt2_override" ] && [ -x "$aapt2_override" ] && [ "$ARCH" != "x86_64" ] && [ "$ARCH" != "amd64" ]; then
    if grep -q '^android.aapt2FromMavenOverride=' "$props_file"; then
      sed -i "s|^android.aapt2FromMavenOverride=.*|android.aapt2FromMavenOverride=$aapt2_override|" "$props_file"
    else
      printf 'android.aapt2FromMavenOverride=%s\n' "$aapt2_override" >> "$props_file"
    fi
  else
    if grep -q '^android.aapt2FromMavenOverride=' "$props_file"; then
      sed -i '/^android.aapt2FromMavenOverride=/d' "$props_file"
    fi
  fi
}

install_android_sdk_components() {
  local sdk_root="${ANDROID_HOME}"
  local sdkmanager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"

  if [ ! -x "$sdkmanager" ]; then
    return 0
  fi

  yes | "$sdkmanager" --sdk_root="$sdk_root" "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.1.12297006" >/dev/null 2>&1 || true
}

persist_termux_exports() {
  local export_block
  export_block=$(cat <<EOF

# Native Termux Android build environment for Agent
export JAVA_HOME="${JAVA_HOME:-${TERMUX_HOME}/usr/lib/jvm/java-21-openjdk}"
export ANDROID_HOME="${ANDROID_HOME}"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="\$JAVA_HOME/bin:\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"
EOF
)

  if [ -f "$HOME/.bashrc" ] && ! grep -q 'Native Termux Android build environment for Agent' "$HOME/.bashrc"; then
    printf '%s\n' "$export_block" >> "$HOME/.bashrc"
  fi
  if [ -f "$HOME/.zshrc" ] && ! grep -q 'Native Termux Android build environment for Agent' "$HOME/.zshrc"; then
    printf '%s\n' "$export_block" >> "$HOME/.zshrc"
  fi
}

main() {
  if [ -z "${TERMUX_PREFIX:-}" ] || ! echo "$TERMUX_PREFIX" | grep -qi 'termux'; then
    log "This native build flow is intended for Termux. Use the Proot-Debian flow elsewhere."
    exit 1
  fi

  install_termux_packages || true
  ensure_host_env
  guard_against_bad_toolchain_architecture
  ensure_sdkmanager || true
  ensure_local_properties
  ensure_gradle_hardening
  install_android_sdk_components || true
  persist_termux_exports

  cd "$REPO_ROOT"
  chmod +x "$REPO_ROOT/gradlew" "$REPO_ROOT/build_apk.sh" 2>/dev/null || true

  log "Native Termux build environment ready."
  log "JAVA_HOME=$JAVA_HOME"
  log "ANDROID_HOME=$ANDROID_HOME"
  log "Build command: cd $REPO_ROOT && ./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1 -Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'"

  exec ./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1 \
    -Dorg.gradle.daemon=false \
    -Dorg.gradle.parallel=false \
    -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'
}

main "$@"
