package de.xrdoge.agent.laufzeit

import java.io.File
import java.util.Locale
import java.util.UUID

class UniversalTaskEngine(
    private val sandboxRoot: File = File(System.getProperty("java.io.tmpdir"), "agent-projects"),
    private val socketBridge: LocalSocketBridge = LocalSocketBridge(),
    private val logStream: LogStreamManager? = null
) {
    private val lock = Any()

    @Volatile
    var lastProject: GeneratedProject? = null
        private set

    data class GeneratedProject(
        val id: String,
        val type: String,
        val task: String,
        val rootDir: File,
        val files: List<String>
    )

    fun generate(taskDescription: String): GeneratedProject = synchronized(lock) {
        val normalized = taskDescription.trim().ifBlank { "Generic automation project" }
        val projectType = detectProjectType(normalized)
        val projectId = UUID.randomUUID().toString().take(8)
        val projectDir = File(sandboxRoot, "${projectType}-${projectId}").apply { mkdirs() }
        generateIntoDirectory(normalized, projectDir, projectType)
    }

    fun generateIntoDirectory(taskDescription: String, targetDir: File): GeneratedProject = synchronized(lock) {
        val normalized = taskDescription.trim().ifBlank { "Generic automation project" }
        val projectType = detectProjectType(normalized)
        generateIntoDirectory(normalized, targetDir, projectType)
    }

    private fun generateIntoDirectory(taskDescription: String, targetDir: File, projectType: String): GeneratedProject {
        targetDir.mkdirs()
        val writtenFiles = scaffoldProject(targetDir, projectType, taskDescription)
        val projectId = UUID.randomUUID().toString().take(8)
        val project = GeneratedProject(projectId, projectType, taskDescription, targetDir, writtenFiles)
        lastProject = project
        logStream?.append("[workspace] created $projectType project in ${targetDir.absolutePath}")
        writeDocumentationSuite(targetDir, projectType, taskDescription)
        project
    }

    suspend fun generateAndValidate(taskDescription: String, targetDir: File? = null): String {
        val project = if (targetDir != null) {
            generateIntoDirectory(taskDescription, targetDir)
        } else {
            generate(taskDescription)
        }
        val validationCommand = when (project.type) {
            "telegram-bot" -> "python3 -m py_compile bot.py"
            "pawn-server" -> "printf '%s\\n' 'Pawn project ready: ${project.rootDir.name}'"
            "web-scraper" -> "python3 -m py_compile scraper.py"
            "async-web-scraper" -> "python3 -m py_compile async_scraper.py"
            "sqlite-service" -> "python3 -m py_compile service.py"
            "sqlite-cronjob" -> "python3 -m py_compile cronjob.py"
            else -> "python3 -m py_compile service.py"
        }

        logStream?.append("[sandbox] validating ${project.type} project")
        val result = socketBridge.executeCommand(
            command = validationCommand,
            workingDirectory = project.rootDir.absolutePath,
            onChunk = { chunk: String ->
                logStream?.append("[sandbox] $chunk")
            }
        )

        return if (result.exitCode == 0) {
            "Sandbox ready: ${project.rootDir.absolutePath} (${project.type})"
        } else {
            "Sandbox generation failed for ${project.type}: ${result.output.take(240)}"
        }
    }

    fun documentationFiles(projectDir: File): List<String> =
        projectDir.listFiles()?.filter { it.isFile && it.extension == "md" }?.map { it.name } ?: emptyList()

    private fun detectProjectType(task: String): String {
        val normalized = task.lowercase(Locale.ROOT)
        return when {
            normalized.contains("telegram") || normalized.contains("bot") -> "telegram-bot"
            normalized.contains("pawn") || normalized.contains("samp") || normalized.contains("open.mp") -> "pawn-server"
            normalized.contains("async") && (normalized.contains("scraper") || normalized.contains("crawler") || normalized.contains("web")) -> "async-web-scraper"
            normalized.contains("cron") && (normalized.contains("sqlite") || normalized.contains("database") || normalized.contains("job")) -> "sqlite-cronjob"
            normalized.contains("scraper") || normalized.contains("crawler") || normalized.contains("web") -> "web-scraper"
            normalized.contains("sqlite") || normalized.contains("database") || normalized.contains("service") -> "sqlite-service"
            else -> "generic-service"
        }
    }

    private fun scaffoldProject(projectDir: File, projectType: String, task: String): List<String> {
        val fileMap = linkedMapOf<String, String>()
        when (projectType) {
            "telegram-bot" -> {
                fileMap["bot.py"] = """
import os
import sqlite3
from pathlib import Path

TOKEN = os.getenv(\"BOT_TOKEN\", \"\")
DB_PATH = Path(__file__).with_name(\"state.db\")


def init_db() -> None:
    connection = sqlite3.connect(DB_PATH)
    connection.execute(\"CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, username TEXT)\")
    connection.commit()
    connection.close()


if __name__ == \"__main__\":
    init_db()
    print(f\"Telegram bot scaffold ready. DB: {DB_PATH}\")
""".trimIndent()
                fileMap["requirements.txt"] = "python-telegram-bot==20.7\n"
                fileMap[".env.example"] = "BOT_TOKEN=your_bot_token\n"
                fileMap["README.md"] = """
# Telegram Bot Sandbox

Ziel: ${task}

- Python bot scaffold
- SQLite state database
- isolated project directory: ${projectDir.absolutePath}
""".trimIndent()
            }
            "pawn-server" -> {
                fileMap["gamemode.pwn"] = """
#include <a_samp>

public OnGameModeInit() {
    print(\"Pawn sandbox initialized\");
    return 1;
}
""".trimIndent()
                fileMap["server.cfg"] = """
port 7777
maxplayers 50
hostname Pawn Sandbox
""".trimIndent()
                fileMap["README.md"] = """
# Pawn / open.mp Sandbox

Ziel: ${task}

- isolated pawn server directory
- ready for open.mp or SA-MP style game logic
""".trimIndent()
            }
            "web-scraper" -> {
                fileMap["scraper.py"] = """
import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).with_name(\"jobs.db\")


def init_db() -> None:
    connection = sqlite3.connect(DB_PATH)
    connection.execute(\"CREATE TABLE IF NOT EXISTS jobs (id INTEGER PRIMARY KEY, source TEXT, payload TEXT)\")
    connection.commit()
    connection.close()


if __name__ == \"__main__\":
    init_db()
    print(f\"Scraper sandbox ready. DB: {DB_PATH}\")
""".trimIndent()
                fileMap["requirements.txt"] = "requests==2.32.3\nsqlite-utils==3.36\n"
                fileMap["README.md"] = """
# Web Scraper Sandbox

Ziel: ${task}

- isolated Python scraper project
- SQLite persistence for jobs and crawled data
""".trimIndent()
            }
            "sqlite-service" -> {
                fileMap["service.py"] = """
import sqlite3
from pathlib import Path

DB_PATH = Path(__file__).with_name(\"app.db\")


def init_db() -> None:
    connection = sqlite3.connect(DB_PATH)
    connection.execute(\"CREATE TABLE IF NOT EXISTS logs (id INTEGER PRIMARY KEY, msg TEXT)\")
    connection.commit()
    connection.close()


if __name__ == \"__main__\":
    init_db()
    print(f\"SQLite service ready. DB: {DB_PATH}\")
""".trimIndent()
                fileMap["requirements.txt"] = "sqlite3\n"
                fileMap["README.md"] = """
# SQLite Service Sandbox

Ziel: ${task}

- isolated service folder
- local SQLite state management
""".trimIndent()
            }
            else -> {
                fileMap["service.py"] = """
print(\"Generic project template ready\")
""".trimIndent()
                fileMap["requirements.txt"] = "\n"
                fileMap["README.md"] = """
# Generic Service Sandbox

Ziel: ${task}

- autonomous project scaffold
- isolated local working directory
""".trimIndent()
            }
        }

        fileMap["project.json"] = """
{
  "type": "${projectType}",
  "task": "${task.replace("\"", "\\\"")}",
  "generated_at": "${System.currentTimeMillis()}"
}
""".trimIndent()

        val writtenFiles = mutableListOf<String>()
        for ((fileName, content) in fileMap) {
            val file = File(projectDir, fileName)
            file.parentFile?.mkdirs()
            file.writeText(content, Charsets.UTF_8)
            writtenFiles.add(file.absolutePath)
        }
        return writtenFiles
    }

    private fun writeDocumentationSuite(projectDir: File, projectType: String, task: String) {
        val stack = when (projectType) {
            "telegram-bot" -> "Python, SQLite, Telegram API, local runtime bridge"
            "pawn-server" -> "Pawn/open.mp, local runtime bridge, game-server orchestration"
            "web-scraper" -> "Python, SQLite, HTTP scraping, scheduler and local logging"
            "sqlite-service" -> "Python, SQLite, local service runtime, socket bridge"
            else -> "Kotlin, Python, local sandbox runtime, socket bridge"
        }

        val title = projectType.replace("-", " ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
        }
        val readme = """
# $title

## Overview
This project was generated by the autonomous agent for: ${task}

## Stack
- Runtime: local device / home tower / partner API routing
- Persistence: SQLite or local file-based state
- Execution: socket bridge + local process runtime
- Documentation: self-generated markdown suite

## Commands
```bash
python3 -m py_compile .
```
""".trimIndent()

        val architecture = """
# Architecture

## Layers
- UI: Jetpack Compose dashboard and user controls
- Runtime: Kotlin agent runtime, execution router, sandbox manager
- Local execution: socket bridge, process watchers, Termux/local shell execution
- Data layer: SQLite, file volume, local state
- Optional delegation: tower or partner APIs when required

## Runtime flow
1. Task is classified by project type.
2. The sandbox is created inside an isolated directory.
3. The execution router chooses local/tower/partner execution based on telemetry.
4. Status and logs are streamed through `LogStreamManager` and the local socket bridge.
""".trimIndent()

        val projectOverview = """
# Project Overview

## Goal
${task}

## Use cases
- Telegram bot or workflow automation
- open.mp / Pawn game logic
- web automation or scraper service
- SQLite-backed backend service

## Execution model
The project is isolated in its own directory and can be validated locally before any external routing happens.
""".trimIndent()

        val changelog = """
# Changelog

## Initial generation
- Created isolated sandbox with type `${projectType}`
- Added project scaffold and local validation hook
- Connected generated project to runtime and dashboards
""".trimIndent()

        val releaseChecklist = """
# Release Checklist

- [ ] Project builds locally
- [ ] Required runtime files exist
- [ ] Logs and status are emitted via socket bridge
- [ ] Execution router decision is acceptable
- [ ] Temporary services are cleaned up
- [ ] Markdown documentation is current
""".trimIndent()

        val docs = mapOf(
            "README.md" to readme,
            "ARCHITECTURE.md" to architecture,
            "PROJECT_OVERVIEW.md" to projectOverview,
            "CHANGELOG.md" to changelog,
            "RELEASE_CHECKLIST.md" to releaseChecklist
        )

        docs.forEach { (name, content) ->
            val file = File(projectDir, name)
            file.writeText(content, Charsets.UTF_8)
        }
    }
}
