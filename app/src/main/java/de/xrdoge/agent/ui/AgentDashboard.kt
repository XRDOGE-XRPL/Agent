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
    var status by remember { mutableStateOf(initialStatus) }
    val logs by runtime.logStream.logsFlow.collectAsState(initial = emptyList())

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
                    }
                }

                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

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
