#!/usr/bin/env bash
set -e
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
export ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-${ANDROID_HOME}}
export PATH="/usr/bin:${PATH}"
export CMAKE_COMMAND=${CMAKE_COMMAND:-/usr/bin/cmake}
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
