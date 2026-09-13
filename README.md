# Autonomer Entwicklungsagent

Vollständiger Entwicklungsagent für Android und lokale Umgebungen (Windows, Linux, Termux).
Der Agent schreibt Code, baut Projekte, führt Tests aus und nutzt optionales lokales LLM (Ollama).

## Bestandteile

- **Nativer C++-Kern** mit Dateisystem, HTTP/Ollama, Git, CMake/CTest/Gradle und Agentenschleife
- **JNI-Brücke** und Kotlin-Oberfläche für Android
- **CLI** `agentenlauf` für den lokalen Betrieb
- **Termux-Laufwerk** `laufwerk/termux_ollama.py`
- **CTest- und JUnit-Suiten** ohne Platzhalter

## Schnellstart (lokal)

```powershell
cmake -S native -B build-native -G "MinGW Makefiles"
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

Oder: `skripte/lokal_bauen.ps1` (Windows) bzw. `skripte/lokal_bauen.sh` (Termux).

## Agentenschleife

```text
agentenlauf --arbeitsverzeichnis <pfad> --aufgabe "<text>" --ollama-url http://127.0.0.1:11434 --modell llama3.2
```

Ollama muss lokal laufen. Ohne Modell bleiben CTest und Dateisystemtests vollständig offline ausführbar.

## Android

SDK-Pfad in `local.properties` (`sdk.dir`). Danach:

```text
gradle :app:assembleDebug :app:testDebugUnitTest
```

## Protokoll

Das Modell antwortet ausschließlich mit JSON:

```json
{
  "schritte": [
    {"aktion": "schreiben", "pfad": "src/datei.cpp", "inhalt": "...", "begruendung": "…"},
    {"aktion": "testen"},
    {"aktion": "fertig", "begruendung": "Tests sind grün"}
  ]
}
```

## Definition of Done

1. CMake-Build und CTest ohne Fehler
2. Android-Unit-Tests grün
3. Debug-APK erzeugbar
4. Dokumentation aktuell
