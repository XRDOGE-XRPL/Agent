package de.xrdoge.agent.laufzeit

import java.io.BufferedReader
import java.io.Closeable
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class LocalSocketBridge(
    private val host: String = "127.0.0.1",
    private val port: Int = 5050,
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 100L,
    private val logStream: LogStreamManager? = null
) {
    data class CommandResult(
        val exitCode: Int,
        val output: String
    )

    data class ResourceEnvelope(
        val reservedCpuPercent: Int,
        val reservedGpuPercent: Int
    )

    fun startServer(handler: (String) -> Unit): Closeable {
        val serverSocket = ServerSocket(port, 50, InetAddress.getByName(host))
        val worker = Thread {
            while (!serverSocket.isClosed) {
                try {
                    val socket = serverSocket.accept()
                    socket.use { client ->
                        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                        val writer = OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8)
                        val payload = reader.readLine() ?: ""
                        if (payload.isNotBlank()) {
                            handler(payload)
                            writer.write("ack:$payload\n")
                            writer.flush()
                        }
                    }
                } catch (_: Exception) {
                    break
                }
            }
        }
        worker.isDaemon = true
        worker.start()
        return serverSocket
    }

    fun send(message: String): String {
        var lastError: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                Socket(host, port).use { socket ->
                    val writer = OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)
                    writer.write(message)
                    writer.write("\n")
                    writer.flush()
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
                    val response = reader.readLine() ?: ""
                    return if (response.isBlank()) "ack:$message" else response
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < maxRetries) {
                    Thread.sleep(baseDelayMs * (attempt + 1))
                }
            }
        }
        val messageText = lastError?.message ?: "connection_failed"
        logStream?.append("[socket] send failed for '$message': $messageText")
        return "socket_error:$messageText"
    }

    fun executeCommand(
        command: String,
        workingDirectory: String = ".",
        onChunk: ((String) -> Unit)? = null
    ): CommandResult {
        val directory = File(workingDirectory).takeIf { it.exists() && it.isDirectory } ?: File(".")
        return try {
            val process = ProcessBuilder(shellPath(), "-c", command)
                .directory(directory)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val text = line ?: ""
                    output.append(text).append('\n')
                    if (text.isNotBlank()) {
                        onChunk?.invoke(text)
                    }
                }
            }
            val exitCode = process.waitFor()
            CommandResult(exitCode, output.toString().trimEnd())
        } catch (e: Exception) {
            CommandResult(1, e.message ?: "Command failed")
        }
    }

    fun reserveLocalCapacity(cpuPercent: Int, gpuPercent: Int): ResourceEnvelope {
        val safeCpu = cpuPercent.coerceIn(20, 30)
        val safeGpu = gpuPercent.coerceIn(20, 30)
        logStream?.append("[socket] reserved cpu=$safeCpu gpu=$safeGpu")
        return ResourceEnvelope(safeCpu, safeGpu)
    }

    private fun shellPath(): String = if (File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"
}

fun isTermuxEnvironment(): Boolean {
    return try {
        File("/data/data/com.termux").exists()
    } catch (_: Exception) {
        false
    }
}
