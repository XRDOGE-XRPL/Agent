# HowTo: Samsung Galaxy S24 + Termux + Ollama + Android-Setup

## Ziel

Dieses Handbuch beschreibt den vollständigen manuellen Setup- und Verifikationspfad für die reale Nutzung des autonomen Agenten auf einem Samsung Galaxy S24. Es deckt die Kombination aus:

- Android-Entwicklungsumgebung
- Samsung-S24-Gerät mit USB-Debugging
- Termux als lokaler Linux-/Shell-Container
- Ollama als lokale LLM-Inferenz-Engine
- `LocalSocketBridge` und Kotlin-/Compose-App-Integration
- Verifikation via `Dispatchers.IO` und echten lokalen Shell-/Prozessaufrufen

## 1. Voraussetzungen

### Hardware und Umgebung

- Samsung Galaxy S24 oder vergleichbares Android-Gerät
- USB-Kabel für direkte Verbindung zum Rechner
- Computer mit Android Studio / SDK / Java 21 in Proot-Debian (die App selbst zielt auf Java 17)
- Zugriff auf Termux im Play Store oder F-Droid
- Optionale Internetverbindung für APK-Download und Modell-Installation

### Software

- Android Studio (empfohlen: aktuelle stabile Version)
- Android SDK + NDK
- Java 21 in Proot-Debian; Android Studio kann zusätzlich JDK 17+ nutzen
- Git
- CMake und C++-Toolchain für native Builds
- Termux
- Ollama auf dem Gerät oder im lokalen Android-Setup

## 2. Android-Gerät vorbereiten

### 2.1 Entwicklermodus aktivieren

Auf dem Samsung S24:

1. Einstellungen öffnen
2. Über das Telefon / Informationen gehen
3. Buildnummer 7-mal tippen
4. Entwicklertools aktivieren
5. USB-Debugging einschalten

### 2.2 Geräte per USB verbinden

Auf dem Rechner:

```bash
adb devices
```

Erwartung:

- Gerät wird als verbunden angezeigt
- ggf. auf dem Gerät die USB-Debugging-Bestätigung akzeptieren

### 2.3 ADB und SDK prüfen

```bash
adb version
```

Wenn kein Gerät erkannt wird:

- USB-Übertragungsmodus aktivieren
- Verbindung erneut prüfen
- ADB-Server neu starten:

```bash
adb kill-server
adb start-server
adb devices
```

## 3. Android SDK und Projekt konfigurieren

### 3.1 Android Studio installieren

- Android Studio installieren
- SDK Manager öffnen
- Android SDK Platform 35 installieren
- Android SDK Build-Tools installieren
- NDK 27.1.12297006 installieren

### 3.2 local.properties setzen

Im Projektstamm:

```properties
sdk.dir=/opt/android-sdk
```

Beispiel:

```properties
sdk.dir=/opt/android-sdk
```

Auf Linux/macOS:

```properties
sdk.dir=/opt/android-sdk
```

### 3.3 Projekt aufbauen

Im Repository:

```bash
./build_apk.sh
./gradlew :app:testDebugUnitTest
```

Wenn die Android-Plugin-Resolveren im lokalen Kontext korrekt funktionieren, sollte der App-Build jetzt durchlaufen.

## 4. Termux installieren und konfigurieren

### 4.1 Termux installieren

- Termux aus dem Store installieren
- Starten und ein Terminalfenster öffnen

### 4.2 Automatischer Bootstrap nutzen

Das Projekt enthält einen eigenen Bootstrap-Layer, der die Termux-Runtime automatisch vorbereitet:

```bash
python3 laufwerk/termux_bootstrap.py --bootstrap
```

oder:

```bash
bash skripte/termux_setup.sh
```

Der Bootstrap prüft automatisch die Termux-/Android-Umgebung, installiert fehlende Basis-Pakete (`git`, `cmake`, `clang`, `python`, `make`, `curl`, `wget`, `openssl`, `termux-api`, `nodejs`, `jq`), erstellt `~/.agent` für Konfiguration und Runtime-Status, initialisiert die Proot-Debian-Umgebung und startet Ollama dort als festen Bestandteil des Laufzeitstacks.

### 4.3 Grundpakete manuell installieren

```bash
pkg update
pkg install git curl cmake clang python make openssh
```

Empfohlen für die Proot-Umgebung:

```bash
pkg install nodejs-lts wget vim
```

### 4.3 Arbeitsverzeichnis vorbereiten

```bash
mkdir -p $HOME/workspace
cd $HOME/workspace
```

## 5. Ollama lokal auf dem Gerät einrichten

Es gibt zwei praktische Wege:

1. Offizielle Ollama-Installation auf dem Android-Gerät
2. Lokaler Ollama-Server im Termux-Container, sofern die lokale Runtime dafür unterstützt wird

