#include "agent/json_werkzeuge.h"

#include <cctype>
#include <sstream>

namespace agent::json {
namespace {

std::size_t schluessel_finden(const std::string& json, const std::string& schluessel) {
    const std::string nadel = "\"" + schluessel + "\"";
    std::size_t pos = 0;
    while ((pos = json.find(nadel, pos)) != std::string::npos) {
        std::size_t nach = pos + nadel.size();
        while (nach < json.size() && std::isspace(static_cast<unsigned char>(json[nach]))) {
            ++nach;
        }
        if (nach < json.size() && json[nach] == ':') {
            return nach + 1;
        }
        ++pos;
    }
    return std::string::npos;
}

std::size_t objekt_ende(const std::string& text, std::size_t start) {
    int tiefe = 0;
    bool in_string = false;
    bool escape = false;
    for (std::size_t i = start; i < text.size(); ++i) {
        const char c = text[i];
        if (in_string) {
            if (escape) {
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                in_string = false;
            }
            continue;
        }
        if (c == '"') {
            in_string = true;
        } else if (c == '{') {
            ++tiefe;
        } else if (c == '}') {
            --tiefe;
            if (tiefe == 0) {
                return i;
            }
        }
    }
    return std::string::npos;
}

}  // namespace

std::string escapen(const std::string& roh) {
    std::string r;
    r.reserve(roh.size() + 8);
    for (unsigned char c : roh) {
        switch (c) {
            case '\\': r += "\\\\"; break;
            case '"': r += "\\\""; break;
            case '\n': r += "\\n"; break;
            case '\r': r += "\\r"; break;
            case '\t': r += "\\t"; break;
            default:
                if (c < 0x20) {
                    std::ostringstream oss;
                    oss << "\\u" << std::hex << std::uppercase << static_cast<int>(c);
                    r += oss.str();
                } else {
                    r += static_cast<char>(c);
                }
        }
    }
    return r;
}

std::string unescapen(const std::string& roh) {
    std::string r;
    r.reserve(roh.size());
    for (std::size_t i = 0; i < roh.size(); ++i) {
        if (roh[i] == '\\' && i + 1 < roh.size()) {
            const char n = roh[++i];
            switch (n) {
                case 'n': r += '\n'; break;
                case 'r': r += '\r'; break;
                case 't': r += '\t'; break;
                case '"': r += '"'; break;
                case '\\': r += '\\'; break;
                case '/': r += '/'; break;
                case 'u':
                    if (i + 4 < roh.size()) {
                        r += '?';
                        i += 4;
                    }
                    break;
                default:
                    r += n;
                    break;
            }
        } else {
            r += roh[i];
        }
    }
    return r;
}

std::optional<std::string> string_feld(const std::string& json, const std::string& schluessel) {
    std::size_t pos = schluessel_finden(json, schluessel);
    if (pos == std::string::npos) {
        return std::nullopt;
    }
    while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos]))) {
        ++pos;
    }
    if (pos >= json.size() || json[pos] != '"') {
        return std::nullopt;
    }
    ++pos;
    std::string roh;
    bool escape = false;
    for (; pos < json.size(); ++pos) {
        const char c = json[pos];
        if (escape) {
            roh.push_back('\\');
            roh.push_back(c);
            escape = false;
        } else if (c == '\\') {
            escape = true;
        } else if (c == '"') {
            return unescapen(roh);
        } else {
            roh.push_back(c);
        }
    }
    return std::nullopt;
}

std::optional<int> int_feld(const std::string& json, const std::string& schluessel) {
    std::size_t pos = schluessel_finden(json, schluessel);
    if (pos == std::string::npos) {
        return std::nullopt;
    }
    while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos]))) {
        ++pos;
    }
    std::size_t ende = pos;
    if (ende < json.size() && (json[ende] == '-' || json[ende] == '+')) {
        ++ende;
    }
    while (ende < json.size() && std::isdigit(static_cast<unsigned char>(json[ende]))) {
        ++ende;
    }
    if (ende == pos) {
        return std::nullopt;
    }
    try {
        return std::stoi(json.substr(pos, ende - pos));
    } catch (...) {
        return std::nullopt;
    }
}

std::string erstes_objekt(const std::string& text) {
    const std::size_t start = text.find('{');
    if (start == std::string::npos) {
        return {};
    }
    const std::size_t ende = objekt_ende(text, start);
    if (ende == std::string::npos) {
        return {};
    }
    return text.substr(start, ende - start + 1);
}

std::vector<std::string> objekt_liste(const std::string& json, const std::string& schluessel) {
    std::vector<std::string> ergebnis;
    std::size_t pos = schluessel_finden(json, schluessel);
    if (pos == std::string::npos) {
        return ergebnis;
    }
    while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos]))) {
        ++pos;
    }
    if (pos >= json.size() || json[pos] != '[') {
        return ergebnis;
    }
    ++pos;
    while (pos < json.size()) {
        while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos]))) {
            ++pos;
        }
        if (pos >= json.size() || json[pos] == ']') {
            break;
        }
        if (json[pos] == '{') {
            const std::size_t ende = objekt_ende(json, pos);
            if (ende == std::string::npos) {
                break;
            }
            ergebnis.push_back(json.substr(pos, ende - pos + 1));
            pos = ende + 1;
        } else {
            break;
        }
        while (pos < json.size() && std::isspace(static_cast<unsigned char>(json[pos]))) {
            ++pos;
        }
        if (pos < json.size() && json[pos] == ',') {
            ++pos;
        }
    }
    return ergebnis;
}

}  // namespace agent::json
