# Projektübersicht für GitHub Landing Page

## Titel
Autonomer Entwicklungsagent für lokale Projekte, Android und LLM-basierte Automatisierung

## Kurzbeschreibung

Ein autonomer Entwicklungsagent für C++-, Android- und lokale Projektumgebungen. Er kann Aufgaben eigenständig interpretieren, Dateien bearbeiten, Builds starten, Tests ausführen und die Ergebnisse iterativ verarbeiten.

## Hauptmerkmale

- C++-basierte Agentenlogik mit klarer Schleifenstruktur
- lokale LLM-Integration über Ollama
- Sicherheitsprüfung für Dateisystemzugriffe
- CMake-/Gradle- und Testausführung
- Android- und Termux-Unterstützung
- kontrollierte JSON-Antworten statt unstrukturierter Freitextbefehle

## Warum dieses Projekt?

Viele Tools erschöpfen sich in Vorschlägen oder Chatbot-Antworten. Dieses Projekt geht einen Schritt weiter: Es verbindet künstliche Intelligenz mit echten Projekt- und Buildprozessen und lässt den Agenten direkt in einem definierten Arbeitsbereich arbeiten.

## Kernprinzipien

- Sicherheit vor Freiheit: nur definierte Arbeitsbereiche erlaubt
- nachvollziehbare Ausführung: klare JSON-Schritte statt freie Zustandslogik
- lokale Nutzung: keine vollständige Cloud-Abhängigkeit erforderlich
- skalierbar: CLI, Android und Termux als passende Ausführungsebenen

## Typische Verwendung

```bash
agentenlauf \
  --arbeitsverzeichnis /workspace/projekt \
  --aufgabe "Füge eine kleine Funktion hinzu und prüfe den Build" \
  --modell llama3.2 \
  --ollama-url http://127.0.0.1:11434
```

## Projektstatus

- native Agentenlogik implementiert
- Build- und Test-Workflows integriert
- Android-Projektbasis vorhanden
- Dokumentation für Betrieb und Architektur erweitert
- laufende Weiterentwicklung in Richtung Stabilität und Produktivität

## Vorteile auf einen Blick

- schnell einsetzbar
- lokal kontrollierbar
- für verschiedenen Einsatzumgebungen vorbereitet
- wissenschaftlich und technisch erweiterbar
- gut für Prototyping und autonome Entwicklungsaufgaben geeignet

## Zielbild

Das langfristige Ziel ist ein verlässlicher Entwicklungsassistent, der Aufgaben im Projektkontext selbstständig ausführt, dabei Sicherheitsregeln respektiert und reproduzierbare Ergebnisse liefert.

## Call to Action

- probiere den lokalen Agenten aus
- baue das Projekt lokal mit CMake oder Gradle
- erweitere die Agentenschleife mit neuen Aktionen und Prüfungen
- nutze die vorhandene Architektur als Basis für weitere Experimente

## Erweiterte Agenten- und Sicherheitsfunktionen

Die jüngste Entwicklungsphase erweitert den Agenten um mehrere praxisrelevante Funktionen:

- sichere Analyse- und Refactoring-Schritte mit zentralem Pfad-Guard und klaren Fehlerpfaden
- Status- und Loganzeigen für aktuelle Build-/Analysephasen im Android-/Compose-Frontend
- robustes JSON- und Unicode-Handling, damit teilweise defekte Modellantworten die Laufzeit nicht lahmlegen
- Offline-/Termux-Stresstests mit wiederholten Iterationsketten, Buildzyklen und Fehlerbehandlungen
- konsistente Doku- und Validierungslogik, damit Architektur, Sicherheit und Nutzung immer synchron bleiben

Damit wird das Projekt nicht nur als LLM-Wrapper, sondern als realer, kontrollierbarer Entwicklungsagent mit mobilem UI und sicherem Workspace-Containment positioniert.

## Kurzfazit

Der autonome Entwicklungsagent verbindet KI, Buildautomatisierung, Sicherheit und Projektkontext in einem einzigen System. Damit ist er ein starkes Grundgerüst für moderne, lokale und mobile Entwicklungshilfe.
