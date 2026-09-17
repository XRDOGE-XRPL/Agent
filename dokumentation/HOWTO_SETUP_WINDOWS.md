# HowtoSetup on Windows

## Ziel

Dieses Dokument beschreibt den vollständigen Setup-Pfad für lokale Entwicklung auf Windows. Es zeigt, wie das Repository vorbereitet, gebaut, getestet und mit dem Agenten gestartet wird. Die Anleitungen decken den nativen C++-Workflow, den Android-Zusatzpfad und den optionalen Ollama-/LLM-Lauf ab.

## 1. Voraussetzungen

Vor dem Start sollten diese Tools installiert sein:

- Git
- CMake 3.16+
- C++20-Compiler
- Optional: MinGW/MSYS2 oder Visual Studio Build Tools
- Optional: Python 3, wenn der lokale Runner oder Bootstrap verwendet wird
- Optional: Java 17+ und Android SDK, falls Android-Unit-Tests oder App-Builds benötigt werden
- Optional: Ollama, falls ein echtes lokales Modell verwendet werden soll

Prüfen, ob die wichtigsten Werkzeuge bereits vorhanden sind:

```powershell
git --version
cmake --version
where gcc
where g++
where python
```

Wenn `cmake` oder ein C++-Compiler fehlt, müssen diese zuerst installiert werden.

## 2. Repository klonen

```powershell
git clone <repo-url>
cd Agent
```

Danach liegt das Projekt im aktuellen Ordner `Agent`.

## 3. Native Build auf Windows

### 3.1 Direkt mit CMake

```powershell
cmake -S native -B build-native -G "MinGW Makefiles"
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### 3.2 Mit dem vorhandenen Build-Skript

```powershell
./skripte/lokal_bauen.ps1
```

Das Skript legt den Build-Ordner `build-native` an, konfiguriert das CMake-Projekt und startet den Testlauf mit `ctest`.

### 3.3 Ergebnis prüfen

Nach erfolgreichem Build sollte die ausführbare Datei in diesem Ordner liegen:

```text
build-native\agentenlauf.exe
```

Wenn die Datei vorhanden ist, ist der native Setup-Schritt erfolgreich.

## 4. CLI starten

Der Agent wird mit den wichtigsten Parametern gestartet:

```powershell
./build-native/agentenlauf.exe --arbeitsverzeichnis C:/workspace/mein-projekt --aufgabe "Erstelle ein Mini-CMake-Projekt mit Test" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Wichtige Argumente:

- `--arbeitsverzeichnis`: Zielordner, in dem der Agent arbeitet
- `--aufgabe`: Beschreibung der Aufgabe in natürlicher Sprache
- `--ollama-url`: URL des Ollama-Servers, z. B. `http://127.0.0.1:11434`
- `--modell`: Modellname, z. B. `llama3.2`
- `--max-iterationen`: Maximale Anzahl der Agenten-Schritte
- `--offline`: läuft ohne externes LLM
- `--git-commits`: erlaubt begrenzte Git-Operationen, falls konfiguriert

Beispiel ohne Ollama:

```powershell
./build-native/agentenlauf.exe --arbeitsverzeichnis C:/workspace/mein-projekt --aufgabe "Prüfe das Projekt und baue es lokal" --offline
```

## 5. Ollama lokal einrichten

Wenn ein echtes LLM verwendet werden soll, muss Ollama lokal erreichbar sein.

### 5.1 Ollama installieren und starten

```powershell
ollama pull llama3.2
ollama serve
```

Nach dem Start lauscht Ollama typischerweise auf:

```text
http://127.0.0.1:11434
```

### 5.2 Mit dem Agenten testen

