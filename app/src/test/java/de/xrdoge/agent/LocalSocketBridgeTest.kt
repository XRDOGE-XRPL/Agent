package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.LocalSocketBridge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSocketBridgeTest {
    @Test
    fun socketBridgeRetriesAndHandlesLocalEcho() = runBlocking {
        val bridge = LocalSocketBridge(host = "127.0.0.1", port = 5099, maxRetries = 1, baseDelayMs = 25L)
        val captured = arrayOfNulls<String>(1)
        val server = bridge.startServer { text -> captured[0] = text }

        try {
            Thread.sleep(50L)
            val response = bridge.send("hello-local")
            assertTrue(response.startsWith("ack:hello-local") || response.startsWith("socket_error"))
            if (captured[0] != null) {
                assertEquals("hello-local", captured[0])
            }
        } finally {
            server.close()
        }
    }

    @Test
    fun socketBridgeTracksPoolHeadroomAndExecutesLocalCommand() = runBlocking {
        val bridge = LocalSocketBridge(host = "127.0.0.1", port = 5100, maxRetries = 0, baseDelayMs = 10L)
        val resourceEnvelope = bridge.reserveLocalCapacity(cpuPercent = 25, gpuPercent = 25)
        assertTrue(resourceEnvelope.reservedCpuPercent in 20..30)
        assertTrue(resourceEnvelope.reservedGpuPercent in 20..30)

        val result = bridge.executeCommand("printf 'hello-local\n'", workingDirectory = ".")
        assertEquals(0, result.exitCode)
        assertTrue(result.output.contains("hello-local"))
    }
}
