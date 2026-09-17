package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import de.xrdoge.agent.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TerminalFragment : Fragment(R.layout.fragment_terminal) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val input = view.findViewById<EditText>(R.id.terminalInput)
        val console = view.findViewById<TextView>(R.id.terminalConsole)
        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)

        view.findViewById<Button>(R.id.terminalRunButton).setOnClickListener {
            val command = input.text?.toString()?.trim().orEmpty()
            if (command.isBlank()) {
                console.text = "[terminal] no command entered"
                return@setOnClickListener
            }
            val service = TerminalExecService(workspaceRoot)
            val output = StringBuilder()
            viewLifecycleOwner.lifecycleScope.launch {
                val result = withContext(Dispatchers.IO) {
                    service.execute(command) { chunk ->
                        output.append(chunk).append('\n')
                        launch(Dispatchers.Main) { console.text = output.toString().trimEnd() }
                    }
                }
                console.text = result
            }
        }

        view.findViewById<Button>(R.id.terminalClearButton).setOnClickListener {
            console.text = ""
        }

        view.findViewById<Button>(R.id.terminalExportButton).setOnClickListener {
            val lines = console.text.toString().split("\n")
            WorkspaceService.exportLog(workspaceRoot, "terminal_exec.log", lines)
            WorkspaceService.appendAgentLog(workspaceRoot, "Terminal export requested")
            console.text = "${console.text}\n[terminal] log exported to ${File(workspaceRoot, "logs/terminal_exec.log").absolutePath}"
        }
    }
}
