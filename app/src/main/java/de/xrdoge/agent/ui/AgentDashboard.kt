package de.xrdoge.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.xrdoge.agent.laufzeit.AgentRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AgentDashboard(
    runtime: AgentRuntime,
    workspace: String,
    initialStatus: String,
    onCheckOllama: suspend (String, String) -> Boolean,
    onRunAgent: suspend (String, String, String, String, Int) -> String
) {
    val scope = rememberCoroutineScope()
    var aufgabe by remember { mutableStateOf("") }
    var verzeichnis by remember { mutableStateOf(workspace) }
    var ollama by remember { mutableStateOf("http://127.0.0.1:11434") }
    var modell by remember { mutableStateOf("llama3.2") }
    var iterationen by remember { mutableStateOf("8") }
    var provider by remember { mutableStateOf("local") }
    var ttlMinutes by remember { mutableStateOf("30") }
    var status by remember { mutableStateOf(initialStatus) }
    val logs by runtime.logStream.logsFlow.collectAsState(initial = emptyList())
    val route by runtime.executionRouter.lastRoute.collectAsState(initial = null)
    val services by runtime.ephemeralServiceManager.servicesFlow.collectAsState(initial = emptyList())
    var docs by remember { mutableStateOf(emptyList<String>()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Autonomer Entwicklungsagent",
                    style = MaterialTheme.typography.headlineSmall
                )
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = verzeichnis,
                            onValueChange = { verzeichnis = it },
                            label = { Text("Arbeitsverzeichnis") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = aufgabe,
                            onValueChange = { aufgabe = it },
                            label = { Text("Aufgabe") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ollama,
                            onValueChange = { ollama = it },
                            label = { Text("Ollama-URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = modell,
                            onValueChange = { modell = it },
                            label = { Text("Modell") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = iterationen,
                            onValueChange = { iterationen = it },
                            label = { Text("Iterationen") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = provider,
                            onValueChange = { provider = it },
                            label = { Text("Provider (local/tower/partner)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ttlMinutes,
                            onValueChange = { ttlMinutes = it },
                            label = { Text("TTL Minuten") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) { onCheckOllama(ollama, modell) }
                                        status = if (ok) "Ollama ist erreichbar." else "Ollama ist nicht erreichbar."
                                        runtime.logStream.append(status)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Ollama prüfen")
                            }
                            Button(
                                onClick = {
                                    if (aufgabe.isBlank()) {
                                        status = "Bitte eine Aufgabe eingeben."
                                        runtime.logStream.append(status)
                                        return@Button
                                    }
                                    scope.launch {
                                        runtime.logStream.append("Agent start: $aufgabe")
                                        val result = withContext(Dispatchers.IO) {
                                            onRunAgent(
                                                verzeichnis,
                                                aufgabe,
                                                ollama,
                                                modell,
                                                iterationen.toIntOrNull() ?: 8
                                            )
                                        }
                                        status = result
                                        runtime.logStream.append(result)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Agent starten")
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (aufgabe.isBlank()) {
                                        status = "Bitte eine Aufgabe eingeben."
                                        runtime.logStream.append(status)
                                        return@Button
                                    }
                                    scope.launch {
                                        runtime.logStream.append("Project generation: $aufgabe")
                                        val result = withContext(Dispatchers.IO) {
                                            runtime.universalTaskEngine.generateAndValidate(aufgabe)
                                        }
                                        status = result
                                        runtime.logStream.append(result)
                                        val projectRoot = runtime.universalTaskEngine.lastProject?.rootDir
                                        docs = if (projectRoot != null) {
                                            runtime.universalTaskEngine.documentationFiles(projectRoot)
                                        } else {
                                            emptyList()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Projekt bauen")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        val ttl = ttlMinutes.toLongOrNull()?.coerceIn(1L, 360L)?.times(60_000L) ?: 30L * 60_000L
                                        val service = withContext(Dispatchers.IO) {
                                            runtime.ephemeralServiceManager.provision(
                                                name = "agent-${System.currentTimeMillis()}",
                                                type = "runtime",
                                                provider = provider.ifBlank { "local" },
                                                ttlMs = ttl,
                                                connection = "local://agent"
                                            )
                                        }
                                        status = "Ephemeral service ready: ${service.name} (${service.provider})"
                                        runtime.logStream.append(status)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Lease 30m")
                            }
                        }
                    }
                }

                val routeText = route?.let { "Route: ${it.provider} :: ${it.reason}" } ?: "Route: waiting for telemetry"
                Text(
                    text = "$status\n$routeText",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                if (services.isNotEmpty()) {
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Ephemeral services", style = MaterialTheme.typography.titleMedium)
                            services.forEach { service ->
                                Text("${service.name} :: ${service.provider} :: ${service.status} :: ${service.ttlMs / 60000}m")
                            }
                        }
                    }
                }

                if (docs.isNotEmpty()) {
                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Project docs", style = MaterialTheme.typography.titleMedium)
                            docs.forEach { doc ->
                                Text(doc)
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1F1F1F))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { entry ->
                            Text(
                                text = entry,
                                color = Color(0xFFECECEC)
                            )
                        }
                    }
                }
            }
        }
    }
}
