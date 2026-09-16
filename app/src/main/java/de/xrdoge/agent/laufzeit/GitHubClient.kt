package de.xrdoge.agent.laufzeit

import java.io.File
import java.util.Locale

data class GitHubRepositorySnapshot(
    val name: String,
    val branch: String,
    val status: String,
    val pullRequests: Int = 0,
    val workflowRuns: Int = 0,
    val localOnly: Boolean = true
) {
    fun summary(): String =
        "$name [$branch] • $status • PRs=$pullRequests • Workflows=$workflowRuns"
}

class GitHubClient {
    fun repositorySnapshot(path: String): GitHubRepositorySnapshot {
        val verzeichnis = File(path)
        val name = verzeichnis.name.ifBlank { "workspace" }
        val branch = gitValue(verzeichnis, "rev-parse --abbrev-ref HEAD") ?: "detached"
        val status = if ((gitValue(verzeichnis, "status --porcelain") ?: "").isBlank()) {
            "clean"
        } else {
            "modified"
        }
        return GitHubRepositorySnapshot(
            name = name,
            branch = branch.trim(),
            status = status,
            pullRequests = 0,
            workflowRuns = 0,
            localOnly = true
        )
    }

    private fun gitValue(verzeichnis: File, argumente: String): String? {
        if (!verzeichnis.exists()) return null
        return try {
            val args = argumente.split(Regex("\\s+")).filter { it.isNotBlank() }.toTypedArray()
            val process = ProcessBuilder("git", *args).directory(verzeichnis)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0) output else null
        } catch (_: Exception) {
            null
        }
    }

    fun pullRequests(path: String): List<String> {
        val snapshot = repositorySnapshot(path)
        return if (snapshot.status == "clean") emptyList() else listOf("local-${snapshot.branch.lowercase(Locale.getDefault())}")
    }
}
