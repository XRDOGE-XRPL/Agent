#!/usr/bin/env bash
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-/opt/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME}}"
cd "$(dirname "$0")"

if [ -f local.properties ]; then
  if grep -q '^sdk.dir=' local.properties; then
    sed -i "s#^sdk.dir=.*#sdk.dir=${ANDROID_HOME}#" local.properties
  else
    printf 'sdk.dir=%s\n' "${ANDROID_HOME}" >> local.properties
  fi
else
  printf 'sdk.dir=%s\n' "${ANDROID_HOME}" > local.properties
fi

./gradlew --stop || true
./gradlew clean assembleDebug --no-daemon --stacktrace
