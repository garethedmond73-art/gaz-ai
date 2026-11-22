package uk.co.gazai

import org.json.JSONArray

/**
 * Parses the machine-readable tail returned by the Endless Investigation branch prompt.
 * Only distinct, non-trivial questions are retained; at most four are added per response.
 */
internal fun parseNewFollowUpQuestions(reply: String, existingQuestions: Collection<String>): List<String> {
    val raw = reply.substringAfter("NEXT_QUESTIONS_JSON:", "")
    val first = raw.indexOf('[')
    val last = raw.lastIndexOf(']')
    if (first < 0 || last <= first) return emptyList()

    val existing = existingQuestions.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    return runCatching {
        val array = JSONArray(raw.substring(first, last + 1))
        val seen = existing.toMutableSet()
        buildList {
            for (index in 0 until array.length()) {
                val question = array.optString(index).trim()
                val key = question.lowercase()
                if (question.length > 8 && key !in seen) {
                    add(question)
                    seen.add(key)
                    if (size == 4) break
                }
            }
        }
    }.getOrDefault(emptyList())
}
