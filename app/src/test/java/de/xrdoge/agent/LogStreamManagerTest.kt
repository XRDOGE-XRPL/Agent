package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.LogStreamManager
import org.junit.Assert.assertEquals
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
}
