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
    fun executionRouterReservesLocalHeadroomForPoolDelegation() {
        val router = ExecutionRouter()
        val route = router.route(
            "Reserve local CPU and GPU headroom while a cloud pool handles the heaviest batch inference",
            ExecutionRouter.Telemetry(
                batteryPercent = 85,
                isOnHomeNetwork = true,
                networkOnline = true,
                latencyMs = 90,
                estimatedComplexity = 8
            )
        )
        assertEquals(ExecutionRouter.Provider.CLOUD_POOL, route.provider)
        assertTrue(route.reservedCpuPercent in 20..30)
        assertTrue(route.reservedGpuPercent in 20..30)
        assertEquals(5, route.treasurySharePercent)
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
