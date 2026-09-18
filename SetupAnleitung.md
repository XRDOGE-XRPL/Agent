# Setup-Anleitung für den Agent

## Ziel

Diese Anleitung beschreibt den vollständigen, validierten Setup-Pfad für den Agent im Repository `XRDOGE-XRPL/Agent`. Sie bündelt:

- den offiziellen Projekt-Setup-Stil
- die gemachten Fehler und die dazugehörigen Ursachen
- die endgültigen, funktionierenden Befehle
- die passende Architektur- und Toolchain-Strategie für ARM64/PRoot-Umgebungen

Das Projekt ist in einer hybriden Umgebung konzipiert:

- native Termux-Umgebung = primärer Host für Android-/Gradle-Workflows
- Proot-Debian = kompatibler Fallback für isolierte Debian-/Build-Checks
- native CMake/CTest = separates Host-Setup für den `native/`-Build

---

## 1. Verlauf: Was wir im Projekt herausgefunden haben

Im Verlauf der Einrichtung wurden mehrere Probleme auf dieselbe Grundursache zurückgeführt:

### 1.1 Falscher NDK/Toolchain-Architektur-Mismatch

Der häufigste Fehler war nicht ein Codefehler im Projekt, sondern ein Architektur-Mismatch.

Beispiele:

- x86_64-Android-NDK wurde in einer ARM64-/PRoot-Umgebung verwendet
- `CC` oder `CXX` zeigte auf veraltete `qemu-bin/clang++`-Pfad-Wrapper
- CMake sah eine gültige NDK-Datei, aber der tatsächliche Compiler war für die falsche Architektur gebaut

Folge:

- `Illegal instruction`
- CMake bricht beim Compiler-Test ab
- Build läuft nicht weiter, obwohl der NDK-Pfad scheinbar korrekt ist

### 1.2 Gradle/SDK-Problem nach dem CMake-Setup

Wenn CMake durchläuft, kann Gradle trotzdem noch scheitern, z. B. durch:

- fehlendes Android SDK
- falsches `ANDROID_HOME` / `ANDROID_SDK_ROOT`
- fehlende Android SDK-Komponenten
- AAPT2-Fehler oder veraltete Build-Tools

### 1.3 Wichtig: Direkter Android-App-Build mit System-`clang` ist kein gültiger Fix

Das manuelle Umdrehen des Android-App-Builds auf `/usr/bin/clang` oder `clang++` ist für den AGP-/Gradle-Android-Build nicht der richtige Weg.

Das Projekt erwartet die Android-NDK-Toolchain über AGP/Gradle, nicht einen hostseitigen System-Compiler als Ersatz.

---

## 2. Gültiger Standard für dieses Repository

Der Projekt-Standard lautet:

1. `JAVA_HOME` und `ANDROID_HOME` korrekt setzen
2. Android SDK unter `/opt/android-sdk` verwenden
3. in ARM64-/PRoot-Umgebungen keine x86_64-NDK-Toolchain verwenden
4. CMake-Probleme vor Gradle lösen
5. native C++-Build separat validieren
6. Android-App-Build nur mit der vorgesehenen Gradle-/AGP-Toolchain ausführen

---

## 3. Voraussetzungen

Vor dem Setup sollten diese Tools vorhanden sein:

- Git
- CMake
- C++20-Compiler (`gcc` oder `clang`)
- Make oder Ninja
- Python 3
- Java 17 oder 21
- Android SDK und NDK
- Ollama, falls LLM-kompatible Ausführung gewünscht ist

Prüfen:

```bash
git --version
cmake --version
gcc --version
python3 --version
java -version
```

---

## 4. Empfohlener Setup-Pfad

### 4.1 Option A: Proot-Debian-Setup (verifiziert für Android-App-Builds)

Im Projektstamm:

```bash
chmod +x setup_host.sh
./setup_host.sh
```

Das Skript richtet den Projekt-Workspace für die Umgebungs- und Laufzeitlogik ein und setzt die Standardpfade, falls sie fehlen.

### 4.2 Option B: Native Termux-Setup (primärer Host für Android/Gradle)

Wenn die Umgebung direkt in Termux läuft:

```bash
chmod +x termux_native_setup.sh
./termux_native_setup.sh
```

Das ist der bevorzugte Standard für Android-/Gradle-Workflows in arm64-/Termux-Umgebungen, weil dort die native Java-/SDK-/Cache-Umgebung sauber auf dem Host läuft.

