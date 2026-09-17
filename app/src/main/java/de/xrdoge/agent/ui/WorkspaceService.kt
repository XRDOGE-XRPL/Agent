package de.xrdoge.agent.ui

import java.io.File

object WorkspaceService {
    fun ensureWorkspaceDirectories(workspaceRoot: File): File {
        workspaceRoot.mkdirs()
        File(workspaceRoot, "src").mkdirs()
        File(workspaceRoot, "logs").mkdirs()
        File(workspaceRoot, "appbuilder").mkdirs()
        File(workspaceRoot, "build").mkdirs()
        return workspaceRoot
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
        file.readText(Charsets.UTF_8)
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
            if (file.exists() && file.isFile) name to file.readText(Charsets.UTF_8) else null
        }.toMap()
    }

    fun loadMemoryMap(root: File): Map<String, String> {
        val file = File(root, "memory.json")
        if (!file.exists()) return emptyMap()
        return try {
            file.readText(Charsets.UTF_8)
                .lineSequence()
                .mapNotNull { line ->
                    val idx = line.indexOf('=')
                    if (idx <= 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
                }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveMemoryMap(root: File, entries: Map<String, String>) {
        val file = File(root, "memory.json")
        val content = entries.entries.sortedBy { it.key.lowercase() }
            .joinToString(separator = "\n") { "${it.key}=${it.value}" }
        file.writeText(content.ifBlank { "" }, Charsets.UTF_8)
    }

    fun exportLog(root: File, fileName: String, lines: List<String>) {
        val targetDir = File(root, "logs")
        targetDir.mkdirs()
        val target = File(targetDir, fileName)
        target.writeText(lines.joinToString(separator = "\n"), Charsets.UTF_8)
    }

    fun listBuildArtifacts(root: File): List<String> {
        val buildDir = File(root, "build")
        if (!buildDir.exists() || !buildDir.isDirectory) return emptyList()
        return buildDir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(buildDir).invariantSeparatorsPath }
            .sorted()
            .toList()
    }
}
