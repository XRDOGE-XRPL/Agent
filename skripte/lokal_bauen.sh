#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
WURZEL="$(cd "$(dirname "$0")/.." && pwd)"
cd "$WURZEL"

echo "=== Nativer Kern (CMake/CTest) ==="
rm -rf build-native
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure

if command -v gradle >/dev/null 2>&1; then
  echo "=== Android-Unit-Tests ==="
  gradle :app:testDebugUnitTest --no-daemon || true
fi

echo "Lokale Validierung abgeschlossen."
