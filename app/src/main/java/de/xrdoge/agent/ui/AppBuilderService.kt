package de.xrdoge.agent.ui

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppBuilderService(
    private val workspaceRoot: File
) {
    fun runBuild(taskName: String): String {
        val root = WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)
        val buildDir = File(root, "build")
        buildDir.mkdirs()

        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val report = File(buildDir, "build_report.txt")
        val summary = buildDir.listFiles()?.size?.toString() ?: "0"
        report.writeText(
            "[${stamp}] task=${taskName.ifBlank { "workspace-build" }}\n" +
                "workspace=${root.absolutePath}\n" +
                "artifacts=${summary}\n" +
                "status=prepared",
            Charsets.UTF_8
        )
        return "Build prepared in ${buildDir.absolutePath}"
    }
}
