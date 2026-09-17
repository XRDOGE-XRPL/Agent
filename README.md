# Autonomer Entwicklungsagent

Dieses Repository enthält einen autonomen Entwicklungsagenten für lokale und Android-Umgebungen. Der Agent kann Projektdateien analysieren, Code ändern, Builds starten, Tests ausführen, Git-Status prüfen und bei Bedarf mit einem lokalen LLM wie Ollama zusammenarbeiten.

Der Schwerpunkt liegt auf drei Nutzungsszenarien:

- Lokale C++-CLI-Ausführung auf Windows, Linux und Termux
- Android-Integration mit Kotlin/Compose und native C++-Komponenten
- Proot-Debian-Stack mit fester Ollama-LLM-Integration im gleichen Userland

## Kernfunktionen

- Autonome Bearbeitung von Dateien im definierten Arbeitsverzeichnis
- Sicherheitsprüfung, damit nur sichere Pfade innerhalb des Projekts geändert werden
- Ausführung von CMake- und Gradle-Builds
- Start von CTest- und JUnit-Tests
- Git-Status- und Commit-Optionen
- Robustes JSON-Protokoll für Modellantworten
- Lang laufende Agentenschleife mit Iterationen und Fehlerkorrektur

## Hauptkomponenten

- **Nativer C++-Kern** in `native/`
  - Dateisystemzugriff
  - HTTP-Kommunikation und Ollama-Integration
  - Git-Helper
  - Build-/Test-Logik
  - Agentenschleife und Protokollierung
- **CLI-Einstieg** in `native/cli/haupteinstieg.cpp`
  - erzeugt das ausführbare Programm `agentenlauf`
- **Android-App** in `app/`
  - Kotlin/Compose-Frontend
  - JNI/Native-Bridge zum C++-Kern
  - Unit-Tests und Debug-Builds
- **Termux/Local-Runner** in `laufwerk/termux_ollama.py`
  - Python-Skript zur lokalen Ausführung auf mobilen Geräten und Linux
- **Build-Skripte** in `skripte/`
  - Windows- und Linux-/Termux-Buildskripte
- **Dokumentation** in `dokumentation/`
  - Architektur, Ausführung und Validierung

## Verzeichnisstruktur

```text
.
├── README.md
├── CHANGELOG.md
├── CMakeLists.txt
├── build.gradle.kts
├── gradlew
├── settings.gradle.kts
├── app/
│   ├── build.gradle.kts
│   ├── src/
│   └── ...
├── native/
│   ├── CMakeLists.txt
│   ├── cli/
│   ├── include/agent/
│   ├── src/
│   └── tests/
├── laufwerk/
│   └── termux_ollama.py
├── skripte/
│   ├── lokal_bauen.ps1
│   └── lokal_bauen.sh
├── dokumentation/
│   ├── ARCHITEKTUR.md
│   ├── AUSFUEHRUNG.md
│   ├── HOWTO_SETUP_WINDOWS.md
│   ├── HOWTO_SETUP_LINUX.md
│   └── VALIDIERUNG.md
└── ...
```

## Setup-HOWTOs

- [HowtoSetup on Windows](dokumentation/HOWTO_SETUP_WINDOWS.md)
- [HowtoSetup on Linux](dokumentation/HOWTO_SETUP_LINUX.md)

## Abhängigkeiten

### Erforderlich für lokale Builds

- CMake 3.16+
- C++20-Compiler
- Git
- Java 21 und Gradle in der Proot-Debian-Umgebung
- Android SDK + NDK unter `/opt/android-sdk`
- Ollama als fester Bestandteil der Proot-Umgebung

### Erforderlich für Android

- Android SDK unter `/opt/android-sdk`
- Android NDK 27.1.12297006
- Compile SDK 34 / Build-Tools 34.0.0
- Kotlin/Compose Plug-ins und Android Gradle Plugin 8.7.3

### Proot-Debian-Standard

- Ollama läuft im selben Debian-Proot-Userland auf `http://127.0.0.1:11434`
- Modell wie `llama3.2` wird automatisch oder manuell geladen
- Termux dient nur als Host-/Launcher-Schicht; die eigentliche Projekt- und Build-Logik läuft in Proot

## Schnellstart

### 1) Lokaler nativer Build

```powershell
cmake -S native -B build-native -G "MinGW Makefiles"
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

Auf Linux/Termux entspricht das:

```bash
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

Alternativ:

```powershell
./skripte/lokal_bauen.ps1
```

