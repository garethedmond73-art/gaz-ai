package uk.co.gazai

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GazAiScreen(this) }
    }
}

private data class ChatLine(val role: String, val text: String)

private class LocalHistory(context: Context) {
    private val prefs = context.getSharedPreferences("gaz_ai_local", Context.MODE_PRIVATE)
    fun load(): List<ChatLine> = try {
        val array = JSONArray(prefs.getString("chat_history", "[]") ?: "[]")
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            ChatLine(item.optString("role"), item.optString("text"))
        }
    } catch (_: Exception) { emptyList() }

    fun save(lines: List<ChatLine>) {
        val array = JSONArray()
        lines.forEach { array.put(JSONObject().put("role", it.role).put("text", it.text)) }
        prefs.edit().putString("chat_history", array.toString()).apply()
    }
}

@androidx.compose.runtime.Composable
private fun GazAiScreen(context: Context) {
    val history = remember { LocalHistory(context) }
    val lines = remember { mutableStateListOf<ChatLine>().apply { addAll(history.load()) } }
    val imported = remember { mutableStateListOf<ImportedDocument>().apply { addAll(UserLibrary.load(context)) } }
    var mode by remember { mutableStateOf("Chat") }
    var draft by remember { mutableStateOf("") }
    var libraryStatus by remember { mutableStateOf("") }
    val conversationListState = rememberLazyListState()
    val ai = viewModel<LocalAiViewModel>()
    val aiState by ai.state.collectAsStateWithLifecycle()

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null } ?: "imported-file"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Could not open the selected file.")
                val doc = UserLibrary.saveImported(context, name, context.contentResolver.getType(uri), bytes)
                imported.clear()
                imported.addAll(UserLibrary.load(context))
                libraryStatus = "Saved ${doc.name} to your on-device library."
            } catch (e: Exception) {
                libraryStatus = e.message ?: "Could not import this file."
            }
        }
    }

    // Follow streamed reply updates as well as newly added messages, so the
    // newest text remains visible while Gaz AI is generating a response.
    LaunchedEffect(lines.size, lines.lastOrNull()?.text) {
        if (lines.isNotEmpty()) conversationListState.scrollToItem(lines.lastIndex)
    }

    LaunchedEffect(aiState.reply, aiState.busy, aiState.error) {
        if (lines.lastOrNull()?.role == "assistant") {
            val visibleReply = when {
                aiState.reply.isNotEmpty() -> aiState.reply.replace("**", "")
                aiState.busy -> "Thinking…"
                aiState.error != null -> aiState.error!!
                else -> lines.last().text
            }
            if (visibleReply != lines.last().text) {
                lines[lines.lastIndex] = ChatLine("assistant", visibleReply)
                history.save(lines)
            }
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF101820)) {
            Column(
                Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("GAZ AI", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Your assistant. Your device. Your data.", color = Color(0xFFB8C7D1), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ModeButton("Chat", mode == "Chat", Modifier.weight(1f)) { mode = "Chat" }
                    ModeButton("Truth", mode == "Truth Detective", Modifier.weight(1f)) { mode = "Truth Detective" }
                    ModeButton("Library", mode == "Library", Modifier.weight(1f)) { mode = "Library" }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text(when (mode) { "Chat" -> "Offline chat"; "Library" -> "Offline knowledge library"; else -> "Investigation workspace" },
                            style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (mode != "Library") when {
                                aiState.ready -> "Your offline AI is ready. Chat and investigations use the same on-device model and library."
                                aiState.loading -> "Restoring the offline AI model. This can take a little while."
                                aiState.downloading -> "Downloading the offline model: ${aiState.downloadProgress}%"
                                aiState.modelDownloaded -> "The model is downloaded and will load automatically."
                                else -> "Download the offline AI model to enable private, on-device answers."
                            } else "Your notes and imported documents stay on this phone.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (mode != "Library" && !aiState.ready) {
                    Button(
                        onClick = { if (aiState.modelDownloaded) ai.loadModel() else ai.downloadModel() },
                        enabled = !aiState.downloading && !aiState.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(when {
                            aiState.downloading -> "Downloading model… ${aiState.downloadProgress}%"
                            aiState.loading -> "Restoring Gaz AI…"
                            aiState.modelDownloaded -> "Retry loading offline AI"
                            else -> "Download offline AI"
                        })
                    }
                }
                if (mode == "Library") {
                    Button(onClick = { filePicker.launch(arrayOf("application/pdf", "application/epub+zip", "text/*", "application/json", "application/xml")) },
                        modifier = Modifier.fillMaxWidth()) { Text("＋ Add files to library") }
                    Text("PDF, EPUB, TXT, Markdown, HTML, CSV and JSON · stored on this phone",
                        color = Color(0xFFB8C7D1), style = MaterialTheme.typography.bodySmall)
                    if (libraryStatus.isNotBlank()) Text(libraryStatus, color = Color(0xFFB99BFF), style = MaterialTheme.typography.bodySmall)
                }
                aiState.error?.let { Text(it, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall) }
                Text(if (mode == "Library") "Your library" else "Conversation", color = Color.White, style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    state = conversationListState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mode == "Library") {
                        items(lines) { line ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.background(Color(0xFFE7EDEB)).padding(12.dp)) {
                                    Text(if (line.role == "user") "Your library question" else "Gaz AI", style = MaterialTheme.typography.labelMedium)
                                    Text(line.text, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                        items(StarterLibrary.entries) { entry ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(entry.collection.uppercase(), style = MaterialTheme.typography.labelSmall)
                                    Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                    Text(entry.body, style = MaterialTheme.typography.bodyMedium)
                                    Text("Source: ${entry.sourceName} · ${entry.sourceUrl}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        items(imported, key = { it.fileName }) { doc ->
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("YOUR IMPORTED FILE", style = MaterialTheme.typography.labelSmall)
                                    Text(doc.name, style = MaterialTheme.typography.titleMedium)
                                    Text(doc.text.take(1400), style = MaterialTheme.typography.bodyMedium)
                                    Text("${doc.text.length} characters · saved on this phone", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        if (imported.isEmpty()) item { Text("No personal files added yet. Tap Add files to library.", color = Color(0xFFB8C7D1)) }
                    } else {
                        if (lines.isEmpty()) item { Text("Start by typing below. Nothing is sent to a cloud AI service.", color = Color(0xFFB8C7D1)) }
                        items(lines) { line ->
                            val isUser = line.role == "user"
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.background(if (isUser) Color(0xFFDCEBFA) else Color(0xFFE7EDEB)).padding(12.dp)) {
                                    Text(
                                        when (line.role) { "user" -> "You"; "note" -> "Investigation note"; else -> "Gaz AI" },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(line.text, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(
                        when (mode) {
                            "Chat" -> "Message Gaz AI"
                            "Truth Detective" -> "Ask an investigation question"
                            else -> "Ask your library a question"
                        }, color = Color(0xFFB8C7D1)
                    ) },
                    minLines = 1, maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontSize = 18.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF9B7AE8), focusedBorderColor = Color(0xFF7653B5),
                        unfocusedBorderColor = Color(0xFF7653B5), focusedLabelColor = Color(0xFFB99BFF),
                        unfocusedLabelColor = Color(0xFFB8C7D1)
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val message = draft.trim()
                        if (message.isNotEmpty()) {
                            lines.add(ChatLine("user", message))
                            if (aiState.ready) {
                                lines.add(ChatLine("assistant", "Thinking…"))
                                val notes = if (mode == "Truth Detective") {
                                    lines.filter { it.role == "note" }.takeLast(10)
                                        .joinToString("\n") { it.text }
                                } else ""
                                ai.send(message, notes)
                            }
                            history.save(lines)
                            draft = ""
                        }
                    }, modifier = Modifier.weight(1f),
                        enabled = draft.isNotBlank() && !aiState.busy && aiState.ready) {
                        Text(when {
                            aiState.busy -> "Thinking…"
                            mode == "Truth Detective" -> "Investigate"
                            mode == "Library" -> "Ask library"
                            else -> "Send"
                        })
                    }
                    if (mode == "Truth Detective") {
                        TextButton(onClick = {
                            val note = draft.trim()
                            if (note.isNotEmpty()) {
                                lines.add(ChatLine("note", "Investigation note: $note"))
                                history.save(lines)
                                draft = ""
                            }
                        }, enabled = draft.isNotBlank() && !aiState.busy) {
                            Text("Save note", color = Color(0xFFB8C7D1))
                        }
                    }
                    TextButton(onClick = { lines.clear(); history.save(lines) }) { Text("Clear history", color = Color(0xFFB8C7D1)) }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick, modifier = modifier) { Text(label) }
    else TextButton(onClick = onClick, modifier = modifier) { Text(label, color = Color(0xFFB8C7D1)) }
}
