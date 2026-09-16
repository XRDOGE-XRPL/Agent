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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.io.File

private fun loadWorkspaceDocs(projectDir: File): Map<String, String> {
    val candidates = listOf(
        "README.md",
        "ARCHITECTURE.md",
        "PROJECT_OVERVIEW.md",
        "CHANGELOG.md",
        "RELEASE_CHECKLIST.md"
    )

    if (!projectDir.exists() || !projectDir.isDirectory) return emptyMap()
    return candidates.mapNotNull { name ->
        val file = File(projectDir, name)
        if (file.exists() && file.isFile) name to file.readText(Charsets.UTF_8) else null
    }.toMap()
}

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
    var bootstrapStatus by remember { mutableStateOf("Bootstrap ready") }
    var selectedTab by remember { mutableStateOf(0) }
    val logs by runtime.logStream.logsFlow.collectAsState(initial = emptyList())
    val route by runtime.executionRouter.lastRoute.collectAsState(initial = null)
    val services by runtime.ephemeralServiceManager.servicesFlow.collectAsState(initial = emptyList())
    var docs by remember { mutableStateOf(emptyMap<String, String>()) }

    LaunchedEffect(workspace) {
        docs = loadWorkspaceDocs(File(workspace))
    }

    val runtimeState = listOf(
        "Bootstrap: $bootstrapStatus",
        "Termux: ${if (System.getProperty("os.name")?.contains("android", ignoreCase = true) == true || System.getenv("PREFIX")?.contains("termux", ignoreCase = true) == true) "detected" else "safe-mode"}",
        "Ollama: ${if (status.contains("Ollama", ignoreCase = true) || status.contains("reachable", ignoreCase = true)) "ready" else "pending"}",
        "IO: ready"
    )
    val tabs = listOf("Dashboard", "Dokumentation")

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

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                if (selectedTab == 0) {
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
                                                loadWorkspaceDocs(projectRoot)
                                            } else {
                                                emptyMap()
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

                    Card {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Runtime State", style = MaterialTheme.typography.titleMedium)
                            runtimeState.forEach { item ->
                                Text(item)
                            }
                        }
                    }

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
                } else {
                    if (docs.isEmpty()) {
                        Card {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Keine Dokumentation im aktuellen Arbeitsverzeichnis gefunden.", style = MaterialTheme.typography.bodyLarge)
                                Text("Erzeuge ein Projekt oder lade das Repo in $workspace, damit README.md, ARCHITECTURE.md, PROJECT_OVERVIEW.md und CHANGELOG.md automatisch gerendert werden.")
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(docs.toList()) { (title, content) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
