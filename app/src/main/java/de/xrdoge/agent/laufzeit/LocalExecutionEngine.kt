package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalExecutionEngine(
    private val logStream: LogStreamManager? = null
) {
    private val mutex = Mutex()

    suspend fun executeCommandSafe(command: String): String {
        return mutex.withLock {
            executeCommand(command)
        }
    }

    suspend fun executeCommand(command: String): String {
        return try {
            val process = ProcessBuilder(shPath(), "-c", command)
                .redirectErrorStream(true)
                .start()
            process.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun execute(
        session: AgentSession,
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int = 1
    ): String {
        val repoLine = "Repo: ${session.repository}"
        val localLine = "Local execution started for '${session.task}' in $arbeitsverzeichnis"
        logStream?.append(repoLine)
        logStream?.append(localLine)
        val command = "pwd && echo 'local exec ok' && echo 'task=$aufgabe'"
        return try {
            val result = executeCommand(command)
            val summary = if (result.isBlank()) "Local execution completed: $aufgabe" else result.trim()
            logStream?.append("[runtime] ${summary.take(240)}")
            summary
        } catch (e: Exception) {
            val message = "Execution failed: ${e.message}"
            logStream?.append(message)
            message
        }
    }

    private fun shPath(): String {
        return if (java.io.File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"
    }
}
