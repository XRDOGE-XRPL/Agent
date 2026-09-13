# Architektur

```
Oberfläche (Kotlin) ── JNI ── C++-Kern
                               ├ Dateisystem
                               ├ HTTP / Ollama
                               ├ Git
                               ├ Build (CMake, CTest, Gradle)
                               └ Agentenschleife
CLI / Termux-Python ────────────┘
```

Sicherheit: Schreibzugriffe nur innerhalb des Arbeitsverzeichnisses (`pfad_ist_sicher`).
HTTP nur unverschlüsselt zu lokalen Ollama-Endpunkten.

Version: 1.0.0
