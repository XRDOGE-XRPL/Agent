# Ausführung und Betrieb

## 1. Voraussetzungen

Die folgenden Voraussetzungen variieren je nach Betriebsart:

### Für native lokale Ausführung

- CMake 3.16 oder neuer
- C++20-Compiler
- Git
- Optional: MinGW oder MSYS2 auf Windows
- Optional: Python 3 für den Termux-/Local-Runner

### Für Android-Builds

- Java 21 in Proot-Debian (empfohlen)
- Android SDK
- Android App target: Java 17 via Gradle/Kotlin JDK-Ausrichtung
- Android NDK 27.1.12297006
- Android Gradle Plugin entsprechend der Konfiguration

### Für Ollama-Integration

- Ollama muss in der Proot-Debian-Umgebung lokal erreichbar sein
- Modell installiert, z. B. `llama3.2`
- Öffentliche oder lokale URL, z. B. `http://127.0.0.1:11434`

## 2. Native Ausführung auf Windows

Auf Windows ist der bevorzugte Pfad die Verwendung des CMake-Projekts und der Build-Skripte.

### Standard-Workflow

```powershell
cmake -S native -B build-native -G "MinGW Makefiles"
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### Komplettskript

```powershell
./skripte/lokal_bauen.ps1
```

### CLI-Hilfe

```powershell
./build-native/agentenlauf.exe --hilfe
```

Die Hilfeseite zeigt die verfügbaren Argumente. Wichtig sind insbesondere:

- `--arbeitsverzeichnis`
- `--aufgabe`
- `--ollama-url`
- `--modell`
- `--max-iterationen`
- `--git-commits`
- `--offline`

## 3. Lokale Ausführung auf Linux / Termux

### Linux / Termux-Setup

```bash
pkg install cmake clang git python
bash skripte/lokal_bauen.sh
```

### Python-Runner

```bash
python laufwerk/termux_ollama.py --arbeitsverzeichnis "$HOME/werkstatt" --aufgabe "Mini-CMake-Projekt mit Test"
```

Der Runner akzeptiert dieselben grundlegenden Einheiten:

- Arbeitsverzeichnis
- Aufgabe an den Agenten
- Modell und URL
- maximale Iterationen
- `--offline` nur als Notfallfallback, nicht als Standardmodus

### Ollama-Verfügbarkeit

Damit der Agent mit einem echten Modell arbeitet, muss Ollama in der Proot-Debian-Umgebung lokal laufen und dort erreichbar sein. Für Android-Emulatoren und Host-Maschinen ist die Standard-Umgebung oft:

- Host: `127.0.0.1`
- Emulator: `10.0.2.2`

Wenn Ollama auf dem Host läuft und das Android-Emulator-Frontend genutzt wird, muss die URL entsprechend auf `10.0.2.2` zeigen.

## 4. Android-Ausführung

### SDK konfigurieren

Die Android-SDK-Umgebung muss in `local.properties` hinterlegt werden:

```properties
sdk.dir=/opt/android-sdk
```

### Debug-Build und Tests

```bash
./gradlew :app:testDebugUnitTest
./build_apk.sh
```

oder mit Gradle direkt:

```bash
./gradlew clean assembleDebug --no-daemon --stacktrace
```

Die Android-App verwendet `externalNativeBuild`, damit die nativen C++-Komponenten in den App-Workflow integriert werden.

## 5. Beispiel für einen kompletten Lauf

```text
agentenlauf \
  --arbeitsverzeichnis /workspace/mein-projekt \
  --aufgabe "Füge eine kleine Hilfsfunktion hinzu und prüfe den CMake-Build" \
  --ollama-url http://127.0.0.1:11434 \
  --modell llama3.2 \
  --max-iterationen 8
