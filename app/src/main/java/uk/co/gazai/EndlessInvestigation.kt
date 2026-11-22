package uk.co.gazai

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class InvestigationStep(
    val id: String,
    val parentId: String,
    val question: String,
    val answer: String,
    val challenge: String,
    val nextQuestion: String,
    val createdAt: Long
)

private class InvestigationTrail(context: Context) {
    private val prefs = context.getSharedPreferences("gaz_endless_investigation", Context.MODE_PRIVATE)

    fun load(): List<InvestigationStep> = runCatching {
        val array = JSONArray(prefs.getString("steps", "[]") ?: "[]")
        (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            InvestigationStep(
                item.optString("id"), item.optString("parentId"),
                item.optString("question"), item.optString("answer"),
                item.optString("challenge"), item.optString("nextQuestion"),
                item.optLong("createdAt")
            )
        }
    }.getOrDefault(emptyList())

    fun save(steps: List<InvestigationStep>) {
        val array = JSONArray()
        steps.forEach { step ->
            array.put(JSONObject()
                .put("id", step.id).put("parentId", step.parentId)
                .put("question", step.question).put("answer", step.answer)
                .put("challenge", step.challenge).put("nextQuestion", step.nextQuestion)
                .put("createdAt", step.createdAt))
        }
        prefs.edit().putString("steps", array.toString()).apply()
    }

    fun clear() { prefs.edit().remove("steps").apply() }
}

