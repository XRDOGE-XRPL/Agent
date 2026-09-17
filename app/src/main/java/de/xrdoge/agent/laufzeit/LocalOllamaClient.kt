package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class LocalOllamaClient(
    private val baseUrl: String = "http://127.0.0.1:11434",
    private val model: String = "llama3.2",
    private val bridge: LocalSocketBridge = LocalSocketBridge(),
    private val logStream: LogStreamManager? = null
) {
    suspend fun reachable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = URL(baseUrl.trimEnd('/') + "/api/tags")
            val connection = request.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3_000
            connection.readTimeout = 3_000
            val code = connection.responseCode
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val normalizedUrl = baseUrl.trimEnd('/')
        val payload = buildString {
            append("{\"model\":\"")
            append(OllamaKlient.jsonEscape(model))
            append("\",\"prompt\":\"")
            append(OllamaKlient.jsonEscape(prompt))
            append("\",\"stream\":false}")
        }

        logStream?.append("[ollama] requesting model=$model")
        bridge.send("ollama:${normalizedUrl}|${OllamaKlient.jsonEscape(model)}|${prompt.take(256)}")

        val connection = URL(normalizedUrl + "/api/generate").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { it.write(payload) }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }

        val json = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (connection.responseCode !in 200..299) {
            val message = "Ollama HTTP ${connection.responseCode}: $json"
            logStream?.append("[ollama] $message")
            throw IllegalStateException(message)
        }

        val response = OllamaKlient.jsonStringFeld(json, "response")
            ?: throw IllegalStateException("Ollama-Antwort ohne Feld response")
        logStream?.append("[ollama] response ready")
        response
    }
}
