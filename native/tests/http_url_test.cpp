#include "agent/http_klient.h"

#include <iostream>

int main() {
    std::string host;
    int port = 0;
    std::string pfad;
    std::string fehler;
    if (!agent::HttpKlient::url_zerlegen("http://127.0.0.1:11434/api/generate", host, port, pfad, fehler)) {
        std::cerr << fehler << "\n";
        return 1;
    }
    if (host != "127.0.0.1" || port != 11434 || pfad != "/api/generate") {
        std::cerr << "URL-Zerlegung falsch: " << host << " " << port << " " << pfad << "\n";
        return 1;
    }
    if (agent::HttpKlient::url_zerlegen("https://beispiel.de", host, port, pfad, fehler)) {
        std::cerr << "HTTPS hätte abgelehnt werden müssen\n";
        return 1;
    }
    if (!agent::HttpKlient::url_zerlegen("http://localhost/tags", host, port, pfad, fehler)) {
        std::cerr << "localhost-URL fehlgeschlagen\n";
        return 1;
    }
    if (host != "localhost" || port != 80 || pfad != "/tags") {
        std::cerr << "Standardport falsch\n";
        return 1;
    }
    return 0;
}
