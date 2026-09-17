# HowtoSetup on Windows

## Ziel

Dieses Handbuch beschreibt den einfachen Setup-Pfad für lokale Entwicklung auf Windows mit dem nativen C++-Projekt des Agenten. Es deckt die wichtigsten Schritte bis zum erfolgreichen Build, Testlauf und dem Start der CLI ab.

## Voraussetzungen

Vor dem Aufbau des Projekts sollten diese Tools installiert sein:

- Git
- CMake 3.16 oder neuer
- C++20-Compiler
- Optional: MinGW/MSYS2 oder Visual Studio Build Tools
- Optional: Python 3, falls der lokale Runner oder Bootstrap genutzt werden soll
- Optional: Ollama, wenn ein echtes LLM-Modell verwendet werden soll

## 1. Repository klonen

```powershell
git clone <repo-url>
cd Agent
```

## 2. Build-Umgebung vorbereiten

### Option A: MinGW/MSYS2

```powershell
cmake -S native -B build-native -G "MinGW Makefiles"
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### Option B: PowerShell-Skript

```powershell
./skripte/lokal_bauen.ps1
```

Das Build-Skript richtet den Standard-Workflow für lokale Native-Entwicklung auf Windows ein und startet danach den CMake-/CTest-Lauf.

## 3. CLI ausführen

Nach erfolgreichem Build liegt die ausführbare Datei typischerweise unter:

```text
build-native\agentenlauf.exe
```

Beispiel:

```powershell
./build-native/agentenlauf.exe --arbeitsverzeichnis C:/workspace/mein-projekt --aufgabe "Erstelle ein Mini-CMake-Projekt mit Test" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Wichtige Parameter:

- `--arbeitsverzeichnis`: Zielordner des Agenten
- `--aufgabe`: Aufgabenbeschreibung
- `--ollama-url`: URL des lokalen Ollama-Servers
- `--modell`: Modellname, z. B. `llama3.2`
- `--max-iterationen`: Maximale Agent-Iterationen
- `--offline`: Lauf ohne externes LLM
- `--git-commits`: Aktiviert begrenzte Git-Commit-Operationen

## 4. Ollama lokal einrichten

Wenn ein reales Modell verwendet werden soll, muss Ollama lokal erreichbar sein.

Installation und Start:

```powershell
ollama pull llama3.2
ollama serve
```

Danach kann der Agent auf einen lokalen Endpoint wie:

```text
http://127.0.0.1:11434
```

zugreifen.

## 5. Troubleshooting

### Build fehlschlägt

- CMake-Version prüfen
- Compilerpfad und Build-Tools verifizieren
- `build-native`-Ordner löschen und neu konfigurieren

```powershell
Remove-Item -Recurse -Force build-native
cmake -S native -B build-native -G "MinGW Makefiles"
```

### Ollama nicht erreichbar

- Prüfen, ob `ollama serve` läuft
- URL und Port prüfen
- Model korrekt installiert
- Bei Bedarf `--offline` nutzen

### Unsichere Pfade

Der Agent erlaubt nur Änderungen innerhalb des aktiv gesetzten Arbeitsverzeichnisses. So bleiben Systempfade und fremde Ordner geschützt.

## 6. Standard-Workflow

1. Repository klonen
2. CMake-Projekt konfigurieren und bauen
3. Tests mit `ctest` validieren
4. CLI mit Projektpfad und Aufgabe starten
5. Falls nötig Ollama aktivieren und Modell auswählen

Damit ist die lokale Windows-Entwicklung für den Agenten in der Regel sofort nutzbar.
