package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.GitHubClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GitHubClientTest {
    @Test
    fun repositorySnapshotLiestGitBranchUndStatus() {
        val dir = File.createTempFile("agent-github-client", "")
        dir.delete()
        dir.mkdirs()
        val init = ProcessBuilder("git", "init").directory(dir).start()
        init.waitFor()
        val branch = ProcessBuilder("git", "checkout", "-b", "feature/local-agent").directory(dir).start()
        branch.waitFor()
        val file = File(dir, "hello.txt")
        file.writeText("hello")
        val client = GitHubClient()
        val snapshot = client.repositorySnapshot(dir.absolutePath)
        assertTrue(snapshot.branch.contains("feature") || snapshot.branch.contains("master") || snapshot.branch.contains("main"))
        assertEquals("modified", snapshot.status)
    }
}
