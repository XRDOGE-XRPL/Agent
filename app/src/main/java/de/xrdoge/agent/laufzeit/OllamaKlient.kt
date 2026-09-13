package de.xrdoge.agent.laufzeit

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class OllamaKlient(
    private val basisUrl: String,
    private val modell: String,
    private val timeoutMs: Int = 120_000
) {
    fun anfragen(prompt: String): String {
        val url = URL(basisUrl.trimEnd('/') + "/api/generate")
        val koerper = buildString {
            append("{")
            append("\"model\":\"").append(jsonEscape(modell)).append("\",")
            append("\"prompt\":\"").append(jsonEscape(prompt)).append("\",")
            append("\"stream\":false")
            append("}")
        }
        val verbindung = url.openConnection() as HttpURLConnection
        verbindung.requestMethod = "POST"
        verbindung.connectTimeout = timeoutMs
        verbindung.readTimeout = timeoutMs
        verbindung.doOutput = true
        verbindung.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        OutputStreamWriter(verbindung.outputStream, StandardCharsets.UTF_8).use { it.write(koerper) }
        val stream = if (verbindung.responseCode in 200..299) {
            verbindung.inputStream
        } else {
            verbindung.errorStream ?: verbindung.inputStream
        }
        val antwort = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (verbindung.responseCode !in 200..299) {
            throw IllegalStateException("Ollama HTTP ${verbindung.responseCode}: $antwort")
        }
        return jsonStringFeld(antwort, "response")
            ?: throw IllegalStateException("Ollama-Antwort ohne Feld response")
    }

    fun erreichbar(): Boolean {
        return try {
            val url = URL(basisUrl.trimEnd('/') + "/api/tags")
            val verbindung = url.openConnection() as HttpURLConnection
            verbindung.requestMethod = "GET"
            verbindung.connectTimeout = 3_000
            verbindung.readTimeout = 3_000
            verbindung.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        fun jsonEscape(roh: String): String {
            val builder = StringBuilder()
            for (zeichen in roh) {
                when (zeichen) {
                    '\\' -> builder.append("\\\\")
                    '"' -> builder.append("\\\"")
                    '\n' -> builder.append("\\n")
                    '\r' -> builder.append("\\r")
                    '\t' -> builder.append("\\t")
                    else -> builder.append(zeichen)
                }
            }
            return builder.toString()
        }

        fun jsonStringFeld(json: String, schluessel: String): String? {
            val nadel = "\"$schluessel\""
            var pos = json.indexOf(nadel)
            while (pos >= 0) {
                var nach = pos + nadel.length
                while (nach < json.length && json[nach].isWhitespace()) nach++
                if (nach < json.length && json[nach] == ':') {
                    nach++
                    while (nach < json.length && json[nach].isWhitespace()) nach++
                    if (nach >= json.length || json[nach] != '"') return null
                    nach++
                    val inhalt = StringBuilder()
                    var escape = false
                    while (nach < json.length) {
                        val c = json[nach]
                        if (escape) {
                            inhalt.append(
                                when (c) {
                                    'n' -> '\n'
                                    'r' -> '\r'
                                    't' -> '\t'
                                    else -> c
                                }
                            )
                            escape = false
                        } else if (c == '\\') {
                            escape = true
                        } else if (c == '"') {
                            return inhalt.toString()
                        } else {
                            inhalt.append(c)
                        }
                        nach++
                    }
                    return null
                }
                pos = json.indexOf(nadel, pos + 1)
            }
            return null
        }
    }
}
