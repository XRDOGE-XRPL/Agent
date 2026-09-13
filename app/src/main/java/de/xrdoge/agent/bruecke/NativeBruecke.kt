package de.xrdoge.agent.bruecke

class NativeBruecke {
    external fun version(): String

    external fun pruefeOllama(url: String): Boolean

    external fun starteSchleife(
        arbeitsverzeichnis: String,
        aufgabe: String,
        ollamaUrl: String,
        modell: String,
        maxIterationen: Int
    ): String

    companion object {
        @Volatile
        var geladen: Boolean = false
            private set

        @Volatile
        var ladefehler: String? = null
            private set

        init {
            try {
                System.loadLibrary("agentkern")
                geladen = true
            } catch (ex: UnsatisfiedLinkError) {
                geladen = false
                ladefehler = ex.message
            }
        }
    }
}
