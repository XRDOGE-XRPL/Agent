#pragma once

#include "agent/http_klient.h"
#include "agent/konfiguration.h"

#include <string>
#include <vector>

namespace agent {

class LlmSchnittstelle {
public:
    virtual ~LlmSchnittstelle() = default;
    virtual std::string anfragen(const std::string& prompt, std::string& fehler) = 0;
};

class OllamaKlient : public LlmSchnittstelle {
public:
    explicit OllamaKlient(Konfiguration konfig);
    std::string anfragen(const std::string& prompt, std::string& fehler) override;
    bool erreichbar(std::string& details) const;

private:
    Konfiguration konfig_;
    HttpKlient http_;
};

class FesteAntwortLlm : public LlmSchnittstelle {
public:
    explicit FesteAntwortLlm(std::vector<std::string> antworten);
    std::string anfragen(const std::string& prompt, std::string& fehler) override;

private:
    std::vector<std::string> antworten_;
    std::size_t index_ = 0;
};

}  // namespace agent
