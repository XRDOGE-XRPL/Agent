package de.xrdoge.agent.ui

import android.os.Bundle
import android.os.FileObserver
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class FileTreeFragment : Fragment(R.layout.fragment_file_tree) {
    private val observers = mutableListOf<FileObserver>()

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

        WorkspaceService.stopRecursiveObserver(observers)
        observers.addAll(WorkspaceService.startRecursiveObserver(workspaceRoot) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) { refreshTree() }
        })
    }

    override fun onStop() {
        WorkspaceService.stopRecursiveObserver(observers)
        super.onStop()
    }

    override fun onDestroyView() {
        WorkspaceService.stopRecursiveObserver(observers)
        super.onDestroyView()
    }
}
