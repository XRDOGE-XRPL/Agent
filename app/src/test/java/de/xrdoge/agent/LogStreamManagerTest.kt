package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.AgentSession
import de.xrdoge.agent.laufzeit.LocalExecutionEngine
import de.xrdoge.agent.laufzeit.LogStreamManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogStreamManagerTest {
    @Test
    fun logStreamManagerKeepsLatestEntriesThreadSafe() {
        val logStream = LogStreamManager(maxEntries = 3)

        logStream.append("one")
        logStream.append("two")
        logStream.append("three")
        logStream.append("four")

        assertEquals(listOf("two", "three", "four"), logStream.logsFlow.value)

        logStream.replace(listOf("alpha", "beta"))
        assertEquals(listOf("alpha", "beta"), logStream.logsFlow.value)
    }

    @Test
    fun localExecutionEngineRecordsRuntimeLogs() {
        val logStream = LogStreamManager(maxEntries = 16)
        val engine = LocalExecutionEngine(logStream = logStream)
        val session = AgentSession(repository = ".", task = "probe runtime")

        val result = engine.execute(
            session = session,
            arbeitsverzeichnis = ".",
            aufgabe = "probe runtime",
            ollamaUrl = "http://127.0.0.1:11434",
            modell = "qwen2.5-coder",
            maxIterationen = 1
        )

        assertTrue(result.isNotBlank())
        assertTrue(logStream.logsFlow.value.any { it.contains("Repo:") || it.contains("Local execution") })
    }
}
