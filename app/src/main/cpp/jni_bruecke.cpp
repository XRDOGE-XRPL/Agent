#include <jni.h>

#include "agent/konfiguration.h"
#include "agent/ollama.h"
#include "agent/protokollierung.h"
#include "agent/schleife.h"
#include "agent/version.h"

#include <android/log.h>
#include <string>

namespace {

std::string jstring_zu_std(JNIEnv* env, jstring wert) {
    if (wert == nullptr) {
        return {};
    }
    const char* zeichen = env->GetStringUTFChars(wert, nullptr);
    std::string ergebnis = zeichen ? zeichen : "";
    env->ReleaseStringUTFChars(wert, zeichen);
    return ergebnis;
}

jstring std_zu_jstring(JNIEnv* env, const std::string& wert) {
    return env->NewStringUTF(wert.c_str());
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_de_xrdoge_agent_bruecke_NativeBruecke_version(JNIEnv* env, jobject) {
    const std::string text = std::string(AGENT_NAME) + " " + AGENT_VERSION;
    return std_zu_jstring(env, text);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_de_xrdoge_agent_bruecke_NativeBruecke_pruefeOllama(JNIEnv* env, jobject, jstring url) {
    agent::Konfiguration konfig;
    konfig.ollama_url = jstring_zu_std(env, url);
    agent::OllamaKlient klient(konfig);
    std::string details;
    return klient.erreichbar(details) ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_de_xrdoge_agent_bruecke_NativeBruecke_starteSchleife(JNIEnv* env, jobject, jstring arbeitsverzeichnis,
                                                         jstring aufgabe, jstring ollamaUrl, jstring modell,
                                                         jint maxIterationen) {
    agent::Konfiguration konfig;
    konfig.arbeitsverzeichnis = jstring_zu_std(env, arbeitsverzeichnis);
    konfig.aufgabe = jstring_zu_std(env, aufgabe);
    konfig.ollama_url = jstring_zu_std(env, ollamaUrl);
    konfig.modell = jstring_zu_std(env, modell);
    konfig.max_iterationen = maxIterationen > 0 ? static_cast<int>(maxIterationen) : 8;

    agent::Protokollierung log;
    log.setzen_beobachter([](const agent::LogEintrag& eintrag) {
        int prio = ANDROID_LOG_INFO;
        switch (eintrag.stufe) {
            case agent::LogStufe::warnung:
                prio = ANDROID_LOG_WARN;
                break;
            case agent::LogStufe::fehler:
                prio = ANDROID_LOG_ERROR;
                break;
            case agent::LogStufe::erfolg:
                prio = ANDROID_LOG_INFO;
                break;
            default:
                break;
        }
        __android_log_print(prio, "AgentKern", "%s", eintrag.nachricht.c_str());
    });

    agent::OllamaKlient llm(konfig);
    agent::AgentSchleife schleife(konfig, llm, log);
    const agent::AgentErgebnis ergebnis = schleife.ausfuehren();
    const std::string text = std::string(ergebnis.erfolgreich ? "ERFOLG: " : "FEHLGESCHLAGEN: ") +
                             ergebnis.zusammenfassung;
    return std_zu_jstring(env, text);
}