### 5.1 Installationspfad A: Ollama im Proot-Debian

Dort, wo das Projekt wirklich ausgeführt wird, muss Ollama im Proot-Debian-Userland laufen:

```bash
curl -fsSL https://ollama.com/install.sh | sh
nohup ollama serve >/tmp/ollama-proot.log 2>&1 &
```

Danach prüfen:

```bash
ollama --version
```

### 5.2 Modell laden

```bash
ollama pull llama3.2
```

Oder ein vergleichbares lokales Modell nach Bedarf.

### 5.3 Server starten

```bash
ollama serve
```

Das Ziel ist ein lokaler Endpunkt wie:

```text
http://127.0.0.1:11434
```

Wenn die App und Ollama auf demselben Gerät laufen, ist das der Standardpfad zur lokalen Anbindung.

## 6. Termux mit dem Projekt verbinden

Die App erwartet in der Regel eine lokale Verbindung zwischen Android- und Termux-/Shell-Ausführung. Die zentrale Logik liegt in:

- `LocalSocketBridge`
- `LocalOllamaClient`
- `AgentRuntime`
- `Dispatchers.IO`-basierte Prozessausführung

### 6.1 Lokale Socket-Bridge verstehen

`LocalSocketBridge` verwendet eine lokale Verbindung auf dem Gerät:

- Host: `127.0.0.1`
- Port: Standardmäßig 5050
- Retries und Backoff für Fehlschläge
- `Dispatchers.IO` für Socket- und Process-Aufrufe

Damit der lokale Prozessfluss funktioniert, muss der dafür vorgesehene Backend-Prozess auf dem Gerät aktiv sein und den Port akzeptieren.

### 6.2 Beispiel-Workflow in Termux

```bash
python3 -m venv $HOME/venv
source $HOME/venv/bin/activate
python -V
```

Danach das Projekt als Arbeitsbereich verwenden oder über eine lokale Script-Umgebung den Agenten starten:

```bash
cd $HOME/workspace
python laufwerk/termux_ollama.py \
  --arbeitsverzeichnis "$HOME/workspace/project" \
  --aufgabe "Erstelle eine kleine CMake-Validierung mit Test" \
  --ollama-url http://127.0.0.1:11434 \
  --modell llama3.2
```

## 7. Android-App mit realem Gerät verbinden

### 7.1 App installieren

```bash
./gradlew :app:installDebug
```

Oder direkt aus Android Studio auf das S24-Gerät installieren.

### 7.2 App starten

- App öffnen
- `AgentDashboard` starten
- Arbeitsverzeichnis festlegen
- Ollama-URL auf `http://127.0.0.1:11434` setzen
- Modellnamen eintragen, z. B. `llama3.2`
- Agent starten

### 7.3 Erwartetes Verhalten

- `LocalSocketBridge` versucht die lokale Verbindung
- `Dispatchers.IO` führt die Anfrage und Shell-Ausführung aus
- `Ollama` liefert eine Rückmeldung als JSON-/Text-Antwort
- Agent-Schritte werden im Dashboard und Logstream dargestellt

## 8. Verifikation der echten End-to-End-Kette

### 8.1 App-Level-Verifikation

In der App prüfen:

- Dashboard rendert korrekt
- Ollama-Check meldet erfolgreiche Verbindung
- aufgerufene Tasks laufen im IO-Dispatcher
- Logs erscheinen im `LogStreamManager`

### 8.2 Prozess-Level-Verifikation

Auf dem Gerät/Termux:

```bash
curl http://127.0.0.1:11434/api/tags
```

Erwartung:

- Modell-Informationen werden zurückgegeben

Zusätzlich Testlauf:

```bash
ollama run llama3.2 "Hallo, prüfe die Verbindung."
```

### 8.3 Shell-Verifikation

```bash
echo "test" && ls -la
```

Erwartung:

- Shell-Ausgabe erscheint konsistent im Lauf und in den Logs

## 9. ExecutionRouter und EphemeralServiceManager testen

Die App enthält Test- und Laufzeitlogik für:

- `ExecutionRouter`
- `EphemeralServiceManager`

Die wichtigsten realen Bedingungen sind:

- WLAN-/Mobilfunkwechsel
- Zeitüberschreitungen
- TTL-Abläufe
- mehrfaches Provisionieren und sofortiges Kill/Cleanup

Empfohlener Testlauf im Android-Kontext:

1. App startet
2. mehrere Jobs mit unterschiedlichen Komplexitätsstufen werden ausgelöst
3. Router wählt lokale vs. tower vs. partner basierend auf Telemetrie
4. TTL-Lebenszyklen werden beobachtet
5. Service wird nach Ablauf sauber bereinigt

## 10. Selbst dokumentierende Loop und Markdown-Sync

Das Projekt enthält bereits eine Dokumentations-Pipeline für:

