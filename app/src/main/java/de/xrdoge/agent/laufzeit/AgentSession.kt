package de.xrdoge.agent.laufzeit

import java.util.UUID

enum class AgentSessionStatus {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class AgentSession(
    val id: String = UUID.randomUUID().toString(),
    val repository: String,
    val task: String,
    var status: AgentSessionStatus = AgentSessionStatus.QUEUED,
    val logs: MutableList<String> = mutableListOf()
)

class AgentSessionRegistry {
    private val lock = Any()
    private val sessions = linkedMapOf<String, AgentSession>()

    fun create(repository: String, task: String): AgentSession = synchronized(lock) {
        val session = AgentSession(repository = repository, task = task)
        sessions[session.id] = session
        session
    }

    fun update(id: String, status: AgentSessionStatus, logEntry: String? = null) = synchronized(lock) {
        val session = sessions[id] ?: return@synchronized
        session.status = status
        if (!logEntry.isNullOrBlank()) {
            session.logs.add(logEntry)
        }
    }

    fun list(): List<AgentSession> = synchronized(lock) {
        sessions.values.toList()
    }
}
