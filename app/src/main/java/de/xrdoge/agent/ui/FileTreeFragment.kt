package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import java.io.File

class FileTreeFragment : Fragment(R.layout.fragment_file_tree) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.fileTreeRecycler)
        val fileContent = view.findViewById<TextView>(R.id.fileTreeContent)
        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)

        val items = WorkspaceService.listProjectTree(workspaceRoot).toMutableList()
        val adapter = FileTreeAdapter(items) { selected ->
            val target = File(workspaceRoot, selected.removeSuffix("/"))
            if (target.exists() && target.isFile) {
                fileContent.text = WorkspaceService.readTextFile(target)
            } else {
                fileContent.text = "Ordner: ${target.absolutePath}"
            }
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        fileContent.text = "Dateinamen anklicken, um Inhalt anzuzeigen."
    }
}
