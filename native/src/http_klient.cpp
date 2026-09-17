#include "agent/http_klient.h"
#include "agent/json_werkzeuge.h"

#include <algorithm>
#include <cctype>
#include <cstring>
#include <sstream>
#include <vector>

#ifdef _WIN32
#ifndef WIN32_LEAN_AND_MEAN
#define WIN32_LEAN_AND_MEAN
#endif
#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <arpa/inet.h>
#include <fcntl.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>
#endif

namespace agent {
namespace {

#ifdef _WIN32
using SocketTyp = SOCKET;
const SocketTyp ungueltig = INVALID_SOCKET;

struct WsaWaechter {
    bool ok = false;
    WsaWaechter() {
        WSADATA data{};
        ok = WSAStartup(MAKEWORD(2, 2), &data) == 0;
    }
    ~WsaWaechter() {
        if (ok) {
            WSACleanup();
        }
    }
};

void schliessen(SocketTyp s) {
    if (s != ungueltig) {
        closesocket(s);
    }
}
#else
using SocketTyp = int;
const SocketTyp ungueltig = -1;
void schliessen(SocketTyp s) {
    if (s != ungueltig) {
        close(s);
    }
}
#endif

bool timeout_setzen(SocketTyp s, int sekunden) {
#ifdef _WIN32
    DWORD ms = static_cast<DWORD>(sekunden * 1000);
    setsockopt(s, SOL_SOCKET, SO_RCVTIMEO, reinterpret_cast<const char*>(&ms), sizeof(ms));
    setsockopt(s, SOL_SOCKET, SO_SNDTIMEO, reinterpret_cast<const char*>(&ms), sizeof(ms));
    return true;
#else
    timeval tv{};
    tv.tv_sec = sekunden;
    tv.tv_usec = 0;
    setsockopt(s, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    setsockopt(s, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
    return true;
#endif
}

std::string klein(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(), [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
    return s;
}

std::string json_fehlermeldung(const std::string& koerper) {
    if (auto wert = json::string_feld(koerper, "error")) {
        return *wert;
    }
    if (auto wert = json::string_feld(koerper, "message")) {
        return *wert;
    }
    if (auto wert = json::string_feld(koerper, "detail")) {
        return *wert;
    }
    return {};
}

std::string chunked_dekodieren(const std::string& roh) {
    std::string r;
    std::size_t pos = 0;
    while (pos < roh.size()) {
        const std::size_t nl = roh.find("\r\n", pos);
        if (nl == std::string::npos) {
            break;
        }
        const std::string groesse_hex = roh.substr(pos, nl - pos);
        std::size_t groesse = 0;
        try {
            groesse = static_cast<std::size_t>(std::stoul(groesse_hex, nullptr, 16));
        } catch (...) {
            break;
        }
        pos = nl + 2;
        if (groesse == 0) {
            break;
        }
        if (pos + groesse > roh.size()) {
            r.append(roh.substr(pos));
            break;
        }
        r.append(roh, pos, groesse);
        pos += groesse;
        if (pos + 1 < roh.size() && roh[pos] == '\r' && roh[pos + 1] == '\n') {
            pos += 2;
        }
    }
    return r;
}

}  // namespace

HttpKlient::HttpKlient(int timeout_sekunden) : timeout_sekunden_(timeout_sekunden) {}

bool HttpKlient::url_zerlegen(const std::string& url, std::string& host, int& port, std::string& pfad,
                              std::string& fehler) {
    std::string rest = url;
    const std::string schema = "http://";
    if (rest.rfind(schema, 0) != 0) {
        fehler = "Nur unverschlüsseltes HTTP wird lokal unterstützt: " + url;
        return false;
    }
    rest = rest.substr(schema.size());
    const std::size_t slash = rest.find('/');
    std::string hostport = slash == std::string::npos ? rest : rest.substr(0, slash);
    pfad = slash == std::string::npos ? "/" : rest.substr(slash);
    port = 80;
    const std::size_t doppel = hostport.find(':');
    if (doppel == std::string::npos) {
        host = hostport;
    } else {
        host = hostport.substr(0, doppel);
        try {
            port = std::stoi(hostport.substr(doppel + 1));
        } catch (...) {
            fehler = "Ungültiger Port in URL";
            return false;
        }
    }
    if (host.empty()) {
        fehler = "Host fehlt";
        return false;
    }
    return true;
}

HttpAntwort HttpKlient::post_json(const std::string& url, const std::string& json_koerper) const {
    return senden("POST", url, json_koerper, "application/json");
}

HttpAntwort HttpKlient::get(const std::string& url) const { return senden("GET", url, {}, {}); }

HttpAntwort HttpKlient::senden(const std::string& methode, const std::string& url, const std::string& koerper,
                               const std::string& inhaltstyp) const {
    HttpAntwort antwort;
    std::string host;
    int port = 0;
    std::string pfad;
    if (!url_zerlegen(url, host, port, pfad, antwort.fehler)) {
        return antwort;
    }

#ifdef _WIN32
    WsaWaechter wsa;
    if (!wsa.ok) {
        antwort.fehler = "Winsock konnte nicht gestartet werden";
        return antwort;
    }
#endif

    addrinfo hints{};
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    addrinfo* res = nullptr;
    const std::string port_text = std::to_string(port);
    if (getaddrinfo(host.c_str(), port_text.c_str(), &hints, &res) != 0 || res == nullptr) {
        antwort.fehler = "Host nicht auflösbar: " + host;
        return antwort;
    }

    SocketTyp sock = ungueltig;
    for (addrinfo* p = res; p != nullptr; p = p->ai_next) {
        sock = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
        if (sock == ungueltig) {
            continue;
        }
        timeout_setzen(sock, timeout_sekunden_);
        if (connect(sock, p->ai_addr, static_cast<int>(p->ai_addrlen)) == 0) {
            break;
        }
        schliessen(sock);
        sock = ungueltig;
    }
    freeaddrinfo(res);
    if (sock == ungueltig) {
        antwort.fehler = "Verbindung zu " + host + ":" + port_text + " fehlgeschlagen";
        return antwort;
    }

    std::ostringstream anfrage;
    anfrage << methode << " " << pfad << " HTTP/1.1\r\n";
    anfrage << "Host: " << host << "\r\n";
    anfrage << "User-Agent: AutonomerEntwicklungsagent/1.0\r\n";
    anfrage << "Accept: application/json\r\n";
    anfrage << "Connection: close\r\n";
    if (!koerper.empty()) {
        anfrage << "Content-Type: " << (inhaltstyp.empty() ? "application/json" : inhaltstyp) << "\r\n";
        anfrage << "Content-Length: " << koerper.size() << "\r\n";
    }
    anfrage << "\r\n";
    if (!koerper.empty()) {
        anfrage << koerper;
    }
    const std::string roh = anfrage.str();
    std::size_t gesendet = 0;
    while (gesendet < roh.size()) {
#ifdef _WIN32
        int n = send(sock, roh.data() + gesendet, static_cast<int>(roh.size() - gesendet), 0);
#else
        ssize_t n = send(sock, roh.data() + gesendet, roh.size() - gesendet, 0);
#endif
        if (n <= 0) {
            antwort.fehler = "Senden fehlgeschlagen";
            schliessen(sock);
            return antwort;
        }
        gesendet += static_cast<std::size_t>(n);
    }

    std::string empfang;
    char puffer[4096];
    while (true) {
#ifdef _WIN32
        int n = recv(sock, puffer, sizeof(puffer), 0);
#else
        ssize_t n = recv(sock, puffer, sizeof(puffer), 0);
#endif
        if (n <= 0) {
            break;
        }
        empfang.append(puffer, static_cast<std::size_t>(n));
    }
    schliessen(sock);

    const std::size_t kopf_ende = empfang.find("\r\n\r\n");
    if (kopf_ende == std::string::npos) {
        antwort.fehler = "Ungültige HTTP-Antwort";
        antwort.koerper = empfang;
        return antwort;
    }
    const std::string kopf = empfang.substr(0, kopf_ende);
    std::string koerper_roh = empfang.substr(kopf_ende + 4);
    std::istringstream kopfzeilen(kopf);
    std::string statuszeile;
    std::getline(kopfzeilen, statuszeile);
    if (!statuszeile.empty() && statuszeile.back() == '\r') {
        statuszeile.pop_back();
    }
    std::istringstream st(statuszeile);
    std::string httpver;
    st >> httpver >> antwort.status;
    std::string kopf_klein = klein(kopf);
    const bool chunked = kopf_klein.find("transfer-encoding: chunked") != std::string::npos;
    if (chunked) {
        koerper_roh = chunked_dekodieren(koerper_roh);
    }
    antwort.koerper = std::move(koerper_roh);
    antwort.ok = antwort.status >= 200 && antwort.status < 300;
    if (!antwort.ok && antwort.fehler.empty()) {
        const std::string json_fehler = json_fehlermeldung(antwort.koerper);
        if (!json_fehler.empty()) {
            antwort.fehler = json_fehler;
        } else {
            antwort.fehler = "HTTP-Status " + std::to_string(antwort.status);
        }
    }
    return antwort;
}

}  // namespace agent
