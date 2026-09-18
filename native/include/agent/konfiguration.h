#pragma once

#include <string>

namespace agent {

struct Konfiguration {
    std::string arbeitsverzeichnis;
    std::string aufgabe;
    std::string ollama_url = "http://127.0.0.1:11434";
    std::string modell = "qwen2.5-coder";
    int max_iterationen = 8;
    int ollama_num_ctx = 4096;
    int max_prompt_zeichen = 6000;
    int http_timeout_sekunden = 120;
    bool git_commits_erlauben = false;
    bool offline_erzwingen = false;
};

}  // namespace agent
