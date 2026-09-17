package de.xrdoge.agent.ui

import java.io.BufferedReader
import java.io.File
import java.io.IOException

class TerminalExecService(
    private val workspaceRoot: File
) {
    fun execute(command: String, onChunk: ((String) -> Unit)? = null): String {
        val root = WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)
        val workingDirectory = File(root, "src").apply { mkdirs() }
        val shell = if (File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"

        return try {
            val process = ProcessBuilder(shell, "-c", command)
                .directory(workingDirectory)
                .redirectErrorStream(true)
                .start()

            val output = StringBuilder()
            val logLines = mutableListOf<String>()
            val reader: BufferedReader = process.inputStream.bufferedReader(Charsets.UTF_8)
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val text = line ?: ""
                    if (text.isNotBlank()) {
                        output.append(text).append('\n')
                        logLines.add(text)
                        onChunk?.invoke(text)
                    }
                }
            } finally {
                reader.close()
            }

            val exitCode = process.waitFor()
            val result = if (output.isBlank()) "[terminal] exit $exitCode" else output.toString().trimEnd()
            WorkspaceService.exportLog(root, "terminal_exec.log", logLines.ifEmpty { listOf(result) })
            WorkspaceService.appendAgentLog(root, "Terminal command: $command -> exit=$exitCode")
            WorkspaceService.updateState(
                root,
                mapOf(
                    "lastTerminalCommand" to command,
                    "lastTerminalExit" to exitCode,
                    "lastTerminalOutput" to result
                )
            )
            result
        } catch (io: IOException) {
            val message = "[terminal] execution failed: ${io.message}"
            WorkspaceService.appendAgentLog(root, "Terminal command error: $command -> ${io.message}")
            WorkspaceService.updateState(root, mapOf("lastTerminalCommand" to command, "lastTerminalOutput" to message))
            message
        } catch (se: SecurityException) {
            val message = "[terminal] permission denied for command: $command"
            WorkspaceService.appendAgentLog(root, "Terminal command blocked: $command")
            WorkspaceService.updateState(root, mapOf("lastTerminalCommand" to command, "lastTerminalOutput" to message))
            message
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            val message = "[terminal] interrupted: $command"
            WorkspaceService.appendAgentLog(root, "Terminal command interrupted: $command")
            WorkspaceService.updateState(root, mapOf("lastTerminalCommand" to command, "lastTerminalOutput" to message))
            message
        }
    }
}
