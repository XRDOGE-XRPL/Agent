package de.xrdoge.agent

import de.xrdoge.agent.laufzeit.OllamaKlient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun jsonStringFeldErsetztDefekteUnicodeEscape() {
        val json = "{\"model\":\"llama\",\"response\":\"\\uD83D\"}"
        assertEquals("\uFFFD", OllamaKlient.jsonStringFeld(json, "response"))
    }

    @Test
    fun jsonStringFeldErsetztDefekteUnicodeHexSequenz() {
        val json = "{\"model\":\"llama\",\"response\":\"A\\uZZZZB\"}"
        assertEquals("A\uFFFDB", OllamaKlient.jsonStringFeld(json, "response"))
    }

    @Test
    fun jsonStringFeldErsetztDefekteEscapeSequenz() {
        val json = "{\"model\":\"llama\",\"response\":\"A\\xB\"}"
        assertEquals("A\uFFFDB", OllamaKlient.jsonStringFeld(json, "response"))
    }

    @Test
    fun jsonStringFeldAkzeptiertTeilweiseTrunkierteZeichenkette() {
        val json = "{\"model\":\"llama\",\"response\":\"A"
        assertEquals("A", OllamaKlient.jsonStringFeld(json, "response"))
    }

    @Test
    fun begrenzePromptSchneidetUeberlangeEingaben() {
        val prompt = "A".repeat(12_000)
        val begrenzt = OllamaKlient.begrenzePrompt(prompt)
        assertTrue(begrenzt.length <= 6_000)
        assertTrue(begrenzt.endsWith("[gekürzt]"))
    }
}
