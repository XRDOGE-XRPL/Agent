package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import java.io.File

class MemoryFragment : Fragment(R.layout.fragment_memory) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val keyField = view.findViewById<EditText>(R.id.memoryKey)
        val valueField = view.findViewById<EditText>(R.id.memoryValue)
        val recyclerView = view.findViewById<RecyclerView>(R.id.memoryRecycler)
        val workspaceRoot = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspaceRoot)

        fun loadEntries(): List<MemoryEntry> {
            return try {
                WorkspaceService.loadMemoryMap(workspaceRoot)
                    .map { (key, value) -> MemoryEntry(key, value) }
                    .sortedBy { it.key.lowercase() }
            } catch (exception: Exception) {
                Toast.makeText(requireContext(), "memory.json ist ungültig: ${exception.message}", Toast.LENGTH_LONG).show()
                emptyList()
            }
        }

        fun refresh() {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            val entries = loadEntries()
            recyclerView.adapter = MemoryEntryAdapter(entries.toMutableList()) { entry ->
                val updated = WorkspaceService.loadMemoryMap(workspaceRoot).toMutableMap().apply {
                    remove(entry.key)
                }
                WorkspaceService.saveMemoryMap(workspaceRoot, updated)
                WorkspaceService.appendAgentLog(workspaceRoot, "Memory deleted: ${entry.key}")
                WorkspaceService.updateState(workspaceRoot, mapOf("memoryEntries" to updated.keys.size))
                refresh()
            }
        }

        view.findViewById<Button>(R.id.memorySaveButton).setOnClickListener {
            val key = keyField.text?.toString()?.trim().orEmpty()
            val value = valueField.text?.toString()?.trim().orEmpty()
            if (key.isBlank()) {
                Toast.makeText(requireContext(), "Ein Schlüssel ist erforderlich.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updated = WorkspaceService.loadMemoryMap(workspaceRoot).toMutableMap().apply {
                put(key, value)
            }
            WorkspaceService.saveMemoryMap(workspaceRoot, updated)
            WorkspaceService.appendAgentLog(workspaceRoot, "Memory saved: $key=$value")
            WorkspaceService.updateState(workspaceRoot, mapOf("memoryEntries" to updated.keys.size))
            WorkspaceService.appendChangeLog(workspaceRoot, "Memory updated: $key")
            keyField.setText("")
            valueField.setText("")
            refresh()
        }

        view.findViewById<Button>(R.id.memoryDeleteButton).setOnClickListener {
            val key = keyField.text?.toString()?.trim().orEmpty()
            if (key.isBlank()) {
                Toast.makeText(requireContext(), "Ein Schlüssel ist erforderlich.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val updated = WorkspaceService.loadMemoryMap(workspaceRoot).toMutableMap().apply {
                remove(key)
            }
            WorkspaceService.saveMemoryMap(workspaceRoot, updated)
            WorkspaceService.appendAgentLog(workspaceRoot, "Memory deleted: $key")
            WorkspaceService.updateState(workspaceRoot, mapOf("memoryEntries" to updated.keys.size))
            WorkspaceService.appendChangeLog(workspaceRoot, "Memory removed: $key")
            keyField.setText("")
            valueField.setText("")
            refresh()
        }

        refresh()
    }
}
