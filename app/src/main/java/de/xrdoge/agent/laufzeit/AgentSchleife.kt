package de.xrdoge.agent.laufzeit

import de.xrdoge.agent.bruecke.NativeBruecke
import java.io.File

class AgentSchleife(
    private val bruecke: NativeBruecke = NativeBruecke()
) {
    fun ausfuehren(
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String {
        File(arbeitsverzeichnis).mkdirs()
        if (NativeBruecke.geladen) {
            return bruecke.starteSchleife(
                arbeitsverzeichnis,
                aufgabe,
                ollamaUrl,
                modell,
                maxIterationen
            )
        }
        return "Nativer Kern nicht geladen: ${NativeBruecke.ladefehler ?: "unbekannt"}"
    }
}
