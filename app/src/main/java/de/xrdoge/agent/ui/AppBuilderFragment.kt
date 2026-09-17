package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import java.io.File

class AppBuilderFragment : Fragment(R.layout.fragment_app_builder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val status = view.findViewById<TextView>(R.id.appBuilderStatus)
        val recyclerView = view.findViewById<RecyclerView>(R.id.appBuilderArtifacts)
        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = FileTreeAdapter(WorkspaceService.listBuildArtifacts(workspaceRoot).toMutableList()) { item ->
            status.text = "Artefakt: $item"
        }

        view.findViewById<Button>(R.id.appBuilderRunButton).setOnClickListener {
            val service = AppBuilderService(workspaceRoot)
            status.text = service.runBuild("workspace build")
            recyclerView.adapter = FileTreeAdapter(WorkspaceService.listBuildArtifacts(workspaceRoot).toMutableList()) { item ->
                status.text = "Artefakt: $item"
            }
        }
    }
}
