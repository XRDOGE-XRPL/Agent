#pragma once

#include "agent/git.h"

#include <string>

namespace agent {

class BuildDienst {
public:
    static ProzessErgebnis cmake_konfigurieren(const std::string& quelle, const std::string& build_ordner);
    static ProzessErgebnis cmake_bauen(const std::string& build_ordner);
    static ProzessErgebnis ctest_laufen(const std::string& build_ordner);
    static ProzessErgebnis gradle_testen(const std::string& projekt_ordner);
    static ProzessErgebnis automatisch(const std::string& arbeitsverzeichnis);
};

}  // namespace agent
