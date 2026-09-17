package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LocalExecutionEngine {
    private val mutex = Mutex()

    suspend fun executeCommandSafe(command: String): String {
        // Kritischen Bereich verlassen, bevor suspendierende Aufrufe getätigt werden
        return mutex.withLock {
            // Zustand vorbereiten, falls nötig
        }.let {
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

    private fun shPath(): String {
        return if (java.io.File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"
    }
}
