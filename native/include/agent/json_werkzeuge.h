#pragma once

#include <optional>
#include <string>
#include <vector>

namespace agent::json {

std::string escapen(const std::string& roh);
std::string unescapen(const std::string& roh);
std::optional<std::string> string_feld(const std::string& json, const std::string& schluessel);
std::optional<int> int_feld(const std::string& json, const std::string& schluessel);
std::string erstes_objekt(const std::string& text);
std::vector<std::string> objekt_liste(const std::string& json, const std::string& schluessel);

}  // namespace agent::json
