package de.xrdoge.agent.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Collections

class TerminalExecService(
    private val workspaceRoot: File
) {
    suspend fun execute(command: String, onChunk: ((String) -> Unit)? = null): String = withContext(Dispatchers.IO) {
        val root = WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)
        val workingDirectory = File(root, "src").apply { mkdirs() }
        val shell = if (File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"

        try {
            val processBuilder = ProcessBuilder(shell, "-c", command)
                .directory(workingDirectory)
                .redirectErrorStream(false)
            val env = processBuilder.environment()
            env["WORKSPACE_ROOT"] = root.absolutePath
            env["PROJECT_DIR"] = workingDirectory.absolutePath
            env["TERM"] = "xterm"
            val process = processBuilder.start()

            val output = StringBuilder()
            val logLines = Collections.synchronizedList(mutableListOf<String>())

            suspend fun readStream(stream: InputStream) {
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val text = line ?: ""
                        if (text.isNotBlank()) {
                            synchronized(output) { output.append(text).append('\n') }
                            logLines.add(text)
                            onChunk?.invoke(text)
                        }
                    }
                }
            }

            val exitCode = coroutineScope {
                val stdoutJob = async(Dispatchers.IO) { readStream(process.inputStream) }
                val stderrJob = async(Dispatchers.IO) { readStream(process.errorStream) }
                val code = process.waitFor()
                stdoutJob.await()
                stderrJob.await()
                code
            }

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
            val message = "[terminal] execution failed: ${io.message ?: "I/O error"}"
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
