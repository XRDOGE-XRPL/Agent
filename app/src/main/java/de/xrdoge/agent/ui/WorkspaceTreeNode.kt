package de.xrdoge.agent.ui

import java.io.File

data class WorkspaceTreeNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: MutableList<WorkspaceTreeNode> = mutableListOf()
) {
    companion object {
        fun fromFile(file: File, root: File): WorkspaceTreeNode {
            val relative = file.relativeTo(root).invariantSeparatorsPath
            return WorkspaceTreeNode(
                name = file.name,
                path = relative,
                isDirectory = file.isDirectory,
                children = if (file.isDirectory) {
                    file.listFiles()?.sortedBy { it.name.lowercase() }?.map { fromFile(it, root) }?.toMutableList() ?: mutableListOf()
                } else {
                    mutableListOf()
                }
            )
        }
    }
}