oder

```bash
bash skripte/lokal_bauen.sh
```

### Termux-Autobootstrap

Auf Termux oder Android/Termux-Umgebungen kann der Laufzeitstack automatisch vorbereitet werden:

```bash
python laufwerk/termux_bootstrap.py --bootstrap
```

oder:

```bash
bash skripte/termux_setup.sh
```

Zusätzlich gibt es einen reinen Health-Check ohne Paketänderungen:

```bash
python laufwerk/termux_bootstrap.py --healthcheck --json
```

Der Bootstrap prüft die Umgebung, installiert fehlende Pakete, initialisiert ein lokales Python-Environment im `~/.agent`-Ordner, prüft Ollama/Port-Status und setzt einen automatischen Self-Healing-/Safe-Mode-Fallback, falls kritische Abhängigkeiten fehlen.

### 2) CLI ausführen

```text
agentenlauf --arbeitsverzeichnis <pfad> --aufgabe "<text>" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Beispiele:

```text
agentenlauf --arbeitsverzeichnis ./workspace --aufgabe "Erstelle ein Mini-CMake-Projekt mit Test" --offline
agentenlauf --arbeitsverzeichnis /home/user/projekt --aufgabe "Füge eine kleine Hilfsfunktion ein und prüfe den Build" --modell llama3.2
```

### 3) Android bauen

`local.properties` mit dem SDK-Pfad ergänzen:

```properties
sdk.dir=/opt/android-sdk
```

Danach:

```bash
./build_apk.sh
./gradlew :app:testDebugUnitTest
```

oder

```bash
./gradlew clean assembleDebug --no-daemon --stacktrace
```

## Funktionsweise der Agentenschleife

Die Anwendung startet in `native/cli/haupteinstieg.cpp` und setzt eine Konfiguration mit:

- Arbeitsverzeichnis
- Aufgabe
- Ollama-URL und Modell
- maximale Iterationen
- optionales Git-Commit-Recht
- Offline-Modus

Dann wird ein `agent::AgentSchleife` mit einem LLM-Client und einer Protokollierungsinstanz gestartet. Die Schleife läuft über mehrere Iterationen:

1. Das System sammelt einen Kontextrahmen mit Projektdateien, aktuellen Fehlern und der Aufgabe.
2. Eine LLM-Antwort wird als JSON erwartet.
3. Die Antwort wird geparst und in Schritte aufgelöst.
4. Aktionen wie `schreiben`, `loeschen`, `lesen`, `bauen`, `testen`, `git_status` und `fertig` werden ausgeführt.
5. Nach jedem Durchlauf wird der Build/Teststatus geprüft.
6. Die Schleife endet erfolgreich, wenn eine passende Abschlussaktion mit gültigen Ergebnissen erfolgt.

## JSON-Protokoll

Das Modell muss ausschließlich JSON ausgeben. Beispiel:

```json
{
  "schritte": [
    {
      "aktion": "schreiben",
      "pfad": "src/datei.cpp",
      "inhalt": "...",
      "begruendung": "Ergänzt die Funktion mit einem sicheren Pfadcheck"
    },
    {
      "aktion": "testen",
      "begruendung": "Prüft, ob die CMake-Tests noch grün sind"
    },
    {
      "aktion": "fertig",
      "begruendung": "Build und Tests sind erfolgreich"
    }
  ]
}
```

Die genaue Behandlung der im JSON enthaltenen Befehle liegt im nativen C++-Kern und im Python-Runner.

## Sicherheitsmodell

Der Agent begrenzt Änderungen auf das definierte Arbeitsverzeichnis. Wichtige Sicherheitsregeln:

- Nur relative Pfade im Zielordner sind zulässig
- Pfade außerhalb der Arbeitsbasis werden verworfen
- Build- und Testausgaben werden protokolliert
- HTTP wird nur für lokale oder explizit erreichbare Endpunkte verwendet
- Git-Commits sind nur erlaubt, wenn sie ausdrücklich freigeschaltet wurden

## Build- und Teststrategie

Das Projekt setzt auf einen zwei-Schichten-Ansatz:

- Native C++-Schicht für die eigentliche Agentenlogik
- Android/Kotlin-Schicht für UI und mobile Integration

Wichtige Prüfungen:

- `cmake --build build-native`
- `ctest --test-dir build-native --output-on-failure`
- `./gradlew :app:testDebugUnitTest`
- `./build_apk.sh`

## Zusätzliche Dokumentation

Die folgenden Dokumente ergänzen die Hauptdoku speziell für verschiedene Zielgruppen:

- `dokumentation/ROADMAP.md` – Roadmap mit geplanten Features und nächsten Entwicklungsstufen
- `dokumentation/PRODUKTBESCHREIBUNG.md` – Produktbeschreibung für Interessenten und Endkunden
- `dokumentation/ENTWICKLERHANDBUCH.md` – technische Referenz für Mitentwickler
- `dokumentation/GITHUB_LANDINGPAGE.md` – kompakte Projektübersicht für GitHub- oder Präsentationsseiten
- `dokumentation/HOWTO_S24_SETUP.md` – vollständige manuelle Einrichtung für Samsung S24, Termux und Ollama

## Nächste Schritte

- Detailliertere Architektur und Funktionsweise im Dokument `dokumentation/ARCHITEKTUR.md`
- Ausführliche Betriebsanleitung in `dokumentation/AUSFUEHRUNG.md`
- Validierungs- und Testprotokoll in `dokumentation/VALIDIERUNG.md`
- Roadmap mit geplanten Erweiterungen in `dokumentation/ROADMAP.md`
- Produkt- und Marketingübersicht in `dokumentation/PRODUKTBESCHREIBUNG.md`
- Entwicklerreferenz in `dokumentation/ENTWICKLERHANDBUCH.md`
- Landing-Page-Text in `dokumentation/GITHUB_LANDINGPAGE.md`
- Manuelle S24-/Termux-/Ollama-Setup-Anleitung in `dokumentation/HOWTO_S24_SETUP.md`

### Erweiterter Analyse-/Refactoring-Lauf

Die Agentenschleife unterstützt jetzt zusätzlich sichere Analyse- und Refactoring-Schritte, die bewusst auf lesbare Prüfungen und bereits validierte Dateipfade beschränkt sind. Typische Muster sind:

- `analysieren`: Prüft Datei- oder Projektkontext ohne Schreibzugriff, validiert den Pfad und dokumentiert Abweichungen
- `refactor_check`: prüft, ob eine geplante Änderung im Arbeitsbereich konsistent bleibt und keine Traversal-/absolute Pfade erzeugt
- `build_check`: kombiniert lokale CMake-/CTest- oder Gradle-Prüfung mit einer strukturierten Rückmeldung der Agentenschleife
- `status`: meldet laufende Iteration, aktive Phase und Sicherheitsstatus an das UI oder den Benutzer

Damit bleibt der Agent in lokalen, mobilen und Android-/Termux-Umgebungen nachvollziehbar, ohne unkontrollierte Dateioperationen oder fehlerhafte JSON-Antworten zu akzeptieren.

## Definition of Done

Ein sinnvoller Abschluss des Projekts ist erreicht, wenn:

1. Native Builds und CTest ohne Fehler durchlaufen
2. Android-Unit-Tests erfolgreich sind
3. Debug-APK erzeugt werden kann
4. Die Dokumentation die reale Architektur, Nutzung und Risiken beschreibt
5. Der Agent in Online- und Offline-Modus stabil arbeitet
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



## Proot-Debian-Workflow

Der Android-App-Build darf ausschließlich in einer isolierten Proot-Debian-Userland-Umgebung laufen. Der Termux-Host selbst bleibt für den APK-Build nicht nutzbar, weil Bionic-C- und Perfetto-JNI-Restriktionen zu SIGABRT/Gradle-Lifecycle-Abbrüchen führen.

```bash
apt-get update
apt-get install -y openjdk-21-jdk gradle unzip wget git curl ca-certificates cmake
curl -fsSL https://ollama.com/install.sh | sh
nohup ollama serve >/tmp/ollama-proot.log 2>&1 &
ollama pull llama3.2
mkdir -p /opt/android-sdk
cat > local.properties <<'EOF'
sdk.dir=/opt/android-sdk
cmake.dir=/usr
EOF
export PATH="/usr/bin:$PATH"
export CMAKE_COMMAND=/usr/bin/cmake
./build_apk.sh
```

Erforderliche SDK-Komponenten: `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`, `ndk;27.1.12297006`.
Ollama ist im Proot-Debian-Userland zwingend aktiv und nicht optional.
Das System-CMake aus Debian muss vor dem SDK-CMake bevorzugt werden, weil das von `sdkmanager` geladene CMake-Paket unter `/opt/android-sdk/cmake/...` für ARM64-Proot x86_64-binär ist und sonst `No such file or directory` verursacht.
