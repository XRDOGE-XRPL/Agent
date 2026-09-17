# Android-Build-Umgebung und Build-Fehlerklärung

## Kurzfassung

Das Repository ist korrekt für Android Gradle Builds mit:

- Android Gradle Plugin 8.7.3
- Gradle 8.9
- Android SDK unter `/opt/android-sdk`
- Java 21 / OpenJDK im Proot-Debian-Userland

Der eigentliche Fehler ist kein Codefehler in der App, sondern ein Werkzeug-/Umgebungs-Mismatch. Die App darf nicht direkt im Termux-Host gebaut werden, sondern muss in der unterstützten Proot-Debian-Umgebung laufen.

## Was im Projekt korrekt ist

Das Repository selbst ist konsistent konfiguriert:

- `build.gradle.kts` verwendet AGP `8.7.3`
- `gradle/wrapper/gradle-wrapper.properties` verwendet Gradle `8.9`
- `app/build.gradle.kts` setzt die erwarteten Android-Abhängigkeiten (`appcompat`, `core-ktx`, Compose, Material, etc.)
- `CMakeLists.txt` auf Root-Ebene aktiviert `CTest` korrekt
- `native/CMakeLists.txt` verwendet C++20 und ist ein normaler Host-CMake-Build

## Was nicht korrekt ist

Die folgenden Ansätze sind für dieses Projekt nicht passend:

1. Direkter Android-Build auf dem Termux-Host
   - Das Projekt erwartet Proot-Debian als Build-Umgebung.
   - Der Host nutzt Android/Bionic-/Kernel-Einschränkungen, die zu Gradle-/AAPT2-Problemen führen können.

2. Direktes Umdrehen des Android-App-Builds auf System-`clang`/`clang++`
   - Das ist kein gültiger Ersatz für die Android NDK Toolchain.
   - `app/src/main/cpp/CMakeLists.txt` wird von AGP/NDK gesteuert und nicht durch einen manuellen Host-Compiler.

3. Die CMake-Warnung als Hauptfehler behandeln
   - `Compatibility with CMake < 3.10 will be removed ...` ist eine Deprecation-Warnung aus dem Android NDK Toolchain-Skript.
   - Das ist kein fataler Build-Fehler.

## Die echte Ursache

Die reale Ursache ist ein Toolchain-/Umgebungs-Mismatch:

- falscher JDK-Pfad oder kein JDK 21/17 in der richtigen Umgebung
- falscher Android SDK-Pfad
- Build im falschen Host-Kontext
- fehlende Plugin-Resolution oder blockierte Downloads aus Maven/Google

In der vorliegenden Sandbox war beispielsweise der aktive Kontext:

- `JAVA_HOME=/usr/lib/jvm/temurin-17-jdk-amd64`
- `ANDROID_HOME=/usr/local/lib/android/sdk`

Das entspricht nicht dem im Repo vorgesehenen Pfad:

- `/opt/android-sdk`
- Java im Proot-Debian-Userland

## Was für diesen Repository-Standard gültig ist

### 1. Proot-Debian-Umgebung

Der gültige Weg für den Android-App-Build ist:

- Proot-Debian starten
- JDK 21/17 installieren
- Android SDK unter `/opt/android-sdk`
- `local.properties` mit `sdk.dir=/opt/android-sdk`
- `cmake.dir=/usr` nur als System-CMake-Override, nicht als Ersatz für den Android NDK

### 2. Standard-Android-Build-Befehl

```bash
cd /root/Agent
./gradlew clean assembleDebug --no-daemon --stacktrace
```

### 3. Native C++-Validierung separat

Der native C++-Build (`native/`) ist ein separates Problem vom Android-App-Build. Dafür ist ein normaler Host-CMake-Prozess mit einer installierten nativen Toolchain zulässig, z. B.:

```bash
apt-get update && apt-get install -y build-essential clang llvm lld cmake ninja-build
cmake -S native -B build-native
cmake --build build-native
ctest --test-dir build-native --output-on-failure
```

Das ist nicht identisch mit dem Android-AGP-Build und darf nicht als Ersatz für den App-Build dienen.

## Was nicht in das Projekt selbst eingebaut werden sollte

Der manuelle Android-CMake-Aufruf mit:

```bash
cmake \
  -H./app/src/main/cpp \
  -DCMAKE_SYSTEM_NAME=Android \
  -DANDROID_PLATFORM=android-26 \
  -DANDROID_ABI=arm64-v8a \
  -DCMAKE_C_COMPILER=/usr/bin/clang \
  -DCMAKE_CXX_COMPILER=/usr/bin/clang++ \
  -GNinja
```

ist nur für eine isolierte Toolchain-Validierung außerhalb des Projekts sinnvoll. Er ist kein korrekter Fix für den normalen Gradle/AGP-Build dieses Repositories.

## Ergebnis

Das Projekt selbst ist nicht fehlerhaft. Der gültige Aufbau ist:

- native Host-Tests separat
- Android-App-Build separat in Proot-Debian/AGP
- keine direkte Android-App-Build-Umstellung auf System-`clang`
- keine direkte Nutzung des Termux-Hosts für den Android-App-Build

## Empfohlene Reihenfolge

1. Proot-Debian aktivieren
2. Java 21/17 und Android SDK unter `/opt/android-sdk` prüfen
3. `local.properties` setzen
4. `./gradlew clean assembleDebug --no-daemon --stacktrace` starten
5. Native C++-Tests separat mit `native/` und CMake validieren

Damit bleibt die Projektstruktur sauber und die Android-Toolchain wird korrekt durch AGP/NDK verwendet.
