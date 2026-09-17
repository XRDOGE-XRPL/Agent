# Entwicklerhandbuch

## Ziel des Handbuchs

Dieses Dokument dient als technisches Referenzwerk für alle Entwickler, die an dem autonomen Agenten arbeiten oder das Projekt erweitern möchten. Es beschreibt die wichtigsten Strukturen, Build-Pfade, Workflows und Erweiterungspunkte.

## 1. Projektübersicht

Das Repository ist in drei Hauptbereiche unterteilt:

- `native/` – C++-Kern mit Logik, Agentenschleife, Build- und Testfunktionalität
- `app/` – Android-Anwendung mit Kotlin/Compose und nativer Integration
- `laufwerk/` und `skripte/` – lokale Verwaltungs- und Ausführungshelper

Zusätzlich gibt es die zentrale Build-Definition in `CMakeLists.txt` und `build.gradle.kts`.

## 2. Build- und Laufumgebungen

### Native Builds

```bash
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

Wichtige Punkte:

- Das native Projekt aktiviert `enable_testing()`
- Ein Makro `agent_test(...)` registriert Testziele automatisch
- Build- und Test-Targets liegen im `native/`-Projekt

### Android-Builds

```bash
./gradlew :app:testDebugUnitTest
./build_apk.sh
```

Wichtige Punkte:

- `compileSdk = 35`
- `ndkVersion = "27.1.12297006"`
- `externalNativeBuild` bindet native CMake-Projektkomponenten ein
- Kotlin/Compose-UI und AndroidX-basierte Strukturen werden verwendet

## 3. Komponentendiagramm

```text
CLI / Termux / Android UI
          |
          v
  Agent-Schleife (C++)
     |-- Dateisystem
     |-- JSON-Protokoll
     |-- Build/Test
     |-- Git
     |-- Ollama HTTP
     |-- Sicherheit / Pfadprüfung
