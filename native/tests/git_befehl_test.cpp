#include "agent/git.h"

#include <iostream>

int main() {
    if (agent::GitDienst::status_kommando().find("git status") == std::string::npos) {
        std::cerr << "status_kommando ungültig\n";
        return 1;
    }
    if (agent::GitDienst::diff_kommando().find("git diff") == std::string::npos) {
        std::cerr << "diff_kommando ungültig\n";
        return 1;
    }
    auto args = agent::GitDienst::commit_argumente("Nachricht");
    if (args.size() != 4 || args[0] != "git" || args[1] != "commit" || args[3] != "Nachricht") {
        std::cerr << "commit_argumente ungültig\n";
        return 1;
    }
    return 0;
}
