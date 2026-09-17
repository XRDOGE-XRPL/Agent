package de.xrdoge.agent.ui

import android.os.FileObserver
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WorkspaceService {
    private const val STATE_FILE = "state.json"
    private const val MANIFEST_FILE = "manifest.json"
    private const val MEMORY_FILE = "memory.json"

    fun ensureWorkspaceDirectories(workspaceRoot: File): File {
        return try {
            if (!workspaceRoot.exists()) workspaceRoot.mkdirs()
            File(workspaceRoot, "src").mkdirs()
            File(workspaceRoot, "logs").mkdirs()
            File(workspaceRoot, "appbuilder").mkdirs()
            File(workspaceRoot, "build").mkdirs()
            ensureProjectMetadata(workspaceRoot)
            workspaceRoot
        } catch (_: SecurityException) {
            workspaceRoot
        }
    }

    fun ensureProjectMetadata(root: File) {
        try {
            val manifestFile = File(root, MANIFEST_FILE)
            if (!manifestFile.exists()) {
                val manifest = JSONObject().apply {
                    put("workspace", root.absolutePath)
                    put("createdAt", System.currentTimeMillis())
                    put("requiredDirs", listOf("src", "logs", "appbuilder", "build"))
                    put("status", "initialized")
                }
                manifestFile.writeText(manifest.toString(2), Charsets.UTF_8)
            }

            val stateFile = File(root, STATE_FILE)
            if (!stateFile.exists()) {
                val state = JSONObject().apply {
                    put("workspace", root.absolutePath)
                    put("agentStatus", "ready")
                    put("lastUpdated", System.currentTimeMillis())
                    put("provider", "local")
                    put("model", "llama3.2")
                    put("iterations", 8)
                    put("ttlMinutes", 30)
                }
                stateFile.writeText(state.toString(2), Charsets.UTF_8)
            }

            val memoryFile = File(root, MEMORY_FILE)
            if (!memoryFile.exists()) {
                memoryFile.writeText("", Charsets.UTF_8)
            }

            val changelogFile = File(root, "CHANGELOG.md")
            if (!changelogFile.exists()) {
                changelogFile.writeText(
                    "# Changelog\n\n## Initial workspace\n- Initialized workspace metadata, logs, and app UI shell.\n",
                    Charsets.UTF_8
                )
            }
        } catch (_: IOException) {
            // Fallback: keep the workspace alive even if a metadata file is temporarily unavailable.
        } catch (_: SecurityException) {
            // Fallback: the app can continue in restricted contexts without crashing.
        }
    }

    fun updateState(root: File, updates: Map<String, Any?>) {
        try {
            val stateFile = File(root, STATE_FILE)
            val state = if (stateFile.exists()) JSONObject(stateFile.readText(Charsets.UTF_8)) else JSONObject()
            updates.forEach { (key, value) ->
                if (value == null) state.remove(key) else state.put(key, value)
            }
            state.put("lastUpdated", System.currentTimeMillis())
            stateFile.writeText(state.toString(2), Charsets.UTF_8)
            appendAgentLog(root, "State updated: ${state.toString(2)}")
        } catch (_: IOException) {
            appendAgentLog(root, "State update failed: I/O error")
        } catch (_: SecurityException) {
            appendAgentLog(root, "State update failed: security restriction")
        }
    }

    fun appendAgentLog(root: File, message: String) {
        val safeRoot = ensureWorkspaceDirectories(root)
        val logDir = File(safeRoot, "logs")
        logDir.mkdirs()
        try {
            val logFile = File(logDir, "agent.log")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            logFile.appendText("[$timestamp] $message\n", Charsets.UTF_8)
        } catch (_: IOException) {
            // ignore write failures; logging must not crash the UI
        }
    }

    fun appendChangeLog(root: File, message: String) {
        val safeRoot = ensureWorkspaceDirectories(root)
        val changeLog = File(safeRoot, "CHANGELOG.md")
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            changeLog.appendText("\n## ${timestamp}\n- $message\n", Charsets.UTF_8)
        } catch (_: IOException) {
            // no-op to avoid crash in restricted or read-only sandboxes
        }
    }

    fun startRecursiveObserver(root: File, onChange: () -> Unit): List<FileObserver> {
        val observers = mutableListOf<FileObserver>()
        fun attach(dir: File) {
            if (!dir.exists() || !dir.isDirectory) return
            val observer = object : FileObserver(dir.absolutePath, FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.MOVED_FROM or FileObserver.MOVED_TO or FileObserver.CLOSE_WRITE) {
                override fun onEvent(event: Int, path: String?) {
                    if (event and (FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.MOVED_FROM or FileObserver.MOVED_TO or FileObserver.CLOSE_WRITE) != 0) {
                        onChange()
                    }
                }
            }
            observer.startWatching()
            observers += observer
            dir.listFiles()?.filter { it.isDirectory }?.forEach(::attach)
        }
        attach(root)
        return observers
    }

    fun listProjectTree(root: File): List<String> {
        val base = ensureWorkspaceDirectories(root)
        if (!base.exists() || !base.isDirectory) return emptyList()
        val results = mutableListOf<String>()
        fun walk(dir: File, relativePath: String) {
            val entries = dir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: return
            for (entry in entries) {
                val childPath = if (relativePath.isBlank()) entry.name else "$relativePath/${entry.name}"
                results.add(childPath + if (entry.isDirectory) "/" else "")
                if (entry.isDirectory) walk(entry, childPath)
            }
        }
        walk(base, "")
        return results
    }

    fun readTextFile(file: File): String = try {
        if (!file.exists() || !file.isFile) "[unable to read file: ${file.absolutePath}]" else file.readText(Charsets.UTF_8)
    } catch (_: Exception) {
        "[unable to read file: ${file.absolutePath}]"
    }

    fun readWorkspaceDocs(projectDir: File): Map<String, String> {
        val candidates = listOf(
            "README.md",
            "ARCHITECTURE.md",
            "PROJECT_OVERVIEW.md",
            "CHANGELOG.md",
            "RELEASE_CHECKLIST.md"
        )
        val base = ensureWorkspaceDirectories(projectDir)
        if (!base.exists() || !base.isDirectory) return emptyMap()
        return candidates.mapNotNull { name ->
            val file = File(base, name)
            if (file.exists() && file.isFile) name to try { file.readText(Charsets.UTF_8) } catch (_: IOException) { "[unable to read file: ${file.absolutePath}]" } else null
        }.toMap()
    }

    fun loadMemoryMap(root: File): Map<String, String> {
        val file = File(root, MEMORY_FILE)
        if (!file.exists()) {
            file.writeText("", Charsets.UTF_8)
            return emptyMap()
        }
        return try {
            file.readText(Charsets.UTF_8)
                .lineSequence()
                .mapNotNull { line ->
                    val idx = line.indexOf('=')
                    if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
                .toMap()
        } catch (_: IOException) {
            emptyMap()
        }
    }

    fun saveMemoryMap(root: File, entries: Map<String, String>) {
        val file = File(root, MEMORY_FILE)
        try {
            val content = entries.entries.sortedBy { it.key.lowercase() }
                .joinToString(separator = "\n") { "${it.key}=${it.value}" }
            file.writeText(content.ifBlank { "" }, Charsets.UTF_8)
        } catch (_: IOException) {
            // no-op: memory persistence must not crash the runtime
        }
    }

    fun exportLog(root: File, fileName: String, lines: List<String>) {
        val targetDir = File(root, "logs")
        targetDir.mkdirs()
        val target = File(targetDir, fileName)
        try {
            target.writeText(lines.joinToString(separator = "\n"), Charsets.UTF_8)
        } catch (_: IOException) {
            // ignore log-export write failures
        }
    }

    fun listBuildArtifacts(root: File): List<String> {
        val buildDir = File(root, "build")
        if (!buildDir.exists() || !buildDir.isDirectory) return emptyList()
        return try {
            buildDir.walkTopDown()
                .filter { it.isFile }
                .map { it.relativeTo(buildDir).invariantSeparatorsPath }
                .sorted()
                .toList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