```powershell
./build-native/agentenlauf.exe --arbeitsverzeichnis C:/workspace/mein-projekt --aufgabe "Erstelle ein kleines Testprojekt und prüfe den Build" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Wenn Ollama nicht installiert ist oder nicht läuft, muss man entweder `--offline` verwenden oder zuerst das lokale Modell starten.

## 6. Android-Setup auf Windows

Wenn zusätzlich Android-Builds oder Unit-Tests gebaut werden sollen, ist das Android SDK nötig.

### 6.1 `local.properties` erstellen

Im Projektstamm erzeugen:

```properties
sdk.dir=/opt/android-sdk
```

Beispiel:

```properties
sdk.dir=/opt/android-sdk
```

### 6.2 App-Build und Tests

```powershell
./build_apk.sh
./gradlew :app:testDebugUnitTest
```

Oder mit Gradle direkt:

```powershell
./gradlew clean assembleDebug --no-daemon --stacktrace
```

Wichtig: Das Projekt verwendet Android Gradle Plugin 8.7.3 und Gradle 8.9 als kompatiblen Stack.

## 7. Python-Runner für Windows/Linux-ähnliche Umgebungen

Das Projekt enthält zusätzlich einen lokalen Laufmechanismus für Shell-/Termux-artige Umgebungen:

```powershell
python laufwerk/termux_ollama.py --arbeitsverzeichnis C:/workspace/agent-test --aufgabe "Mini-CMake-Projekt mit Test"
```

Der Runner akzeptiert grundsätzlich dieselben Grundparameter:

- `--arbeitsverzeichnis`
- `--aufgabe`
- `--ollama-url`
- `--modell`
- `--max-iterationen`
- `--offline`
- `--bootstrap`

## 8. Termux-/Bootstrap auf Android/Termux

Für Android/Termux-Umgebungen kann der Bootstrap aktiviert werden:

```bash
python3 laufwerk/termux_bootstrap.py --bootstrap
```

Oder

```bash
bash skripte/termux_setup.sh
```

Der Bootstrap prüft die Umgebung, installiert fehlende Pakete, initialisiert Laufzeit-Ordner und prüft die Ollama-/Socket-Umgebung mit Safe-Mode-Fallback.

## 9. Sicherheitsmodell

Der Agent arbeitet nur innerhalb des konfigurierten Arbeitsverzeichnisses. Pfade mit `..` oder Bereiche außerhalb des Projekts werden als unsicher abgelehnt. Das verhindert ungewollte Schreib- und Löschaktionen außerhalb des Zielbereichs.

## 10. Troubleshooting

### Build schlägt fehl

- CMake-Version prüfen
- Compilerpfad und Build-Tools überprüfen
- `build-native` neu löschen und neu konfigurieren

```powershell
Remove-Item -Recurse -Force build-native
cmake -S native -B build-native -G "MinGW Makefiles"
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### `agentenlauf.exe` fehlt

- Sicherstellen, dass der Build erfolgreich abgeschlossen wurde
- `build-native` prüfen
- CMake-Generierung erneut ausführen

### `local.properties` fehlt oder SDK-Pfad ist falsch

- `local.properties` im Projektstamm anlegen
- SDK-Pfad mit dem installierten Android SDK abgleichen

### Ollama ist nicht erreichbar

- prüfen, ob `ollama serve` läuft
- URL auf `http://127.0.0.1:11434` prüfen
- Modell mit `ollama pull llama3.2` installieren
- Bei Bedarf auf `--offline` umschalten

### Agent kann außerhalb des Arbeitsbereichs nichts ändern

Das ist beabsichtigt und Teil der Sicherheitslogik. Der Agent darf nur innerhalb des definierten Projektordners schreiben.

## 11. Standard-Workflow für Windows

1. Repository klonen
2. CMake/Compiler installieren
3. Projekt bauen und testen
4. CLI mit Aufgabenbeschreibung starten
5. Optional: Ollama starten und Modell verwenden
6. Bei Android: SDK in `local.properties` setzen und Gradle-Tests ausführen

Damit ist der Setup-Pfad auf Windows vollständig und ohne offene Unklarheiten nutzbar.
## Proot-Debian-Build (verpflichtend)

Android- und JNI-Builds dürfen auf dem Termux-Host nicht direkt ausgeführt werden. Der Host nutzt Bionic/Perfetto- und Kernel-Restriktionen, die bei `SIGABRT`/JNI-Abstürzen und Gradle-Lifecycle-Problemen auftreten können. Der robuste und reproduzierbare Weg ist ein isolierter Proot-Debian-Container mit Java 21, Android SDK unter `/opt/android-sdk` und der lokalen Gradle-Ausführung dort.

```bash
# Beispiel: Android SDK unter /opt/android-sdk
mkdir -p /opt/android-sdk
cat > local.properties <<'EOF'
sdk.dir=/opt/android-sdk
EOF
./build_apk.sh
```

Das Repository erwartet `sdk.dir=/opt/android-sdk` im Projektstamm. Der direkte Host-Build bleibt in Termux deaktiviert.

