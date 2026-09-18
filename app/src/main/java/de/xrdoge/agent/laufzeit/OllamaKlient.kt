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
    private val timeoutMs: Int = 120_000,
    private val numCtx: Int = 4096,
    private val maxPromptChars: Int = 6_000
) {
    fun anfragen(prompt: String): String {
        val url = URL(basisUrl.trimEnd('/') + "/api/generate")
        val sichererPrompt = begrenzePrompt(prompt, maxPromptChars)
        val koerper = buildString {
            append("{")
            append("\"model\":\"").append(jsonEscape(modell)).append("\",")
            append("\"prompt\":\"").append(jsonEscape(sichererPrompt)).append("\",")
            append("\"stream\":false,")
            append("\"options\":{\"num_ctx\":").append(kotlin.math.max(512, numCtx)).append(",\"temperature\":0.2}")
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
        fun begrenzePrompt(prompt: String, maxChars: Int = 6_000): String {
            if (prompt.length <= maxChars) return prompt
            val suffix = "... [gekürzt]"
            val keep = maxChars - suffix.length
            return if (keep <= 0) {
                prompt.take(maxChars)
            } else {
                prompt.take(keep) + suffix
            }
        }

        private fun isHexDigit(ch: Char): Boolean = ch.isDigit() || ch.lowercaseChar() in 'a'..'f' || ch.lowercaseChar() in 'A'..'F'

        private fun decodeUnicodeHex(json: String, startIndex: Int): Pair<String, Int>? {
            if (startIndex + 4 > json.length) return null
            val hex = json.substring(startIndex, startIndex + 4)
            if (!hex.all(::isHexDigit)) return null

            var codepoint = hex.toInt(16)
            var cursor = startIndex + 4

            if (codepoint in 0xD800..0xDBFF && cursor + 6 <= json.length && json[cursor] == '\\' && json[cursor + 1] == 'u') {
                val lowHex = json.substring(cursor + 2, cursor + 6)
                if (lowHex.all(::isHexDigit)) {
                    val lowCodepoint = lowHex.toInt(16)
                    if (lowCodepoint in 0xDC00..0xDFFF) {
                        codepoint = 0x10000 + ((codepoint - 0xD800) shl 10) + (lowCodepoint - 0xDC00)
                        cursor += 6
                    }
                }
            }

            if (codepoint in 0xD800..0xDFFF) {
                return "\uFFFD" to cursor
            }
            if (codepoint < 0 || codepoint > 0x10FFFF) {
                return "\uFFFD" to cursor
            }
            return String(Character.toChars(codepoint)) to cursor
        }

        fun jsonEscape(roh: String): String {
            val builder = StringBuilder()
            for (zeichen in roh) {
                when (zeichen) {
                    '\\' -> builder.append("\\\\")
                    '"' -> builder.append("\\\"")
                    '\b' -> builder.append("\\b")
                    '\u000C' -> builder.append("\\f")
                    '\n' -> builder.append("\\n")
                    '\r' -> builder.append("\\r")
                    '\t' -> builder.append("\\t")
                    else -> if (zeichen.code < 0x20) {
                        builder.append("\\u")
                        builder.append(zeichen.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(zeichen)
                    }
                }
            }
            return builder.toString()
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
                                when {
                                    ch == '\\' -> {
                                        if (nach + 1 >= json.length) {
                                            inhalt.append('\uFFFD')
                                            return inhalt.toString().ifEmpty { null }
                                        }
                                        when (val escaped = json[nach + 1]) {
                                            'b' -> inhalt.append('\b').also { nach += 2 }
                                            'f' -> inhalt.append('\u000C').also { nach += 2 }
                                            'n' -> inhalt.append('\n').also { nach += 2 }
                                            'r' -> inhalt.append('\r').also { nach += 2 }
                                            't' -> inhalt.append('\t').also { nach += 2 }
                                            '"' -> inhalt.append('"').also { nach += 2 }
                                            '\\' -> inhalt.append('\\').also { nach += 2 }
                                            '/' -> inhalt.append('/').also { nach += 2 }
                                            'u' -> {
                                                val decoded = decodeUnicodeHex(json, nach + 2)
                                                if (decoded == null) {
                                                    inhalt.append('\uFFFD')
                                                    val fallbackEnd = (nach + 6).coerceAtMost(json.length)
                                                    nach = fallbackEnd
                                                } else {
                                                    val (value, nextIndex) = decoded
                                                    inhalt.append(value)
                                                    nach = nextIndex
                                                }
                                            }
                                            else -> {
                                                inhalt.append('\uFFFD')
                                                nach += 2
                                            }
                                        }
                                    }
                                    ch == '"' -> return inhalt.toString()
                                    else -> {
                                        inhalt.append(ch)
                                        nach++
                                    }
                                }
                            }
                            return inhalt.toString().ifEmpty { null }
                        }
                    }
                }
                pos++
            }
            return null
        }
    }
}
