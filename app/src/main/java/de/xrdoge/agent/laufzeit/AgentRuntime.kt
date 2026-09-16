package de.xrdoge.agent.laufzeit

class AgentRuntime {
    val sessionRegistry = AgentSessionRegistry()
    val logStream = LogStreamManager()
    val socketBridge = LocalSocketBridge()
    val executionEngine = LocalExecutionEngine(logStream = logStream)
    val githubClient = GitHubClient()
}
