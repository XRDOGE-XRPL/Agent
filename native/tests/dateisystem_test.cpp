#include "agent/dateisystem.h"

#include <filesystem>
#include <iostream>
#include <string>

namespace fs = std::filesystem;

int main() {
    const fs::path tmp = fs::temp_directory_path() / "agent-dateisystem-test";
    fs::remove_all(tmp);
    fs::create_directories(tmp);

    const std::string ziel = (tmp / "unter" / "notiz.txt").string();
    std::string fehler;
    if (!agent::Dateisystem::schreiben(ziel, "Hallo Agent", fehler)) {
        std::cerr << "schreiben fehlgeschlagen: " << fehler << "\n";
        return 1;
    }
    std::string inhalt;
    if (!agent::Dateisystem::lesen(ziel, inhalt, fehler) || inhalt != "Hallo Agent") {
        std::cerr << "lesen fehlgeschlagen\n";
        return 1;
    }
    if (!agent::Dateisystem::pfad_ist_sicher(tmp.string(), ziel)) {
        std::cerr << "sicherer Pfad wurde abgelehnt\n";
        return 1;
    }
    const std::string unsicher = (tmp.parent_path() / "aussen.txt").string();
    if (agent::Dateisystem::pfad_ist_sicher(tmp.string(), unsicher)) {
        std::cerr << "unsicherer Pfad wurde akzeptiert\n";
        return 1;
    }
    const std::string traversal = "../" + tmp.filename().string() + "/sicher.txt";
    if (agent::Dateisystem::pfad_ist_sicher(tmp.string(), traversal)) {
        std::cerr << "Pfad-Traversal wurde akzeptiert\n";
        return 1;
    }
    const std::string uebersicht = agent::Dateisystem::uebersicht(tmp.string());
    if (uebersicht.find("notiz.txt") == std::string::npos) {
        std::cerr << "Übersicht unvollständig\n";
        return 1;
    }
    fs::remove_all(tmp);
    return 0;
}
