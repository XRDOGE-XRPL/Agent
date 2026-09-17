package de.xrdoge.agent.ui

import de.xrdoge.agent.laufzeit.LocalSocketBridge
import java.io.File

class TerminalExecService(
    private val workspaceRoot: File
) {
    private val bridge = LocalSocketBridge()

    fun execute(command: String): String {
        val root = WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)
        val workingDirectory = File(root, "src").apply { mkdirs() }
        val result = bridge.executeCommand(command, workingDirectory.absolutePath)
        val lines = result.output.lines().filter { it.isNotBlank() }
        WorkspaceService.exportLog(root, "terminal_exec.log", lines)
        return if (result.output.isBlank()) "[terminal] exit ${result.exitCode}" else result.output
    }
}
