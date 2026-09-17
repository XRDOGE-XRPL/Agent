#include "agent/ollama.h"
#include "agent/json_werkzeuge.h"

#include <sstream>

namespace agent {

OllamaKlient::OllamaKlient(Konfiguration konfig)
    : konfig_(std::move(konfig)), http_(konfig_.http_timeout_sekunden) {}

std::string OllamaKlient::anfragen(const std::string& prompt, std::string& fehler) {
    if (konfig_.offline_erzwingen) {
        fehler = "Offline-Modus: Keine LLM-Anfrage";
        return {};
    }
    const std::string prompt_kurz = prompt.size() > 12000 ? prompt.substr(0, 12000) : prompt;

    std::ostringstream body;
    body << "{"
         << "\"model\":\"" << json::escapen(konfig_.modell) << "\","
         << "\"prompt\":\"" << json::escapen(prompt_kurz) << "\","
         << "\"stream\":false,"
         << "\"options\":{\"temperature\":0.2,\"num_predict\":2048}"
         << "}";
    std::string basis = konfig_.ollama_url;
    if (!basis.empty() && basis.back() == '/') {
        basis.pop_back();
    }
    const HttpAntwort antwort = http_.post_json(basis + "/api/generate", body.str());
    if (!antwort.ok) {
        fehler = antwort.fehler.empty() ? "Ollama-Anfrage fehlgeschlagen" : antwort.fehler;
        if (!antwort.koerper.empty()) {
            const std::string kurz = antwort.koerper.substr(0, 400);
            if (!kurz.empty()) {
                fehler += " | " + kurz;
            }
        }
        return {};
    }
    auto text = json::string_feld(antwort.koerper, "response");
    if (!text) {
        fehler = "Ollama-Antwort ohne Feld response";
        return antwort.koerper;
    }
    return *text;
}

bool OllamaKlient::erreichbar(std::string& details) const {
    std::string basis = konfig_.ollama_url;
    if (!basis.empty() && basis.back() == '/') {
        basis.pop_back();
    }
    const HttpAntwort antwort = http_.get(basis + "/api/tags");
    details = antwort.ok ? antwort.koerper : antwort.fehler;
    return antwort.ok;
}

FesteAntwortLlm::FesteAntwortLlm(std::vector<std::string> antworten) : antworten_(std::move(antworten)) {}

std::string FesteAntwortLlm::anfragen(const std::string& prompt, std::string& fehler) {
    (void)prompt;
    if (index_ >= antworten_.size()) {
        fehler = "Keine weiteren Mock-Antworten";
        return {};
    }
    return antworten_[index_++];
}

}  // namespace agent
