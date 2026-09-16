package de.xrdoge.agent.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import de.xrdoge.agent.AgentAnwendung
import de.xrdoge.agent.bruecke.NativeBruecke
import de.xrdoge.agent.laufzeit.AgentSessionStatus
import de.xrdoge.agent.laufzeit.OllamaKlient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class HauptAktivitaet : AppCompatActivity() {
    private val runtime by lazy { (application as AgentAnwendung).runtime }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val workspace = File(filesDir, "werkstatt").absolutePath
        val repoSnapshot = runtime.githubClient.repositorySnapshot(workspace)
        val initialStatus = "${repoSnapshot.summary()} | Native=${NativeBruecke.geladen}"
        runtime.logStream.append(initialStatus)

        setContent {
            AgentDashboard(
                runtime = runtime,
                workspace = workspace,
                initialStatus = initialStatus,
                onCheckOllama = { url, model ->
                    if (NativeBruecke.geladen) {
                        NativeBruecke().pruefeOllama(url)
                    } else {
                        val localClient = de.xrdoge.agent.laufzeit.LocalOllamaClient(baseUrl = url, model = model, bridge = runtime.socketBridge, logStream = runtime.logStream)
                        localClient.reachable()
                    }
                },
                onRunAgent = { dir, task, ollamaUrl, model, iterations ->
                    val session = runtime.sessionRegistry.create(dir, task)
                    runtime.sessionRegistry.update(
                        session.id,
                        AgentSessionStatus.RUNNING,
                        "Agent session ${session.id.take(8)} is running"
                    )
                    val result = withContext(Dispatchers.IO) {
                        runtime.executionEngine.execute(
                            session = session,
                            arbeitsverzeichnis = dir,
                            aufgabe = task,
                            ollamaUrl = ollamaUrl,
                            modell = model,
                            maxIterationen = iterations
                        )
                    }
                    val status = runtime.sessionRegistry.list().firstOrNull { it.id == session.id }?.status
                    if (status == AgentSessionStatus.FAILED) "Session failed" else result
                }
            )
        }
    }
}
