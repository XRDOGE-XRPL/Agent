package de.xrdoge.agent.laufzeit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class LocalExecutionEngine(
    private val agentSchleife: AgentSchleife = AgentSchleife(),
    private val githubClient: GitHubClient = GitHubClient(),
    private val logStream: LogStreamManager = LogStreamManager(),
    private val socketBridge: LocalSocketBridge = LocalSocketBridge(logStream = logStream)
) {
    private val lock = Any()

    fun execute(
        session: AgentSession,
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String = runBlocking {
        executeAsync(session, arbeitsverzeichnis, aufgabe, ollamaUrl, modell, maxIterationen)
    }

    suspend fun executeAsync(
        session: AgentSession,
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val repoSnapshot = githubClient.repositorySnapshot(arbeitsverzeichnis)
            val statusLine = "Repo: ${repoSnapshot.summary()}"
            if (!session.logs.contains(statusLine)) {
                session.logs.add(statusLine)
            }
            logStream.append(statusLine)

            val sanitizedDir = arbeitsverzeichnis.ifBlank { File(".").absolutePath }
            val directory = File(sanitizedDir)
            directory.mkdirs()

            val runtimeProbe = socketBridge.executeCommand(
                command = "printf '%s\\n' 'runtime-ready'",
                workingDirectory = sanitizedDir,
                timeoutMs = 5_000L
            ) { chunk ->
                val cleaned = chunk.trim()
                if (cleaned.isNotBlank()) {
                    logStream.append("[local-runtime] $cleaned")
                }
            }

            if (runtimeProbe.exitCode != 0) {
                val probeMessage = "Local runtime probe failed: ${runtimeProbe.output.take(200)}"
                logStream.append(probeMessage)
                session.logs.add(probeMessage)
            }

            try {
                logStream.append("Local execution started for task: ${aufgabe.take(140)}")
                val result = agentSchleife.ausfuehren(
                    sanitizedDir,
                    aufgabe,
                    ollamaUrl,
                    modell,
                    maxIterationen.coerceAtLeast(1)
                )
                session.status = AgentSessionStatus.COMPLETED
                val finalMessage = "Local execution finished successfully."
                session.logs.add(finalMessage)
                logStream.append(finalMessage)
                result
            } catch (ex: Exception) {
                session.status = AgentSessionStatus.FAILED
                val fallback = "Local execution failed: ${ex.message ?: "unknown error"}"
                session.logs.add(fallback)
                logStream.append(fallback)
                fallback
            }
        }
    }
}
