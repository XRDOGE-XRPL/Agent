# Roadmap und geplante Erweiterungen

## Überblick

Dieses Repository ist bereits als echter Entwicklungsagent mit nativer C++-Basis, Android-Frontend und lokaler Ollama-Integration konzipiert. Die Roadmap fokussiert sich auf die wichtigsten nächsten Schritte, damit das Projekt aus einem funktionsfähigen Prototypen zu einer robusten, produktiven Plattform wird.

## Aktueller Status

Der aktuelle Stand umfasst:

- native C++-Agentenlogik mit CLI-Startpunkt
- Dateisystemschutz und Pfadvalidierung
- Build-, Test- und Git-Integration
- Android-App-Basis mit Compose und NDK-Anbindung
- lokaler Runner für Termux/Linux
- Dokumentation für Architektur, Betrieb und Validierung

## Kurzfristige Ziele (nächste Iterationen)

### 1. Stabilisierung der Agentenschleife

- robustere Behandlung von teilweise fehlerhaften JSON-Antworten
- bessere Fehlererkennung bei fehlenden Modellantworten
- klarere Rückgabewerte für erfolgreiche und fehlgeschlagene Schritte
- bessere Wiederholungslogik nach Build- oder Testfehlern

### 2. Sicherheits- und Audit-Erweiterungen

- detaillierte Protokollierung aller Dateisystem-Operationen
- Zugriffsbeschränkung mit strengeren Regeln für Schreib-/Löschbefehle
- zusätzliche Prüfungen für ungesicherte oder bösartige Einträge im Modellprompt
- Statuslogik für erlaubte und blockierte Vorgänge

### 3. Projekt- und Kontextverwaltung

- bessere Analyse des bestehenden Projektkontexts
- Auswahl relevanter Dateien statt vollständiger Dateiliste
- Kontextfenster für größere Repositories
- Memento-/Memories-Mechanismus für projektbezogene Erkenntnisse

### 4. Android- und UI-Verbesserungen

- erweiterte Statusanzeige für laufende Agentenschritte
- Live-Log-Streaming im UI
- Konfigurationsdialog für Modell, URL, Arbeitsverzeichnis und Sicherheitsoptionen
- bessere Fehler- und Wiederherstellungsmechanik im App-Workflow

## Mittelfristige Ziele

### 1. Erweiterte Tool- und Workflow-Integration

- zusätzlicher Ablauf für Test-Case-Erzeugung und automatische Korrektur
- Integration von Standard-Entwicklungswerkzeugen wie `git diff`, `grep`, `make` und `ninja`
- Unterstützung weiterer LLM-Backends neben Ollama

### 2. Bessere Multi-Repository-Unterstützung

- mehrere Arbeitsbereiche gleichzeitig verwalten
- Wechsel zwischen Prototyp-, Test- und Produktkonfigurationen
- Projekt-spezifische Trust- und Safety-Regeln

### 3. Produktionsreife

- konfigurierbare Berechtigungen
- auditierbare Aktivitätslogs
- sichere Standard-Policy-Dateien
- gezielte Fehler- und Statusberichte für CI/CD-Umgebungen

## Langfristige Ziele

### 1. Autonomer Entwicklungsassistent mit Kontextgedächtnis

Der Agent soll nicht nur Befehle ausführen, sondern auch langfristig Projektwissen aufbauen. Dazu gehören:

- vorhandene Architekturkenntnisse
- Nutzungsmuster und typische Fehlerquellen
- wiederverwendbare Sicherheitsregeln
- Lernprozesse über Aufgaben und Änderungen

### 2. Erweiterte Kollaborationsfunktionen

- gemeinsame Projektarbeitskontexte
- Reviewer-Feedback integration
- strukturierte Veränderungsvorschläge mit Commit- und PR-Logik

### 3. Enterprise- und Team-Use-Cases

- Multi-User-Konfigurationen
- Team- oder Organisationseinstellungen
- Kontinuierliche Validierung in CI
- governance-konforme Sicherheitsschwellen

## Priorisierung

Die höchste Priorität hat derzeit:

1. Sicherheitsstabilität
2. Skalierbarkeit der Agentenschleife
3. bessere Kontextverarbeitung
4. Android-Produktivitätsfeatures
5. erweiterte Tool-Integration

## Metriken für Erfolg

Ein erfolgreicher Fortschritt kann anhand dieser Kriterien gemessen werden:

- reduzierter Fehlerquotient bei unvollständigen JSON-Antworten
- stabilere Builds in CLI- und Android-Modus
- niedrigeres Risiko bei Pfad- und Dateisystemzugriffen
- bessere Durchlaufzeit und weniger Iterations-Fehler
- höhere Zuverlässigkeit bei Projekten mit mehreren Dateien

## Fazit

Die Roadmap ist darauf ausgerichtet, den Agenten von einem experimentellen Projekt zu einer verlässlichen Entwicklungsplattform zu machen. Die wichtigsten Hebel sind dabei Sicherheit, Kontextqualität, Stabilität und eine klare Benutzeroberfläche auf Android und CLI-Ebene.
