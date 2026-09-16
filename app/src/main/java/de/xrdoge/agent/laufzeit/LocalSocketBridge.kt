package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.math.min

class LocalSocketBridge(
    private val host: String = "127.0.0.1",
    private val port: Int = 5050,
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 100L,
    private val logStream: LogStreamManager? = null
) {
    private val lock = Any()

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var isListening = false

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val command: String
    )

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

    suspend fun executeCommand(
        command: String,
        workingDirectory: String = ".",
        timeoutMs: Long = 15_000L,
        onChunk: ((String) -> Unit)? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        val resolvedDir = workingDirectory.ifBlank { "." }
        val safeDirectory = File(resolvedDir).takeIf { it.exists() && it.isDirectory() } ?: File(".")
        val commandLine = if (isWindows()) listOf("cmd", "/C", command) else listOf("/bin/sh", "-lc", command)
        val process = ProcessBuilder(commandLine)
            .directory(safeDirectory)
            .redirectErrorStream(true)
            .start()

        val output = StringBuilder()
        val reader = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
        val buffer = CharArray(2048)
        var charsRead: Int

        while (reader.read(buffer).also { charsRead = it } != -1) {
            val chunk = String(buffer, 0, charsRead)
            output.append(chunk)
            val normalized = chunk.replace("\r", "").trimEnd()
            if (normalized.isNotBlank()) {
                onChunk?.invoke(normalized)
                logStream?.append("[local-process] $normalized")
            }
        }

        val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
        val exitCode = if (finished) process.exitValue() else -1
        if (!finished) {
            process.destroyForcibly()
            val timeoutMessage = "[local-process] command timed out after ${timeoutMs}ms: $command"
            onChunk?.invoke(timeoutMessage)
            logStream?.append(timeoutMessage)
            return@withContext CommandResult(exitCode, output.toString() + timeoutMessage, command)
        }

        CommandResult(exitCode, output.toString().trim(), command)
    }

    fun startServer(listener: (String) -> Unit): AutoCloseable = startServer(listener, null)

    fun startServer(listener: (String) -> Unit, logSink: ((String) -> Unit)?): AutoCloseable = synchronized(lock) {
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
                        logSink?.invoke(payload)
                        logStream?.append("[socket] $payload")
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

    private fun isWindows(): Boolean = System.getProperty("os.name")?.startsWith("Windows", ignoreCase = true) == true
}
