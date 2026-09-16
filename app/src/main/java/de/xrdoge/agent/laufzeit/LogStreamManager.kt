package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LogStreamManager {
    private val lock = Any()
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logsFlow: StateFlow<List<String>> = _logs.asStateFlow()

    fun append(message: String) {
        if (message.isBlank()) return
        synchronized(lock) {
            val updated = _logs.value.toMutableList()
            updated.add(message)
            _logs.value = updated
        }
    }

    fun replace(messages: List<String>) {
        synchronized(lock) {
            _logs.value = messages.toList()
        }
    }

    fun clear() {
        synchronized(lock) {
            _logs.value = emptyList()
        }
    }
}