---

## 5. Android SDK und Java sicher prüfen

### 5.1 Umgebungsvariablen setzen

```bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export ANDROID_NDK=/root/android-ndk/27.3.13750724
export JAVA_HOME=/usr/lib/jvm/default-java
export PATH="$JAVA_HOME/bin:$PATH"
```

Prüfen:

```bash
echo $ANDROID_HOME
echo $ANDROID_SDK_ROOT
echo $ANDROID_NDK
echo $JAVA_HOME
```

Erwartung:

- `ANDROID_HOME` -> `/opt/android-sdk`
- `ANDROID_SDK_ROOT` -> `/opt/android-sdk`
- `ANDROID_NDK` -> korrektes NDK-Verzeichnis, z. B. `/root/android-ndk/27.3.13750724`
- `JAVA_HOME` -> gültiger Java-Pfad

### 5.2 SDK-Struktur prüfen

```bash
ls -la /opt/android-sdk
ls -la /opt/android-sdk/ndk
ls -la /opt/android-sdk/build-tools
```

Erwartung:

- SDK-Ordner vorhanden
- `build-tools` mit `aapt2` vorhanden
- `ndk` oder `ndk/27.x` vorhanden

---

## 6. NDK-Problem lösen: Der echte Fehlerpfad

### 6.1 Wenn CMake das Toolchain-File nicht findet

Prüfen:

```bash
ls -la /root/android-ndk/27.3.13750724/build/cmake/android.toolchain.cmake
```

Erwartung:

- die Datei muss existieren
- kein `No such file or directory`

Wenn das Archiv unvollständig entpackt war, manuell erneut auspacken:

```bash
mkdir -p /root/android-ndk
unzip -q /tmp/ndk.zip -d /root/android-ndk/
mv /root/android-ndk/android-ndk-r27c /root/android-ndk/27.3.13750724
```

Dann prüfen:

```bash
ls /root/android-ndk/27.3.13750724
```

Erwartung:

- Ordner wie `build`, `toolchains`, `sources` sichtbar

### 6.2 Wichtig für ARM64/PRoot: keine x86_64-Toolchain benutzen

Auf ARM64-Systemen darf kein `linux-x86_64`-NDK oder alter `qemu-bin/clang++`-Pfad verwendet werden.

Das führt zu:

- `Illegal instruction`
- CMake-Compilerfehler
- totales Abbrechen des Builds

Die gültige Lösung ist:

- native ARM64-NDK verwenden
- oder den System-Compiler sauber setzen
- aber keinen x86_64-Host-Toolchain-Path erzwingen

Beispiel für den korrekten Wechsel auf ein ARM64-NDK:

```bash
cd /opt/Agent/aapt2 && \
rm -rf /root/android-ndk /tmp/ndk.zip && \
mkdir -p /root/android-ndk && \
wget -O /tmp/ndk.zip https://dl.google.com/android/repository/android-ndk-r27c-linux-arm64.zip && \
unzip -q /tmp/ndk.zip -d /root/android-ndk/ && \
mv /root/android-ndk/android-ndk-r27c /root/android-ndk/27.3.13750724
```

Wenn der Build direkt mit der nativen System-Toolchain validiert werden muss, dann sauber:

```bash
export CC=/usr/bin/clang
export CXX=/usr/bin/clang++
```

Wichtig: Das gilt für eine reine CMake-Validierung oder eine native Host-Prüfung. Für den Android-App-Gradle-Build selbst muss die AGP-/NDK-Toolchain verwendet werden.

---

## 7. Android Gradle-Build korrekt starten

Im Projektstamm:

```bash
cd /opt/Agent
./gradlew clean assembleDebug --no-daemon --stacktrace --max-workers=1 \
  -Dorg.gradle.daemon=false \
  -Dorg.gradle.parallel=false \
  -Dorg.gradle.jvmargs='-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false'
```

Erwartung:

- `BUILD SUCCESSFUL` oder ein klarer, zeilenbasierter Fehler im Java/Kotlin-Quellcode
- keine `Illegal instruction`-Abstürze aus dem NDK/Compiler

### 7.1 Wichtige Gradle-Property-Checks

Das Projekt setzt die Standard-Flags über `gradle.properties` bzw. das Setup-Skript:

