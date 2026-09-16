# Autonomer Entwicklungsagent

Dieses Repository enthält einen autonomen Entwicklungsagenten für lokale und Android-Umgebungen. Der Agent kann Projektdateien analysieren, Code ändern, Builds starten, Tests ausführen, Git-Status prüfen und bei Bedarf mit einem lokalen LLM wie Ollama zusammenarbeiten.

Der Schwerpunkt liegt auf drei Nutzungsszenarien:

- Lokale C++-CLI-Ausführung auf Windows, Linux und Termux
- Android-Integration mit Kotlin/Compose und native C++-Komponenten
- Offline- und Online-Modus mit optionalem lokalen LLM-Backend

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
│   └── VALIDIERUNG.md
└── ...
```

## Abhängigkeiten

### Erforderlich für lokale Builds

- CMake 3.16+
- C++17-Compiler
- Git
- Optional: Android SDK + NDK für App-Builds
- Optional: Java 17 + Gradle / Android Studio
- Optional: Ollama mit lokalem Modell für echte LLM-Aktivierung

### Erforderlich für Android

- Android SDK
- Android NDK 27.1.12297006
- Compile SDK 35
- Kotlin/Compose Plug-ins und Android Gradle Plugin

### Optional

- Ollama auf `http://127.0.0.1:11434` oder einer erreichbaren lokalen Adresse
- Modell wie `llama3.2`
- Termux auf Android-Geräten

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
sdk.dir=C\:/Users/<user>/AppData/Local/Android/Sdk
```

Danach:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

oder

```bash
gradle :app:assembleDebug :app:testDebugUnitTest
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
- `./gradlew :app:assembleDebug`

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

## Definition of Done

Ein sinnvoller Abschluss des Projekts ist erreicht, wenn:

1. Native Builds und CTest ohne Fehler durchlaufen
2. Android-Unit-Tests erfolgreich sind
3. Debug-APK erzeugt werden kann
4. Die Dokumentation die reale Architektur, Nutzung und Risiken beschreibt
5. Der Agent in Online- und Offline-Modus stabil arbeitet