```

Falls Ollama ausfällt, kann der Agent nur noch als Ausweichlauf in einem Offline-/Testmodus gestartet werden. Der normale Betrieb bleibt immer mit Ollama in Proot.

```text
agentenlauf --arbeitsverzeichnis /workspace/mein-projekt --aufgabe "Erstelle ein Testprojekt" --offline
```

## 6. Git-Versionierung

Die Agentenschleife kann Git-Status und ggf. Commit-Operationen verarbeiten. Git-Aktionen sind jedoch ausdrücklich kontrolliert und nur aktiv, wenn die Konfiguration dies zulässt.

Typische Auslöser:

- `--git-commits` beim CLI-Lauf
- entsprechende interne Konfiguration im Agenten

So bleibt das Projekt vor unbeabsichtigten, automatischen Commits geschützt.

## 7. Verfügbare Bedienelemente

Die CLI-Befehle und Laufvarianten sind bewusst einfach gehalten:

- lokale Build- und Testausführung mit CMake/CTest
- Android-Build- und Testausführung mit Gradle
- fester LLM-Lauf über Ollama in Proot
- Termux-Runner für mobile und eingeschränkte Umgebungen

## 8. Fehlerbehandlung und Troubleshooting

### Probleme mit Ollama

Wenn der LLM-Endpunkt nicht erreichbar ist:

- prüfen, ob Ollama in der Proot-Umgebung gestartet ist
- URL und Port prüfen
- Modell in Proot installiert haben
- nur als Notfall `--offline` verwenden, aber nicht im regulären Betrieb

### Probleme mit dem Build

- CMake-Version prüfen
- Compiler installieren
- `build-native`-Ordner bereinigen und neu konfigurieren
- Gradle-Wrapper oder SDK-Pfad prüfen

### Unsichere Pfade

Der Agent verweigert Änderungen außerhalb des aktiven Arbeitsverzeichnisses. Das ist Teil der Sicherheitslogik und verhindert das Schreiben in System- oder fremde Bereiche.

## 9. Empfohlene Standard-Workflows

### Für lokale Entwicklung

1. CMake-Projekt konfigurieren
2. `agentenlauf` mit Projektpfad und Aufgabe starten
3. Build und Tests überwachen
4. Erst bei Erfolg beenden

### Für Android-Entwicklung

1. SDK konfigurieren
2. App bauen und testen
3. Ollama-URL für Emulator/Host anpassen
4. Agentenlauf mit eingeschränktem Arbeitsbereich starten

### Für Termux

1. `pkg install` der benötigten Pakete
2. Laufwerk mit Aufgabe starten
3. Builds und Testläufe lokal überwachen
4. Modell und API-Endpunkt validieren

## 9. Erweiterte Analyse-/Refactoring-Aktionen

Der Laufpfad wurde um zusätzliche, sichere Analyse- und Refactoring-Schritte erweitert. Diese werden in der Agentenschleife als eigene Zustände behandelt und immer mit einer Pfadvalidierung verknüpft:

- `analysieren` prüft relative, in-root Pfade und liest nur in den zulässigen Arbeitsbereich
- `refactor_check` prüft den vorgeschlagenen "Vor-Schreib"-Zustand, bevor Änderungen wirksam werden
- `build_check` kombiniert Laufzeitstatus, Build-Erfolg und Testresultat in einem konsistenten Schritt
- `status` gibt den aktuellen Iterationszustand und Sicherheitsstatus an die UI-/Console-Ausgabe weiter

Für echte lokale Validierungsläufe gilt zusätzlich:

- mehrere Analyse-/Build-Zyklen werden in einer einzigen Iterationskette ohne Zustandsverlust durchgeführt
- Fehlerpfade werden protokolliert, damit die nächste Runde mit dem korrekten Kontext startet
- JSON- und Unicode-Inhalte werden robust geparst, selbst wenn das LLM teilweise defekte oder unvollständige Antworten liefert

## 10. Zusammenfassung

Der Agent ist für mehrere Betriebsumgebungen vorbereitet: native lokale Entwicklung, Android-App-Builds und mobile Termux-Umgebungen. Die Ausführung ist bewusst kontrolliert, dokumentiert und an sichere Verhaltensregeln gebunden. Dadurch kann der Agent in einer produktiven Umgebung zuverlässig eingesetzt werden, ohne unverhältnismäßig viele Risiken oder unkontrollierte Dateiänderungen zuzulassen.
## Hybrides Build-Setup: native Termux + Proot-Fallback

Für Android-/Gradle-Builds ist native Termux der primäre Host, weil dort Java 21, Android SDK, Gradle-Caches und temporäre Dateien direkt auf dem nativen Dateisystem laufen und damit den PRoot-/Syscall-Overhead vermeiden. Proot-Debian bleibt als kompatibler Fallback für isolierte Debian-spezifische Aufgaben oder Validierungsprüfungen bestehen.

Empfohlener Standard:

```bash
./termux_native_setup.sh
# oder direkt in Termux:
cd /data/data/com.termux/files/home/Agent
./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1   -Dorg.gradle.daemon=false   -Dorg.gradle.parallel=false   -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'
```

Das Projekt nutzt `JAVA_HOME` und `ANDROID_HOME` aus der nativen Termux-Umgebung und setzt die Gradle-Parameter für ressourcenbeschränkte ARM64-/Proot-Umgebungen automatisch. Proot-Debian wird nur noch für kompatibilitätsorientierte oder isolierte Setup-/Testschritte verwendet.

