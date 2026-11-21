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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF101820)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("GAZ AI", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                Text("Your assistant. Your device. Your data.", color = Color(0xFFB8C7D1))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("Chat", mode == "Chat") { mode = "Chat" }
                    ModeButton("Truth Detective", mode == "Truth Detective") { mode = "Truth Detective" }
                }
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(if (mode == "Chat") "Offline chat" else "Investigation workspace",
                            style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (mode == "Chat")
                                "Your conversation is saved on this phone. The local AI model still needs to be added."
                            else
                                "Capture a question, then organise evidence for and against it. AI analysis will be enabled when the local model is integrated.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text("Conversation", color = Color.White, style = MaterialTheme.typography.titleMedium)
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
                    label = { Text(if (mode == "Chat") "Message Gaz AI" else "Add an investigation note") },
                    minLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val message = draft.trim()
                        if (message.isNotEmpty()) {
                            lines.add(ChatLine("user", message))
                            lines.add(ChatLine("assistant",
                                "Saved on this device. The offline language model is not connected yet, so Gaz AI cannot generate an AI answer at this stage."))
                            history.save(lines)
                            draft = ""
                        }
                    }, enabled = draft.isNotBlank()) { Text("Save") }
                    TextButton(onClick = {
                        lines.clear()
                        history.save(lines)
                    }) { Text("Clear history", color = Color(0xFFB8C7D1)) }
                }
                Spacer(Modifier.height(2.dp))
                Text("Prototype 0.1 • Local storage enabled • AI engine not yet installed",
                    color = Color(0xFFB8C7D1), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(label) }
    else TextButton(onClick = onClick) { Text(label, color = Color(0xFFB8C7D1)) }
}
