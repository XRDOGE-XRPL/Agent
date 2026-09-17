package de.xrdoge.agent.ui

import android.os.Bundle
import android.os.FileObserver
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import java.io.File

class FileTreeFragment : Fragment(R.layout.fragment_file_tree) {
    private var observer: FileObserver? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.fileTreeRecycler)
        val fileContent = view.findViewById<TextView>(R.id.fileTreeContent)
        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)

        fun refreshTree() {
            val items = WorkspaceService.listProjectTree(workspaceRoot).toMutableList()
            val adapter = recyclerView.adapter as? FileTreeAdapter ?: FileTreeAdapter(items) { selected ->
                val target = File(workspaceRoot, selected.removeSuffix("/"))
                fileContent.text = if (target.isFile) WorkspaceService.readTextFile(target) else "Ordner: ${target.absolutePath}"
            }
            adapter.replace(items)
            recyclerView.adapter = adapter
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        refreshTree()
        fileContent.text = "Dateinamen anklicken, um Inhalt anzuzeigen."

        observer = object : FileObserver(workspaceRoot.absolutePath) {
            override fun onEvent(event: Int, path: String?) {
                requireActivity().runOnUiThread {
                    refreshTree()
                }
            }
        }
        observer?.startWatching()
    }

    override fun onDestroyView() {
        observer?.stopWatching()
        observer = null
        super.onDestroyView()
    }
}
