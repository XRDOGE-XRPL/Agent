# Projektübersicht

## Status

Das Repository kombiniert ein Android-Frontend mit einem lokalen autonomen Agenten-Workflow. Der aktuelle Stand umfasst:
- Dashboard mit 6 Tabs
- Terminal-Streaming und Shell-Ausführung
- Dateibaum mit automatischer Aktualisierung
- Code-Editor-/Preview-Fenster für Dateien im Workspace
- Build-Status- und Artefaktanzeige
- Memory-CRUD für `memory.json`
- Workspace-Initialisierung mit `File.mkdirs()`
- State- und Log-Integration

## Komponenten

### App-Frontend
- `app/src/main/java/de/xrdoge/agent/ui/MainActivity.kt`
- `app/src/main/res/layout/*.xml`
- Material3-/Jetpack-Compose-Äquivalente bzw. Fragment-UI

### Runtime
- `app/src/main/java/de/xrdoge/agent/laufzeit/AgentRuntime.kt`
- `LogStreamManager.kt`
- `LocalSocketBridge.kt`
- `LocalExecutionEngine.kt`
- `UniversalTaskEngine.kt`

### Native Kern
- `native/`
- CMake/CTest-Setup sowie lokale Dateisystem-/Build-Prüfungen

### Dokumentation
- `README.md`
- `ARCHITECTURE.md`
- `PROJECT_OVERVIEW.md`
- `CHANGELOG.md`
- `dokumentation/`

## Arbeitsverzeichnis

Der primäre Projektworkspace im App-Laufzeitkontext ist:

`/werkstatt`

mit Unterordnern:
- `src/`
- `logs/`
- `appbuilder/`
- `build/`

## Feature-Abdeckung

### Abgedeckt
- UI-Tabs und Navigation
- Terminal-Output und Export
- Dateibaum mit Observer
- Memory-Write/Delete-Operations
- State-/Log-Synchronisation
- Dokumentationssuite

### Teilweise/Umgebungsabhängig
- echte Android-Gradle-Build-Ausführung auf einem SDK-fähigen Host
- echte Termux/Proot-Debian-Integration auf einem mobilen Host

## nächsten Schritte

- Android SDK / AGP-Repository korrekt in der Umgebung konfigurieren
- tatsächlichen Gradle-/APK-Build auf physischer Android-Umgebung verifizieren
- Terminal- und File-Tree-Interaktion gegen reale lokale Dateien validieren
