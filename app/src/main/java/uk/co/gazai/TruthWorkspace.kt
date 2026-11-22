package uk.co.gazai

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class TruthEvidence(
    val id: String, val group: String, val claim: String,
    val source: String, val support: String, val challenge: String
)
private data class TruthLink(val id: String, val from: String, val to: String, val reason: String)

private class TruthBoard(context: Context) {
    private val prefs = context.getSharedPreferences("gaz_truth_board", Context.MODE_PRIVATE)
    fun loadEvidence(): List<TruthEvidence> = runCatching {
        val a = JSONArray(prefs.getString("evidence", "[]"))
        val seenIds = mutableSetOf<String>()
        var repairedIds = false
        val records = (0 until a.length()).map { i -> a.getJSONObject(i).let {
            val storedId = it.getString("id")
            val uniqueId = if (seenIds.add(storedId)) storedId else UUID.randomUUID().toString().also { seenIds.add(it); repairedIds = true }
            TruthEvidence(uniqueId, it.getString("group"), it.getString("claim"),
                it.optString("source"), it.optString("support"), it.optString("challenge"))
        } }
        // Repair old boards with repeated IDs so two separate records can be connected.
        if (repairedIds) save(records, loadLinks())
        records
    }.getOrDefault(emptyList())
    fun loadLinks(): List<TruthLink> = runCatching {
        val a = JSONArray(prefs.getString("links", "[]"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let {
            TruthLink(it.getString("id"), it.getString("from"), it.getString("to"), it.optString("reason"))
        } }
    }.getOrDefault(emptyList())
    fun clear() { prefs.edit().remove("evidence").remove("links").apply() }
    fun save(evidence: List<TruthEvidence>, links: List<TruthLink>) {
        val e = JSONArray()
        evidence.forEach { e.put(JSONObject().put("id",it.id).put("group",it.group).put("claim",it.claim)
            .put("source",it.source).put("support",it.support).put("challenge",it.challenge)) }
        val l = JSONArray()
        links.forEach { l.put(JSONObject().put("id",it.id).put("from",it.from).put("to",it.to).put("reason",it.reason)) }
        prefs.edit().putString("evidence",e.toString()).putString("links",l.toString()).apply()
    }
}


internal fun saveFreeAiAnswerToTruthBoard(context: Context, question: String, answer: String): Boolean {
    val cleanAnswer = answer.trim()
    if (cleanAnswer.isBlank()) return false
    val board = TruthBoard(context)
    val evidence = board.loadEvidence().toMutableList()
    if (evidence.any { it.source == "Free AI second opinion" && it.support.trim() == cleanAnswer }) return false
    val cleanQuestion = question.trim().ifBlank { "Free AI second opinion" }
    evidence.add(0, TruthEvidence(
        UUID.randomUUID().toString(),
        "Human experience",
        "Free AI: ${cleanQuestion.take(180)}",
        "Free AI second opinion",
        cleanAnswer.take(12000),
        "AI-generated answer; treat as a research lead, not verified evidence. Check original sources and record counter-evidence."
    ))
    board.save(evidence, board.loadLinks())
    return true
}

private val witnessGroups = listOf(
    "Scripture", "Science", "Philosophy", "Human experience", "Symbols & language patterns"
)
private val starterQuestions = listOf(
    "What do different traditions mean by the inner self, and where do their teachings differ?",
    "What does neuroscience establish about consciousness, and what remains unexplained?",
    "Is history a repeating cycle, a spiral, or a mixture of recurring patterns and change?",
    "How can we test apparent number, symbol and language patterns against coincidence?",
    "What does conservation of energy tell us, and what does it not tell us about personal consciousness?"
)

@Composable
internal fun TruthWorkspace(context: Context, ai: LocalAiViewModel, aiState: LocalAiState) {
    val board = remember { TruthBoard(context) }
    val truthFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        disabledTextColor = Color(0xFFE0E7EF),
        cursorColor = Color(0xFFFFD166),
        focusedBorderColor = Color(0xFFB99BFF),
        unfocusedBorderColor = Color(0xFF9B7AE8),
        focusedLabelColor = Color(0xFFD4C2FF),
        unfocusedLabelColor = Color(0xFFB8C7D1),
        focusedContainerColor = Color(0xFF202D39),
        unfocusedContainerColor = Color(0xFF202D39),
        disabledContainerColor = Color(0xFF202D39)
    )
    val evidence = remember { mutableStateListOf<TruthEvidence>().apply { addAll(board.loadEvidence()) } }
    val links = remember { mutableStateListOf<TruthLink>().apply { addAll(board.loadLinks()) } }
    var question by remember { mutableStateOf("") }
    var claim by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(witnessGroups.first()) }
    var source by remember { mutableStateOf("") }
    var support by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf("") }
    var connectionReason by remember { mutableStateOf("") }
    var fromId by remember { mutableStateOf("") }
    var toId by remember { mutableStateOf("") }
    var showAddEvidence by remember { mutableStateOf(false) }
    var showAddLink by remember { mutableStateOf(false) }
    var showClearBoard by remember { mutableStateOf(false) }
    var pendingQuestion by remember { mutableStateOf("") }
    var lastSavedReply by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf("Investigate") }

    LaunchedEffect(aiState.reply, aiState.busy) {
        if (pendingQuestion.isNotBlank() && !aiState.busy && aiState.reply.isNotBlank() &&
            aiState.reply != "Thinking…" && aiState.reply != lastSavedReply) {
            // Extract distinct findings from the investigation answer for the evidence ledger.
            val extracted = runCatching {
                val raw = aiState.reply.substringAfter("LEDGER_JSON:", "")
                val first = raw.indexOf('[')
                val last = raw.lastIndexOf(']')
                require(first >= 0 && last > first) { "No structured ledger items" }
                val array = JSONArray(raw.substring(first, last + 1))
                (0 until array.length()).mapNotNull { index ->
                    val item = array.optJSONObject(index) ?: return@mapNotNull null
                    val claimText = item.optString("claim").trim()
                    if (claimText.isBlank()) return@mapNotNull null
                    val requestedGroup = item.optString("group").trim()
                    val safeGroup = witnessGroups.firstOrNull { it.equals(requestedGroup, true) }
                        ?: "Human experience"
                    TruthEvidence(
                        UUID.randomUUID().toString(), safeGroup, claimText.take(500),
                        item.optString("source").take(500),
                        item.optString("support").take(1200),
                        item.optString("challenge").ifBlank {
                            "No counter-evidence identified in this answer; further research needed."
                        }.take(1200)
                    )
                }.take(8)
            }.getOrDefault(emptyList())

            if (extracted.isNotEmpty()) {
                extracted.forEach { record -> evidence.add(0, record) }
            } else {
                // Keep a safe fallback if the model does not return valid structured records.
                evidence.add(0, TruthEvidence(UUID.randomUUID().toString(), "Human experience",
                    "AI investigation: ${pendingQuestion.take(180)}",
                    "Truth Detective on-device model" + if (aiState.onlineSearchEnabled) " with optional web context" else " · offline",
                    aiState.reply, "AI-generated research is provisional. Verify original sources and seek counter-evidence."))
            }
            board.save(evidence, links)
            lastSavedReply = aiState.reply
            pendingQuestion = ""
        }
    }

    val contextNotes = evidence.take(12).joinToString("\n") {
        "[${it.group}] ${it.claim}\nSource: ${it.source}\nSupport: ${it.support.take(450)}\nChallenge: ${it.challenge.take(300)}"
    } + "\n\nRecorded connections: " + links.take(20).joinToString("; ") { link ->
        val a = evidence.find { it.id == link.from }?.claim ?: "Unknown"
        val b = evidence.find { it.id == link.to }?.claim ?: "Unknown"
        "$a ↔ $b: ${link.reason}"
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("Investigate" to "Investigate", "Evidence" to "Evidence",
                "Connections" to "Links", "Endless" to "Endless").forEach { (tabKey, label) ->
                if (tab == tabKey) Button(onClick = { tab = tabKey }, modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp)) {
                    Text(label, fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
                else TextButton(onClick = { tab = tabKey }, modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 5.dp)) {
                    Text(label, color = Color(0xFFD4C2FF), fontSize = 10.sp, maxLines = 1, softWrap = false)
                }
            }
        }
        when (tab) {
            "Investigate" -> {
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Five witness groups", color = Color.White, style = MaterialTheme.typography.titleSmall)
                witnessGroups.forEach { Text("• $it", color = Color(0xFFB8C7D1), fontSize = 12.sp) }
                Text("Start with a question", color = Color.White, style = MaterialTheme.typography.titleSmall)
                starterQuestions.forEach { q ->
                    TextButton(
                        onClick = { question = q },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(q, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start,
                            fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFFD4C2FF))
                    }
                }
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Investigation question", color = Color(0xFFB99BFF)) },
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        disabledTextColor = Color(0xFFE0E7EF),
                        cursorColor = Color(0xFFFFD166),
                        focusedBorderColor = Color(0xFFB99BFF),
                        unfocusedBorderColor = Color(0xFF9B7AE8),
                        focusedLabelColor = Color(0xFFB99BFF),
                        unfocusedLabelColor = Color(0xFFB8C7D1),
                        focusedContainerColor = Color(0xFF202D39),
                        unfocusedContainerColor = Color(0xFF202D39),
                        disabledContainerColor = Color(0xFF202D39)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val q = question.trim()
                        if (q.isNotBlank() && aiState.ready && !aiState.busy) {
                            pendingQuestion = q
                            lastSavedReply = aiState.reply
                            ai.send("Investigate this question carefully. First give a clear, readable answer. Separate established evidence, interpretation and speculation. Include the strongest counter-evidence, what remains unknown, and sources only when available. Do not invent sources or facts. Treat all saved board material as provisional context. At the very end, append a section beginning exactly with LEDGER_JSON: followed by a valid JSON array of 3 to 8 distinct evidence records extracted only from the answer. Each object must have these string fields: group (one of Scripture, Science, Philosophy, Human experience, Symbols & language patterns), claim (one concise standalone finding), source (specific source title, reference or URL if actually available, otherwise empty string), support (what in the answer supports this finding), challenge (counter-evidence or an honest statement that none was identified). Do not use markdown fences around the JSON. Do not create duplicate or trivial records.\n\nQUESTION: $q", contextNotes, aiState.onlineSearchEnabled)
                        }
                    }, enabled = question.isNotBlank() && aiState.ready && !aiState.busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = Color(0xFF344353),
                            disabledContentColor = Color(0xFFE0E7EF),
                            containerColor = Color(0xFF7653B5),
                            contentColor = Color.White
                        )) { Text(if (aiState.busy) "Investigating…" else "Investigate", maxLines = 1, softWrap = false) }
                    OutlinedButton(onClick = { showAddEvidence = true }, modifier = Modifier.weight(1f)) { Text("＋ Add evidence") }
                }
                if (pendingQuestion.isNotBlank()) Text("Investigation running. When Truth Detective finishes, the answer is added to the evidence ledger as provisional.", color = Color(0xFFFFD18A), fontSize = 12.sp)
                if (aiState.reply.isNotBlank()) {
                    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp)) {
                        Text("Truth Detective investigation", style = MaterialTheme.typography.titleSmall)
                        Text(aiState.reply, style = MaterialTheme.typography.bodyMedium)
                    } }
                }
                if (aiState.error != null) Text(aiState.error!!, color = Color(0xFFFFB4AB))
            }
                }
            "Endless" -> {
                EndlessInvestigation(context, ai, aiState)
            }
            "Evidence" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Evidence ledger · ${evidence.size}", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Row {
                        TextButton(onClick = { showAddEvidence = true }) { Text("＋ Add") }
                        TextButton(onClick = { showClearBoard = true }) { Text("Clear all") }
                    }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(evidence, key = { it.id }) { item ->
                        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.group.uppercase(), color = Color(0xFF7653B5), style = MaterialTheme.typography.labelSmall)
                            Text(item.claim, style = MaterialTheme.typography.titleSmall)
                            Text("Source: ${item.source.ifBlank { "Not recorded" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Supporting: ${item.support.ifBlank { "Not recorded" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Challenge: ${item.challenge.ifBlank { "Not recorded" }}", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { evidence.remove(item); links.removeAll { it.from == item.id || it.to == item.id }; board.save(evidence,links) }) {
                                Text("Delete evidence", color = Color(0xFFB85C65))
                            }
                        } }
                    }
                    if (evidence.isEmpty()) item { Text("No evidence saved yet. Add a record or investigate a question.", color = Color(0xFFB8C7D1)) }
                }
            }
            else -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Connection map · ${links.size}", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    TextButton(onClick = { fromId = evidence.firstOrNull()?.id ?: ""; toId = evidence.firstOrNull { it.id != fromId }?.id ?: ""; connectionReason = ""; showAddLink = true }) { Text("＋ Connect") }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(links, key = { it.id }) { link ->
                        val from = evidence.find { it.id == link.from }
                        val to = evidence.find { it.id == link.to }
                        if (from != null && to != null) Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(from.claim, style = MaterialTheme.typography.titleSmall)
                                Text("↕", color = Color(0xFFB99BFF))
                                Text(to.claim, style = MaterialTheme.typography.titleSmall)
                                Text(link.reason, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { links.remove(link); board.save(evidence,links) }) { Text("Remove connection") }
                            }
                        }
                    }
                    if (links.isEmpty()) item { Text("Connect two evidence records to start mapping possible relationships. Connections are hypotheses, not proof.", color = Color(0xFFB8C7D1)) }
                }
            }
        }
    }

    if (showClearBoard) AlertDialog(
        onDismissRequest = { showClearBoard = false },
        title = { Text("Clear investigation board?") },
        text = { Text("This will delete all evidence records and connections saved in Truth Detective. Your Truth Detective conversation and library files will not be deleted. Do you want to proceed?") },
        confirmButton = { TextButton(onClick = { evidence.clear(); links.clear(); board.clear(); showClearBoard = false }) { Text("Delete all") } },
        dismissButton = { TextButton(onClick = { showClearBoard = false }) { Text("Cancel") } }
    )
    if (showAddEvidence) AlertDialog(
        onDismissRequest = { showAddEvidence = false },
        title = { Text("Add evidence to ledger") },
        text = { Column(
            modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(value = claim, onValueChange = { claim = it }, label = { Text("Claim or observation") }, colors = truthFieldColors)
            witnessGroups.forEach { label ->
                Row { RadioButton(selected = group == label, onClick = { group = label }); Text(label, modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp) }
            }
            OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source") }, colors = truthFieldColors)
            OutlinedTextField(value = support, onValueChange = { support = it }, label = { Text("Supporting evidence") }, colors = truthFieldColors)
            OutlinedTextField(value = challenge, onValueChange = { challenge = it }, label = { Text("Counter-evidence / alternative") }, colors = truthFieldColors)
        } },
        confirmButton = { TextButton(onClick = {
            if (claim.isNotBlank()) {
                evidence.add(0, TruthEvidence(UUID.randomUUID().toString(),group,claim.trim(),source.trim(),support.trim(),challenge.trim()))
                board.save(evidence,links); claim=""; source=""; support=""; challenge=""; showAddEvidence=false
            }
        }) { Text("Save") } },
        dismissButton = { TextButton(onClick = { showAddEvidence=false }) { Text("Cancel") } }
    )
    if (showAddLink) AlertDialog(
        onDismissRequest = { showAddLink = false },
        title = { Text("Connect evidence") },
        text = { Column(
            modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Choose two different records. The connection is a research hypothesis.", style = MaterialTheme.typography.bodySmall)
            evidence.forEachIndexed { index, item ->
                Row {
                    RadioButton(selected = fromId == item.id, onClick = {
                        fromId = item.id
                        if (toId == item.id) toId = evidence.firstOrNull { it.id != item.id }?.id ?: ""
                    })
                    Text("A · ${index + 1}. ${item.claim.take(65)}", modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
                }
            }
            HorizontalDivider()
            evidence.forEachIndexed { index, item ->
                Row {
                    RadioButton(selected = toId == item.id, onClick = {
                        toId = item.id
                        if (fromId == item.id) fromId = evidence.firstOrNull { it.id != item.id }?.id ?: ""
                    })
                    Text("B · ${index + 1}. ${item.claim.take(65)}", modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
                }
            }
            if (fromId.isNotBlank() && fromId == toId) {
                Text("Choose a different record for A and B.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(value = connectionReason, onValueChange = { connectionReason = it }, label = { Text("Why might they connect?") }, colors = truthFieldColors)
        } },
        confirmButton = { TextButton(onClick = {
            if (fromId.isNotBlank() && toId.isNotBlank() && fromId != toId) {
                links.add(TruthLink(UUID.randomUUID().toString(),fromId,toId,connectionReason.trim()))
                board.save(evidence,links); connectionReason=""; showAddLink=false
            }
        }, enabled = evidence.size > 1 && fromId.isNotBlank() && toId.isNotBlank() &&
            fromId != toId && connectionReason.isNotBlank() &&
            evidence.any { it.id == fromId } && evidence.any { it.id == toId } &&
            links.none { setOf(it.from, it.to) == setOf(fromId, toId) }) { Text("Save connection") } },
        dismissButton = { TextButton(onClick = { showAddLink=false }) { Text("Cancel") } }
    )
}
