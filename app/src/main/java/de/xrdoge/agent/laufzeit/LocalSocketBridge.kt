package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlin.math.min

class LocalSocketBridge(
    private val host: String = "127.0.0.1",
    private val port: Int = 5050,
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 100L
) {
    private val lock = Any()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isListening = false

    suspend fun send(payload: String): String = withContext(Dispatchers.IO) {
        var attempt = 0
        while (attempt <= maxRetries) {
            try {
                return@withContext Socket(host, port).use { socket ->
                    socket.soTimeout = 2_000
                    val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                    writer.write(payload.trim())
                    writer.write("\n")
                    writer.flush()

                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                    val response = reader.readLine()
                    if (!response.isNullOrBlank()) {
                        response
                    } else {
                        "socket_ack: ${payload.take(128)}"
                    }
                }
            } catch (error: Exception) {
                if (attempt >= maxRetries) {
                    return@withContext "socket_error: ${error.javaClass.simpleName}: ${error.message ?: "unknown"}"
                }
                val delay = min(baseDelayMs * (1L shl attempt), 2_000L)
                Thread.sleep(delay)
                attempt += 1
            }
        }
        "socket_error: unavailable"
    }

    fun startServer(listener: (String) -> Unit): AutoCloseable = synchronized(lock) {
        if (isListening) {
            return@synchronized object : AutoCloseable {
                override fun close() = stopServer()
            }
        }

        val socket = ServerSocket(port)
        serverSocket = socket
        isListening = true

        Thread {
            while (!socket.isClosed && isListening) {
                try {
                    val accepted = socket.accept()
                    val reader = BufferedReader(InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8))
                    val payload = reader.readLine() ?: ""
                    if (payload.isNotBlank()) {
                        listener(payload)
                        val writer = OutputStreamWriter(accepted.getOutputStream(), StandardCharsets.UTF_8)
                        writer.write("ack:${payload}\n")
                        writer.flush()
                    }
                    accepted.close()
                } catch (_: Exception) {
                    if (socket.isClosed || !isListening) return@Thread
                }
            }
        }.apply {
            isDaemon = true
            name = "local-socket-bridge"
            start()
        }

        object : AutoCloseable {
            override fun close() = stopServer()
        }
    }

    fun stopServer() = synchronized(lock) {
        isListening = false
        serverSocket?.close()
        serverSocket = null
    }
}
