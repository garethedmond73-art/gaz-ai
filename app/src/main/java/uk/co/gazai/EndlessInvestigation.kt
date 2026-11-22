package uk.co.gazai

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class InvestigationQuestion(
    val id: String,
    val parentId: String?,
    val question: String,
    val answer: String = ""
)

private class InvestigationTreeStore(context: Context) {
    private val prefs = context.getSharedPreferences("gaz_endless_investigation", Context.MODE_PRIVATE)

    fun load(): List<InvestigationQuestion> = runCatching {
        val array = JSONArray(prefs.getString("questions", "[]") ?: "[]")
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val question = item.optString("question").trim()
            if (question.isBlank()) return@mapNotNull null
            InvestigationQuestion(
                id = item.optString("id").ifBlank { UUID.randomUUID().toString() },
                parentId = item.optString("parentId").takeIf { it.isNotBlank() && it != "null" },
                question = question,
                answer = item.optString("answer")
            )
        }
    }.getOrDefault(emptyList())

    fun save(nodes: List<InvestigationQuestion>) {
        val array = JSONArray()
        nodes.forEach { node ->
            array.put(JSONObject()
                .put("id", node.id)
                .put("parentId", node.parentId ?: "")
                .put("question", node.question)
                .put("answer", node.answer))
        }
        prefs.edit().putString("questions", array.toString()).apply()
    }
}

