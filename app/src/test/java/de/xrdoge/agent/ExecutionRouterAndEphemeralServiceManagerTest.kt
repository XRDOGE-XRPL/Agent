package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.EphemeralServiceManager
import de.xrdoge.agent.laufzeit.ExecutionRouter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionRouterAndEphemeralServiceManagerTest {
    @Test
    fun executionRouterSelectsTowerForHeavierHomeNetworkTasks() {
        val router = ExecutionRouter()
        val route = router.route(
            "Train a large multimodal model pipeline on the home tower",
            ExecutionRouter.Telemetry(
                batteryPercent = 80,
                isOnHomeNetwork = true,
                networkOnline = true,
                latencyMs = 80,
                estimatedComplexity = 7
            )
        )
        assertEquals(ExecutionRouter.Provider.TOWER, route.provider)
    }

    @Test
    fun ephemeralServiceManagerProvisionsAndCleansUpOnTtl() = runBlocking {
        val manager = EphemeralServiceManager()
        val service = manager.provision("demo-db", "sqlite", provider = "local", ttlMs = 100L)
        assertEquals("ACTIVE", service.status)
        assertEquals(1, manager.list().size)

        val terminated = manager.terminate(service.id)
        assertTrue(terminated)
        assertEquals(0, manager.list().size)
    }
}
