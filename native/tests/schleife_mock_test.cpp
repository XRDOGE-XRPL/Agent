#include "agent/dateisystem.h"
#include "agent/json_werkzeuge.h"
#include "agent/ollama.h"
#include "agent/protokollierung.h"
#include "agent/schleife.h"

#include <filesystem>
#include <fstream>
#include <iostream>

namespace fs = std::filesystem;

int main() {
    const fs::path tmp = fs::temp_directory_path() / "agent-schleife-test";
    fs::remove_all(tmp);
    fs::create_directories(tmp);

    {
        std::ofstream cmake((tmp / "CMakeLists.txt").string());
        cmake << "cmake_minimum_required(VERSION 3.16)\n"
              << "project(Mini LANGUAGES CXX)\n"
              << "enable_testing()\n"
              << "add_executable(mini_ok mini.cpp)\n"
              << "add_test(NAME mini_ok COMMAND mini_ok)\n";
    }

    const std::string mini_cpp =
        "#include <iostream>\nint main(){ std::cout << \"ok\\n\"; return 0; }\n";
    const std::string json1 =
        std::string("{\"schritte\":[{\"aktion\":\"schreiben\",\"pfad\":\"mini.cpp\",\"inhalt\":\"") +
        agent::json::escapen(mini_cpp) + "\",\"begruendung\":\"Testdatei\"}]}";
    const std::string json2 = "{\"schritte\":[{\"aktion\":\"testen\"}]}";
    const std::string json3 =
        "{\"schritte\":[{\"aktion\":\"fertig\",\"begruendung\":\"Tests sind grün\"}]}";

    agent::FesteAntwortLlm llm({json1, json2, json3});
    agent::Konfiguration konfig;
    konfig.arbeitsverzeichnis = tmp.string();
    konfig.aufgabe = "Erzeuge ein Mini-CMake-Projekt, das CTest besteht.";
    konfig.max_iterationen = 5;
    konfig.offline_erzwingen = true;

    agent::Protokollierung log;
    agent::AgentSchleife schleife(konfig, llm, log);
    const agent::AgentErgebnis ergebnis = schleife.ausfuehren();
    if (!ergebnis.erfolgreich) {
        std::cerr << "Schleife nicht erfolgreich: " << ergebnis.zusammenfassung << "\n"
                  << ergebnis.letztes_build_protokoll << "\n";
        return 1;
    }
    std::string inhalt, fehler;
    const std::string pfad = (tmp / "mini.cpp").string();
    if (!agent::Dateisystem::lesen(pfad, inhalt, fehler) || inhalt.find("int main") == std::string::npos) {
        std::cerr << "mini.cpp fehlt oder ist unvollständig\n";
        return 1;
    }
    fs::remove_all(tmp);
    return 0;
}
