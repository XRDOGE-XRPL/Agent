package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ExecutionRouter {
    enum class Provider {
        LOCAL,
        TOWER,
        PARTNER,
        CLOUD_POOL
    }

    data class ResourcePolicy(
        val reservedCpuPercent: Int = 25,
        val reservedGpuPercent: Int = 25,
        val treasurySharePercent: Int = 5
    ) {
        init {
            require(reservedCpuPercent in 20..30) { "CPU reserve must stay within 20%-30%" }
            require(reservedGpuPercent in 20..30) { "GPU reserve must stay within 20%-30%" }
            require(treasurySharePercent == 5) { "Treasury diversion is fixed at 5%" }
        }
    }

    data class Telemetry(
        val batteryPercent: Int = 100,
        val isOnHomeNetwork: Boolean = true,
        val networkOnline: Boolean = true,
        val latencyMs: Int = 50,
        val estimatedComplexity: Int = 1
    )

    data class RouteDecision(
        val provider: Provider,
        val reason: String,
        val estimatedComplexity: Int,
        val reservedCpuPercent: Int = 25,
        val reservedGpuPercent: Int = 25,
        val treasurySharePercent: Int = 5
    )

    private val _lastRoute = MutableStateFlow<RouteDecision?>(null)
    val lastRoute: StateFlow<RouteDecision?> = _lastRoute.asStateFlow()

    fun route(
        taskDescription: String,
        telemetry: Telemetry = Telemetry(),
        resourcePolicy: ResourcePolicy = ResourcePolicy()
    ): RouteDecision {
        val complexity = estimateComplexity(taskDescription)
        val expectedCpuReserve = computePoolReserve(complexity, resourcePolicy.reservedCpuPercent)
        val expectedGpuReserve = computePoolReserve(complexity, resourcePolicy.reservedGpuPercent)
        val target = when {
            !telemetry.networkOnline -> RouteDecision(
                Provider.LOCAL,
                "Network unavailable; falling back to local execution",
                complexity,
                expectedCpuReserve,
                expectedGpuReserve,
                resourcePolicy.treasurySharePercent
            )
            telemetry.batteryPercent <= 15 && complexity <= 2 -> RouteDecision(
                Provider.LOCAL,
                "Battery saver mode; local inference only",
                complexity,
                expectedCpuReserve,
                expectedGpuReserve,
                resourcePolicy.treasurySharePercent
            )
            complexity >= 7 -> RouteDecision(
                Provider.CLOUD_POOL,
                "High complexity requires external pool leasing while reserving 20%-30% headroom",
                complexity,
                expectedCpuReserve,
                expectedGpuReserve,
                resourcePolicy.treasurySharePercent
            )
            telemetry.isOnHomeNetwork && complexity >= 3 -> RouteDecision(
                Provider.TOWER,
                "Home network and higher workload; use tower compute",
                complexity,
                expectedCpuReserve,
                expectedGpuReserve,
                resourcePolicy.treasurySharePercent
            )
            complexity >= 5 -> RouteDecision(
                Provider.PARTNER,
                "Complex workload benefits from partner routing with local resource budget protection",
                complexity,
                expectedCpuReserve,
                expectedGpuReserve,
                resourcePolicy.treasurySharePercent
            )
            else -> RouteDecision(
                Provider.LOCAL,
                "Small or medium task fits local execution",
                complexity,
                expectedCpuReserve,
                expectedGpuReserve,
                resourcePolicy.treasurySharePercent
            )
        }
        _lastRoute.value = target
        return target
    }

    fun estimateComplexity(taskDescription: String): Int {
        val normalized = taskDescription.lowercase(Locale.ROOT)
        var score = 1
        if (normalized.contains("train") || normalized.contains("llm") || normalized.contains("model") || normalized.contains("video") || normalized.contains("build")) score += 2
        if (normalized.contains("telegram") || normalized.contains("bot") || normalized.contains("scraper") || normalized.contains("database") || normalized.contains("service")) score += 1
        if (normalized.contains("pawn") || normalized.contains("samp") || normalized.contains("open.mp") || normalized.contains("game")) score += 2
        if (normalized.contains("multi") || normalized.contains("parallel") || normalized.contains("cron") || normalized.contains("worker")) score += 2
        return score.coerceIn(1, 8)
    }

    fun computePoolReserve(complexity: Int, baseReservePercent: Int): Int {
        val multiplier = when {
            complexity >= 7 -> 1.20
            complexity >= 5 -> 1.10
            else -> 1.0
        }
        return (baseReservePercent * multiplier).toInt().coerceIn(20, 30)
    }

    fun leaseCloudNow(taskDescription: String, customerId: String = "internal-primary"): CloudLeaseRouter.LeaseReservation {
        val route = route(taskDescription)
        return CloudLeaseRouter().reserve(
            customerId = customerId,
            workload = taskDescription,
            route = route
        )
    }
}
