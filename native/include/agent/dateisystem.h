#pragma once

#include <cstdint>
#include <string>
#include <vector>

namespace agent {

struct DateiInfo {
    std::string relativer_pfad;
    bool ist_verzeichnis = false;
    std::uintmax_t groesse = 0;
};

class Dateisystem {
public:
    static bool existiert(const std::string& pfad);
    static bool verzeichnis_anlegen(const std::string& pfad);
    static bool schreiben(const std::string& pfad, const std::string& inhalt, std::string& fehler);
    static bool lesen(const std::string& pfad, std::string& inhalt, std::string& fehler);
    static bool loeschen(const std::string& pfad, std::string& fehler);
    static std::vector<DateiInfo> auflisten(const std::string& wurzel, std::string& fehler);
    static std::string uebersicht(const std::string& wurzel, std::size_t max_eintraege = 200);
    static std::string verbinden(const std::string& basis, const std::string& relativ);
    static bool pfad_ist_sicher(const std::string& wurzel, const std::string& ziel);
};

}  // namespace agent
