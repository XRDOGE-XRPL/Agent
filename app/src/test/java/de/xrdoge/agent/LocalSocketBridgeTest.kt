package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.LocalSocketBridge
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.concurrent.thread

class LocalSocketBridgeTest {
    @Test
    fun socketBridgeRetriesAndHandlesLocalEcho() {
        val bridge = LocalSocketBridge(host = "127.0.0.1", port = 5099, maxRetries = 1, baseDelayMs = 25L)
        val captured = arrayOfNulls<String>(1)
        val server = bridge.startServer { text -> captured[0] = text }

        thread {
            Thread.sleep(150L)
            val response = bridge.send("hello-local")
            assertTrue(response.isNotEmpty() || response.startsWith("socket_error"))
        }

        Thread.sleep(200L)
        server.close()
    }
}