- `org.gradle.daemon=false`
- `org.gradle.parallel=false`
- `org.gradle.workers.max=1`
- `org.gradle.jvmargs=-Xmx1g -Xms256m -Dfile.encoding=UTF-8 -Dcom.android.build.gradle.internal.aapt.Aapt2Daemon=false`

Wenn eine lokale AAPT2-Installation vorhanden ist, darf sie als Override verwendet werden; wenn nicht, gilt die Standardauflösung über das Android SDK.

---

## 8. Native CMake/CTest separat validieren

Der native Build ist ein separater Validierungspfad und kein Ersatz für den Android-App-Build.

```bash
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

Erwartung:

- native Build läuft stabil
- Test-Suite läuft durch
- kein C++-Compilerfehler im Host-Setup

---

## 9. Ollama-Setup für den Agentenlauf

Wenn der Agent mit einem echten Modell laufen soll, muss Ollama erreichbar sein.

### 9.1 Ollama installieren

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

### 9.2 Server starten

```bash
nohup ollama serve >/tmp/ollama.log 2>&1 &
```

Prüfen:

```bash
curl http://127.0.0.1:11434
```

### 9.3 Modell laden

```bash
ollama pull qwen2.5-coder
```

Oder für ressourcenbeschränkte Umgebungen:

```bash
ollama pull qwen2.5-coder:1.5b
```

### 9.4 Agent starten

```bash
./build-native/agentenlauf \
  --arbeitsverzeichnis /tmp/test-werkstatt \
  --aufgabe "Systemstatus prüfen" \
  --ollama-url http://127.0.0.1:11434 \
  --modell qwen2.5-coder:1.5b
```

Wenn Ollama nicht erreichbar ist, kann `--offline` zwar als Notfallmodus genutzt werden, aber das ist kein Standard-Setup für die produktive Agentenausführung.

---

## 10. Falsche Wege, die wir bewusst ausgeschlossen haben

Die folgenden Ansätze haben sich im Verlauf als falsch oder unbrauchbar für den Projektstandard herausgestellt:

- Android-App-Build mit `linux-x86_64`-NDK in ARM64-/PRoot-Umgebung
- manuelles Erzwingen eines `qemu-bin/clang++`-Pfads
- direkte Nutzung eines hostseitigen System-Compilers als Ersatz für die Android NDK Toolchain
- direkte Android-App-Builds ohne korrekte `JAVA_HOME`/`ANDROID_HOME`-Konfiguration
- CMake-Erkennung als Hauptfehler statt als Teil des Toolchain-/Architektur-Problems

---

## 11. Offizielle Reihenfolge des erfolgreichen Setups

Die Reihenfolge, die im Projekt als stabil und nachvollziehbar gilt, ist:

1. Proot-Debian oder Termux-Setup starten
2. Java und Android SDK validieren
3. `ANDROID_HOME` / `ANDROID_SDK_ROOT` / `ANDROID_NDK` korrekt setzen
4. NDK-Architektur prüfen und falsche x86_64-Toolchains vermeiden
5. `./gradlew clean assembleDebug ...` starten
6. native CMake/CTest separat validieren
7. Ollama starten und Modell laden
8. Agent mit `--ollama-url` und `--modell` starten

---

## 12. Kurzfassung: der eigentliche Kern der Lösung

Der echte Fehler war kein App-Code-Fehler, sondern ein Build-Umgebungsfehler:

- falscher NDK/Compiler im ARM64-/PRoot-Container
- falscher `CC`/`CXX`-Pfad
- fehlende SDK-Komponenten oder falscher `ANDROID_HOME`

Die Lösung war:

- keine x86_64-Toolchain in ARM64-Umgebungen verwenden
- native ARM64-NDK oder saubere System-Toolchain wählen
- Android SDK und Gradle korrekt konfigurieren
- den Agenten anschließend mit funktionierendem Ollama-/LLM-Backend starten

---

## 13. Abschluss

Das Projekt ist so aufgebaut, dass der native Build, der Android-Build und der LLM-/Ollama-Lauf sauber getrennt validiert werden müssen. Die wichtigsten Fehlermuster kamen dabei nicht aus der App selbst, sondern aus der Umgebung:

- Toolchain-Architektur
- NDK-Entpackung
- SDK-Pfade
- CMake- und Gradle-Konfiguration
- Prozess-Setup von Ollama

Wenn diese Schritte sauber eingehalten werden, ist der Agent in der beschriebenen Setup-Umgebung reproduzierbar ausführbar.
