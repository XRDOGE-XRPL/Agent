package de.xrdoge.agent.laufzeit

import de.xrdoge.agent.bruecke.NativeBruecke
import java.io.File

class AgentSchleife(
    private val bruecke: NativeBruecke = NativeBruecke()
) {
    private val lock = Any()

    fun ausfuehrenSicher(
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String = synchronized(lock) {
        ausfuehren(arbeitsverzeichnis, aufgabe, ollamaUrl, modell, maxIterationen)
    }

    fun ausfuehren(
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String {
        val pfad = arbeitsverzeichnis.ifBlank { File(".").absolutePath }
        val dir = File(pfad)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val iterationen = maxIterationen.coerceAtLeast(1)
        if (NativeBruecke.geladen) {
            return bruecke.starteSchleife(
                pfad,
                aufgabe.ifBlank { "No task provided" },
                ollamaUrl.ifBlank { "http://127.0.0.1:11434" },
                modell.ifBlank { "qwen2.5-coder" },
                iterationen
            )
        }
        return "Nativer Kern nicht geladen: ${NativeBruecke.ladefehler ?: "unbekannt"}"
    }
}
