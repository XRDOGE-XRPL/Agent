# Validierungsprotokoll und Qualitätskriterien

## 1. Ziel der Validierung

Die Validierung soll sicherstellen, dass der Agent in den wichtigsten Betriebsmodi zuverlässig funktioniert:

- native lokale Ausführung
- Android-Builds mit Unit-Tests
- Ollama-Integration mit lokaler LLM-Ausführung
- robuster Ablauf ohne unkontrollierte Dateisystemeingriffe

## 2. Validierungsstrategie

Das Projekt verwendet eine kombinierte Strategie aus:

- CMake/CTest für native Komponenten
- Gradle/JUnit für Android-Logik
- Python-Compilierung für das Termux-/Local-Runner-Skript
- Betriebsmustertests für die Agentenschleife und den JSON-Protokolldurchlauf

## 3. Native Validierung

### Build

```bash
cmake -S native -B build-native
cmake --build build-native
```

Erwartung:

- keine Compilerfehler
- keine Linkerprobleme
- statische Bibliothek und CLI-Binary werden erzeugt

### CTest

```bash
ctest --test-dir build-native --output-on-failure
```

Typische Prüfungen:

- `dateisystem_test`
- `json_protokoll_test`
- `git_befehl_test`
- `http_url_test`
- `schleife_mock_test`

Diese Tests validieren das Dateisystem, die JSON-Verarbeitung, Git-Interaktion, URL- und HTTP-Handling sowie die Agentenschleife mit einem Mock-LLM.

Seit der Erweiterung der Aktionspipeline wird zusätzlich ein `analysieren`-Schritt im Protokoll validiert. Der Testfall prüft, dass `analysieren` als gültige Aktion erkannt wird, pfadgebundene Sicherheitsprüfungen greifen und bei JSON-Escapes/Unicode-Sequenzen keine Parserfehler entstehen.

### Erfolgsmerkmal

Der gesamte native Build gilt als erfolgreich, wenn:

- alle Testcases bestanden sind
- keine Build-Warnungen auf kritische Laufzeitfehler hindeuten
- die CLI-Hilfe die erwarteten Argumente korrekt ausgibt

## 4. Android-Validierung

### Unit-Tests

```bash
./gradlew :app:testDebugUnitTest
```

Erwartung:

- JUnit-Tests erfolgreich
- keine NullPointer- oder Konfigurationseffekte
- App-Module initialisiert und testbar

In der aktuellen Sandbox-Umgebung war der AGP-Download aus `google()`/`mavenCentral()` blockiert; dadurch scheitert der Gradle-Lauf vor dem eigentlichen Teststart an der Plugin-Auflösung. Die App-Logik selbst ist aber auf AGP 8.7.3 + Gradle 8.9 konfiguriert und mit den vorhandenen Projektdateien kompatibel.

### Debug-APK

```bash
./build_apk.sh
```

Erwartung:

- APK wird im Output-Ordner erzeugt
- native Bibliotheken gebunden
- App-Build funktioniert mit `compileSdk 35` und NDK 27.1.12297006

Der direkte Debug-Build ist in der Sandbox derzeit nur eingeschränkt ausführbar, weil der Android-Plugin-Resolver im Ausführungskontext keine Verbindung zur Google-Maven-Repository-URL herstellen konnte.

## 5. Laufwerk-Validierung

### Python-Syntaxprüfung

```bash
python -m py_compile laufwerk/termux_ollama.py
```

Erwartung:

- keine Syntax- oder Importfehler
- Bedienoberfläche akzeptiert Argumente

### Laufzeit-Check

```bash
python laufwerk/termux_ollama.py --arbeitsverzeichnis "$HOME/werkstatt" --aufgabe "Mini-CMake-Projekt mit Test"
```

Erwartung:

- Agent startet mit Aufgabe
- Dateisystemaktualisierungen erfolgen nur im Zielverzeichnis
- Build-/Testschritte werden protokolliert

## 6. Ollama-/LLM-Validierung

Für echte Modell-Interaktionen:

```text
agentenlauf --arbeitsverzeichnis <pfad> --aufgabe "<text>" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Erwartung:

- Ollama-Endpoint erreichbar
- JSON-Antworten korrekt geparst
- Agentenschritte werden ausgeführt
- Build-/Teststatus wird nach jeder Iteration berücksichtigt

Wenn Ollama nicht erreichbar ist, darf der Agent im Offline-Test- oder Mock-Modus weiterlaufen, aber die LLM-abhängigen Funktionen bleiben dann eingeschränkt.

## 7. Sicherheitsvalidierung

Die Sicherheitslogik darf folgende Regeln nicht verletzen:

- keine Schreibzugriffe außerhalb des Arbeitsverzeichnisses
- keine unkontrollierten Pfade mit `..` oder absoluten Pfadangaben
- keine Ausführung von Git-Commits ohne explizite Freigabe
- keine Anfragen an unzulässige oder öffentliche HTTP-Targets im Standardbetrieb

## 8. Erfolgsstatus

Der Agent gilt als validiert, wenn die folgenden Kriterien erfüllt sind:

1. CMake-Build und CTest werden ohne Fehler durchlaufen
2. Android-Unit-Tests bestehen
3. Debug-APK wird korrekt gebaut
4. Termux-Runner und CLI starten ohne Syntaxfehler
5. Agentenschleife arbeitet im Offline- und LLM-Modus stabil
6. Die Sicherheitsgrenzen für Dateisystem und Git bleiben wirksam

## 8.1 Erweiterte Analyse-/Refactoring-Validierung

Für die neue Analyse-Pipeline gelten zusätzlich diese Qualitätsanforderungen:

- jeder Dateizugriff muss den Pfad-Guard durchlaufen
- relative Pfade sind Standard, absolute oder traversierende Pfade erzeugen einen sofortigen Fehler
- `analysieren` darf nur lesend arbeiten und muss den Dateikontext sauber dokumentieren
- `refactor_check` darf keine unkontrollierten Schreib- oder Löschoperationen auslösen
- JSON-Antworten mit Unicode- oder Escape-Sequenzen müssen vollständig geparst werden, auch wenn sie fragmentarisch oder teilweise fehlerhaft sind

Diese Validierungsregeln sind besonders wichtig für Termux-Instanzen mit begrenzter Ressourcenplanung, da dort lange Iterationsschleifen und wiederholte Build-/Analysezyklen sonst leicht in unvollständigen Antwort- oder Dateibeschädigungsszenarien enden.

## 9. Aktueller Status

Der aktuelle Projektzustand ist als deploybarer Debug-Build mit erfolgreich validierten lokalen Testläufen definiert. Die Agentenschleife kann sowohl mit Mock-Daten als auch mit Ollama in einer echten lokalen Umgebung genutzt werden.

## 10. Qualitätsfazit

Die Kombination aus native C++-Validierung und Android-Grundlagen schafft eine solide Basis für weitere Erweiterungen. Die Dokumentation und die Teststrategie decken dabei die wichtigsten Ablaufpfade ab und machen die Architektur, den Betrieb und die Sicherheitsmaßnahmen für Entwickler nachvollziehbar.
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

