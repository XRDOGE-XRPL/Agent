package de.xrdoge.agent.laufzeit

class CloudLeaseRouter {
    data class LeaseReservation(
        val customerId: String,
        val workload: String,
        val provider: String,
        val cpuQuotaPercent: Int,
        val gpuQuotaPercent: Int,
        val treasurySharePercent: Int = 5,
        val id: String = java.util.UUID.randomUUID().toString().take(8)
    )

    fun reserve(
        customerId: String,
        workload: String,
        route: ExecutionRouter.RouteDecision,
        provider: String = "cloud-pool"
    ): LeaseReservation {
        val cpuQuota = route.reservedCpuPercent.coerceIn(20, 30)
        val gpuQuota = route.reservedGpuPercent.coerceIn(20, 30)
        return LeaseReservation(
            customerId = customerId,
            workload = workload,
            provider = provider,
            cpuQuotaPercent = cpuQuota,
            gpuQuotaPercent = gpuQuota,
            treasurySharePercent = route.treasurySharePercent
        )
    }
}