- `README.md`
- `ARCHITEKTUR.md`
- `PROJECT_OVERVIEW.md` (falls ergänzt)
- `CHANGELOG.md`
- weitere `dokumentation/*.md` Dateien

Für den realen Pflege-Workflow:

1. Code oder Projektstrukturen ändern
2. relevante Doku aktualisieren
3. Build-/Testlauf erneut ausführen
4. Dashboard-Dokumentations-Tab prüfen
5. Markdown-Rendern verifizieren

Wenn die Dokumentationsdateien im Dashboard angezeigt werden, muss der UI-Renderpfad sie sauber in der Oberfläche anzeigen können.

## 11. Troubleshooting

### App kann keine Verbindung zu Ollama herstellen

Prüfen:

- Ollama läuft lokal
- Port 11434 erreichbar
- URL korrekt
- Modell wurde heruntergeladen
- `Dispatchers.IO` nicht blockiert

### Termux läuft nicht

Prüfen:

- Termux installiert und gestartet
- `pkg install` vollständig durchgelaufen
- permission / stored data korrekt

### LocalSocketBridge meldet Fehler

Prüfen:

- Port frei
- Process server läuft
- `127.0.0.1` erreichbar
- keine Firewall- oder Android-Restriktionen

### Build schlägt fehl

Prüfen:

```bash
./gradlew --version
./build_apk.sh
```

Wichtige Punkte:

- Android SDK installiert
- NDK vorhanden
- `local.properties` korrekt
- Java-Version 17+

## 11.1 Erweiterte Sicherheits- und Laufzeitvalidierung auf Samsung S24

Für reale lokale Tests auf dem Galaxy S24 gelten zusätzlich diese Regeln:

- jedes Projekt- und Dateischritt muss im definierten Workspace bleiben
- das `LocalSocketBridge` darf keine absoluten oder traversierenden Pfade zu Termux- oder Android-Dateien akzeptieren
- Build-/Analysezyklen werden in mehreren Iterationen ausgeführt, damit Status-, Log- und Fehlerpfad wiederholt validiert werden
- der UI-Layer zeigt nicht nur den letzten Zustand, sondern auch die letzte Modellantwort, Laufzeitphase und Sicherheitswarnung an
- Modellantworten mit Unicode- oder Escape-Sprüngen müssen im Kotlin-/JSON-Parser robust dekodiert werden, ohne das UI oder die JNI-Logik zu beschädigen

Diesen Prüfungen sollte vor jedem realen Produktionseinsatz ein vollständiger Offline- und Online-Testlauf vorausgehen.

## 12. Empfohlener End-to-End-Checkliste

### Vor dem Lauf

- [ ] Android Studio installiert
- [ ] SDK + NDK gesetzt
- [ ] ADB-Gerät verbunden
- [ ] Termux installiert
- [ ] Ollama gestartet
- [ ] Modell geladen
- [ ] `local.properties` gesetzt

### Beim Lauf

- [ ] App auf S24 installiert
- [ ] Dashboard geöffnet
- [ ] Ollama-Check erfolgreich
- [ ] Agent starten möglich
- [ ] Shell-/Prozess-Ausgabe erscheint
- [ ] Logs und Status korrekt

### Nach dem Lauf

- [ ] Build erfolgreich
- [ ] Tests grün
- [ ] Service-Lifecycle sauber beendet
- [ ] Dokumentations-Tab korrekt gerendert
- [ ] keine Lecks oder blockierten IO-Dispatcher-Ausführungen

## 13. Fazit

Die echte S24- und Termux-Integration nutzt die vorhandenen Bestandteile des Projekts in einer realen Mobilumgebung: Android-App, lokale Shell-/Process-Ausführung, Ollama-Inferenz, lokaler Socket-Pfad und dokumentationsbasierte UI. Wenn die nachstehende Checkliste sauber durchlaufen wird, ist der Agent in einem echten Gerät-Kontext vollständig nutzbar.
## Proot-Debian-Build (verpflichtend)

Android- und JNI-Builds dürfen auf dem Termux-Host nicht direkt ausgeführt werden. Der Host nutzt Bionic/Perfetto- und Kernel-Restriktionen, die bei `SIGABRT`/JNI-Abstürzen und Gradle-Lifecycle-Problemen auftreten können. Der robuste und reproduzierbare Weg ist ein isolierter Proot-Debian-Container mit Java 21, Android SDK unter `/opt/android-sdk` und der lokalen Gradle-Ausführung dort.

```bash
# Beispiel: Android SDK unter /opt/android-sdk
mkdir -p /opt/android-sdk
cat > local.properties <<'EOF'
sdk.dir=/opt/android-sdk
EOF
./build_apk.sh
```

Das Repository erwartet `sdk.dir=/opt/android-sdk` im Projektstamm. Der direkte Host-Build bleibt in Termux deaktiviert.

