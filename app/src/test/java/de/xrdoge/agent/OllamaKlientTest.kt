package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.OllamaKlient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OllamaKlientTest {
    @Test
    fun jsonEscapeErsetztUmbrueche() {
        assertEquals("Hallo\\nWelt", OllamaKlient.jsonEscape("Hallo\nWelt"))
        assertEquals("\\\"Zitat\\\"", OllamaKlient.jsonEscape("\"Zitat\""))
    }

    @Test
    fun jsonStringFeldLiestResponse() {
        val json = "{\"model\":\"llama\",\"response\":\"Ergebnis\\nOK\",\"done\":true}"
        assertEquals("Ergebnis\nOK", OllamaKlient.jsonStringFeld(json, "response"))
    }

    @Test
    fun jsonStringFeldFehlt() {
        assertNull(OllamaKlient.jsonStringFeld("{\"done\":true}", "response"))
    }

    @Test
    fun jsonStringFeldDekodiertUnicodeUndEscape() {
        val json = "{\"model\":\"llama\",\"response\":\"Ergebnis\\u0021\\nOK\"}"
        assertEquals("Ergebnis!\nOK", OllamaKlient.jsonStringFeld(json, "response"))
    }
}
