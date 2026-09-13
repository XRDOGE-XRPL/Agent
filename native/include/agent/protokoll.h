#pragma once

#include <string>
#include <vector>

namespace agent {

enum class AktionTyp {
    schreiben,
    loeschen,
    lesen,
    bauen,
    testen,
    git_status,
    git_commit,
    fertig,
    abbrechen,
    unbekannt
};

struct AgentSchritt {
    AktionTyp typ = AktionTyp::unbekannt;
    std::string pfad;
    std::string inhalt;
    std::string begruendung;
    std::string roh;
};

class Protokoll {
public:
    static AktionTyp typ_von_text(const std::string& text);
    static std::string text_von_typ(AktionTyp typ);
    static std::vector<AgentSchritt> parsen(const std::string& llm_text);
    static AgentSchritt objekt_parsen(const std::string& objekt);
};

}  // namespace agent
