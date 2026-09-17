package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AppBuilderFragment : Fragment(R.layout.fragment_app_builder) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val status = view.findViewById<TextView>(R.id.appBuilderStatus)
        val recyclerView = view.findViewById<RecyclerView>(R.id.appBuilderArtifacts)
        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)

        fun refreshArtifacts() {
            val items = WorkspaceService.listBuildArtifacts(workspaceRoot).toMutableList()
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = FileTreeAdapter(items) { item ->
                status.text = "Artefakt: $item"
            }
        }

        refreshArtifacts()

        view.findViewById<Button>(R.id.appBuilderRunButton).setOnClickListener {
            val service = AppBuilderService(workspaceRoot)
            viewLifecycleOwner.lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    service.runBuild("workspace build") { message ->
                        launch(Dispatchers.Main) { status.text = message }
                    }
                }
                refreshArtifacts()
            }
        }
    }
}
