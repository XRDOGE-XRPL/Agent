package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.xrdoge.agent.R
import java.io.File

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val workspaceField = view.findViewById<EditText>(R.id.dashboardWorkspace)
        val taskField = view.findViewById<EditText>(R.id.dashboardTask)
        val ollamaField = view.findViewById<EditText>(R.id.dashboardOllamaUrl)
        val modelField = view.findViewById<EditText>(R.id.dashboardModel)
        val iterationsField = view.findViewById<EditText>(R.id.dashboardIterations)
        val providerField = view.findViewById<EditText>(R.id.dashboardProvider)
        val ttlField = view.findViewById<EditText>(R.id.dashboardTtl)
        val statusText = view.findViewById<TextView>(R.id.dashboardStatus)

        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)
        workspaceField.setText(workspaceRoot.absolutePath)
        ollamaField.setText("http://127.0.0.1:11434")
        modelField.setText("llama3.2")
        iterationsField.setText("8")
        providerField.setText("local")
        ttlField.setText("30")

        view.findViewById<Button>(R.id.dashboardStartButton).setOnClickListener {
            statusText.text = "Agent start dispatched for: ${taskField.text.toString().ifBlank { "default task" }}"
        }
        view.findViewById<Button>(R.id.dashboardPauseButton).setOnClickListener {
            statusText.text = "Agent paused"
        }
        view.findViewById<Button>(R.id.dashboardStopButton).setOnClickListener {
            statusText.text = "Agent stopped"
        }
    }
}