@Composable
internal fun EndlessInvestigation(
    context: Context,
    ai: LocalAiViewModel,
    aiState: LocalAiState,
    evidenceContext: String
) {
    val store = remember { InvestigationTreeStore(context) }
    val nodes = remember { mutableStateListOf<InvestigationQuestion>().apply { addAll(store.load()) } }
    var selectedId by remember { mutableStateOf(nodes.firstOrNull()?.id.orEmpty()) }
    var draft by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf("") }
    var pendingNodeId by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val selected = nodes.firstOrNull { it.id == selectedId }

    fun persist() = store.save(nodes)

    LaunchedEffect(aiState.reply, aiState.busy, pendingAction) {
        if (pendingAction.isBlank() || aiState.busy || aiState.reply.isBlank() || aiState.reply == "Thinking…") return@LaunchedEffect
        val targetId = pendingNodeId
        when (pendingAction) {
            "answer" -> {
                val index = nodes.indexOfFirst { it.id == targetId }
                if (index >= 0) {
                    nodes[index] = nodes[index].copy(answer = aiState.reply.trim())
                    persist()
                    status = "Answer saved. Challenge it or generate the next questions."
                }
            }
            "branches" -> {
                val marker = "NEXT_QUESTIONS_JSON:"
                val raw = aiState.reply.substringAfter(marker, "")
                val first = raw.indexOf('[')
                val last = raw.lastIndexOf(']')
                val questions = runCatching {
                    require(first >= 0 && last > first) { "The AI did not return a question list." }
                    val array = JSONArray(raw.substring(first, last + 1))
                    (0 until array.length()).mapNotNull { i ->
                        array.optString(i).trim().takeIf { it.length > 8 }
                    }.distinct().take(4)
                }.getOrDefault(emptyList())
                val existing = nodes.filter { it.parentId == targetId }.map { it.question.lowercase() }.toSet()
                val additions = questions.filter { it.lowercase() !in existing }.map {
                    InvestigationQuestion(UUID.randomUUID().toString(), targetId, it)
                }
                nodes.addAll(additions)
                persist()
                status = if (additions.isEmpty()) {
                    "No new distinct follow-up questions were returned. Try again or write your own."
                } else {
                    "${additions.size} follow-up questions added to the tree."
                }
            }
        }
        pendingAction = ""
        pendingNodeId = ""
    }

    fun investigate(node: InvestigationQuestion) {
        if (!aiState.ready || aiState.busy) return
        selectedId = node.id
        pendingNodeId = node.id
        pendingAction = "answer"
        status = "Investigating this branch…"
        ai.send(
            "You are Truth Detective. Investigate the question below. Give a clear, readable answer. " +
                "Separate (1) established evidence and source-backed facts, (2) interpretation and inference, " +
                "(3) speculation, (4) strongest counterarguments or alternative explanations, and " +
                "(5) what remains unknown. Never present your own reasoning as independent evidence. " +
                "Only name or cite sources that are actually present in the supplied context or that you genuinely know; " +
                "say when evidence is unavailable. Challenge assumptions in the question as well as your own conclusions. " +
                "Do not force a conclusion. End with one short paragraph titled 'What would change this conclusion?'.\n\n" +
                "INVESTIGATION QUESTION: ${node.question}",
            evidenceContext,
            aiState.onlineSearchEnabled
        )
    }

    fun suggestBranches(node: InvestigationQuestion) {
        if (!aiState.ready || aiState.busy || node.answer.isBlank()) return
        selectedId = node.id
        pendingNodeId = node.id
        pendingAction = "branches"
        status = "Finding useful next questions…"
        ai.send(
            "You are extending a Truth Detective investigation. Based on the question and answer below, " +
                "generate 3 or 4 genuinely useful next questions. Each must open a distinct line of inquiry, " +
                "challenge an assumption, test evidence, seek counter-evidence, or explore an important implication. " +
                "Do not answer the questions. Avoid duplicates and leading questions. Return a valid JSON array of strings " +
                "at the very end, prefixed exactly with NEXT_QUESTIONS_JSON:. Do not wrap the JSON in markdown fences.\n\n" +
                "QUESTION: ${node.question}\n\nANSWER: ${node.answer.take(9000)}",
            evidenceContext,
            aiState.onlineSearchEnabled
        )
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("THE ENDLESS INVESTIGATION", style = MaterialTheme.typography.titleMedium, color = Color(0xFF7653B5))
                Text("One question leads to another. Follow different branches, keep your findings and challenge every conclusion.",
                    style = MaterialTheme.typography.bodySmall)
                Text("Questions are saved on this phone. AI answers are research leads, not independent proof.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF667784))
            }
        }

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Start a new investigation") },
            placeholder = { Text("What do you want to understand?") },
            minLines = 2,
            maxLines = 3
        )
        Button(
            onClick = {
                val text = draft.trim()
                if (text.isNotBlank()) {
                    val root = InvestigationQuestion(UUID.randomUUID().toString(), null, text)
                    nodes.add(root)
                    persist()
                    draft = ""
                    investigate(root)
                }
            },
            enabled = draft.isNotBlank() && aiState.ready && !aiState.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Start investigation") }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question tree · ${nodes.size} questions", color = Color.White, style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { status = "Select any question below to explore that branch." }) { Text("How it works") }
        }
        if (nodes.isEmpty()) {
            Text("Your question tree will grow here. Start with one question; each answer can open several new paths.",
                color = Color(0xFFB8C7D1), style = MaterialTheme.typography.bodySmall)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(nodes, key = { it.id }) { node ->
                    val depth = generateSequence(node.parentId) { parent ->
                        nodes.firstOrNull { it.id == parent }?.parentId
                    }.count().coerceAtMost(5)
                    Card(Modifier.fillMaxWidth().padding(start = (depth * 12).dp)) {
                        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(if (node.parentId == null) "STARTING QUESTION" else "FOLLOW-UP · LEVEL ${depth + 1}",
                                color = Color(0xFF7653B5), style = MaterialTheme.typography.labelSmall)
                            TextButton(
                                onClick = { selectedId = node.id },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    node.question,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = if (node.id == selectedId) Color(0xFF7653B5) else MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                if (node.answer.isBlank()) "Not investigated yet" else "Answer saved · ${node.answer.length} characters",
                                color = Color(0xFF667784), style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        selected?.let { node ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.heightIn(max = 250.dp).verticalScroll(rememberScrollState()).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("SELECTED QUESTION", color = Color(0xFF7653B5), style = MaterialTheme.typography.labelSmall)
                    Text(node.question, style = MaterialTheme.typography.titleSmall)
                    if (node.answer.isNotBlank()) {
                        Text("INVESTIGATION FINDINGS", style = MaterialTheme.typography.labelSmall)
                        Text(node.answer, style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("This branch has not been investigated yet.", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { investigate(node) },
                            enabled = aiState.ready && !aiState.busy,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (node.answer.isBlank()) "Investigate" else "Revisit") }
                        OutlinedButton(
                            onClick = { suggestBranches(node) },
                            enabled = node.answer.isNotBlank() && aiState.ready && !aiState.busy,
                            modifier = Modifier.weight(1f)
                        ) { Text("Next questions") }
                    }
                }
            }
        }

        if (aiState.busy) Text("Truth Detective is thinking…", color = Color(0xFFFFD18A), fontSize = 12.sp)
        if (status.isNotBlank()) Text(status, color = Color(0xFFB99BFF), fontSize = 12.sp)
        aiState.error?.let { Text(it, color = Color(0xFFFFB4AB), fontSize = 12.sp) }
    }
}
