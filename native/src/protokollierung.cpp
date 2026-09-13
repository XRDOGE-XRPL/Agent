#include "agent/protokollierung.h"

namespace agent {

void Protokollierung::setzen_beobachter(Beobachter beobachter) {
    std::lock_guard<std::mutex> sperre(mutex_);
    beobachter_ = std::move(beobachter);
}

void Protokollierung::schreiben(LogStufe stufe, const std::string& nachricht) {
    LogEintrag eintrag{stufe, nachricht};
    Beobachter kopie;
    {
        std::lock_guard<std::mutex> sperre(mutex_);
        eintraege_.push_back(eintrag);
        kopie = beobachter_;
    }
    if (kopie) {
        kopie(eintrag);
    }
}

void Protokollierung::info(const std::string& nachricht) { schreiben(LogStufe::info, nachricht); }
void Protokollierung::warnung(const std::string& nachricht) { schreiben(LogStufe::warnung, nachricht); }
void Protokollierung::fehler(const std::string& nachricht) { schreiben(LogStufe::fehler, nachricht); }
void Protokollierung::erfolg(const std::string& nachricht) { schreiben(LogStufe::erfolg, nachricht); }

std::vector<LogEintrag> Protokollierung::kopie() const {
    std::lock_guard<std::mutex> sperre(mutex_);
    return eintraege_;
}

}  // namespace agent
