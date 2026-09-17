package de.xrdoge.agent.ui

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppBuilderService(
    private val workspaceRoot: File
) {
    fun runBuild(taskName: String, onStatus: ((String) -> Unit)? = null): String {
        val root = WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)
        val buildDir = File(root, "build")
        buildDir.mkdirs()

        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val report = File(buildDir, "build_report.txt")
        val taskLabel = taskName.ifBlank { "workspace-build" }

        val initialStatus = "Build started @ $stamp for $taskLabel"
        onStatus?.invoke(initialStatus)
        WorkspaceService.appendAgentLog(root, initialStatus)
        WorkspaceService.updateState(root, mapOf("buildStatus" to initialStatus))

        val gradlew = File(root, "gradlew")
        val buildScript = File(root, "build_apk.sh")
        val command = when {
            buildScript.exists() -> "bash ${buildScript.absolutePath}"
            gradlew.exists() -> "cd ${root.absolutePath} && chmod +x ${gradlew.absolutePath} && ${gradlew.absolutePath} assembleDebug --no-daemon"
            else -> "printf '%s\\n' 'workspace build scaffold ready for $taskLabel'"
        }

        return try {
            val shell = if (File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh"
            val processBuilder = ProcessBuilder(shell, "-c", command)
                .directory(root)
                .redirectErrorStream(true)
            val env = processBuilder.environment()
            env["WORKSPACE_ROOT"] = root.absolutePath
            env["PROJECT_DIR"] = root.absolutePath
            env["TERM"] = "xterm"
            val process = processBuilder.start()

            val output = StringBuilder()
            val logLines = mutableListOf<String>()
            val reader: BufferedReader = process.inputStream.bufferedReader(Charsets.UTF_8)
            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val text = line ?: ""
                    if (text.isNotBlank()) {
                        logLines.add(text)
                        output.append(text).append('\n')
                        onStatus?.invoke(text)
                    }
                }
            } finally {
                reader.close()
            }

            val exitCode = process.waitFor()
            val buildOutput = if (output.isBlank()) "[build] exit $exitCode" else output.toString().trimEnd()
            val buildStatus = if (exitCode == 0) {
                "Build succeeded: ${buildDir.absolutePath}\nArtifacts: ${WorkspaceService.listBuildArtifacts(root).size}\nOutput:\n$buildOutput"
            } else {
                "Build failed (exit $exitCode): $buildOutput"
            }

            try {
                report.writeText(
                    "[${stamp}] task=${taskLabel}\n" +
                        "workspace=${root.absolutePath}\n" +
                        "artifacts=${WorkspaceService.listBuildArtifacts(root).size}\n" +
                        "status=${if (exitCode == 0) "success" else "failed"}\n" +
                        "output=${buildOutput.replace("\n", " ").take(400)}",
                    Charsets.UTF_8
                )
            } catch (_: IOException) {
                // Ignore build report failures without crashing the app.
            }

            WorkspaceService.exportLog(root, "build.log", logLines.ifEmpty { listOf(buildOutput) })
            WorkspaceService.appendAgentLog(root, buildStatus)
            WorkspaceService.updateState(root, mapOf("buildStatus" to buildStatus))
            WorkspaceService.appendChangeLog(root, "AppBuilder: $buildStatus")
            onStatus?.invoke(buildStatus)
            buildStatus
        } catch (io: IOException) {
            val message = "Build failed: ${io.message ?: "I/O error"}"
            WorkspaceService.appendAgentLog(root, message)
            WorkspaceService.updateState(root, mapOf("buildStatus" to message))
            onStatus?.invoke(message)
            message
        } catch (se: SecurityException) {
            val message = "Build failed: security restriction while creating process"
            WorkspaceService.appendAgentLog(root, message)
            WorkspaceService.updateState(root, mapOf("buildStatus" to message))
            onStatus?.invoke(message)
            message
        } catch (ie: InterruptedException) {
            Thread.currentThread().interrupt()
            val message = "Build interrupted while running $taskLabel"
            WorkspaceService.appendAgentLog(root, message)
            WorkspaceService.updateState(root, mapOf("buildStatus" to message))
            onStatus?.invoke(message)
            message
        }
    }
}