@Composable
internal fun EndlessInvestigation(context: Context, ai: LocalAiViewModel, aiState: LocalAiState) {
    val trail = remember { InvestigationTrail(context) }
    val steps = remember { mutableStateListOf<InvestigationStep>().apply { addAll(trail.load()) } }
    var question by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf("") }
    var pendingQuestion by remember { mutableStateOf("") }
    var pendingNodeId by remember { mutableStateOf("") }
    var lastProcessedReply by remember { mutableStateOf("") }
    var showClear by remember { mutableStateOf(false) }

    val latest = steps.lastOrNull()
    val trailContext = steps.takeLast(5).joinToString("\n\n") { step ->
        "QUESTION: ${step.question}\nANSWER (AI-generated; not independently verified): ${step.answer.take(1000)}" +
            if (step.challenge.isNotBlank()) "\nCOUNTER-INVESTIGATION: ${step.challenge.take(700)}" else ""
    }

    LaunchedEffect(aiState.reply, aiState.busy, pendingAction) {
        val reply = aiState.reply.trim()
        if (pendingAction.isNotBlank() && !aiState.busy && reply.isNotBlank() &&
            reply != "Thinking…" && reply != lastProcessedReply) {
            val marker = "NEXT_QUESTION:"
            val nextQuestion = reply.substringAfter(marker, "").lineSequence()
                .firstOrNull()?.trim()?.trim('"', '“', '”').orEmpty()
            val mainText = reply.substringBefore(marker).trim().ifBlank { reply }
            if (pendingAction == "challenge") {
                val index = steps.indexOfFirst { it.id == pendingNodeId }
                if (index >= 0) {
                    steps[index] = steps[index].copy(challenge = mainText, nextQuestion = nextQuestion)
                    trail.save(steps)
                }
            } else {
                val parent = steps.lastOrNull()?.id.orEmpty()
                steps.add(InvestigationStep(
                    UUID.randomUUID().toString(), parent, pendingQuestion,
                    mainText, "", nextQuestion, System.currentTimeMillis()
                ))
                trail.save(steps)
                question = nextQuestion
            }
            lastProcessedReply = reply
            pendingAction = ""
            pendingQuestion = ""
            pendingNodeId = ""
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF202D39))) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("THE ENDLESS INVESTIGATION", color = Color(0xFFFFD166), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("One question opens another. Follow the evidence, challenge every answer and keep the trail of how your thinking changes.",
                    color = Color(0xFFE0E7EF), fontSize = 12.sp)
                Text("AI answers are research leads, not independent proof. Sources must be checked and uncertainty kept visible.",
                    color = Color(0xFFFFD18A), fontSize = 11.sp)
            }
        }

        Text("Start a question or continue the trail", color = Color.White, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = question,
            onValueChange = { question = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What do you want to investigate?") },
            minLines = 2,
            maxLines = 4
        )
        Button(
            onClick = {
                val q = question.trim()
                if (q.isNotBlank() && aiState.ready && !aiState.busy) {
                    pendingAction = "investigate"
                    pendingQuestion = q
                    lastProcessedReply = ""
                    ai.send(
                        "Investigate this question as the next step in an ongoing Truth Detective investigation. Answer clearly and concisely. Separate (1) established evidence with sources actually available, (2) interpretation, (3) speculation, and (4) strongest counterarguments and unknowns. Do not claim you have directly observed reality or evidence outside the information supplied to you. Do not invent citations. Treat previous findings as provisional and correct them if needed. End with exactly one useful, open NEXT_QUESTION: line that follows logically from the investigation. The next question must be a real question, not a conclusion.\n\nCURRENT QUESTION: $q",
                        trailContext,
                        aiState.onlineSearchEnabled
                    )
                }
            },
            enabled = question.isNotBlank() && aiState.ready && !aiState.busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (aiState.busy) "Investigating…" else "Investigate and find next question") }

        if (!aiState.ready) {
            Text("Load the offline AI from the Chat tab before starting an investigation.", color = Color(0xFFFFD18A), fontSize = 12.sp)
        }
        if (pendingAction.isNotBlank()) {
            Text("Following the question…", color = Color(0xFFFFD18A), fontSize = 12.sp)
        }
        if (aiState.error != null) Text(aiState.error!!, color = Color(0xFFFFB4AB), fontSize = 12.sp)

        if (latest != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LATEST STEP · ${steps.size}", color = Color(0xFF7653B5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(latest.question, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("INVESTIGATION", color = Color(0xFFB99BFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(latest.answer, color = Color(0xFFE0E7EF), fontSize = 12.sp)
                    if (latest.challenge.isNotBlank()) {
                        Text("COUNTER-INVESTIGATION", color = Color(0xFFFFD166), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(latest.challenge, color = Color(0xFFE0E7EF), fontSize = 12.sp)
                    }
                    if (latest.nextQuestion.isNotBlank()) {
                        HorizontalDivider()
                        Text("NEXT QUESTION", color = Color(0xFFFFD166), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(latest.nextQuestion, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        OutlinedButton(
                            onClick = { question = latest.nextQuestion },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Follow this question") }
                    }
                    OutlinedButton(
                        onClick = {
                            if (aiState.ready && !aiState.busy) {
                                pendingAction = "challenge"
                                pendingNodeId = latest.id
                                lastProcessedReply = ""
                                ai.send(
                                    "Act as a rigorous but fair challenger. Examine the previous answer and identify its weakest assumptions, missing evidence, alternative explanations and strongest counterarguments. Correct factual errors where possible. Clearly separate sourced evidence from your reasoning and speculation. Do not invent sources. End with exactly one useful NEXT_QUESTION: line that would move the investigation forward.\n\nQUESTION: ${latest.question}\n\nANSWER TO CHALLENGE:\n${latest.answer}",
                                    trailContext,
                                    aiState.onlineSearchEnabled
                                )
                            }
                        },
                        enabled = aiState.ready && !aiState.busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Challenge this answer") }
                }
            }
        } else {
            Text("Your question trail will appear here. Each step is saved on this phone.", color = Color(0xFFB8C7D1), fontSize = 12.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("QUESTION TRAIL · ${steps.size} steps", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            TextButton(onClick = { showClear = true }, enabled = steps.isNotEmpty()) { Text("Clear trail") }
        }
        steps.asReversed().forEachIndexed { reverseIndex, step ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF202D39))) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("STEP ${steps.size - reverseIndex}", color = Color(0xFFB99BFF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(step.question, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(step.answer.take(260) + if (step.answer.length > 260) "…" else "",
                        color = Color(0xFFB8C7D1), fontSize = 11.sp)
                    if (step.challenge.isNotBlank()) Text("Challenged ✓", color = Color(0xFFFFD166), fontSize = 10.sp)
                }
            }
        }
    }

    if (showClear) AlertDialog(
        onDismissRequest = { showClear = false },
        title = { Text("Clear question trail?") },
        text = { Text("This deletes the saved Endless Investigation trail from this phone. The main evidence ledger and library are not changed.") },
        confirmButton = {
            TextButton(onClick = { steps.clear(); trail.clear(); question = ""; showClear = false }) { Text("Delete trail") }
        },
        dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } }
    )
}
