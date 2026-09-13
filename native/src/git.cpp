#include "agent/git.h"
#include "agent/dateisystem.h"

#include <array>
#include <cstdio>
#include <filesystem>
#include <sstream>

#ifdef _WIN32
#define POPEN _popen
#define PCLOSE _pclose
#else
#include <sys/wait.h>
#define POPEN popen
#define PCLOSE pclose
#endif

namespace agent {
namespace fs = std::filesystem;

std::string GitDienst::status_kommando() { return "git status --porcelain=v1 -uall"; }
std::string GitDienst::diff_kommando() { return "git diff --stat"; }
std::string GitDienst::log_kommando() { return "git --no-pager log -5 --oneline"; }

std::vector<std::string> GitDienst::commit_argumente(const std::string& nachricht) {
    return {"git", "commit", "-m", nachricht};
}

ProzessErgebnis GitDienst::ausfuehren(const std::string& arbeitsverzeichnis, const std::string& kommando) {
    ProzessErgebnis ergebnis;
    std::string vollstaendig;
#ifdef _WIN32
    vollstaendig = "cd /d \"" + arbeitsverzeichnis + "\" && " + kommando + " 2>&1";
#else
    vollstaendig = "cd \"" + arbeitsverzeichnis + "\" && " + kommando + " 2>&1";
#endif
    FILE* pipe = POPEN(vollstaendig.c_str(), "r");
    if (!pipe) {
        ergebnis.fehler = "Prozess konnte nicht gestartet werden";
        return ergebnis;
    }
    std::array<char, 512> puffer{};
    std::ostringstream aus;
    while (fgets(puffer.data(), static_cast<int>(puffer.size()), pipe) != nullptr) {
        aus << puffer.data();
    }
#ifdef _WIN32
    ergebnis.code = PCLOSE(pipe);
#else
    const int status = PCLOSE(pipe);
    ergebnis.code = WIFEXITED(status) ? WEXITSTATUS(status) : status;
#endif
    ergebnis.ausgabe = aus.str();
    return ergebnis;
}

ProzessErgebnis GitDienst::status(const std::string& arbeitsverzeichnis) {
    return ausfuehren(arbeitsverzeichnis, status_kommando());
}

ProzessErgebnis GitDienst::diff(const std::string& arbeitsverzeichnis) {
    return ausfuehren(arbeitsverzeichnis, diff_kommando());
}

bool GitDienst::repository_vorhanden(const std::string& arbeitsverzeichnis) {
    return Dateisystem::existiert((fs::path(arbeitsverzeichnis) / ".git").string());
}

}  // namespace agent
