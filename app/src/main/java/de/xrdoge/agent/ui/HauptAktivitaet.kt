package de.xrdoge.agent.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.xrdoge.agent.AgentAnwendung
import de.xrdoge.agent.R
import de.xrdoge.agent.bruecke.NativeBruecke
import de.xrdoge.agent.laufzeit.AgentSessionStatus
import de.xrdoge.agent.laufzeit.OllamaKlient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HauptAktivitaet : AppCompatActivity() {
    private val adapter = ProtokollAdapter()
    private val runtime by lazy { (application as AgentAnwendung).runtime }

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

        val workspace = File(filesDir, "werkstatt").absolutePath
        verzeichnisFeld.setText(workspace)
        ollamaFeld.setText("http://127.0.0.1:11434")
        modellFeld.setText("llama3.2")
        iterationenFeld.setText("8")

        val repoSnapshot = runtime.githubClient.repositorySnapshot(workspace)
        val initialStatus = "${repoSnapshot.summary()} | Native=${NativeBruecke.geladen}"
        statusText.text = initialStatus
        adapter.hinzufuegen(initialStatus)

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
                val session = runtime.sessionRegistry.create(workspace, aufgabe)
                runtime.sessionRegistry.update(session.id, AgentSessionStatus.RUNNING, "Agent session ${session.id.take(8)} is running")

                val ergebnis = withContext(Dispatchers.IO) {
                    runtime.executionEngine.execute(
                        session = session,
                        arbeitsverzeichnis = workspace,
                        aufgabe = aufgabe,
                        ollamaUrl = ollamaFeld.text.toString(),
                        modell = modellFeld.text.toString(),
                        maxIterationen = iterationenFeld.text.toString().toIntOrNull() ?: 8
                    )
                }

                val status = runtime.sessionRegistry.list().firstOrNull { it.id == session.id }?.status
                val finalMessage = if (status == AgentSessionStatus.FAILED) "Session failed" else ergebnis
                adapter.hinzufuegen(finalMessage)
                statusText.text = finalMessage
                startKnopf.isEnabled = true
            }
        }
    }
}
