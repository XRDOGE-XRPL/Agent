package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ExecutionRouter {
    enum class Provider {
        LOCAL,
        TOWER,
        PARTNER
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
        val estimatedComplexity: Int
    )

    private val _lastRoute = MutableStateFlow<RouteDecision?>(null)
    val lastRoute: StateFlow<RouteDecision?> = _lastRoute.asStateFlow()

    fun route(taskDescription: String, telemetry: Telemetry = Telemetry()): RouteDecision {
        val complexity = estimateComplexity(taskDescription)
        val target = when {
            !telemetry.networkOnline -> RouteDecision(Provider.LOCAL, "Network unavailable; falling back to local execution", complexity)
            telemetry.batteryPercent <= 15 && complexity <= 2 -> RouteDecision(Provider.LOCAL, "Battery saver mode; local inference only", complexity)
            telemetry.isOnHomeNetwork && complexity >= 3 -> RouteDecision(Provider.TOWER, "Home network and higher workload; use tower compute", complexity)
            complexity >= 6 -> RouteDecision(Provider.PARTNER, "High complexity requires external provider", complexity)
            else -> RouteDecision(Provider.LOCAL, "Small or medium task fits local execution", complexity)
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
}
