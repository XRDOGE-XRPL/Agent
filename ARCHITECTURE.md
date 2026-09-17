# Architektur

## Zielbild

Der Agent besteht aus einem lokalen Android-Frontend, einem Java/Kotlin-Workflow-Layer und einem nativen bzw. lokal ausführbaren Laufzeit-Stack. Das System soll Aufgaben im Arbeitsverzeichnis `/werkstatt` orchestrieren, systematische Logs und State-Daten verwalten und Build-/Execution-Schritte über definierte Services abstrahieren.

## Schichten

### 1. UI-Schicht

- `app/src/main/java/de/xrdoge/agent/ui/MainActivity.kt`
- `DashboardFragment.kt`
- `DocumentationFragment.kt`
- `TerminalFragment.kt`
- `FileTreeFragment.kt`
- `AppBuilderFragment.kt`
- `MemoryFragment.kt`

Diese Fragments bilden die 6 Tabs der App:
- Dashboard
- Dokumentation
- Terminal & Exec
- Dateibaum & Code-Editor
- AppBuilder Pipeline
- Memory & Gedächtnis

### 2. Arbeitsbereichs- und Workspace-Service

- `WorkspaceService.kt`

Der Service garantiert die Initialisierung von:
- `src/`
- `logs/`
- `appbuilder/`
- `build/`

Zusätzlich verwaltet er:
- `manifest.json`
- `state.json`
- `memory.json`
- `CHANGELOG.md`
- `logs/agent.log`
- `logs/terminal_exec.log`

### 3. Laufzeit- und Backend-Schicht

- `AgentRuntime.kt`
- `LocalSocketBridge.kt`
- `LogStreamManager.kt`
- `UniversalTaskEngine.kt`
- `LocalExecutionEngine.kt`
- `ExecutionRouter.kt`
- `EphemeralServiceManager.kt`

Diese Schicht kapselt Befehlsausführung, Protokollierung, Ressourcenverwaltung, Lastenausgleich und Build-Scaffolding.

### 4. Native Brücke / C++-Kern

- `native/`

Der C++-Kern übernimmt XPath-/Pfadvalidierung, Datei-Operationen und Build-Test-Checks. Die Android-App nutzt dabei JNI und einen abgeleiteten Laufzeitpfad über `NativeBruecke` sowie lokale Socket-/Process-Executionsmechanismen.

## Datenfluss

1. Der Benutzer setzt in Dashboard oder Terminal eine Aufgabe.
2. Die App initialisiert sicher das Arbeitsverzeichnis `/werkstatt` und die Standardordner.
3. `TerminalExecService` startet `ProcessBuilder` im Ordner `/werkstatt/src`.
4. Die Ausgabe wird in Echtzeit an `TerminalFragment` weitergereicht und in `logs/terminal_exec.log` gespeichert.
5. `WorkspaceService` aktualisiert `state.json`, `logs/agent.log` und `CHANGELOG.md` nach jeder Aktion.
6. `MemoryFragment` liest und schreibt `memory.json` direkt.
7. `AppBuilderService` startet Build-Prozesse im Hintergrund und liefert Status und Artefaktlisten zurück.
8. `FileTreeFragment` überwacht den Workspace mit `FileObserver` und refreshes den Baum bei Änderungen.

## Zustandsmodell

`state.json` enthält die systemrelevanten Laufzeitmetadaten:
- `workspace`
- `agentStatus`
- `lastUpdated`
- `provider`
- `model`
- `iterations`
- `ttlMinutes`
- `buildStatus`
- `lastTerminalCommand`

## Logging-Muster

- `logs/agent.log` – allgemeine Agenten- und Systemaktivitäten
- `logs/terminal_exec.log` – Terminalausgaben
- `CHANGELOG.md` – menschlich lesbare Chronik von Systemupdates und Aktionen

## Sicherheitsprinzipien

- Keine `..`-Pfadsegmente in kritischen Dateivorgängen
- sichere Initialisierung nur im Workspace
- logische Trennung von UI, Runtime und generierter Projektdaten
- Abschlusswerte werden in `state.json` synchronisiert
