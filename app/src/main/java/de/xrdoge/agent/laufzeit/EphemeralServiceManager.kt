package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID

class EphemeralServiceManager(
    private val logStream: LogStreamManager? = null
) {
    private val mutex = Mutex()
    private val services = linkedMapOf<String, ManagedService>()
    private val _servicesFlow = MutableStateFlow<List<ManagedService>>(emptyList())
    val servicesFlow: StateFlow<List<ManagedService>> = _servicesFlow.asStateFlow()

    data class ManagedService(
        val id: String = UUID.randomUUID().toString(),
        val name: String,
        val type: String,
        val provider: String,
        val ttlMs: Long,
        val createdAt: Long = System.currentTimeMillis(),
        val expiresAt: Long = createdAt + ttlMs,
        val status: String = "ACTIVE",
        val connection: String = "unknown"
    )

    suspend fun provision(
        name: String,
        type: String,
        provider: String = "local",
        ttlMs: Long = 30L * 60L * 1000L,
        connection: String = "local://default"
    ): ManagedService = mutex.withLock {
        val service = ManagedService(
            name = name,
            type = type,
            provider = provider,
            ttlMs = ttlMs,
            connection = connection
        )
        services[service.id] = service
        _servicesFlow.value = services.values.toList()
        logStream?.append("[ephemeral] provisioned ${service.name} (${service.type}) for ${ttlMs}ms")
        service
    }

    suspend fun terminate(serviceId: String): Boolean = mutex.withLock {
        val service = services.remove(serviceId) ?: return@withLock false
        val cleaned = service.copy(status = "TERMINATED")
        _servicesFlow.value = services.values.toList()
        logStream?.append("[ephemeral] terminated ${cleaned.name} (${cleaned.id})")
        true
    }

    suspend fun cleanupExpired(): Int = mutex.withLock {
        val expired = services.filterValues { it.expiresAt <= System.currentTimeMillis() }
        expired.keys.forEach { services.remove(it) }
        _servicesFlow.value = services.values.toList()
        val count = expired.size
        if (count > 0) {
            logStream?.append("[ephemeral] cleaned ${count} expired services")
        }
        count
    }

    suspend fun killAll(): Int = mutex.withLock {
        val count = services.size
        services.clear()
        _servicesFlow.value = emptyList()
        if (count > 0) {
            logStream?.append("[ephemeral] killed all ${count} services")
        }
        count
    }

    suspend fun remainingMs(serviceId: String): Long = withContext(Dispatchers.Default) {
        mutex.withLock {
            val service = services[serviceId] ?: return@withLock 0L
            (service.expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }

    fun list(): List<ManagedService> = services.values.toList()
}
