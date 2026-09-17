#include "agent/konfiguration.h"
#include "agent/ollama.h"
#include "agent/protokollierung.h"
#include "agent/schleife.h"
#include "agent/version.h"

#include <iostream>
#include <string>

namespace {

void hilfe() {
    std::cout << AGENT_NAME << " " << AGENT_VERSION << "\n"
              << "Nutzung:\n"
              << "  agentenlauf --arbeitsverzeichnis <pfad> --aufgabe <text> [optionen]\n"
              << "Optionen:\n"
              << "  --ollama-url <url>     Standard: http://127.0.0.1:11434\n"
              << "  --modell <name>        Standard: qwen2.5-coder\n"
              << "  --max-iterationen <n>  Standard: 8\n"
              << "  --git-commits          Erlaubt git commit\n"
              << "  --offline              Keine Netzwerkanfragen\n"
              << "  --hilfe                Diese Hilfe\n";
}

std::string arg_wert(int argc, char** argv, const std::string& name, const std::string& fallback) {
    for (int i = 1; i < argc; ++i) {
        if (name == argv[i] && i + 1 < argc) {
            return argv[i + 1];
        }
    }
    return fallback;
}

bool flag(int argc, char** argv, const std::string& name) {
    for (int i = 1; i < argc; ++i) {
        if (name == argv[i]) {
            return true;
        }
    }
    return false;
}

}  // namespace

int main(int argc, char** argv) {
    if (flag(argc, argv, "--hilfe") || flag(argc, argv, "-h") || argc < 2) {
        hilfe();
        return argc < 2 ? 1 : 0;
    }

    agent::Konfiguration konfig;
    konfig.arbeitsverzeichnis = arg_wert(argc, argv, "--arbeitsverzeichnis", "");
    konfig.aufgabe = arg_wert(argc, argv, "--aufgabe", "");
    konfig.ollama_url = arg_wert(argc, argv, "--ollama-url", konfig.ollama_url);
    konfig.modell = arg_wert(argc, argv, "--modell", konfig.modell);
    konfig.max_iterationen = std::stoi(arg_wert(argc, argv, "--max-iterationen", "8"));
    konfig.git_commits_erlauben = flag(argc, argv, "--git-commits");
    konfig.offline_erzwingen = flag(argc, argv, "--offline");

    if (konfig.arbeitsverzeichnis.empty() || konfig.aufgabe.empty()) {
        std::cerr << "Fehler: --arbeitsverzeichnis und --aufgabe sind erforderlich.\n";
        hilfe();
        return 1;
    }

    agent::Protokollierung log;
    log.setzen_beobachter([](const agent::LogEintrag& e) {
        const char* stufe = "INFO";
        switch (e.stufe) {
            case agent::LogStufe::warnung: stufe = "WARN"; break;
            case agent::LogStufe::fehler: stufe = "FEHLER"; break;
            case agent::LogStufe::erfolg: stufe = "OK"; break;
            default: break;
        }
        std::cerr << "[" << stufe << "] " << e.nachricht << "\n";
    });

    agent::OllamaKlient llm(konfig);
    if (!konfig.offline_erzwingen) {
        std::string details;
        if (!llm.erreichbar(details)) {
            std::cerr << "Warnung: Ollama nicht erreichbar unter " << konfig.ollama_url
                      << " (" << details << ")\n"
                      << "Die Schleife versucht es dennoch; für Tests --offline nutzen.\n";
        }
    }

    agent::AgentSchleife schleife(konfig, llm, log);
    const agent::AgentErgebnis ergebnis = schleife.ausfuehren();
    std::cout << (ergebnis.erfolgreich ? "ERFOLG" : "FEHLGESCHLAGEN") << " nach "
              << ergebnis.iterationen << " Iteration(en): " << ergebnis.zusammenfassung << "\n";
    return ergebnis.erfolgreich ? 0 : 2;
}