```

## 4. Wichtige Dateien und Rollen

### `native/cli/haupteinstieg.cpp`

- Einstiegspunkt für den CLI-Agenten
- liest Argumente
- erzeugt Konfiguration
- startet die Agentenschleife
- verwaltet Logs und Fehlerausgabe

### `native/include/agent/konfiguration.h`

- definiert die Laufparameter wie:
  - `arbeitsverzeichnis`
  - `aufgabe`
  - `ollama_url`
  - `modell`
  - `max_iterationen`
  - `git_commits_erlauben`
  - `offline_erzwingen`

### `native/include/agent/schleife.h`

- zentrale Schnittstelle der Agentenschleife
- definiert `AgentErgebnis`
- enthält die Ablaufsteuerung und das Systemprompt

### `native/src/json_werkzeuge.cpp`

- verarbeitet modelseitige JSON-Antworten
- validiert formale Struktur
- versucht robustes Parsing bei teilweise fehlerhaften Daten

### `native/src/dateisystem.cpp`

- schützt vor unsicheren Pfaden
- prüft, ob Änderungen innerhalb des Arbeitsbaums liegen

### `native/src/build.cpp`

- startet CMake-/Gradle-/Test-Operationen
- sammelt die Ausgabe und evaluiert Erfolg/Fehler

### `laufwerk/termux_ollama.py`

- Python-Runner für lokale oder mobile Ausführung
- sammelt Kontext
- sendet Anfrage an Ollama
- verarbeitet JSON-Schritte und führt die Aktionen aus

## 5. Erweiterungspunkte

### Neue Agentenaktionen

Um eine neue Aktion einzubauen, sind in der Regel diese Stellen anzupassen:

- JSON-Parser der LLM-Antworten
- Auswertung der `aktion` in der Schleife
- Interpreter für die konkrete Ausführung im Dateisystem oder Build-Runner
- Logging und Fehlerbehandlung

### Neue Modell-Backends

Ein neues Backend lässt sich typischerweise durch eine erweiterte LLM-Schnittstelle ergänzen. Dafür sind relevante Punkte:

- URL- und Auth-Handling
- Nachrichten-/Prompt-Format
- Parse- und Response-Validierung
- Online-/Offline-Fallback

### Neue Build- und Test-Integrationen

Wenn weitere Toolchains ergänzt werden sollen, können neue Runner in `build.cpp` oder in den Python-Wrappern ergänzt werden. Dabei sollte immer eine eindeutige Erfolgs-/Fehlerlogik verwendet werden.

## 6. Coding-Richtlinien

- C++17 verwenden
- klare, kleine Funktionen und strukturierte Typen
- JSON-Antworten strikt und kontrolliert behandeln
- Dateisystemzugriffe nur innerhalb des Arbeitsbereichs zulassen
- Fehler immer mit aussagekräftigem Status dokumentieren
- keine unkontrollierten Build- oder Shell-Befehle ohne Sicherheitsprüfung

## 7. Debugging

### Native Debugging

- mit CMake-Builds und `ctest` lokal prüfen
- Log-Ausgaben aus `Protokollierung` nutzen
- einzelne Testtargets mit `ctest -R <name> --output-on-failure` ausführen

### Android-Debugging

- Gradle-Builds und Unit-Tests separat laufen lassen
- SDK- und NDK-Pfade validieren
- Android-Logging für native und Kotlin-Seiten nutzen

### Laufwerk-/Python-Tests

```bash
python -m py_compile laufwerk/termux_ollama.py
```

## 8. Qualitätsmaßstäbe

Ein guter Beitrag sollte:

- build- und testfähig sein
- keine unkontrollierten Pfadangriffe erlauben
- die JSON-Struktur respektieren
- Logs und Rückgabewerte sauber pflegen
- dokumentierte Änderungen mitbringen

## 9. Beispiel für eine typische Erweiterung

Ein typischer Erweiterungsfall wäre das Hinzufügen einer neuen Agentenaktion wie `skripte_ausfuehren`:

1. Modellantwort erweitern
2. Parser erweitern
3. Ausführung in `AgentSchleife` ergänzen
4. Build-/Testlogik für den neuen Laufpfad einbauen
5. Doku und Validierung ergänzen

## 9.1 Erweiterte Analyse-/Refactoring-Aktionen

Der Agent wurde zusätzlich um eine klar definierte Analyse- und Refactoring-Schicht erweitert. Für neue Aktionen sollten Entwickler diese Regeln beachten:

1. Jede Aktion wird zuerst im JSON-Parser validiert und danach im Agentenschleifen-Handler ausgewertet.
2. Dateioperationen laufen immer über denselben Pfad-Guard, damit keine Traversal- oder absolute Pfade in das Arbeitsverzeichnis eindringen.
3. Analyseaktionen sind grundsätzlich lesend und liefern nur Status, Quelltextkontext oder Sicherheitswarnungen zurück.
4. Refactoring-Checks müssen vor dem Schreiben prüfen, ob das Ziel im zulässigen Arbeitsbereich liegt und ob der Ziel-Zustand noch konsistent mit dem bisherigen Build-/Teststatus ist.
5. Wenn der Fehlpfad erkannt wird, muss der Handler eine definitive Fehlerkennung liefern, statt den Prozess stillschweigend fortzusetzen.

Diese Kombination aus parserrobuster JSON-Behandlung, klarer Dispatch-Logik und zentraler Pfadprüfung reduziert die Anzahl schwerer Laufzeitfehler deutlich und ist die Grundlage für längere Iterationsketten in lokalen und mobilen Umgebungen.

## 10. Fazit

Das Projekt ist als modulare Agentenarchitektur gedacht. Die wichtigsten Erweiterungspunkte liegen im nativen Kern, in der Sicherheit, in der LLM-Integration und in der Android-/Termux-Ausführung. Wer diese Strukturen nachvollzieht, kann das System gezielt erweitern, ohne die Kernprinzipien zu verletzen.
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

