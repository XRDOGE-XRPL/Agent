#pragma once

#include <string>

namespace agent {

struct HttpAntwort {
    bool ok = false;
    int status = 0;
    std::string koerper;
    std::string fehler;
};

class HttpKlient {
public:
    explicit HttpKlient(int timeout_sekunden = 120);
    HttpAntwort post_json(const std::string& url, const std::string& json_koerper) const;
    HttpAntwort get(const std::string& url) const;

    static bool url_zerlegen(const std::string& url, std::string& host, int& port,
                             std::string& pfad, std::string& fehler);

private:
    int timeout_sekunden_;
    HttpAntwort senden(const std::string& methode, const std::string& url,
                       const std::string& koerper, const std::string& inhaltstyp) const;
};

}  // namespace agent
