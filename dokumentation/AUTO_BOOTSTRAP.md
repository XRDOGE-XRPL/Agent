# Auto-Bootstrap für Termux und Android-Laufzeit

## Ziel

Dieses Handbuch beschreibt den vollständigen Auto-Bootstrap-Flow für die Runtime auf Termux und Android-Umgebungen. Der zentrale Gedanke ist: Sobald das Tool auf dem Gerät läuft, soll es die komplette Laufzeitumgebung automatisch vorbereiten, statt dass Nutzer manuell jede Abhängigkeit einzeln nachinstallieren müssen.

## Grundprinzip

Der Bootstrap-Layer prüft bei jedem Start die Umgebung und führt dann die folgenden Schritte automatisch aus:

1. Umgebung erkennen (Termux, Android, Architektur, Speicher, Betriebssystem)
2. benötigte Pakete prüfen
3. fehlende Pakete automatisch installieren
4. Verzeichnisstruktur in `~/.agent` aufbauen
5. Konfigurationsdateien erzeugen
6. Python-Umgebung initialisieren
7. Ollama-Status prüfen und ggf. starten oder safe-mode fallen lassen
8. lokale Socket-/Runtime-Komponenten validieren
9. Agentenlauf starten oder in einen sicheren Degradationsmode gehen

## Ablauf

### 1. Umgebungserkennung

Der Bootstrap erkennt:

- Termux aktiv ja/nein
- Android-/Linux-Kontext
- CPU-/Architekturtyp
- RAM/Memory-Status
- Pfade für Konfiguration und Logs

### 2. Paket-Check

Die Standardpakete für den automatischen Bootstrap sind:

- `git`
- `cmake`
- `clang`
- `python`
- `make`
- `curl`
- `wget`
- `openssl`
- `termux-api`
- optional: `nodejs`
- optional: `jq`

Fehlende Pakete werden automatisch mittels `pkg install -y ...` ergänzt.

### 3. Runtime-Verzeichnisse

Der Bootstrap legt die Standardstruktur an:

```text
~/.agent/
├── config.json
├── status.json
├── logs/
├── venv/
└── runtime/
```

## Verwendung

### Direkt per Python

```bash
python3 laufwerk/termux_bootstrap.py --bootstrap
```

### Healthcheck ohne Installation

```bash
python3 laufwerk/termux_bootstrap.py --healthcheck
```

### Safe Mode

```bash
python3 laufwerk/termux_bootstrap.py --healthcheck --json
```

### Wrapper-Skript

```bash
bash skripte/termux_setup.sh
```

## Ollama-Integration

Der Bootstrap prüft:

- ist `ollama` installiert?
- falls nicht: installierbare OLLAMA-Variante versuchen
- ist das Standardmodell vorhanden?
- ist der Port `127.0.0.1:11434` erreichbar?

Wenn Ollama nicht verfügbar ist, bleibt der Agent im Safe-Mode und funktioniert ohne LLM-Anbindung, statt abstürzen zu müssen.

## Sicherheitsprinzipien

Der Bootstrap arbeitet bewusst mit folgenden Regeln:

- nur im Benutzerbereich installieren
- keine Root-Operationen ohne Notwendigkeit
- keine Schreibzugriffe außerhalb des definierten Arbeitsverzeichnisses
- keine unkontrollierten Shell-Befehle ohne abgedeckte Fallback-Logik
- alle Zustände werden in `~/.agent` protokolliert

## Dashboard-Integration

Der Runtime-Status soll im Dashboard sichtbar sein, damit der Nutzer z. B. den nachfolgenden Zustand erkennt:

- Bootstrap aktiv
- Abhängigkeiten prüfen
- Termux runtime ready
- Safe mode aktiv
- Ollama bereit

## Vorteile des Auto-Bootstraps

- keine manuelle Paket-Einzelinstallation mehr
- schneller erster Start auf Termux
- konsistente Laufzeitumgebung
- besseres User Experience in Android-/Termux-Umgebungen
- Automatische Fallback-Strategie bei unvollständigen Setups

## Erweiterte Runtime- und Offline-Validierung

Der Bootstrap ist nicht nur für die Installation zuständig, sondern auch für die Validierung der laufenden Agentenlogik. Wichtige Aspekte sind:

- Start des Agenten im sicheren Modus, wenn Ollama oder kritische Laufzeitkomponenten fehlen
- nachvollziehbare Statusmeldungen für `bootstrap`, `dependencies`, `runtime-check`, `ollama`, `safe-mode` und `ready`
- Wiederholungs- und Fehlerbehandlungslogik für längere Iterationsketten in Android-/Termux-Umgebungen
- strikte Vermeidung von Pfadtraversalen oder unkontrollierten Schreibzugriffen außerhalb des Arbeitsbereichs
- Unterstützung für offline-validierte Builds, wenn das LLM-Backend nicht verfügbar ist

Dadurch bleibt der Agent in eingeschränkten mobilen Umgebungen stabil, selbst wenn Modellzugriff, Paketinstallation oder lokale Shell-Ausführungen nicht vollständig verfügbar sind.

## Fazit

Der Bootstrap ist die zentrale Brücke zwischen einer manuellen Installation und einer echten “Termux-first”-Automation. Sobald das Tool auf Termux erkannt wird, versucht es automatisch den kompletten Runtime-Stack zu präparieren und bleibt dabei durch Safe-Mode-Mechaniken robust.
## Hybrides Build-Setup: native Termux + Proot-Fallback

Für Android-/Gradle-Builds ist native Termux der primäre Host, weil dort Java 21, Android SDK, Gradle-Caches und temporäre Dateien direkt auf dem nativen Dateisystem laufen und damit den PRoot-/Syscall-Overhead vermeiden. Proot-Debian bleibt als kompatibler Fallback für isolierte Debian-spezifische Aufgaben oder Validierungsprüfungen bestehen.

Empfohlener Standard:

```bash
./termux_native_setup.sh
# oder direkt in Termux:
cd /data/data/com.termux/files/home/Agent
./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1   -Dorg.gradle.daemon=false   -Dorg.gradle.parallel=false   -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'
```

Das Projekt nutzt `JAVA_HOME` und `ANDROID_HOME` aus der nativen Termux-Umgebung und setzt die Gradle-Parameter für ressourcenbeschränkte ARM64-/Proot-Umgebungen automatisch. Proot-Debian wird nur noch für kompatibilitätsorientierte oder isolierte Setup-/Testschritte verwendet.

