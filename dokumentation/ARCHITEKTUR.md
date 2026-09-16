# Architektur

## Überblick

Das Projekt ist als mehrschichtige Architektur aufgebaut. Die eigentliche Logik sitzt im nativen C++-Kern, während Android- und Python-Frontends diese Logik als Betriebsumgebung nutzen. Ziel ist ein autonomer Agent, der ein vorgegebenes Ziel in einem Arbeitsverzeichnis selbständig umsetzt, die ausgeführten Änderungen überwacht und bei Bedarf mit einem lokalen Modell kommuniziert.

```text
Android-App (Kotlin/Compose)
        │
        ├── UI und Anwendungslogik
        ├── Sicht auf native Ausführung
        └── optionales LLM-/Build-Feedback
        │
        ▼
JNI / native Bridge
        │
        ▼
C++-Kern (native/)
  ├── Dateisystem- und Pfadvalidierung
  ├── HTTP-/Ollama-Anbindung
  ├── Git-Helfer
  ├── Build-/Test-Runner
  ├── JSON-Protokollparser
  └── Agentenschleife
        │
        ├── lokale CMake-/CTest-Ausführung
        ├── Android-Gradle-Builds
        └── Termux-/Python-Laufwerk
```

## 1. Schichtenmodell

### 1.1 Präsentationsschicht

Die Android-Anwendung in `app/` ist das mobile Frontend. Sie verwendet Compose, Material 3 und AndroidX-Komponenten. Die App dient als UI für den Agenten, kann den Lauf triggern und Informationen zum Zustand, Logs und Build-Ergebnissen darstellen.

Wichtige Bestandteile:

- `app/src/main/java/...` für App-Logik und UI
- Lokale Android-Umgebung mit `externalNativeBuild`
- Kotlin-Compose-UI mit Lebenszyklus- und Coroutine-Integration

### 1.2 Native Bridge

Die Android-App nutzt die nativen Bibliotheken über CMake und JNI. Dadurch kann die App dieselbe Logik nutzen, die auch für die CLI-Version verwendet wird. Die native Schnittstelle ist im CMake-Projekt unter `app/src/main/cpp` und im nativen Buildsystem hinterlegt.

### 1.3 C++-Kern

Der Kern liegt in `native/` und enthält die Kernfunktionalitäten des Agenten. Das CMake-Projekt erzeugt eine statische Bibliothek `agentkern_static` und daraus ein ausführbares Programm `agentenlauf`.

Wichtige Module:

- `dateisystem.cpp` / `dateisystem.h`
  - Pfadprüfung und sichere Dateiverarbeitung
- `http_klient.cpp` / `http_klient.h`
  - HTTP-Verbindungen und URL-Handling
- `ollama.cpp` / `ollama.h`
  - Ollama-API-Aufrufe und Modellzugriff
- `json_werkzeuge.cpp` / `json_werkzeuge.h`
  - Parsing und Validierung von Modelantworten
- `git.cpp` / `git.h`
  - Git-Status und Commit-Integration
- `build.cpp` / `build.h`
  - CMake-/Gradle- und Testausführung
- `schleife.cpp` / `schleife.h`
  - Hauptlogik der Iterationen
- `protokoll.cpp`, `protokollierung.cpp`
  - Strukturierte Logausgabe und Beobachter-Mechanik

## 2. Funktionsweise der Agentenschleife

Die eigentliche Ausführung beginnt im Einstiegspunkt `native/cli/haupteinstieg.cpp`.

Der Ablauf ist in groben Schritten:

1. Konfiguration aus CLI-Argumenten lesen
2. `OllamaKlient` initialisieren
3. `AgentSchleife` mit LLM- und Log-Backend starten
4. Aufgaben-Kontext aufbauen
5. Modellantwort als JSON erwarten
6. JSON in `schritte` zerlegen
7. Maßnahmen wie Schreiben, Löschen, Bauen, Testen oder Beenden ausführen
8. Ergebnisse dokumentieren und erneut iterieren, bis Ziel erreicht oder Limit erreicht ist

Diese Schleife wird durch `max_iterationen` begrenzt und kann im Offline-Modus ohne Ollama arbeiten.

## 3. Regelwerk und Sicherheitsprinzipien

Das Projekt setzt mehrere Sicherheitsmaßnamen um:

- Pfadvalidierung verhindert den Zugriff außerhalb des Arbeitsverzeichnisses
- HTTP-Calls sind konzipiert für lokale Endpunkte und nicht für öffentliche Remote-Backends
- Git-Commits sind nur aktivierbar, wenn explizit erlaubt
- Build- und Testausgaben werden als Fehler- und Erfolgsindikatoren genutzt
- Das Modell antwortet nur mit JSON, wodurch die Ausführung besser kontrollierbar bleibt

## 4. Datenfluss

```text
Aufgabe (CLI / Android / Termux)
        │
        ▼
Konfiguration
        │
        ▼
AgentSchleife
        │
        ├── Kontext sammeln (Dateien, Fehler, zuletzt gemeldete Ergebnisse)
        ├── LLM-Anfrage an Ollama
        ├── JSON-Parsing
        ├── Ausführung von Aktionen im Workspace
        ├── Build/Test-Validierung
        └── Erfolgs- oder Fehlerstatus zurückgeben
```

## 5. Build- und Testarchitektur

Das native CMake-Projekt nutzt:

- `enable_testing()`
- `agent_test(...)`-Makro zum Registrieren einzelner Test-Targets
- eine statische Bibliothek für die Logik
- ausführbare Targets für die CLI

Beispieltestziele:

- `dateisystem_test`
- `json_protokoll_test`
- `git_befehl_test`
- `http_url_test`
- `schleife_mock_test`

Für Android greift das Gradle-Projekt auf `externalNativeBuild` und `ndk` zurück und erzeugt dabei eine App mit Debug- und Release-Konfigurationen.

## 6. Termux- und lokale Laufwerk-Integration

`laufwerk/termux_ollama.py` dient als leichtgewichtiger automatischer Runner für mobile und lokale Umgebungen. Er:

- verarbeitet Aufgaben über argparse
- prüft das Arbeitsverzeichnis
- sammelt Dateinamen und Kontext
- ruft Ollama auf
- verarbeitet JSON-Schritte
- schreibt Dateien, prüft Builds und beendet nach einem erfolgreichen Ziel

Dadurch kann der Agent auch auf Android- oder Termux-Umgebungen mit einem einzigen Python-Skript laufen.

## 7. Designziele

Das Projekt ist bewusst auf diese Ziele ausgerichtet:

- einfache lokale Ausführung ohne komplexe Installation
- direkte Nutzung von CMake und Git-Tools
- mobile Ausführbarkeit mit Android/Termux
- robuste, kontrollierte Ausgabe durch JSON-Schema
- reproduzierbare Builds und Tests
- begrenzte Sicherheitsangriffe durch Pfad- und Bereichsprüfung

## 8. Technische Einschätzung

Die Architektur ist im Vergleich zu einem reinen „LLM-Wrapper“ weitgehend als echter Agent aufgeführt: Sie kombiniert Dateisystemzugriff, Build- und Testlogik, Git-Interaktion, Modellzugriff und Ablaufsteuerung. Dadurch ist sie für lokale Automatisierung, prototypische Wissensarbeit und DevOps-Automation nutzbar, auch wenn sie bewusst auf Sicherheit und nachvollziehbare Befehlsstrukturen setzt.

## Version

Version: 1.0.0
Dokumentationsstand: erweitert und ergänzt
