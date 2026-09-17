package de.xrdoge.agent.laufzeit

fun isTermuxEnvironment(): Boolean {
    return try {
        java.io.File("/data/data/com.termux").exists()
    } catch (e: Exception) {
        false
    }
}
