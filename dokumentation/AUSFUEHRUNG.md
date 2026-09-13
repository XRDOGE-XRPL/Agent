# Ausführung

## Windows

1. Java 17+, CMake, MinGW-g++, Git und optional Gradle/Android-SDK
2. `skripte/lokal_bauen.ps1`
3. CLI: `build-native\agentenlauf.exe --hilfe`

## Termux

```bash
pkg install cmake clang git python
sh skripte/lokal_bauen.sh
python laufwerk/termux_ollama.py --arbeitsverzeichnis "$HOME/werkstatt" --aufgabe "Mini-CMake-Projekt mit Test"
```

Ollama auf demselben Gerät oder per USB/LAN unter `--ollama-url` erreichbar machen.

## Android-Emulator

`10.0.2.2` statt `127.0.0.1` verwenden, wenn Ollama auf dem Host läuft.

## Git-Versionierung

Die Schleife liest `git status`. Commits nur mit `--git-commits` (CLI) bzw. wenn die Konfiguration es erlaubt.
