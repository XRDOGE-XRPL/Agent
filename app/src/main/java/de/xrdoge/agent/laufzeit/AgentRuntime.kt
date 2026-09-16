package de.xrdoge.agent.laufzeit

class AgentRuntime {
    val sessionRegistry = AgentSessionRegistry()
    val executionEngine = LocalExecutionEngine()
    val githubClient = GitHubClient()
}
