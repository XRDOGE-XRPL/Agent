package de.xrdoge.agent.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.xrdoge.agent.R
import java.io.File

class DocumentationFragment : Fragment(R.layout.fragment_documentation) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contentText = view.findViewById<TextView>(R.id.documentationContent)
        val workspace = File(requireContext().filesDir, "werkstatt")
        WorkspaceService.ensureWorkspaceDirectories(workspace)
        val docs = WorkspaceService.readWorkspaceDocs(workspace)
        val rendered = if (docs.isEmpty()) {
            "Keine Markdown-Dokumentation im aktuellen Arbeitsverzeichnis gefunden.\nErzeuge ein Projekt oder lade das Repo in /werkstatt, damit README.md, ARCHITECTURE.md, PROJECT_OVERVIEW.md und CHANGELOG.md automatisch gerendert werden."
        } else {
            docs.entries.sortedBy { it.key }
                .joinToString(separator = "\n\n---\n\n") { "${it.key}\n${it.value}" }
        }
        contentText.text = rendered
    }
}
