package de.xrdoge.agent.ui

import java.io.File
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
        val summary = buildDir.listFiles()?.size?.toString() ?: "0"

        val status = "Build started @ $stamp for ${taskName.ifBlank { "workspace-build" }}"
        onStatus?.invoke(status)
        WorkspaceService.appendAgentLog(root, status)
        WorkspaceService.updateState(root, mapOf("buildStatus" to status))

        val scriptCandidates = listOf(
            File(root, "build_apk.sh"),
            File(root, "gradlew"),
            File(root, "src")
        )
        val command = when {
            scriptCandidates[0].exists() -> "bash ${scriptCandidates[0].absolutePath}"
            scriptCandidates[1].exists() -> "cd ${root.absolutePath} && ./gradlew assembleDebug --no-daemon"
            else -> "printf '%s\\n' 'workspace build scaffold ready for ${taskName.ifBlank { "workspace-build" }}'"
        }

        val process = ProcessBuilder(if (File("/system/bin/sh").exists()) "/system/bin/sh" else "/bin/sh", "-c", command)
            .directory(root)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val exitCode = process.waitFor()
        val buildStatus = if (exitCode == 0) {
            "Build succeeded: ${buildDir.absolutePath}\nArtifacts: $summary\nOutput:\n$output"
        } else {
            "Build failed (exit $exitCode): $output"
        }

        report.writeText(
            "[${stamp}] task=${taskName.ifBlank { "workspace-build" }}\n" +
                "workspace=${root.absolutePath}\n" +
                "artifacts=${summary}\n" +
                "status=${if (exitCode == 0) "success" else "failed"}\n" +
                "output=${output.replace("\n", " ").take(400)}",
            Charsets.UTF_8
        )
        WorkspaceService.appendAgentLog(root, buildStatus)
        WorkspaceService.updateState(root, mapOf("buildStatus" to buildStatus))
        WorkspaceService.appendChangeLog(root, "AppBuilder: $buildStatus")
        return buildStatus
    }
}
