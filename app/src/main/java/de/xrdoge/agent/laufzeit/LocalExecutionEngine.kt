package de.xrdoge.agent.laufzeit

import java.io.File

class LocalExecutionEngine(
    private val agentSchleife: AgentSchleife = AgentSchleife(),
    private val githubClient: GitHubClient = GitHubClient()
) {
    private val lock = Any()

    fun execute(
        session: AgentSession,
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String = synchronized(lock) {
        val repoSnapshot = githubClient.repositorySnapshot(arbeitsverzeichnis)
        val statusLine = "Repo: ${repoSnapshot.summary()}"
        if (!session.logs.contains(statusLine)) {
            session.logs.add(statusLine)
        }

        val sanitizedDir = arbeitsverzeichnis.ifBlank { File(".").absolutePath }
        val directory = File(sanitizedDir)
        directory.mkdirs()

        try {
            val result = agentSchleife.ausfuehren(
                sanitizedDir,
                aufgabe,
                ollamaUrl,
                modell,
                maxIterationen.coerceAtLeast(1)
            )
            session.status = AgentSessionStatus.COMPLETED
            session.logs.add("Local execution finished successfully.")
            result
        } catch (ex: Exception) {
            session.status = AgentSessionStatus.FAILED
            val fallback = "Local execution failed: ${ex.message ?: "unknown error"}"
            session.logs.add(fallback)
            fallback
        }
    }
}
