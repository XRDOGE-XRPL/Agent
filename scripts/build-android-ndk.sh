#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${ANDROID_NDK:="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/ndk/27.1.12297006"}"
: "${ANDROID_ABI:=arm64-v8a}"
: "${ANDROID_PLATFORM:=android-30}"
: "${BUILD_DIR:=${ROOT_DIR}/build-android}"

if [[ -z "${ANDROID_NDK}" || ! -d "${ANDROID_NDK}" ]]; then
  echo "ANDROID_NDK is not set or invalid. Export it or install the Android NDK under the SDK." >&2
  exit 1
fi

cmake -S "${ROOT_DIR}" -B "${BUILD_DIR}" \
  -DCMAKE_TOOLCHAIN_FILE="${ROOT_DIR}/cmake/toolchains/android-standalone.cmake" \
  -DANDROID_NDK="${ANDROID_NDK}" \
  -DANDROID_ABI="${ANDROID_ABI}" \
  -DANDROID_PLATFORM="${ANDROID_PLATFORM}" \
  -DAGENT_BUILD_TESTS=OFF \
  -DAGENT_BUILD_HOST_TOOLS=OFF

cmake --build "${BUILD_DIR}" --target agentenlauf -j"$(nproc 2>/dev/null || echo 2)"
