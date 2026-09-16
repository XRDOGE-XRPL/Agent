package de.xrdoge.agent.laufzeit

import java.io.File

class AgentRuntime(
    sandboxRoot: File = File(System.getProperty("java.io.tmpdir"), "agent_projects")
) {
    val sandboxRoot: File = sandboxRoot.apply { mkdirs() }
    val sessionRegistry = AgentSessionRegistry()
    val logStream = LogStreamManager()
    val socketBridge = LocalSocketBridge(logStream = logStream)
    val localOllamaClient = LocalOllamaClient(bridge = socketBridge, logStream = logStream)
    val executionRouter = ExecutionRouter()
    val ephemeralServiceManager = EphemeralServiceManager(logStream = logStream)
    val universalTaskEngine = UniversalTaskEngine(sandboxRoot = sandboxRoot, socketBridge = socketBridge, logStream = logStream)
    val executionEngine = LocalExecutionEngine(logStream = logStream)
    val githubClient = GitHubClient()
}
