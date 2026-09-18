# Ollama-Context-Fix und Stabilisierung des Agenten-Loops

## Überblick

Der lokale Agenten-Loop war in einem Zustand, in dem der Ollama-Server bei jedem Generate-Request das harte interne Prompt-Limit von 2050 Token erreicht hat. Bei einem Repository-Kontext mit mehr als 4700 Token führte das zu einer massiven Trunkierung des Inputs. Dadurch verlor der Agent die wesentlichen Strukturinformationen, interpretierte die unvollständige Antwort fehlerhaft und driftete in eine Endlosschleife aus Parser-Kollisionen, HTTP-Fehlern und Timeout-Abbrüchen.

Die Behebung setzt an der eigentlichen Ursache an: Das Kontextfenster des Servers wird explizit erhöht, die Eingabepayload wird vorab begrenzt und die Request-Parameter werden konsistent mit `options.num_ctx` übergeben.

## Wie: technische Implementierung der Lösung

### 1. Explizites Kontextfenster für Ollama

Im Bootstrap-Pfad `laufwerk/termux_bootstrap.py` wird der Server beim Start nicht mehr nur „blind“ gestartet, sondern mit einem expliziten größeren Kontextfenster:

- `ollama serve --host ... --port ... --num-ctx 4096`
- `OLLAMA_NUM_CTX=4096`
- `OLLAMA_CONTEXT_LENGTH=4096`

Dadurch wird das Standard-Limit von 2050 Token explizit aufgehoben.

### 2. Prompt-Vorabbeschneidung vor der Generation

Der Prompt wurde an den kritischen Stellen vor dem LLM-Aufruf begrenzt, damit kein überlanger Repository-Kontext mehr in die Generation gelangt:

- `laufwerk/termux_ollama.py`:
  - neue Funktion `prompt_begrenzen()`
  - `ollama_anfragen(..., num_ctx=...)`
  - `options: { "num_ctx": ..., "temperature": 0.2 }`
- `app/src/main/java/de/xrdoge/agent/laufzeit/OllamaKlient.kt`:
  - `numCtx` und `maxPromptChars`
  - `begrenzePrompt(prompt, maxPromptChars)`
  - `options` im JSON-Body inklusive `num_ctx`
- `app/src/main/java/de/xrdoge/agent/laufzeit/LocalOllamaClient.kt`:
  - identische Prompt-Begrenzung und `num_ctx`-Übermittlung
- `native/src/ollama.cpp`:
  - sichere Trunkierung vor dem Request
  - `num_ctx` im HTTP-Body
- `native/src/schleife.cpp`:
  - `prompt_begrenzen()` beim Aufbau des Benutzerprompts für die Iteration

### 3. Konfigurations- und CLI-Anpassungen

Die Laufzeitkonfiguration wurde erweitert, um das Verhalten reproduzierbar zu machen:

- `native/include/agent/konfiguration.h`
  - `ollama_num_ctx = 4096`
  - `max_prompt_zeichen = 6000`
- `native/cli/haupteinstieg.cpp`
  - neue Argumente: `--ollama-num-ctx` und `--max-prompt-zeichen`

Damit wird der Agent nicht mehr nur auf implizite Hardware-/Server-Defaults angewiesen.

## Was: betroffene Skripte und Dateien

Die Lösung betraf die folgenden Dateien im Repository:

- `laufwerk/termux_bootstrap.py`
- `laufwerk/termux_ollama.py`
- `app/src/main/java/de/xrdoge/agent/laufzeit/OllamaKlient.kt`
- `app/src/main/java/de/xrdoge/agent/laufzeit/LocalOllamaClient.kt`
- `native/include/agent/konfiguration.h`
- `native/src/ollama.cpp`
- `native/src/schleife.cpp`
- `native/cli/haupteinstieg.cpp`

Die Änderungen sind bewusst klein und lokal auf den LLM-Request- und Startup-Pfad begrenzt, damit keine Seiteneffekte in andere Ablaufbereiche entstehen.

## Wieso: Ursache des Vorfalls

Der eigentliche Fehler lag nicht am Parsing-Mechanismus allein, sondern an der harten, unzureichenden Kontextgrenze des Ollama-Servers:

- Das Server-Log zeigte wiederholt `truncating input prompt limit=2050 prompt=4737`.
- Der komplette Kontext aus Dateistruktur, Aufgabenstellung und Historie war damit auf etwa 2050 Token abgeschnitten.
- Durch die Trunkierung verloren die Modelle die für den nächsten Entscheidungs- und Änderungs-Schritt entscheidenden Informationen.
- Die nachfolgende unbeabsichtigte Ausgabe war oft kein gültiges JSON mehr, wodurch der Agent in der Parser-Kollision landete.

Der direkte technische Fehler war also: Der Server startete mit einem viel zu kleinen Kontextfenster und der Agent übergab einen zu großen Prompt ohne präventive Begrenzung.

## Warum: Ziel der Behebung

Die Maßnahme soll die folgende Kette dauerhaft verhindern:

1. Prompt-Trunkierung durch harte Server-Limits
2. Verlust von Strukturkontext und Aufgabeninformation
3. Nicht mehr parsbare oder unvollständige Modellantworten
4. Parser-Kollisionen im Agenten
5. HTTP-500/Timeout-Abbrüche bei CPU-Inferenz
6. Endlosschleifen und vorzeitige Prozessabbrüche

Durch die Kombination aus größerem `num_ctx` und von vornherein begrenztem Prompt-Kontext bleibt der Agent stabil im Ablauf, kann gültige JSON-Entscheidungen produzieren und arbeitet ohne die wiederholten, fehlerhaften Iterationszyklen weiter.

## Weshalb: Stabilität im lokalen Entwicklungsumfeld

Die lokale Ausführung läuft rein auf der CPU, mit langen Latenzen und deutlichen Zeitfenstern pro Iteration. Gerade deshalb dürfen prompt- und transportseitige Abbruchgrenzen nicht mehr implizit an den Standardwerten hängen.

Die Anpassung garantiert:

- keine unkontrollierte Trunkierung bei Repository-Kontexten mittlerer Größe
- keine fehlerhaften JSON-Antworten durch überladene Prompt-Strings
- robustere Iterationen bei CPU-basierten Inferenzzeiten
- stabilen Agenten-Lauf im lokalen Entwicklungs- und Testbetrieb

## Ergebnis

Der Agent arbeitet jetzt mit einem bewusst konfigurierten, ausreichend großen Kontextfenster und mit modular begrenzten Prompt-Inputs. Dadurch werden die root cause der früheren Endlosschleife und der Kollision zwischen truncation, parser-failure und Timeout-Abbruch wirksam unterbunden.
