#include "agent/json_werkzeuge.h"

#include <cctype>
#include <sstream>

namespace agent::json {
namespace {

std::string utf8_von_codepunkt(unsigned int codepunkt) {
    std::string zeichen;
    if (codepunkt <= 0x7F) {
        zeichen.push_back(static_cast<char>(codepunkt));
    } else if (codepunkt <= 0x7FF) {
        zeichen.push_back(static_cast<char>(0xC0 | ((codepunkt >> 6) & 0x1F)));
        zeichen.push_back(static_cast<char>(0x80 | (codepunkt & 0x3F)));
    } else if (codepunkt <= 0xFFFF) {
        zeichen.push_back(static_cast<char>(0xE0 | ((codepunkt >> 12) & 0x0F)));
        zeichen.push_back(static_cast<char>(0x80 | ((codepunkt >> 6) & 0x3F)));
        zeichen.push_back(static_cast<char>(0x80 | (codepunkt & 0x3F)));
    } else if (codepunkt <= 0x10FFFF) {
        zeichen.push_back(static_cast<char>(0xF0 | ((codepunkt >> 18) & 0x07)));
        zeichen.push_back(static_cast<char>(0x80 | ((codepunkt >> 12) & 0x3F)));
        zeichen.push_back(static_cast<char>(0x80 | ((codepunkt >> 6) & 0x3F)));
        zeichen.push_back(static_cast<char>(0x80 | (codepunkt & 0x3F)));
    }
    return zeichen;
}

std::size_t schluessel_finden(const std::string& json, const std::string& schluessel) {
    const std::string nadel = "\"" + schluessel + "\"";
    bool in_string = false;
    bool escape = false;
    for (std::size_t pos = 0; pos < json.size(); ++pos) {
        const char c = json[pos];
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
            const std::size_t key_start = pos;
            const std::size_t key_end = key_start + nadel.size();
            if (json.compare(key_start, nadel.size(), nadel) == 0) {
                std::size_t nach = key_end;
                while (nach < json.size() && std::isspace(static_cast<unsigned char>(json[nach]))) {
                    ++nach;
                }
                if (nach < json.size() && json[nach] == ':') {
                    return nach + 1;
                }
            }
            in_string = true;
        }
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

namespace {

bool hex4_zu_codepunkt(const std::string& text, std::size_t start, unsigned int& codepunkt) {
    if (start + 4 > text.size()) {
        return false;
    }
    const std::string hex = text.substr(start, 4);
    for (char ch : hex) {
        if (!std::isxdigit(static_cast<unsigned char>(ch))) {
            return false;
        }
    }
    codepunkt = static_cast<unsigned int>(std::stoul(hex, nullptr, 16));
    return true;
}

}  // namespace

std::string unescapen(const std::string& roh) {
    std::string r;
    r.reserve(roh.size());
    for (std::size_t i = 0; i < roh.size(); ++i) {
        if (roh[i] == '\\' && i + 1 < roh.size()) {
            const char n = roh[++i];
            switch (n) {
                case 'b': r += '\b'; break;
                case 'f': r += '\f'; break;
                case 'n': r += '\n'; break;
                case 'r': r += '\r'; break;
                case 't': r += '\t'; break;
                case '"': r += '"'; break;
                case '\\': r += '\\'; break;
                case '/': r += '/'; break;
                case 'u': {
                    unsigned int codepunkt = 0;
                    if (!hex4_zu_codepunkt(roh, i + 1, codepunkt)) {
                        r += '?';
                        break;
                    }
                    if (codepunkt >= 0xD800 && codepunkt <= 0xDBFF) {
                        if (i + 6 < roh.size() && roh.compare(i + 5, 2, "\\u") == 0) {
                            unsigned int low = 0;
                            if (hex4_zu_codepunkt(roh, i + 7, low) && low >= 0xDC00 && low <= 0xDFFF) {
                                codepunkt = 0x10000 + ((codepunkt - 0xD800) << 10) + (low - 0xDC00);
                                i += 6;
                                r += utf8_von_codepunkt(codepunkt);
                                i += 4;
                                break;
                            }
                        }
                        r += "\xEF\xBF\xBD";
                        i += 4;
                        break;
                    }
                    if (codepunkt >= 0xDC00 && codepunkt <= 0xDFFF) {
                        r += "\xEF\xBF\xBD";
                        i += 4;
                        break;
                    }
                    r += utf8_von_codepunkt(codepunkt);
                    i += 4;
                    break;
                }
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
