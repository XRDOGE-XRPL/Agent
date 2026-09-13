#include "agent/dateisystem.h"

#include <algorithm>
#include <filesystem>
#include <fstream>
#include <sstream>

namespace agent {
namespace fs = std::filesystem;

namespace {

bool ist_ausgeschlossen(const fs::path& relativ) {
    for (const auto& teil : relativ) {
        const std::string name = teil.string();
        if (name == ".git" || name == "build" || name == "build-native" || name == "build-lokal" ||
            name == ".gradle" || name == ".cxx" || name == ".idea" || name == "__pycache__") {
            return true;
        }
    }
    return false;
}

fs::path normalisiert(const std::string& pfad) {
    std::error_code ec;
    fs::path p = fs::u8path(pfad);
    auto abs = fs::weakly_canonical(p, ec);
    if (ec) {
        return fs::absolute(p);
    }
    return abs;
}

}  // namespace

bool Dateisystem::existiert(const std::string& pfad) {
    std::error_code ec;
    return fs::exists(fs::u8path(pfad), ec);
}

bool Dateisystem::verzeichnis_anlegen(const std::string& pfad) {
    std::error_code ec;
    fs::create_directories(fs::u8path(pfad), ec);
    return !ec;
}

bool Dateisystem::schreiben(const std::string& pfad, const std::string& inhalt, std::string& fehler) {
    try {
        fs::path p = fs::u8path(pfad);
        if (p.has_parent_path()) {
            fs::create_directories(p.parent_path());
        }
        std::ofstream aus(p, std::ios::binary | std::ios::trunc);
        if (!aus) {
            fehler = "Datei konnte nicht geöffnet werden: " + pfad;
            return false;
        }
        aus.write(inhalt.data(), static_cast<std::streamsize>(inhalt.size()));
        if (!aus) {
            fehler = "Schreiben fehlgeschlagen: " + pfad;
            return false;
        }
        return true;
    } catch (const std::exception& ex) {
        fehler = ex.what();
        return false;
    }
}

bool Dateisystem::lesen(const std::string& pfad, std::string& inhalt, std::string& fehler) {
    try {
        std::ifstream ein(fs::u8path(pfad), std::ios::binary);
        if (!ein) {
            fehler = "Datei nicht lesbar: " + pfad;
            return false;
        }
        std::ostringstream puffer;
        puffer << ein.rdbuf();
        inhalt = puffer.str();
        return true;
    } catch (const std::exception& ex) {
        fehler = ex.what();
        return false;
    }
}

bool Dateisystem::loeschen(const std::string& pfad, std::string& fehler) {
    std::error_code ec;
    fs::remove_all(fs::u8path(pfad), ec);
    if (ec) {
        fehler = ec.message();
        return false;
    }
    return true;
}

std::vector<DateiInfo> Dateisystem::auflisten(const std::string& wurzel, std::string& fehler) {
    std::vector<DateiInfo> liste;
    std::error_code ec;
    fs::path basis = fs::u8path(wurzel);
    if (!fs::exists(basis, ec)) {
        fehler = "Arbeitsverzeichnis fehlt: " + wurzel;
        return liste;
    }
    for (fs::recursive_directory_iterator it(basis, fs::directory_options::skip_permission_denied, ec),
         ende;
         it != ende; it.increment(ec)) {
        if (ec) {
            ec.clear();
            continue;
        }
        const fs::path relativ = fs::relative(it->path(), basis, ec);
        if (ec || ist_ausgeschlossen(relativ)) {
            if (it->is_directory()) {
                it.disable_recursion_pending();
            }
            continue;
        }
        DateiInfo info;
        info.relativer_pfad = relativ.generic_string();
        info.ist_verzeichnis = it->is_directory(ec);
        if (!info.ist_verzeichnis) {
            info.groesse = it->file_size(ec);
        }
        liste.push_back(std::move(info));
    }
    std::sort(liste.begin(), liste.end(), [](const DateiInfo& a, const DateiInfo& b) {
        return a.relativer_pfad < b.relativer_pfad;
    });
    return liste;
}

std::string Dateisystem::uebersicht(const std::string& wurzel, std::size_t max_eintraege) {
    std::string fehler;
    auto liste = auflisten(wurzel, fehler);
    std::ostringstream oss;
    if (!fehler.empty()) {
        oss << "Fehler: " << fehler << "\n";
    }
    oss << "Dateien (" << liste.size() << "):\n";
    std::size_t n = 0;
    for (const auto& eintrag : liste) {
        if (n++ >= max_eintraege) {
            oss << "... weitere Einträge ausgelassen\n";
            break;
        }
        oss << (eintrag.ist_verzeichnis ? "[V] " : "[D] ") << eintrag.relativer_pfad;
        if (!eintrag.ist_verzeichnis) {
            oss << " (" << eintrag.groesse << " Byte)";
        }
        oss << "\n";
    }
    return oss.str();
}

std::string Dateisystem::verbinden(const std::string& basis, const std::string& relativ) {
    return (fs::u8path(basis) / fs::u8path(relativ)).string();
}

bool Dateisystem::pfad_ist_sicher(const std::string& wurzel, const std::string& ziel) {
    try {
        const fs::path w = normalisiert(wurzel);
        const fs::path z = normalisiert(ziel);
        const auto wtext = w.generic_string();
        const auto ztext = z.generic_string();
        if (ztext.size() < wtext.size()) {
            return false;
        }
        if (ztext.compare(0, wtext.size(), wtext) != 0) {
            return false;
        }
        return ztext.size() == wtext.size() || ztext[wtext.size()] == '/';
    } catch (...) {
        return false;
    }
}

}  // namespace agent
