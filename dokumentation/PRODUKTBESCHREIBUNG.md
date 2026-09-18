# Produktbeschreibung für Endkunden und Interessenten

## Einleitung

Der autonome Entwicklungsagent ist ein Werkzeug für Projekte, die schnell, zuverlässig und weitgehend automatisiert entwickelt, gebaut und validiert werden sollen. Er kombiniert die Fähigkeiten eines lokalen Entwicklungsassistenten mit einem kontrollierten Ablauf aus Dateisystemzugriff, Git-Checks, Build-Ausführung und Testvalidierung.

## Kernidee

Anstatt nur ein Chat-Interface zu bieten, arbeitet der Agent direkt im Projektkontext. Er kann Dateien lesen und ändern, Builds starten, Tests ausführen und Feedback aus dem Ergebnis in die nächste Iteration übernehmen. Dadurch wird der Agent zu einer praktischen Begleitfunktion für Entwickler, Teams und mobile Nutzungsszenarien.

## Zielgruppen

### 1. Einzelentwickler

Entwickler, die schnell Prototypen, kleine Features oder CMake-/Gradle-basierte Projekte automatisiert erweitern wollen.

### 2. Teams mit lokaler Entwicklung

Teams, die Versionierung, Projektstruktur und Validierung kontrolliert automatisieren möchten, ohne beliebige Dateisystemzugriffe zu erlauben.

### 3. Mobile und Android-Workflows

Nutzer, die einen Agenten im Android-Kontext ausführen oder lokal über Termux/CLI arbeiten möchten.

### 4. Forschung und Prototyping

Umgebungen, in denen neue Agent-Workflows, LLM-Interaktion und Build-/Test-Automatisierung schnell experimentell getestet werden sollen.

## Hauptvorteile

### Automatisierung ohne vollständige Freigabe des Dateisystems

Die Sicherheitsarchitektur begrenzt Zugriffe auf das definierte Arbeitsverzeichnis. Dadurch wird das Risiko unverantwortlicher oder unkontrollierter Änderungen reduziert.

### Lokale LLM-Unterstützung

Die Integration mit Ollama ermöglicht die Verwendung eines lokalen Modells, ohne vollständig auf öffentliche Cloud-Dienste angewiesen zu sein.

### Mehrere Betriebsumgebungen

Der Agent kann lokal im Terminal, in Termux und in einer Android-App laufen. Dadurch eignet er sich für verschiedene Arbeitsmodelle und Geräte.

### Strukturierte und nachvollziehbare Ausführung

Die Agentenlogik arbeitet mit klaren Befehlen und JSON-Protokollen statt mit unstrukturierten Freitextbefehlen. Das macht Fehler leichter überprüfbar und die Ausführung stabiler.

## Schlüssel-Funktionen

- Bearbeitung von Projektdateien im definierten Arbeitsbereich
- CMake- und Gradle-Ausführung
- Teststart und Auswertung
- Git-Status und Commit-Optionen
- Sicherheitsvalidierung für Pfade und Dateizugriffe
- lokale Integrationen mit Ollama
- Android- und CLI-Ausführung

## Typische Nutzungsszenarien

### Szenario 1: Feature erweitern

Ein Entwickler gibt eine Aufgabenbeschreibung ein. Der Agent prüft die vorhandene Struktur, ergänzt den passenden Code, läuft Build und Tests und liefert ein konsistentes Ergebnis zurück.

### Szenario 2: Prototyp mit Terminal-Arbeitsfluss

In einem lokalen C++- oder CMake-Projekt wird eine neue Funktion angefordert. Der Agent kann den Workflow autonom über mehrere Iterationen steuern.

### Szenario 3: Mobile Testerlebnis

Die Android-App nutzt denselben Arbeitsansatz für Build- und Testbetrieb. Damit wird ein einfacher Überblick über Status und Ausführung im mobilen Kontext ermöglicht.

### Szenario 4: Offline- oder Testbetrieb

Wenn kein LLM verfügbar ist, bleibt die Anwendung in einem kontrollierten Offline-/Mock-Modus nutzbar. Das ist wichtig für CI-Umgebungen, lokale Tests und Entwicklung ohne externe Abhängigkeit.

## Warum dieses Produkt relevant ist

In modernen Entwicklungsprozessen wird schnell klar: Es gibt einen hohen Bedarf an Werkzeugen, die nicht nur Code vorschlagen, sondern tatsächlich in der Projektsituation arbeiten. Dieses Produkt bedient genau diesen Bedarf, indem es einen autonomen, aber kontrollierbaren Agenten bereitstellt.

## Produktwert

Das Projekt schafft einen Mehrwert durch:

- Zeitersparnis bei Routine-Aufgaben
- schnellere Iteration bei Builds und Tests
- konsequente Ausführung in klaren, überprüfbaren Schritten
- lokales Arbeiten ohne vollständige Cloud-Abhängigkeit
- gute Grundlage für spätere Erweiterungen in AI-Assistenz und DevOps-Automation

## Risiken und Grenzen

Ein autonomer Agent kann nicht unbegrenzt vertraut werden. Die wichtigsten Grenzen sind:

- die Notwendigkeit klarer Aufgaben und Sicherheitsregeln
- starke Abhängigkeit von den vorhandenen Build- und Test-Workflows
- Einschränkungen bei unklaren oder zu großen Projektkontexten
- Bedarf an weiteren Validierungs- und Sicherheitsmechanismen in späteren Versionen

## Erweiterte Produktmerkmale für Analyse, Status und Stabilität

Das Produkt wurde erweitert um eine durchdachte Analyse- und Statusschicht:

- `analysieren` erkennt und validiert Projektkontext, Dateistrukturen und kleinere Code-Bausteine ohne unkontrollierte Schreibzugriffe
- der Laufzeitstatus zeigt die aktuelle Iteration, aktive Phase und Sicherheitswarnungen direkt im UI oder in der console-basierten Laufzeit an
- die mobile Oberfläche kann Live-Logs der JNI-/Bridge-Schicht, Modellantworten und Fehlerzustände visualisieren, wodurch Android- und Termux-Nutzung transparent bleibt
- lokale Offline- und Safe-Mode-Fallbacks schützen vor fehlenden LLM-Backends oder unvollständigen Setup-Zuständen
- die robuste JSON-Behandlung verhindert, dass teilweise kaputte Modellantworten den Agentenlauf oder die UI-Schicht aus dem Takt bringen

Diese Ergänzungen machen das Produkt nicht nur stärker für Entwicklungsautomatisierung, sondern auch für reale Nutzung in begrenzten oder ressourcenarmen Umgebungen.

## Fazit

Der autonome Entwicklungsagent ist ein praxisnahes Werkzeug für moderne Softwareentwicklung: lokal nutzbar, Android-fähig, sicher durch Pfadbegrenzung und geeignet für automatisierte Build- und Test-Workflows. Die Kombination aus Kontrolle, LLM-Unterstützung und Projekt-Integration macht das Projekt für Prototyping, lokale Automatisierung und spätere Produktivitäts-Workflows relevant.
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

