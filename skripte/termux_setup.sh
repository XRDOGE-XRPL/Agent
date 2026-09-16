#!/usr/bin/env bash
set -u

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
PYTHON_BIN="${PYTHON_BIN:-}"
if [ -z "$PYTHON_BIN" ]; then
  if command -v python3 >/dev/null 2>&1; then
    PYTHON_BIN="python3"
  elif command -v python >/dev/null 2>&1; then
    PYTHON_BIN="python"
  else
    echo "Keine Python-Installation gefunden. Bitte python3 oder python installieren." >&2
    exit 1
  fi
fi

"$PYTHON_BIN" "$ROOT_DIR/laufwerk/termux_bootstrap.py" --bootstrap --healthcheck "$@"
