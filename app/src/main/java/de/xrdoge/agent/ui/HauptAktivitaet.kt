package de.xrdoge.agent.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.R
import de.xrdoge.agent.bruecke.NativeBruecke
import de.xrdoge.agent.laufzeit.AgentSchleife
import de.xrdoge.agent.laufzeit.OllamaKlient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HauptAktivitaet : AppCompatActivity() {
    private val adapter = ProtokollAdapter()
    private val schleife = AgentSchleife()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.aktivitaet_haupt)

        val aufgabeFeld = findViewById<EditText>(R.id.feldAufgabe)
        val verzeichnisFeld = findViewById<EditText>(R.id.feldVerzeichnis)
        val ollamaFeld = findViewById<EditText>(R.id.feldOllama)
        val modellFeld = findViewById<EditText>(R.id.feldModell)
        val iterationenFeld = findViewById<EditText>(R.id.feldIterationen)
        val statusText = findViewById<TextView>(R.id.textStatus)
        val startKnopf = findViewById<Button>(R.id.knopfStart)
        val pruefenKnopf = findViewById<Button>(R.id.knopfOllama)
        val liste = findViewById<RecyclerView>(R.id.listeProtokoll)

        verzeichnisFeld.setText(File(filesDir, "werkstatt").absolutePath)
        ollamaFeld.setText("http://127.0.0.1:11434")
        modellFeld.setText("llama3.2")
        iterationenFeld.setText("8")

        liste.layoutManager = LinearLayoutManager(this)
        liste.adapter = adapter

        val version = if (NativeBruecke.geladen) {
            try {
                NativeBruecke().version()
            } catch (_: Exception) {
                getString(R.string.kern_nicht_geladen)
            }
        } else {
            getString(R.string.kern_nicht_geladen)
        }
        statusText.text = version
        adapter.hinzufuegen(version)

        pruefenKnopf.setOnClickListener {
            lifecycleScope.launch {
                val url = ollamaFeld.text.toString()
                val ok = withContext(Dispatchers.IO) {
                    if (NativeBruecke.geladen) {
                        NativeBruecke().pruefeOllama(url)
                    } else {
                        OllamaKlient(url, modellFeld.text.toString()).erreichbar()
                    }
                }
                val meldung = if (ok) getString(R.string.ollama_ok) else getString(R.string.ollama_fehler)
                statusText.text = meldung
                adapter.hinzufuegen(meldung)
            }
        }

        startKnopf.setOnClickListener {
            val aufgabe = aufgabeFeld.text.toString().trim()
            if (aufgabe.isEmpty()) {
                statusText.text = getString(R.string.aufgabe_fehlt)
                return@setOnClickListener
            }
            startKnopf.isEnabled = false
            adapter.leeren()
            adapter.hinzufuegen(getString(R.string.schleife_startet))
            lifecycleScope.launch {
                val ergebnis = withContext(Dispatchers.IO) {
                    schleife.ausfuehren(
                        verzeichnisFeld.text.toString(),
                        aufgabe,
                        ollamaFeld.text.toString(),
                        modellFeld.text.toString(),
                        iterationenFeld.text.toString().toIntOrNull() ?: 8
                    )
                }
                adapter.hinzufuegen(ergebnis)
                statusText.text = ergebnis
                startKnopf.isEnabled = true
            }
        }
    }
}
