package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.LogStreamManager
import de.xrdoge.agent.laufzeit.UniversalTaskEngine
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class UniversalTaskEngineTest {
    @Test
    fun universalTaskEngineCreatesIsolatedProjectsForDifferentDomains() {
        val sandboxRoot = Files.createTempDirectory("agent-sandbox").toFile()
        val engine = UniversalTaskEngine(sandboxRoot = sandboxRoot, logStream = LogStreamManager())

        val telegramProject = engine.generate("Build a Telegram bot with SQLite state management")
        assertTrue(telegramProject.rootDir.exists())
        assertTrue(telegramProject.rootDir.listFiles()?.any { it.name == "bot.py" } == true)

        val scraperProject = engine.generate("Create a web scraper that stores jobs in SQLite")
        assertTrue(scraperProject.rootDir.exists())
        assertTrue(scraperProject.rootDir.listFiles()?.any { it.name == "scraper.py" } == true)
        assertNotEquals(telegramProject.rootDir.absolutePath, scraperProject.rootDir.absolutePath)
    }
}
