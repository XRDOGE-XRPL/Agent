#include "agent/schleife.h"
#include "agent/build.h"
#include "agent/dateisystem.h"
#include "agent/git.h"
#include "agent/protokoll.h"
#include "agent/version.h"

#include <filesystem>
#include <sstream>

namespace agent {
namespace fs = std::filesystem;
namespace {
std::string prompt_begrenzen(const std::string& text, std::size_t max_zeichen) {
    if (text.size() <= max_zeichen) {
        return text;
    }
    const std::string suffix = "... [gekürzt]";
    if (max_zeichen <= suffix.size()) {
        return text.substr(0, max_zeichen);
    }
    return text.substr(0, max_zeichen - suffix.size()) + suffix;
}
}  // namespace

AgentSchleife::AgentSchleife(Konfiguration konfig, LlmSchnittstelle& llm, Protokollierung& log)
    : konfig_(std::move(konfig)), llm_(llm), log_(log) {}

std::string AgentSchleife::system_prompt() {
    return std::string(AGENT_NAME) + " v" + AGENT_VERSION +
           "\nDu bist ein autonomer Entwicklungsagent. Antworte AUSSCHLIESSLICH mit JSON.\n"
           "Schema:\n"
           "{\"schritte\":[{\"aktion\":\"schreiben|loeschen|lesen|analysieren|bauen|testen|git_status|fertig|abbrechen\","
           "\"pfad\":\"relativer/pfad\",\"inhalt\":\"dateiinhalt\",\"begruendung\":\"kurz\"}]}\n"
           "Regeln:\n"
           "- Nur relative Pfade innerhalb des Arbeitsverzeichnisses.\n"
           "- Keine Platzhalter, vollständiger Dateiinhalt bei schreiben.\n"
           "- Nach Änderungen bauen/testen.\n"
           "- Bei grünen Tests aktion fertig senden.\n"
           "- Sprache der Begründungen: Deutsch.\n";
}

std::string AgentSchleife::benutzer_prompt(const Konfiguration& konfig, const std::string& kontext,
                                           const std::string& letzte_fehler, int iteration) {
    std::ostringstream oss;
    oss << system_prompt() << "\n";
    oss << "Iteration: " << iteration << "/" << konfig.max_iterationen << "\n";
    oss << "Arbeitsverzeichnis: " << konfig.arbeitsverzeichnis << "\n";
    oss << "Aufgabe: " << konfig.aufgabe << "\n\n";
    oss << "Kontext:\n" << kontext << "\n";
    if (!letzte_fehler.empty()) {
        oss << "Letzte Fehler:\n" << letzte_fehler << "\n";
    }
    oss << "Liefere den nächsten JSON-Schritt.\n";
    return oss.str();
}

AgentErgebnis AgentSchleife::ausfuehren() {
    AgentErgebnis ergebnis;
    if (konfig_.arbeitsverzeichnis.empty() || konfig_.aufgabe.empty()) {
        ergebnis.zusammenfassung = "Arbeitsverzeichnis und Aufgabe sind Pflichtfelder";
        log_.fehler(ergebnis.zusammenfassung);
        return ergebnis;
    }
    Dateisystem::verzeichnis_anlegen(konfig_.arbeitsverzeichnis);

    for (int i = 1; i <= konfig_.max_iterationen; ++i) {
        ergebnis.iterationen = i;
        log_.info("Starte Iteration " + std::to_string(i));
        const std::string kontext = Dateisystem::uebersicht(konfig_.arbeitsverzeichnis);
        std::string git_kontext;
        if (GitDienst::repository_vorhanden(konfig_.arbeitsverzeichnis)) {
            auto st = GitDienst::status(konfig_.arbeitsverzeichnis);
            git_kontext = "Git-Status:\n" + st.ausgabe;
        } else {
            git_kontext = "Kein Git-Repository vorhanden.\n";
        }
        const std::string prompt =
            benutzer_prompt(konfig_, prompt_begrenzen(kontext + "\n" + git_kontext, konfig_.max_prompt_zeichen), letzte_fehler_, i);
        std::string llm_fehler;
        const std::string antwort = llm_.anfragen(prompt, llm_fehler);
        if (antwort.empty()) {
            letzte_fehler_ = llm_fehler.empty() ? "Leere Modellantwort" : llm_fehler;
            log_.fehler(letzte_fehler_);
            continue;
        }
        auto schritte = Protokoll::parsen(antwort);
        if (schritte.empty()) {
            letzte_fehler_ = "Antwort enthielt kein gültiges JSON-Protokoll";
            log_.warnung(letzte_fehler_);
            continue;
        }

        bool fertig = false;
        for (const auto& schritt : schritte) {
            log_.info("Aktion: " + Protokoll::text_von_typ(schritt.typ) +
                      (schritt.pfad.empty() ? "" : " -> " + schritt.pfad));
            switch (schritt.typ) {
                case AktionTyp::schreiben: {
                    const std::string ziel = Dateisystem::verbinden(konfig_.arbeitsverzeichnis, schritt.pfad);
                    if (!Dateisystem::pfad_ist_sicher(konfig_.arbeitsverzeichnis, ziel)) {
                        letzte_fehler_ = "Unsicherer Pfad abgelehnt: " + schritt.pfad;
                        log_.fehler(letzte_fehler_);
                        break;
                    }
                    std::string fehler;
                    if (!Dateisystem::schreiben(ziel, schritt.inhalt, fehler)) {
                        letzte_fehler_ = fehler;
                        log_.fehler(fehler);
                    } else {
                        log_.erfolg("Datei geschrieben: " + schritt.pfad);
                    }
                    break;
                }
                case AktionTyp::loeschen: {
                    const std::string ziel = Dateisystem::verbinden(konfig_.arbeitsverzeichnis, schritt.pfad);
                    if (!Dateisystem::pfad_ist_sicher(konfig_.arbeitsverzeichnis, ziel)) {
                        letzte_fehler_ = "Unsicherer Pfad abgelehnt: " + schritt.pfad;
                        log_.fehler(letzte_fehler_);
                        break;
                    }
                    std::string fehler;
                    if (!Dateisystem::loeschen(ziel, fehler)) {
                        letzte_fehler_ = fehler;
                        log_.fehler(fehler);
                    }
                    break;
                }
                case AktionTyp::lesen: {
                    const std::string ziel = Dateisystem::verbinden(konfig_.arbeitsverzeichnis, schritt.pfad);
                    std::string inhalt, fehler;
                    if (Dateisystem::lesen(ziel, inhalt, fehler)) {
                        letzte_fehler_.clear();
                        log_.info("Datei gelesen: " + schritt.pfad + " (" + std::to_string(inhalt.size()) + " Byte)");
                    } else {
                        letzte_fehler_ = fehler;
                    }
                    break;
                }
                case AktionTyp::analysieren: {
                    const std::string ziel = Dateisystem::verbinden(konfig_.arbeitsverzeichnis, schritt.pfad);
                    if (!Dateisystem::pfad_ist_sicher(konfig_.arbeitsverzeichnis, ziel)) {
                        letzte_fehler_ = "Unsicherer Pfad abgelehnt: " + schritt.pfad;
                        log_.fehler(letzte_fehler_);
                        break;
                    }
                    std::string inhalt, fehler;
                    if (!Dateisystem::lesen(ziel, inhalt, fehler)) {
                        letzte_fehler_ = fehler;
                        log_.fehler(fehler);
                        break;
                    }
                    std::size_t zeilen = 1;
                    for (char ch : inhalt) {
                        if (ch == '\n') {
                            ++zeilen;
                        }
                    }
                    letzte_fehler_.clear();
                    log_.info("Datei analysiert: " + schritt.pfad + " (" + std::to_string(zeilen) +
                              " Zeilen, " + std::to_string(inhalt.size()) + " Byte)");
                    break;
                }
                case AktionTyp::bauen:
                case AktionTyp::testen: {
                    auto bau = BuildDienst::automatisch(konfig_.arbeitsverzeichnis);
                    letztes_build_ = bau.ausgabe;
                    ergebnis.letztes_build_protokoll = bau.ausgabe;
                    letzter_build_ok_ = bau.code == 0;
                    if (letzter_build_ok_) {
                        log_.erfolg("Build/Tests erfolgreich");
                        letzte_fehler_.clear();
                    } else {
                        letzte_fehler_ = bau.fehler + "\n" + bau.ausgabe;
                        log_.fehler("Build/Tests fehlgeschlagen");
                    }
                    break;
                }
                case AktionTyp::git_status: {
                    auto st = GitDienst::status(konfig_.arbeitsverzeichnis);
                    log_.info(st.ausgabe.empty() ? "Git-Status leer (sauber oder kein Repo)" : st.ausgabe);
                    break;
                }
                case AktionTyp::git_commit: {
                    if (!konfig_.git_commits_erlauben) {
                        log_.warnung("Git-Commits sind deaktiviert");
                        break;
                    }
                    GitDienst::ausfuehren(konfig_.arbeitsverzeichnis, "git add -A");
                    const std::string msg = schritt.begruendung.empty() ? "Agent: automatische Änderung" : schritt.begruendung;
                    GitDienst::ausfuehren(konfig_.arbeitsverzeichnis, "git commit -m \"" + msg + "\"");
                    break;
                }
                case AktionTyp::fertig:
                    fertig = true;
                    ergebnis.zusammenfassung =
                        schritt.begruendung.empty() ? "Agent meldet Fertigstellung" : schritt.begruendung;
                    break;
                case AktionTyp::abbrechen:
                    ergebnis.zusammenfassung = "Abbruch durch Modell: " + schritt.begruendung;
                    log_.warnung(ergebnis.zusammenfassung);
                    return ergebnis;
                case AktionTyp::unbekannt:
                default:
                    letzte_fehler_ = "Unbekannte Aktion im Protokoll";
                    log_.warnung(letzte_fehler_);
                    break;
            }
        }
        if (fertig) {
            if (letzter_build_ok_) {
                ergebnis.erfolgreich = true;
                log_.erfolg(ergebnis.zusammenfassung);
                return ergebnis;
            }
            if (letzte_fehler_.empty()) {
                auto bau = BuildDienst::automatisch(konfig_.arbeitsverzeichnis);
                ergebnis.letztes_build_protokoll = bau.ausgabe;
                if (bau.code == 0) {
                    ergebnis.erfolgreich = true;
                    log_.erfolg("Fertig nach abschließendem Build");
                    return ergebnis;
                }
                letzte_fehler_ = bau.fehler + "\n" + bau.ausgabe;
                log_.fehler("Fertig gemeldet, aber Build fehlgeschlagen – Schleife geht weiter");
            }
        }
    }
    if (ergebnis.zusammenfassung.empty()) {
        ergebnis.zusammenfassung = "Maximale Iterationen erreicht ohne erfolgreichen Abschluss";
    }
    log_.fehler(ergebnis.zusammenfassung);
    return ergebnis;
}

}  // namespace agent
