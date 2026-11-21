package uk.co.gazai

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    fun load(): List<ChatLine> {
        val raw = prefs.getString("chat_history", "[]") ?: "[]"
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val item = array.getJSONObject(index)
                ChatLine(item.optString("role"), item.optString("text"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(lines: List<ChatLine>) {
        val array = JSONArray()
        lines.forEach { line ->
            array.put(JSONObject().put("role", line.role).put("text", line.text))
        }
        prefs.edit().putString("chat_history", array.toString()).apply()
    }
}

@androidx.compose.runtime.Composable
private fun GazAiScreen(context: Context) {
    val history = remember { LocalHistory(context) }
    val lines = remember { mutableStateListOf<ChatLine>().apply { addAll(history.load()) } }
    var mode by remember { mutableStateOf("Chat") }
    var draft by remember { mutableStateOf("") }
    val ai = viewModel<LocalAiViewModel>()
    val aiState by ai.state.collectAsStateWithLifecycle()

    LaunchedEffect(aiState.reply, aiState.busy, aiState.error) {
        if (lines.lastOrNull()?.role == "assistant") {
            val visibleReply = when {
                aiState.reply.isNotEmpty() -> aiState.reply
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
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101820)) {
            Column(
                modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("GAZ AI", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("Your assistant. Your device. Your data.", color = Color(0xFFB8C7D1), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("Chat", mode == "Chat") { mode = "Chat" }
                    ModeButton("Truth Detective", mode == "Truth Detective") { mode = "Truth Detective" }
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(8.dp)) {
                        Text(if (mode == "Chat") "Offline chat" else "Investigation workspace",
                            style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (mode == "Chat") {
                                when {
                                    aiState.ready -> "Your offline AI is ready. Messages are answered on this phone."
                                    aiState.loading -> "Loading the offline AI model. This can take a little while."
                                    aiState.downloading -> "Downloading the offline model: " + aiState.downloadProgress + "%"
                                    aiState.modelDownloaded -> "The model is downloaded and ready to load."
                                    else -> "Your conversation is saved on this phone. Download the small local AI model to enable offline answers."
                                }
                            } else {
                                "Capture a question, then organise evidence for and against it. Your notes stay on this phone."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (mode == "Chat" && !aiState.ready) {
                    Button(
                        onClick = { if (aiState.modelDownloaded) ai.loadModel() else ai.downloadModel() },
                        enabled = !aiState.downloading && !aiState.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                aiState.downloading -> "Downloading model… " + aiState.downloadProgress + "%"
                                aiState.loading -> "Loading Gaz AI…"
                                aiState.modelDownloaded -> "Load offline AI"
                                else -> "Download offline AI (328 MiB)"
                            }
                        )
                    }
                }
                aiState.error?.let { message ->
                    Text(message, color = Color(0xFFFFB4AB), style = MaterialTheme.typography.bodySmall)
                }
                Text("Conversation", color = Color.White, style = MaterialTheme.typography.titleSmall)
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lines.isEmpty()) {
                        item { Text("Start by typing below. Nothing is sent to a cloud AI service.", color = Color(0xFFB8C7D1)) }
                    }
                    items(lines) { line ->
                        val isUser = line.role == "user"
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                            Column(Modifier.background(if (isUser) Color(0xFFDCEBFA) else Color(0xFFE7EDEB)).padding(12.dp)) {
                                Text(if (isUser) "You" else "Gaz AI", style = MaterialTheme.typography.labelMedium)
                                Text(line.text)
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            if (mode == "Chat") "Message Gaz AI" else "Add an investigation note",
                            color = Color(0xFFB8C7D1)
                        )
                    },
                    minLines = 1,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontSize = 18.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF9B7AE8),
                        focusedBorderColor = Color(0xFF7653B5),
                        unfocusedBorderColor = Color(0xFF7653B5),
                        focusedLabelColor = Color(0xFFB99BFF),
                        unfocusedLabelColor = Color(0xFFB8C7D1)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val message = draft.trim()
                        if (message.isNotEmpty()) {
                            lines.add(ChatLine("user", message))
                            if (mode == "Chat" && aiState.ready) {
                                lines.add(ChatLine("assistant", ""))
                                ai.send(message)
                            } else if (mode == "Chat") {
                                lines.add(ChatLine("assistant",
                                    "Your message is saved on this phone. Download and load the offline AI model above to get an answer."))
                            } else {
                                lines.add(ChatLine("assistant", "Investigation note saved on this phone."))
                            }
                            history.save(lines)
                            draft = ""
                        }
                    }, enabled = draft.isNotBlank() && !aiState.busy) {
                        Text(
                            if (mode == "Chat" && aiState.busy) "Thinking…"
                            else if (mode == "Chat") "Send" else "Save note",
                            color = Color.White
                        )
                    }
                    TextButton(onClick = {
                        lines.clear()
                        history.save(lines)
                    }) { Text("Clear history", color = Color(0xFFB8C7D1)) }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else TextButton(onClick = onClick) { Text(label, color = Color(0xFFB8C7D1)) }
}
