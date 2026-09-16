package de.xrdoge.agent.laufzeit

class AgentRuntime {
    val sessionRegistry = AgentSessionRegistry()
    val logStream = LogStreamManager()
    val socketBridge = LocalSocketBridge(logStream = logStream)
    val localOllamaClient = LocalOllamaClient(bridge = socketBridge, logStream = logStream)
    val executionEngine = LocalExecutionEngine(logStream = logStream)
    val githubClient = GitHubClient()
}
