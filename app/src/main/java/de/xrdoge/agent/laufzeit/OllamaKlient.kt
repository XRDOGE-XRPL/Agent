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

        private fun jsonUnescape(roh: String): String {
            val out = StringBuilder()
            var pos = 0
            while (pos < roh.length) {
                if (roh[pos] == '\\' && pos + 1 < roh.length) {
                    when (val escaped = roh[pos + 1]) {
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        '"' -> out.append('"')
                        '\\' -> out.append('\\')
                        '/' -> out.append('/')
                        'u' -> {
                            if (pos + 5 < roh.length) {
                                val hex = roh.substring(pos + 2, pos + 6)
                                if (hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                    out.append(hex.toInt(16).toChar())
                                    pos += 4
                                } else {
                                    out.append('u')
                                }
                            } else {
                                out.append('u')
                            }
                        }
                        else -> out.append(escaped)
                    }
                    pos += 2
                    continue
                }
                out.append(roh[pos])
                pos++
            }
            return out.toString()
        }

        fun jsonStringFeld(json: String, schluessel: String): String? {
            val nadel = "\"$schluessel\""
            var pos = 0
            while (pos < json.length) {
                if (json[pos] == '"') {
                    val start = pos
                    val end = start + nadel.length
                    if (end <= json.length && json.substring(start, end) == nadel) {
                        var nach = end
                        while (nach < json.length && json[nach].isWhitespace()) nach++
                        if (nach < json.length && json[nach] == ':') {
                            nach++
                            while (nach < json.length && json[nach].isWhitespace()) nach++
                            if (nach >= json.length || json[nach] != '"') return null
                            nach++
                            val inhalt = StringBuilder()
                            while (nach < json.length) {
                                val ch = json[nach]
                                if (ch == '\\') {
                                    if (nach + 1 >= json.length) {
                                        inhalt.append('\\')
                                        break
                                    }
                                    when (val escaped = json[nach + 1]) {
                                        'b', 'f', 'n', 'r', 't', '"', '\\', '/' -> {
                                            inhalt.append('\\')
                                            inhalt.append(escaped)
                                            nach += 2
                                            continue
                                        }
                                        'u' -> {
                                            if (nach + 5 < json.length) {
                                                val hex = json.substring(nach + 2, nach + 6)
                                                if (hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
                                                    inhalt.append('\\')
                                                    inhalt.append('u')
                                                    inhalt.append(hex)
                                                    nach += 6
                                                    continue
                                                }
                                            }
                                            inhalt.append('u')
                                            nach += 2
                                            continue
                                        }
                                        else -> {
                                            inhalt.append(escaped)
                                            nach += 2
                                            continue
                                        }
                                    }
                                }
                                if (ch == '"') {
                                    return jsonUnescape(inhalt.toString())
                                }
                                inhalt.append(ch)
                                nach++
                            }
                            return jsonUnescape(inhalt.toString())
                        }
                    }
                }
                pos++
            }
            return null
        }
    }
}
