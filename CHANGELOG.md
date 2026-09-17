# Changelog

## 2026-09-17

- Finaler UI-Stand mit 6 Tabs ergänzt: Dashboard, Dokumentation, Terminal, Dateibaum, AppBuilder und Memory.
- Workspace-Initialisierung für `src/`, `logs/`, `appbuilder/` und `build/` ergänzt.
- TerminalExecService implementiert mit `ProcessBuilder`, Live-Output und log-file export.
- FileTreeFragment erweitert mit `FileObserver` für automatische Baum-Refreshes.
- Memory-CRUD direkt an `memory.json` gebunden.
- AppBuilderService ergänzt mit Background-Thread und Build-Status-Feedback.
- `state.json`, `manifest.json`, `memory.json` angelegt bzw. aktualisiert.
- Dokumentationsdateien `ARCHITECTURE.md` und `PROJECT_OVERVIEW.md` ergänzt.
- Build-Validierung wiederholt, aber durch AGP-Repository-Problem in der Sandbox blockiert.

## 1.0.0

- Initiale Struktur des autonomen Entwicklungsagenten eingerichtet.
- Android-App, CLI, native Kern und Laufwerk in das Repositorium integriert.
