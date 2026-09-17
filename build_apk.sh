#!/usr/bin/env bash
set -e

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
    /usr/lib/jvm/temurin-17-jdk-amd64 \
    /usr/lib/jvm/temurin-21-jdk \
    /usr/lib/jvm/temurin-17-jdk \
    /usr/lib/jvm/default-java \
    /usr/lib/jvm/default; do
    if [ -x "$candidate/bin/java" ]; then
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

if [ -n "${PREFIX:-}" ] && echo "$PREFIX" | grep -qi 'termux'; then
  echo "Android builds MUST NOT run directly on the Termux host. Use the Proot Debian bootstrap instead:" >&2
  echo "  python laufwerk/termux_bootstrap.py --bootstrap" >&2
  exit 1
fi

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
  echo "Expected locations include /usr/lib/jvm/java-21-openjdk-amd64, /usr/lib/jvm/temurin-21-jdk-amd64 or /usr/lib/jvm/default-java." >&2
  exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

cd "$(dirname "$0")"

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
./gradlew clean assembleDebug --no-daemon --stacktrace
