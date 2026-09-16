#include "agent/json_werkzeuge.h"
#include "agent/protokoll.h"

#include <iostream>

int main() {
    const std::string escaped = agent::json::escapen("Zeile\n\"Zitat\"");
    if (escaped.find("\\n") == std::string::npos || escaped.find("\\\"") == std::string::npos) {
        std::cerr << "escapen fehlerhaft\n";
        return 1;
    }
    if (agent::json::unescapen("A\\nB") != "A\nB") {
        std::cerr << "unescapen fehlerhaft\n";
        return 1;
    }
    if (agent::json::string_feld("{\"status\":\"Hallo\\u0021\"}", "status").value_or("") != "Hallo!") {
        std::cerr << "unicode-escape fehlerhaft\n";
        return 1;
    }
    if (agent::json::string_feld("{\"status\":\"Hallo\\nWelt\"}", "status").value_or("") != "Hallo\nWelt") {
        std::cerr << "escaped-string fehlerhaft\n";
        return 1;
    }
    if (agent::json::string_feld("{\"status\":\"Hallo\\xWelt\"}", "status").value_or("") != "HalloxWelt") {
        std::cerr << "malformed-escape fehlerhaft\n";
        return 1;
    }

    const std::string antwort =
        "Hier JSON:\n{\"schritte\":[{\"aktion\":\"schreiben\",\"pfad\":\"a.txt\","
        "\"inhalt\":\"Hallo\\nWelt\",\"begruendung\":\"Testdatei\"},"
        "{\"aktion\":\"bauen\"}]}\n";
    auto schritte = agent::Protokoll::parsen(antwort);
    if (schritte.size() != 2) {
        std::cerr << "erwartete 2 Schritte, erhalten " << schritte.size() << "\n";
        return 1;
    }
    if (schritte[0].typ != agent::AktionTyp::schreiben || schritte[0].pfad != "a.txt" ||
        schritte[0].inhalt != "Hallo\nWelt") {
        std::cerr << "Schreibschritt falsch geparst\n";
        return 1;
    }
    if (schritte[1].typ != agent::AktionTyp::bauen) {
        std::cerr << "Bauschritt falsch geparst\n";
        return 1;
    }
    if (agent::Protokoll::typ_von_text("fertig") != agent::AktionTyp::fertig) {
        std::cerr << "typ_von_text fehlerhaft\n";
        return 1;
    }
    return 0;
}
