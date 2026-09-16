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

    suspend fun send(payload: String): String = withContext(Dispatchers.IO) {
        var attempt = 0
        while (true) {
            try {
                Socket(host, port).use { socket ->
                    val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                    writer.write(payload)
                    writer.flush()
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                    val response = reader.readLine() ?: ""
                    return@withContext response
                }
            } catch (_: Exception) {
                if (attempt >= maxRetries) {
                    return@withContext "socket_error: $payload"
                }
                attempt += 1
                val delay = min(baseDelayMs * (1L shl attempt), 2_000L)
                Thread.sleep(delay)
            }
        }
    }

    fun startServer(listener: (String) -> Unit): AutoCloseable = synchronized(lock) {
        val socket = ServerSocket(port)
        serverSocket = socket
        Thread {
            while (!socket.isClosed) {
                try {
                    val accepted = socket.accept()
                    val reader = BufferedReader(InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8))
                    val text = reader.readLine() ?: ""
                    listener(text)
                    accepted.close()
                } catch (_: Exception) {
                    if (socket.isClosed) break
                }
            }
        }.apply { isDaemon = true; start() }
        object : AutoCloseable {
            override fun close() {
                synchronized(lock) {
                    serverSocket?.close()
                    serverSocket = null
                }
            }
        }
    }
}
