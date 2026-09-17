# HowtoSetup on Linux

## Ziel

Dieses Dokument beschreibt den vollständigen Setup-Pfad für die lokale Ausführung des Agenten auf Linux. Es zeigt, wie das Repository in der Proot-Debian-Umgebung vorbereitet, kompiliert, getestet und gestartet wird. Der Projektstandard ist ein fester Ollama-/LLM-Lauf innerhalb derselben Proot-Umgebung; der Termux-Host dient nur als Launcher.

## 1. Voraussetzungen

Vor dem Start sollten diese Pakete installiert sein:

- Git
- CMake 3.16+
- C++20-Compiler, z. B. `gcc` oder `clang`
- Make oder Ninja
- Python 3
- Java 21 in Proot-Debian
- Android SDK + NDK unter `/opt/android-sdk`
- Ollama als fester Bestandteil der Proot-Umgebung

Prüfen:

```bash
git --version
cmake --version
gcc --version
python3 --version
```

Falls ein Paket fehlt, installieren und anschließend die Version prüfen.

## 2. Repository klonen

```bash
git clone <repo-url>
cd Agent
```

## 3. Native Build auf Linux

### 3.1 Direkt mit CMake

```bash
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### 3.2 Mit dem Shell-Skript

```bash
bash skripte/lokal_bauen.sh
```

Das Skript setzt den Standard-Workflow für den lokalen CMake-/CTest-Lauf auf Linux und prüft den nativen Build automatisch.

### 3.3 Ergebnis prüfen

Nach erfolgreichem Build liegt die ausführbare Datei typischerweise hier:

```text
build-native/agentenlauf
```

Wenn diese Datei vorhanden ist, funktioniert der native Setup-Schritt.

## 4. CLI starten

Beispiel für einen lokalen Lauf:

```bash
./build-native/agentenlauf --arbeitsverzeichnis "$HOME/workspace/mein-projekt" --aufgabe "Erstelle ein Mini-CMake-Projekt mit Test" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Wichtige Argumente:

- `--arbeitsverzeichnis`: Ordner, in dem der Agent arbeitet
- `--aufgabe`: Aufgabe an den Agenten
- `--ollama-url`: Beispiel `http://127.0.0.1:11434`
- `--modell`: z. B. `llama3.2`
- `--max-iterationen`: Maximale Anzahl an Iterationen
- `--offline`: Lauf ohne externes Modell
- `--git-commits`: aktiviert begrenzte Git-Schritte, falls erlaubt

Beispiel ohne Ollama:

```bash
./build-native/agentenlauf --arbeitsverzeichnis "$HOME/workspace/mein-projekt" --aufgabe "Prüfe das Projekt und baue es lokal" --offline
```

## 5. Ollama lokal einrichten

Für den Projektsandard muss Ollama in der Proot-Debian-Umgebung lokal erreichbar sein.

### 5.1 Ollama in Proot starten

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3.2
nohup ollama serve >/tmp/ollama-proot.log 2>&1 &
```

Danach ist der Standard-Endpunkt in der Regel:

```text
http://127.0.0.1:11434
```

### 5.2 Agent testen

```bash
./build-native/agentenlauf --arbeitsverzeichnis "$HOME/workspace/mein-projekt" --aufgabe "Erstelle ein kleines Testprojekt und prüfe den Build" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Wenn Ollama nicht läuft, muss die Proot-Umgebung zuerst neu initialisiert und das Modell geladen werden. Der `--offline`-Modus ist nur Ersatz für Ausnahmen, nicht der Standardpfad.

## 6. Android-Setup auf Linux

Wenn Android-Builds oder Unit-Tests mitlaufen sollen, ist das Android SDK nötig.

### 6.1 `local.properties` setzen

Im Projektstamm erzeugen:

```properties
sdk.dir=/opt/android-sdk
cmake.dir=/usr
```

Beispiel:

```properties
sdk.dir=/opt/android-sdk
cmake.dir=/usr
```

Das System-CMake aus Debian (`/usr/bin/cmake`) muss vor dem SDK-internen CMake bevorzugt werden, da das von `sdkmanager` installierte CMake für x86_64-Binärdateien auf ARM64-Proot nicht lauffähig ist.

### 6.2 App-Build und Tests

```bash
./build_apk.sh
./gradlew :app:testDebugUnitTest
```

Oder direkt mit Gradle:

```bash
./gradlew clean assembleDebug --no-daemon --stacktrace
```

Das Projekt ist für AGP 8.7.3 und Gradle 8.9 konfiguriert.

## 7. Python-Runner und Termux-Variante

Für mobile oder eingeschränkte Umgebungen kann der Python-Runner genutzt werden:

```bash
python3 laufwerk/termux_ollama.py --arbeitsverzeichnis "$HOME/werkstatt" --aufgabe "Mini-CMake-Projekt mit Test"
```

Der Runner unterstützt dieselben Grundparameter:

- `--arbeitsverzeichnis`
- `--aufgabe`
- `--ollama-url`
- `--modell`
- `--max-iterationen`
- `--offline`
- `--bootstrap`

### 7.1 Bootstrap auf Termux/Android

```bash
python3 laufwerk/termux_bootstrap.py --bootstrap
```

Oder:

```bash
bash skripte/termux_setup.sh
```

Der Bootstrap prüft die Laufzeitumgebung, installiert fehlende Pakete und validiert die Ollama-/Socket-Umgebung mit Safe-Mode-Fallback.

## 8. Sicherheitsmodell

Der Agent arbeitet nur innerhalb des konfigurierten Arbeitsverzeichnisses. Pfade mit `..` oder Zugriffe außerhalb des Projektbereichs werden als unsicher erkannt und abgelehnt. Das verhindert unbeabsichtigtes Schreiben in fremde oder systemnahe Ordner.

## 9. Troubleshooting

### Build schlägt fehl

- CMake-Version und Compiler prüfen
- `build-native` bereinigen und erneut konfigurieren

```bash
rm -rf build-native
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

### `agentenlauf` fehlt

- Prüfen, ob der native Build erfolgreich abgeschlossen wurde
- Build-Ordner beachten
- CMake-Konfiguration erneut ausführen

### `local.properties` fehlt oder SDK-Pfad falsch

- `local.properties` im Projektstamm erstellen
- SDK-Pfad mit dem tatsächlich installierten Pfad abgleichen

### Ollama nicht erreichbar

- `ollama serve` starten
- URL/Port prüfen
- Modell korrekt installieren
- Bei Bedarf `--offline` nutzen

### Agent ändert nur im Zielordner

Das ist beabsichtigt. Der Agent darf nur im konfigurierten Arbeitsbereich schreiben.

## 10. Standard-Workflow für Linux

1. Repository klonen
2. Abhängigkeiten installieren
3. CMake- und CTest-Build ausführen
4. CLI mit Aufgabenbeschreibung starten
5. Ollama in Proot starten und das Modell laden
6. Bei Android: SDK in `local.properties` setzen und Gradle-Tests ausführen

Damit ist der Setup-Pfad auf Linux vollständig und ohne offene Fragen nutzbar.
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

