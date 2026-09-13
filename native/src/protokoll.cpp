#include "agent/protokoll.h"
#include "agent/json_werkzeuge.h"

#include <algorithm>
#include <cctype>

namespace agent {
namespace {

std::string klein(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(), [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return s;
}

}  // namespace

AktionTyp Protokoll::typ_von_text(const std::string& text) {
    const std::string t = klein(text);
    if (t == "schreiben" || t == "write") return AktionTyp::schreiben;
    if (t == "loeschen" || t == "löschen" || t == "delete") return AktionTyp::loeschen;
    if (t == "lesen" || t == "read") return AktionTyp::lesen;
    if (t == "bauen" || t == "build") return AktionTyp::bauen;
    if (t == "testen" || t == "test") return AktionTyp::testen;
    if (t == "git_status" || t == "git-status") return AktionTyp::git_status;
    if (t == "git_commit" || t == "git-commit") return AktionTyp::git_commit;
    if (t == "fertig" || t == "done") return AktionTyp::fertig;
    if (t == "abbrechen" || t == "abort") return AktionTyp::abbrechen;
    return AktionTyp::unbekannt;
}

std::string Protokoll::text_von_typ(AktionTyp typ) {
    switch (typ) {
        case AktionTyp::schreiben: return "schreiben";
        case AktionTyp::loeschen: return "loeschen";
        case AktionTyp::lesen: return "lesen";
        case AktionTyp::bauen: return "bauen";
        case AktionTyp::testen: return "testen";
        case AktionTyp::git_status: return "git_status";
        case AktionTyp::git_commit: return "git_commit";
        case AktionTyp::fertig: return "fertig";
        case AktionTyp::abbrechen: return "abbrechen";
        default: return "unbekannt";
    }
}

AgentSchritt Protokoll::objekt_parsen(const std::string& objekt) {
    AgentSchritt schritt;
    schritt.roh = objekt;
    auto aktion = json::string_feld(objekt, "aktion");
    if (!aktion) {
        aktion = json::string_feld(objekt, "action");
    }
    schritt.typ = aktion ? typ_von_text(*aktion) : AktionTyp::unbekannt;
    schritt.pfad = json::string_feld(objekt, "pfad").value_or(json::string_feld(objekt, "path").value_or(""));
    schritt.inhalt = json::string_feld(objekt, "inhalt").value_or(json::string_feld(objekt, "content").value_or(""));
    schritt.begruendung =
        json::string_feld(objekt, "begruendung").value_or(json::string_feld(objekt, "reason").value_or(""));
    return schritt;
}

std::vector<AgentSchritt> Protokoll::parsen(const std::string& llm_text) {
    std::vector<AgentSchritt> schritte;
    const std::string objekt = json::erstes_objekt(llm_text);
    if (objekt.empty()) {
        return schritte;
    }
    auto liste = json::objekt_liste(objekt, "schritte");
    if (liste.empty()) {
        liste = json::objekt_liste(objekt, "steps");
    }
    if (!liste.empty()) {
        for (const auto& eintrag : liste) {
            schritte.push_back(objekt_parsen(eintrag));
        }
        return schritte;
    }
    schritte.push_back(objekt_parsen(objekt));
    return schritte;
}

}  // namespace agent
