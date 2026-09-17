#include "agent/build.h"
#include "agent/dateisystem.h"

#include <filesystem>
#include <cstdlib>

namespace agent {
namespace fs = std::filesystem;

namespace {

std::string java_home_fallback() {
    const char* env = std::getenv("JAVA_HOME");
    if (env && *env) {
        return env;
    }

    const std::string kandidaten[] = {
        "/usr/lib/jvm/java-21-openjdk-amd64",
        "/usr/lib/jvm/java-21-openjdk",
        "/usr/lib/jvm/default-java",
        "/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk",
        "/data/data/com.termux/files/usr/lib/jvm/default-java",
        "/usr/lib/jvm/java-17-openjdk-amd64",
        "/usr/lib/jvm/java-17-openjdk",
        "/usr/lib/jvm/java-11-openjdk-amd64",
        "/usr/lib/jvm/java-11-openjdk"
    };

    for (const std::string& kandidat : kandidaten) {
        if (Dateisystem::existiert(kandidat + "/bin/java")) {
            return kandidat;
        }
    }
    return {};
}

std::string gradle_kommando(const std::string& projekt_ordner, const std::string& wrapper_name) {
    const fs::path wrapper = fs::path(projekt_ordner) / wrapper_name;
    if (Dateisystem::existiert(wrapper.string())) {
#if defined(_WIN32)
        return wrapper_name + " test --no-daemon";
#else
        return "sh ./" + wrapper_name + " test --no-daemon";
#endif
    }
    return "gradle test --no-daemon";
}

}  // namespace

ProzessErgebnis BuildDienst::cmake_konfigurieren(const std::string& quelle, const std::string& build_ordner) {
    Dateisystem::verzeichnis_anlegen(build_ordner);
#ifdef _WIN32
    const std::string generator = "MinGW Makefiles";
    const std::string cmd = "cmake -S \"" + quelle + "\" -B \"" + build_ordner + "\" -G \"" + generator + "\"";
#else
    const std::string cmd = "cmake -S \"" + quelle + "\" -B \"" + build_ordner + "\"";
#endif
    return GitDienst::ausfuehren(quelle, cmd);
}

ProzessErgebnis BuildDienst::cmake_bauen(const std::string& build_ordner) {
    return GitDienst::ausfuehren(build_ordner, "cmake --build .");
}

ProzessErgebnis BuildDienst::ctest_laufen(const std::string& build_ordner) {
    return GitDienst::ausfuehren(build_ordner, "ctest --output-on-failure");
}

ProzessErgebnis BuildDienst::gradle_testen(const std::string& projekt_ordner) {
    const std::string java_home = java_home_fallback();
    std::string env_prefix;
    if (!java_home.empty()) {
        env_prefix = "JAVA_HOME=\"" + java_home + "\" PATH=\"" + java_home + "/bin:$PATH\" ";
    }

#ifdef _WIN32
    const std::string wrapper = (fs::path(projekt_ordner) / "gradlew.bat").string();
    if (Dateisystem::existiert(wrapper)) {
        return GitDienst::ausfuehren(projekt_ordner, env_prefix + "gradlew.bat test --no-daemon");
    }
    return GitDienst::ausfuehren(projekt_ordner, env_prefix + "gradle test --no-daemon");
#else
    const std::string wrapper = (fs::path(projekt_ordner) / "gradlew").string();
    if (Dateisystem::existiert(wrapper)) {
        return GitDienst::ausfuehren(projekt_ordner, env_prefix + "sh ./gradlew test --no-daemon");
    }
    return GitDienst::ausfuehren(projekt_ordner, env_prefix + "gradle test --no-daemon");
#endif
}

ProzessErgebnis BuildDienst::automatisch(const std::string& arbeitsverzeichnis) {
    const bool hat_cmake = Dateisystem::existiert((fs::path(arbeitsverzeichnis) / "CMakeLists.txt").string());
    const bool hat_gradle = Dateisystem::existiert((fs::path(arbeitsverzeichnis) / "build.gradle.kts").string()) ||
                            Dateisystem::existiert((fs::path(arbeitsverzeichnis) / "build.gradle").string());

    ProzessErgebnis gesamt;
    std::string protokol;

    if (hat_cmake) {
        const std::string build_ordner = (fs::path(arbeitsverzeichnis) / "build-lokal").string();
        auto konf = cmake_konfigurieren(arbeitsverzeichnis, build_ordner);
        protokol += "CMake-Konfiguration:\n" + konf.ausgabe + "\n";
        if (konf.code != 0) {
            gesamt.code = konf.code;
            gesamt.ausgabe = protokol;
            gesamt.fehler = "CMake-Konfiguration fehlgeschlagen";
            return gesamt;
        }
        auto bau = cmake_bauen(build_ordner);
        protokol += "CMake-Build:\n" + bau.ausgabe + "\n";
        if (bau.code != 0) {
            gesamt.code = bau.code;
            gesamt.ausgabe = protokol;
            gesamt.fehler = "CMake-Build fehlgeschlagen";
            return gesamt;
        }
        auto test = ctest_laufen(build_ordner);
        protokol += "CTest:\n" + test.ausgabe + "\n";
        gesamt.code = test.code;
        gesamt.ausgabe = protokol;
        if (test.code != 0) {
            gesamt.fehler = "CTest fehlgeschlagen";
        }
        return gesamt;
    }

    if (hat_gradle) {
        auto test = gradle_testen(arbeitsverzeichnis);
        gesamt = test;
        if (test.code != 0) {
            gesamt.fehler = "Gradle-Tests fehlgeschlagen";
        }
        return gesamt;
    }

    gesamt.code = 1;
    gesamt.fehler = "Kein CMakeLists.txt oder Gradle-Build gefunden";
    gesamt.ausgabe = gesamt.fehler;
    return gesamt;
}

}  // namespace agent
