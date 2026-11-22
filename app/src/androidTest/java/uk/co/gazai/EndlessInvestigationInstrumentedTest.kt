package uk.co.gazai

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EndlessInvestigationInstrumentedTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("gaz_endless_investigation", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("gaz_truth_board", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun questionTreePersistsRootsBranchesAndAnswers() {
        val store = InvestigationTreeStore(context)
        val root = InvestigationQuestion("root", null, "What is consciousness?", "Initial answer")
        val branch = InvestigationQuestion("branch", "root", "What does neuroscience show?")
        store.save(listOf(root, branch))
        val loaded = InvestigationTreeStore(context).load()
        assertEquals(listOf(root, branch), loaded)
    }

    @Test fun branchParserRemovesExistingAndRepeatedQuestionsAndCapsAtFour() {
        val reply = """Reasoning...
NEXT_QUESTIONS_JSON:["Question one long enough?","Question two long enough?","Question one long enough?","Question three long enough?","Question four long enough?","Question five long enough?"]"""
        val parsed = parseNewFollowUpQuestions(reply, listOf("QUESTION TWO LONG ENOUGH?"))
        assertEquals(listOf(
            "Question one long enough?",
            "Question three long enough?",
            "Question four long enough?",
            "Question five long enough?"
        ), parsed)
    }

    @Test fun branchParserRejectsMissingMalformedAndTrivialQuestions() {
        assertTrue(parseNewFollowUpQuestions("No marker here", emptyList()).isEmpty())
        assertTrue(parseNewFollowUpQuestions("NEXT_QUESTIONS_JSON: not json", emptyList()).isEmpty())
        assertTrue(parseNewFollowUpQuestions("NEXT_QUESTIONS_JSON:[\"tiny\"]", emptyList()).isEmpty())
    }

    @Test fun endlessAnswerIsSavedOnceWithQuestionAndCaveat() {
        val question = "Does history repeat in cycles?"
        val answer = "There are recurring patterns, but the evidence does not prove exact cycles."
        assertTrue(saveEndlessAnswerToTruthBoard(context, question, answer))
        assertFalse(saveEndlessAnswerToTruthBoard(context, question, answer))
        val json = context.getSharedPreferences("gaz_truth_board", Context.MODE_PRIVATE).getString("evidence", "[]")!!
        val array = org.json.JSONArray(json)
        assertEquals(1, array.length())
        val record = array.getJSONObject(0)
        assertEquals("Endless investigation: $question", record.getString("claim"))
        assertEquals(answer, record.getString("support"))
        assertTrue(record.getString("challenge").contains("not independent proof"))
    }

    @Test fun distinctAnswersAreSavedAsSeparateEvidenceRecords() {
        assertTrue(saveEndlessAnswerToTruthBoard(context, "Question A", "Answer A"))
        assertTrue(saveEndlessAnswerToTruthBoard(context, "Question B", "Answer B"))
        val array = org.json.JSONArray(context.getSharedPreferences("gaz_truth_board", Context.MODE_PRIVATE).getString("evidence", "[]")!!)
        assertEquals(2, array.length())
    }
}
