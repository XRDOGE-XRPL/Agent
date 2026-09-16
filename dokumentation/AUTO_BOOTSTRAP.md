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

### Check ohne Installation

```bash
python3 laufwerk/termux_bootstrap.py --check
```

### Safe Mode

```bash
python3 laufwerk/termux_bootstrap.py --check --json
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

## Fazit

Der Bootstrap ist die zentrale Brücke zwischen einer manuellen Installation und einer echten “Termux-first”-Automation. Sobald das Tool auf Termux erkannt wird, versucht es automatisch den kompletten Runtime-Stack zu präparieren und bleibt dabei durch Safe-Mode-Mechaniken robust.
