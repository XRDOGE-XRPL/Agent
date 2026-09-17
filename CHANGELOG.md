# Changelog

## 2026-09-17

- `setup_host.sh` ergänzt: automatisch initialisiert das Android-Termux-Workspace unter `/werkstatt/`, erzeugt `src/`, `logs/`, `appbuilder/` und `build/`, validiert `manifest.json`, `state.json` und `memory.json` und setzt die Laufzeitrechte für Gradle-/Build-Skripte.
- ARM64-Build-Härtung verankert: `gradle.properties` mit `android.aapt2.daemon.enabled=false` und `android.aapt2FromMavenOverride=/opt/android-sdk/build-tools/37.0.0/aapt2` sowie natives `app/src/main/jniLibs/arm64-v8a`-Verzeichnis abgesichert.
- README um den direkten Host-Setup- und lokalen ARM64-Build-Workflow erweitert.
- Finale Audit-Härtung der UI- und Service-Schicht für 6 Tabs, Terminal, Dateibaum, AppBuilder und Memory durchgeführt.
- `TerminalExecService` gegen leere Stream-Reader, IO-Fehler, Interrupts und Security-Blockaden abgesichert; Ausgaben werden in `logs/terminal_exec.log` mit Rotation geschrieben und in `state.json` synchronisiert.
- `WorkspaceService` erweitert um defensive Initialisierung, robuste JSON-Validierung für `memory.json`, automatische Standarddateien und rekursive `FileObserver`-Registrierung; `FileTreeFragment` räumt Watcher im Lifecycle sauber auf.
- `AppBuilderService` verbessert: Shell- und Gradle-Ausführung mit Umgebungsvariablen, Exit-Code-Checks, Status-Callbacks an das UI und robuste Speicherung in `logs/build.log` samt Fehlerweitergabe.
- `MemoryFragment` und `MemoryEntryAdapter` stabilisiert: fehlende bzw. beschädigte `memory.json` werden automatisch korrigiert; CRUD-Operationen laufen ohne Crash weiter und melden ungültige Daten über eine Validierungsnachricht.
- XML-/View-Mapping für alle sechs Fragment-Klassen erneut validiert und keine uninitialisierten IDs identifiziert.
- Dokumentations- und Repo-Review abgeschlossen; `README.md`, `ARCHITECTURE.md`, `PROJECT_OVERVIEW.md` und `CHANGELOG.md` auf den aktuellen Service- und UI-Stand abgestimmt.
- Android Gradle Plugin auf AGP `8.7.3` korrigiert, um den dokumentierten kompatiblen Toolchain-Stack zu alignen.
- Build-Validierung mit `./gradlew tasks --all` wiederholt; der Gradle-Lauf scheitert in der Sandbox weiterhin am externen Plugin-Repository-Zugriff, nicht am Projektcode selbst.

## 1.0.0

- Initiale Struktur des autonomen Entwicklungsagenten eingerichtet.
- Android-App, CLI, native Kern und Laufwerk in das Repositorium integriert.
