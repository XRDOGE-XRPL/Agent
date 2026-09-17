# Changelog

## 2026-09-17

- Finale Audit-Härtung der UI- und Service-Schicht für 6 Tabs, Terminal, Dateibaum, AppBuilder und Memory durchgeführt.
- `TerminalExecService` gegen leere Stream-Reader, IO-Fehler, Interrupts und Security-Blockaden abgesichert; Ausgaben werden weiter in `logs/terminal_exec.log` geschrieben und in `state.json` synchronisiert.
- `WorkspaceService` erweitert um defensive Initialisierung, Fallbacks für fehlende Datein und echte `FileObserver`-Registrierung auf dem Workspace-Baum; `FileTreeFragment` nutzt nun einen rekursiven Observer-Mechanismus, der Änderungen im gesamten `/werkstatt/`-Baum triggert.
- `AppBuilderService` verbessert: Hintergrund-Thread-Run, Pufferung von Build-Output, Status-Callbacks an das UI und robuste Speicherung in `logs/build.log` sowie `state.json`.
- `MemoryFragment` und `MemoryEntryAdapter` überprüft und stabilisiert: fehlende `memory.json` wird automatisch erzeugt; CRUD-Operationen laufen ohne Crash bei leeren oder beschädigten Dateien weiter.
- XML-/View-Mapping für alle sechs Fragment-Klassen erneut validiert und keine uninitialisierten IDs identifiziert.
- Dokumentations- und Repo-Review abgeschlossen; `README.md`, `ARCHITECTURE.md`, `PROJECT_OVERVIEW.md` und `CHANGELOG.md` auf den aktuellen Service- und UI-Stand abgestimmt.
- Build-Validierung mit `./gradlew assembleDebug` wiederholt; die Sandbox bleibt durch das Android-Plugin-Repository / AGP-Resolver-Problem blockiert, nicht durch Projekt-Logikfehler.

## 1.0.0

- Initiale Struktur des autonomen Entwicklungsagenten eingerichtet.
- Android-App, CLI, native Kern und Laufwerk in das Repositorium integriert.
