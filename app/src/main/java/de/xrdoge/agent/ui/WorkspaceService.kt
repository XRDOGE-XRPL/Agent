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
    private const val LOG_ROTATION_LIMIT_BYTES = 1024 * 1024L

    fun ensureWorkspaceDirectories(workspaceRoot: File): File {
        val root = workspaceRoot.absoluteFile
        return try {
            if (!root.exists() && !root.mkdirs()) {
                return root
            }
            File(root, "src").mkdirs()
            File(root, "logs").mkdirs()
            File(root, "appbuilder").mkdirs()
            File(root, "build").mkdirs()
            ensureProjectMetadata(root)
            root
        } catch (_: SecurityException) {
            root
        }
    }

    fun ensureProjectMetadata(root: File) {
        try {
            val logDir = File(root, "logs").apply { mkdirs() }
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
                memoryFile.writeText("{}", Charsets.UTF_8)
            } else if (!memoryFile.readText(Charsets.UTF_8).trim().isEmpty()) {
                val raw = memoryFile.readText(Charsets.UTF_8)
                if (!raw.trim().startsWith("{") && !raw.trim().startsWith("[")) {
                    memoryFile.writeText("{}", Charsets.UTF_8)
                } else {
                    try {
                        JSONObject(raw)
                    } catch (_: Exception) {
                        memoryFile.writeText("{}", Charsets.UTF_8)
                    }
                }
            }

            val changelogFile = File(root, "CHANGELOG.md")
            if (!changelogFile.exists()) {
                changelogFile.writeText(
                    "# Changelog\n\n## Initial workspace\n- Initialized workspace metadata, logs, and app UI shell.\n",
                    Charsets.UTF_8
                )
            }

            val agentLog = File(logDir, "agent.log")
            if (!agentLog.exists()) agentLog.writeText("", Charsets.UTF_8)
            val terminalLog = File(logDir, "terminal_exec.log")
            if (!terminalLog.exists()) terminalLog.writeText("", Charsets.UTF_8)
        } catch (_: IOException) {
            // The app must continue even if metadata files cannot be written.
        } catch (_: SecurityException) {
            // Restricted contexts must not crash the UI.
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
            appendTextWithRotation(logFile, "[$timestamp] $message\n")
        } catch (_: IOException) {
            // Logging must never crash the app.
        }
    }

    fun appendChangeLog(root: File, message: String) {
        val safeRoot = ensureWorkspaceDirectories(root)
        val changeLog = File(safeRoot, "CHANGELOG.md")
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            changeLog.appendText("\n## ${timestamp}\n- $message\n", Charsets.UTF_8)
        } catch (_: IOException) {
            // Restricted or read-only sandboxes must not crash the app.
        }
    }

    fun startRecursiveObserver(root: File, onChange: () -> Unit): List<FileObserver> {
        val observers = mutableListOf<FileObserver>()
        fun attach(dir: File) {
            if (!dir.exists() || !dir.isDirectory) return
            val observer = object : FileObserver(
                dir.absolutePath,
                FileObserver.CREATE or FileObserver.DELETE or FileObserver.MODIFY or FileObserver.MOVED_FROM or FileObserver.MOVED_TO or FileObserver.CLOSE_WRITE
            ) {
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

    fun stopRecursiveObserver(observers: MutableList<FileObserver>) {
        observers.forEach { it.stopWatching() }
        observers.clear()
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
        val base = ensureWorkspaceDirectories(projectDir)
        if (!base.exists() || !base.isDirectory) return emptyMap()
        return base.walkTopDown()
            .filter { it.isFile && it.extension.equals("md", ignoreCase = true) }
            .sortedBy { it.relativeTo(base).invariantSeparatorsPath }
            .associate { relative ->
                relative.relativeTo(base).invariantSeparatorsPath to try {
                    relative.readText(Charsets.UTF_8)
                } catch (_: IOException) {
                    "[unable to read file: ${relative.absolutePath}]"
                }
            }
    }

    fun loadMemoryMap(root: File): Map<String, String> {
        val file = File(root, MEMORY_FILE)
        if (!file.exists()) {
            file.writeText("{}", Charsets.UTF_8)
            return emptyMap()
        }
        return try {
            val raw = file.readText(Charsets.UTF_8).trim()
            if (raw.isEmpty()) return emptyMap()
            val json = JSONObject(raw)
            buildMap {
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    put(key, json.getString(key))
                }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveMemoryMap(root: File, entries: Map<String, String>) {
        val file = File(root, MEMORY_FILE)
        try {
            val json = JSONObject()
            entries.entries.sortedBy { it.key.lowercase() }.forEach { (key, value) -> json.put(key, value) }
            file.writeText(json.toString(2), Charsets.UTF_8)
        } catch (_: IOException) {
            // In-memory persistence must never crash the runtime.
        }
    }

    fun exportLog(root: File, fileName: String, lines: List<String>) {
        val safeRoot = ensureWorkspaceDirectories(root)
        val targetDir = File(safeRoot, "logs")
        targetDir.mkdirs()
        val target = File(targetDir, fileName)
        try {
            val content = lines.joinToString(separator = "\n")
            appendTextWithRotation(target, if (content.isBlank()) "" else "$content\n")
        } catch (_: IOException) {
            // Ignore write failures in restricted environments.
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

    private fun appendTextWithRotation(file: File, content: String) {
        val parent = file.parentFile ?: return
        parent.mkdirs()
        if (file.exists() && file.length() >= LOG_ROTATION_LIMIT_BYTES) {
            val rotated = File(parent, file.nameWithoutExtension + ".bak")
            if (rotated.exists()) rotated.delete()
            file.renameTo(rotated)
        }
        file.appendText(content, Charsets.UTF_8)
    }
}
