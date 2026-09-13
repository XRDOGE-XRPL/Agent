#pragma once

#include "agent/konfiguration.h"
#include "agent/ollama.h"
#include "agent/protokollierung.h"

#include <memory>
#include <string>

namespace agent {

struct AgentErgebnis {
    bool erfolgreich = false;
    int iterationen = 0;
    std::string zusammenfassung;
    std::string letztes_build_protokoll;
};

class AgentSchleife {
public:
    AgentSchleife(Konfiguration konfig, LlmSchnittstelle& llm, Protokollierung& log);
    AgentErgebnis ausfuehren();
    static std::string system_prompt();
    static std::string benutzer_prompt(const Konfiguration& konfig, const std::string& kontext,
                                       const std::string& letzte_fehler, int iteration);

private:
    Konfiguration konfig_;
    LlmSchnittstelle& llm_;
    Protokollierung& log_;
    std::string letzte_fehler_;
    std::string letztes_build_;
    bool letzter_build_ok_ = false;
};

}  // namespace agent
