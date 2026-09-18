# Autonomer Entwicklungsagent

Das Repository bildet einen autonomen Entwicklungsagenten mit Android-Frontend, lokalen Workspace-Services und Laufzeit-Bridge für lokale Prozesse, LLM-Checks und Build-Orchestrierung ab.

## Aktueller Systemstatus

Die aktuelle Stabilisierung umfasst eine harte Initialisierung des Workspace unter `/werkstatt`, robustes JSON-Handling für `memory.json`, logische Schutzmechanismen gegen beschädigte Dateien und fehlende Verzeichnisse sowie einen defensiven Prozess- und UI-Update-Flow für Terminal und Build-Ausführung.

Die App enthält aktuell 6 Tabs:
- Dashboard
- Dokumentation
- Terminal & Exec
- Dateibaum & Code-Editor
- AppBuilder Pipeline
- Memory & Gedächtnis

Die Workspace-Initialisierung sichert automatisch die Ordner `src/`, `logs/`, `appbuilder/` und `build/` unter `/werkstatt`.

## Kernfunktionen

- App-Start mit `File.mkdirs()` und strukturierter Workspace-Erzeugung
- Terminal-Ausführung im Arbeitsverzeichnis `/werkstatt/src/`
- Echtzeit-Output an das TerminalFragment
- FileTree mit `FileObserver`-Refresh bei Dateiveränderungen
- Direktes Lesen/Bearbeiten von Dateien aus dem Workspace
- Build-Status und Artefaktliste für AppBuilder
- `memory.json`-CRUD mit direktem UI-Handling
- `state.json`- und `CHANGELOG.md`-Synchronisation
- `logs/agent.log` und `logs/terminal_exec.log` als Laufzeitprotokolle

## Hauptkomponenten

- `app/src/main/java/de/xrdoge/agent/ui/MainActivity.kt` – Tab-Host
- `WorkspaceService.kt` – Initialisierung, Status, Logs, Memory, State
- `TerminalExecService.kt` – Shell-Ausführung mit Live-Output
- `AppBuilderService.kt` – Build-Schleife mit Status- und Artefakt-Feedback
- `FileTreeFragment.kt` – Dateibaum mit Observer
- `MemoryFragment.kt` – JSON-CRUD für Speicher-Einträge
- `AgentRuntime.kt` – Laufzeit-Container mit LogStream, Router und Engine
- `LocalSocketBridge.kt` – lokale Process-/Socket-Funktionen
- `UniversalTaskEngine.kt` – generierte Projekt- und Doku-Suite

## Arbeitsverzeichnis

Der Standard-Workspace ist:

`/werkstatt`

mit Unterordnern:
- `src/`
- `logs/`
- `appbuilder/`
- `build/`

Darüber hinaus werden persistente Metadaten und Dokumentationsdateien verwaltet:
- `manifest.json`
- `state.json`
- `memory.json`
- `README.md`
- `ARCHITECTURE.md`
- `PROJECT_OVERVIEW.md`
- `CHANGELOG.md`

## Architektur und Datenfluss

1. Benutzer wählt im Dashboard eine Aufgabe und Parameter.
2. Die App validiert und initialisiert den Workspace.
3. Terminal-Befehle erfolgen im `/werkstatt/src/`-Ordner.
4. Ausgabe und Status werden in Logs und `state.json` geschrieben.
5. Dateisystemänderungen werden über `FileObserver` im FileTree erkannt.
6. Anpassungen an `memory.json` werden direkt im Memory-Tab persistiert.
7. Build-Skripte werden im Background-Thread gestartet und die Artefaktliste aktualisiert.

## Host-Setup für Termux / Android

Das Repository unterstützt jetzt ein hybrides Setup:

- Native Termux ist der primäre Build-Host für Java/Gradle/SDK/Cache.
- Proot-Debian bleibt ein kompatibler Fallback für isolierte bzw. Debian-spezifische Befehle.

Native Termux-Variante:

```bash
chmod +x termux_native_setup.sh
./termux_native_setup.sh
```

Damit wird direkt in Termux ein passendes JDK 21, Android SDK und die Laufzeitvariablen (`JAVA_HOME`, `ANDROID_HOME`) vorbereitet. Anschließend wird der echte Android-Build mit reduzierter Parallelität und stabilen Gradle-Flags gestartet.

Proot-/Debian-Variante:

```bash
chmod +x setup_host.sh
./setup_host.sh
```

Das Skript erstellt automatisch den kompletten Workspace unter `/werkstatt/` mit den erforderlichen Ordnern `src/`, `logs/`, `appbuilder/` und `build/`. Zusätzlich werden die Fallback-Metadaten `manifest.json`, `state.json` und `memory.json` validiert bzw. bei Bedarf neu generiert. Danach werden `gradlew` und alle internen Build-Skripte mit `chmod +x` freigegeben.

Für den ARM64-/Android-Build gilt der harte Standard:

```bash
./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1 \
  -Dorg.gradle.daemon=false \
  -Dorg.gradle.parallel=false \
  -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'
```

Die `gradle.properties` aktivieren dabei dauerhaft:

- `org.gradle.daemon=false`
- `org.gradle.parallel=false`
- `org.gradle.workers.max=1`
- `org.gradle.jvmargs=-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false`
- valider `android.aapt2FromMavenOverride` nur, wenn eine echte lokale AAPT2-Binärdatei vorhanden ist

Damit werden Build-Abbrüche in restriktiven ARM64-Umgebungen unterdrückt und das native Lib-Verzeichnis `app/src/main/jniLibs/arm64-v8a` sauber eingebunden.

## Aufbau des Repositories

```text
.
├── README.md
├── ARCHITECTURE.md
├── PROJECT_OVERVIEW.md
├── CHANGELOG.md
├── manifest.json
├── state.json
├── memory.json
├── app/
│   └── src/main/java/de/xrdoge/agent/...
├── native/
├── laufwerk/
├── dokumentation/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── build_apk.sh
```

## Validierung

Die UI-Integration wurde in den relevanten App-Klassen ergänzt. Ein lokaler Gradle-Aufruf wird in der aktuellen Sandbox durch ein Android-Toolchain-Problem blockiert:

- AGP `com.android.application:8.7.3` konnte im aktuellen Runner nicht aufgelöst werden

Das betrifft die build-Umgebung, nicht die Architektur der eingebauten UI- und Workspace-Komponenten.

## Nutzung

- Dashboard-Parameter für Ollama-URL, Modell, Iterationen, Provider und TTL aktualisieren
- Task und Arbeitsverzeichnis im Dashboard eingeben
- Terminal-Befehle über das Terminal-Tab ausführen
- Dateibaum-Dateien anklicken und Inhalte lesen oder editieren
- Status des AppBuilder anzeigen lassen
- Memory-Einträge direkt verwalten

## Dokumentationsreferenzen

- `ARCHITECTURE.md`
- `PROJECT_OVERVIEW.md`
- `CHANGELOG.md`
- `dokumentation/`
