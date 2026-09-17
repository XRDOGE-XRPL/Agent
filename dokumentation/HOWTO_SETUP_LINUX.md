# HowtoSetup on Linux

## Ziel

Dieses Handbuch beschreibt den Standard-Setup-Pfad für die lokale Ausführung des Agenten auf Linux. Der Fokus liegt auf der nativen C++-CLI-Version, optionaler Ollama-Integration und dem sicheren Start im lokalen Projektordner.

## Voraussetzungen

Die folgenden Tools sollten installiert sein:

- Git
- CMake 3.16+
- C++20-Compiler (z. B. `clang` oder `g++`)
- Make oder Ninja
- Python 3 (für Runner und Bootstrap-Tools)
- Optional: Ollama mit installiertem Modell

## 1. Repository klonen

```bash
git clone <repo-url>
cd Agent
```

## 2. Build konfigurieren und starten

### Standard-Workflow

```bash
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### Komplettskript

```bash
bash skripte/lokal_bauen.sh
```

Dies führt den üblichen lokalen Build- und Testlauf für das native Projekt aus.

## 3. CLI starten

Nach erfolgreichem Build sollte das ausführbare Programm unter `build-native/` liegen:

```bash
./build-native/agentenlauf --arbeitsverzeichnis "$HOME/workspace/mein-projekt" --aufgabe "Erstelle ein Mini-CMake-Projekt mit Test" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Wichtige Optionen:

- `--arbeitsverzeichnis`: Arbeitsordner
- `--aufgabe`: Beschreibung der Aufgabe
- `--ollama-url`: Endpoint von Ollama
- `--modell`: z. B. `llama3.2`
- `--max-iterationen`: Maximale Iterationen der Agentenschleife
- `--offline`: Lauf ohne externes LLM
- `--git-commits`: Zulassung begrenzter Git-Schritte

## 4. Ollama einrichten

Wenn ein ein echtes Modell verwendet werden soll, muss Ollama lokal oder im Netzwerk erreichbar sein.

```bash
ollama pull llama3.2
ollama serve
```

Danach kann der Agent mit der URL:

```text
http://127.0.0.1:11434
```

arbeiten.

## 5. Termux-/Linux-Runner

Auf Linux oder Termux kann auch der Python-Runner genutzt werden:

```bash
python laufwerk/termux_ollama.py --arbeitsverzeichnis "$HOME/werkstatt" --aufgabe "Mini-CMake-Projekt mit Test"
```

Dieser Lauf nutzt dieselben Kernprinzipien wie die native CLI-Variante, ist aber für mobile bzw. eingeschränkte Umgebungen geeignet.

## 6. Troubleshooting

### Build schlägt fehl

- CMake/Compiler-Version prüfen
- `build-native` bereinigen und neu konfigurieren

```bash
rm -rf build-native
cmake -S native -B build-native
cmake --build build-native
```

### Ollama nicht erreichbar

- Prüfen, ob der Server läuft
- Achten auf Host/Port und Modellname
- Falls nötig `--offline` verwenden

### Sicherheitsrestriktionen

Der Agent arbeitet nur innerhalb des gewählten Arbeitsverzeichnisses. Schreibzugriffe außerhalb dieses Bereichs werden dafür blockiert.

## 7. Empfohlener Standard-Workflow

1. Repository klonen
2. Abhängigkeiten prüfen
3. Build und Tests ausführen
4. Agent mit Arbeitsverzeichnis und Aufgabe starten
5. Optional Ollama aktivieren und Modell verwenden

Damit ist die lokale Linux-Umgebung für den Agenten in wenigen Schritten nutzbar.
