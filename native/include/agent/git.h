#pragma once

#include <string>
#include <vector>

namespace agent {

struct ProzessErgebnis {
    int code = -1;
    std::string ausgabe;
    std::string fehler;
};

class GitDienst {
public:
    static std::string status_kommando();
    static std::string diff_kommando();
    static std::string log_kommando();
    static std::vector<std::string> commit_argumente(const std::string& nachricht);

    static ProzessErgebnis ausfuehren(const std::string& arbeitsverzeichnis,
                                      const std::string& kommando);
    static ProzessErgebnis status(const std::string& arbeitsverzeichnis);
    static ProzessErgebnis diff(const std::string& arbeitsverzeichnis);
    static bool repository_vorhanden(const std::string& arbeitsverzeichnis);
};

}  // namespace agent
