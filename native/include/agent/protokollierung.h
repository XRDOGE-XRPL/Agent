#pragma once

#include <functional>
#include <mutex>
#include <string>
#include <vector>

namespace agent {

enum class LogStufe { info, warnung, fehler, erfolg };

struct LogEintrag {
    LogStufe stufe;
    std::string nachricht;
};

class Protokollierung {
public:
    using Beobachter = std::function<void(const LogEintrag&)>;

    void setzen_beobachter(Beobachter beobachter);
    void schreiben(LogStufe stufe, const std::string& nachricht);
    void info(const std::string& nachricht);
    void warnung(const std::string& nachricht);
    void fehler(const std::string& nachricht);
    void erfolg(const std::string& nachricht);
    std::vector<LogEintrag> kopie() const;

private:
    mutable std::mutex mutex_;
    std::vector<LogEintrag> eintraege_;
    Beobachter beobachter_;
};

}  // namespace agent
