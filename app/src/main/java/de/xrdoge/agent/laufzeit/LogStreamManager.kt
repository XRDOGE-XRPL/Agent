package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class LogStreamManager(
    private val maxEntries: Int = 500
) {
    private val lock = ReentrantLock()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logs.asStateFlow()

    fun append(message: String) {
        val safeMessage = message.trim()
        if (safeMessage.isBlank()) return
        lock.withLock {
            val updated = _logs.value.toMutableList()
            updated.add(safeMessage)
            while (updated.size > maxEntries) {
                updated.removeAt(0)
            }
            _logs.value = updated
        }
    }

    fun replace(messages: List<String>) {
        val sanitized = messages.map { it.trim() }.filter { it.isNotBlank() }
        lock.withLock {
            _logs.value = sanitized
        }
    }

    fun clear() {
        lock.withLock {
            _logs.value = emptyList()
        }
    }
}
