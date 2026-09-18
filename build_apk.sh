#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(CDPATH= cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
HOST_ARCH="$(uname -m 2>/dev/null || printf 'unknown')"

resolve_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    printf '%s\n' "$JAVA_HOME"
    return 0
  fi

  for candidate in \
    /usr/lib/jvm/java-21-openjdk-amd64 \
    /usr/lib/jvm/java-21-openjdk-arm64 \
    /usr/lib/jvm/java-21-openjdk \
    /usr/lib/jvm/java-17-openjdk-amd64 \
    /usr/lib/jvm/java-17-openjdk-arm64 \
    /usr/lib/jvm/temurin-21-jdk-amd64 \
    /usr/lib/jvm/temurin-21-jdk-arm64 \
    /usr/lib/jvm/temurin-17-jdk-amd64 \
    /usr/lib/jvm/temurin-21-jdk \
    /usr/lib/jvm/temurin-17-jdk \
    /usr/lib/jvm/default-java \
    /usr/lib/jvm/default; do
    if [ -n "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  if command -v java >/dev/null 2>&1; then
    local java_bin
    java_bin=$(command -v java)
    local resolved
    resolved=$(readlink -f "$java_bin" 2>/dev/null || true)
    if [ -n "$resolved" ] && [ -x "${resolved%/bin/java}/bin/java" ]; then
      printf '%s\n' "${resolved%/bin/java}"
      return 0
    fi
  fi

  return 1
}

resolve_android_home() {
  if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
    printf '%s\n' "$ANDROID_HOME"
    return 0
  fi

  for candidate in \
    /opt/android-sdk \
    /usr/local/lib/android/sdk \
    /usr/lib/android-sdk \
    /usr/local/share/android-sdk; do
    if [ -d "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  return 1
}

resolve_android_aapt2() {
  local sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/opt/android-sdk}}"
  local candidate

  if [ -d "$sdk_root/build-tools" ]; then
    while IFS= read -r candidate; do
      if [ -n "$candidate" ] && [ -x "$candidate" ]; then
        printf '%s\n' "$candidate"
        return 0
      fi
    done < <(find "$sdk_root/build-tools" -type f -name 'aapt2' 2>/dev/null | sort)
  fi

  if [ -x "$sdk_root/build-tools/37.0.0/aapt2" ]; then
    printf '%s\n' "$sdk_root/build-tools/37.0.0/aapt2"
    return 0
  fi

  if [ -x "$sdk_root/build-tools/34.0.0/aapt2" ]; then
    printf '%s\n' "$sdk_root/build-tools/34.0.0/aapt2"
    return 0
  fi

  return 1
}

sanitize_gradle_properties() {
  local props_file="$REPO_ROOT/gradle.properties"
  local aapt2_override

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

  aapt2_override="$(resolve_android_aapt2 || true)"

  if [ -n "$aapt2_override" ] && [ -x "$aapt2_override" ] && [ "$HOST_ARCH" != "x86_64" ] && [ "$HOST_ARCH" != "amd64" ]; then
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

ensure_host_runtime_libraries() {
  if command -v apt-get >/dev/null 2>&1; then
    export DEBIAN_FRONTEND=noninteractive
    apt-get update -qq
    apt-get install -y --no-install-recommends libstdc++6 zlib1g libc6 >/dev/null 2>&1 || true
  elif command -v pkg >/dev/null 2>&1; then
    pkg install -y libstdc++ zlib >/dev/null 2>&1 || true
  fi
}

if [ -n "${PREFIX:-}" ] && echo "$PREFIX" | grep -qi 'termux'; then
  if [ -x "$REPO_ROOT/termux_native_setup.sh" ]; then
    echo "Termux native build path selected; running native Android toolchain setup..." >&2
    exec "$REPO_ROOT/termux_native_setup.sh" "$@"
  fi
  echo "Android builds on Termux must use the native setup path. Run ./termux_native_setup.sh" >&2
  exit 1
fi

ensure_host_runtime_libraries

ANDROID_HOME=$(resolve_android_home || true)
if [ -z "$ANDROID_HOME" ]; then
  echo "ANDROID_HOME not found. Set ANDROID_HOME or install the Android SDK under /opt/android-sdk or /usr/local/lib/android/sdk." >&2
  exit 1
fi
export ANDROID_HOME
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-${ANDROID_HOME}}
export PATH="/usr/bin:${PATH}"
export CMAKE_COMMAND=${CMAKE_COMMAND:-/usr/bin/cmake}

JAVA_HOME=$(resolve_java_home || true)
if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is not set and no supported JDK was found. Install OpenJDK 17/21 and rerun the Android build." >&2
  echo "Expected locations include /usr/lib/jvm/java-21-openjdk-amd64, /usr/lib/jvm/java-21-openjdk-arm64, /usr/lib/jvm/temurin-21-jdk-amd64 or /usr/lib/jvm/default-java." >&2
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

cd "$REPO_ROOT"

if [ "${HOST_ARCH}" = "aarch64" ] || [ "${HOST_ARCH}" = "arm64" ]; then
  export ANDROID_BUILD_ABI="arm64-v8a"
fi

sanitize_gradle_properties

if ! command -v cmake >/dev/null 2>&1; then
  if command -v apt-get >/dev/null 2>&1; then
    export DEBIAN_FRONTEND=noninteractive
    apt-get update
    apt-get install -y cmake
  else
    echo "System CMake is required inside Proot Debian. Install cmake before running the Android build." >&2
    exit 1
  fi
fi

if [ -f local.properties ]; then
  if grep -q '^sdk.dir=' local.properties; then
    sed -i "s#^sdk.dir=.*#sdk.dir=${ANDROID_HOME}#" local.properties
  else
    printf 'sdk.dir=%s\n' "${ANDROID_HOME}" >> local.properties
  fi
  if grep -q '^cmake.dir=' local.properties; then
    sed -i "s#^cmake.dir=.*#cmake.dir=/usr#" local.properties
  else
    printf 'cmake.dir=/usr\n' >> local.properties
  fi
else
  printf 'sdk.dir=%s\ncmake.dir=/usr\n' "${ANDROID_HOME}" > local.properties
fi

if [ -d "$ANDROID_HOME/cmdline-tools/latest/bin" ]; then
  yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses || true
  "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-34" "build-tools;34.0.0" "ndk;27.1.12297006" || true
fi

./gradlew --stop || true
./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1 -Dorg.gradle.daemon=false -Dorg.gradle.parallel=false -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'
