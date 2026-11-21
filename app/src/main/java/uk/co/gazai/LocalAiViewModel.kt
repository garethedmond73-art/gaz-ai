package uk.co.gazai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.cancellation.CancellationException

data class LocalAiState(
    val modelDownloaded: Boolean = false,
    val downloading: Boolean = false,
    val downloadProgress: Int = 0,
    val loading: Boolean = false,
    val ready: Boolean = false,
    val busy: Boolean = false,
    val reply: String = "",
    val error: String? = null
)

class LocalAiViewModel(app: Application) : AndroidViewModel(app) {
    companion object {
        // Keep the downloaded model and runtime on a known-compatible release pair.
        // This is the September 2026 Qwen export supported by LiteRT-LM 0.17.1.
        const val MODEL_NAME = "GazAI-Qwen3-0.6B-compatible.litertlm"
        const val MODEL_FILE = "Qwen3-0.6B_dynamic_wi4b32_afp32.litertlm"
        const val MODEL_REVISION = "e6bb3e4a1eb3f663153f0c1c12b735d64be657a7"
        const val MODEL_URL =
            "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/$MODEL_REVISION/$MODEL_FILE"
        const val MODEL_MIN_BYTES = 300L * 1024L * 1024L
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + executor.asCoroutineDispatcher())
    private val modelFile = File(app.filesDir, MODEL_NAME)
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private val _state = MutableStateFlow(LocalAiState(modelDownloaded = modelFile.exists()))
    val state: StateFlow<LocalAiState> = _state.asStateFlow()

    fun downloadModel() {
        if (_state.value.downloading || _state.value.modelDownloaded) return
        scope.launch {
            _state.value = _state.value.copy(downloading = true, downloadProgress = 0, error = null)
            val partial = File(modelFile.parentFile, "$MODEL_NAME.part")
            try {
                withContext(Dispatchers.IO) {
                    val connection = (URL(MODEL_URL).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 20000
                        readTimeout = 60000
                        instanceFollowRedirects = true
                        requestMethod = "GET"
                    }
                    try {
                        connection.connect()
                        if (connection.responseCode !in 200..299) {
                            error("Model download failed: HTTP ${connection.responseCode}")
                        }
                        val expected = connection.contentLengthLong
                        connection.inputStream.use { input ->
                            partial.outputStream().buffered().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var total = 0L
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                    total += count
                                    if (expected > 0) {
                                        val percent = ((total * 100) / expected).toInt().coerceIn(0, 99)
                                        _state.value = _state.value.copy(downloadProgress = percent)
                                    }
                                }
                                output.flush()
                                if (expected > 0 && total != expected) {
                                    error("Download incomplete. Please try again.")
                                }
                                if (total < MODEL_MIN_BYTES) {
                                    error("The downloaded model file is too small to be valid.")
                                }
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
                if (!partial.renameTo(modelFile)) {
                    partial.copyTo(modelFile, overwrite = true)
                    partial.delete()
                }
                _state.value = _state.value.copy(
                    modelDownloaded = true, downloading = false, downloadProgress = 100
                )
                loadModel()
            } catch (e: Exception) {
                partial.delete()
                _state.value = _state.value.copy(
                    downloading = false, error = e.message ?: "Model download failed"
                )
            }
        }
    }

    fun loadModel() {
        if (_state.value.loading || _state.value.ready) return
        if (!modelFile.exists()) {
            _state.value = _state.value.copy(error = "Download the offline model first.")
            return
        }
        scope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val config = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = getApplication<Application>().cacheDir.absolutePath
                )
                val newEngine = Engine(config)
                newEngine.initialize()
                engine = newEngine
                conversation = newEngine.createConversation(
                    ConversationConfig(
                        systemInstruction = Contents.of(
                            "You are Gaz AI, a helpful, honest and curious assistant. " +
                                "Answer the user's exact question, not a different question. " +
                                "Start with a direct answer and use plain, natural language. " +
                                "Do not invent a template, essay structure, or unrelated advice. " +
                                "If the question is ambiguous, ask one short clarifying question. " +
                                "If you are unsure, say so. You are running offline and must not " +
                                "claim to have searched the internet. Use supplied local library notes when relevant. " +
                                "Distinguish source facts from interpretation and uncertainty. " +
                                "Mention the supplied source title when using a library note."
                        )
                    )
                )
                _state.value = _state.value.copy(loading = false, ready = true, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false, ready = false,
                    error = "Could not load the offline model: ${e.message ?: "unknown error"}"
                )
            }
        }
    }

    fun send(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || !_state.value.ready || _state.value.busy) return
        scope.launch {
            _state.value = _state.value.copy(busy = true, reply = "", error = null)
            try {
                withTimeout(120_000) {
                    val libraryNotes = StarterLibrary.promptContext(prompt)
                    val groundedPrompt = if (libraryNotes.isBlank()) prompt else
                        "Question: $prompt\\n\\nRelevant offline library notes (use only where relevant; do not treat them as exhaustive):\\n$libraryNotes"
                    checkNotNull(conversation).sendMessageAsync(groundedPrompt).collect { message ->
                        _state.value = _state.value.copy(reply = _state.value.reply + message.toString())
                    }
                }
                if (_state.value.reply.isBlank()) {
                    error("The offline model finished without producing a reply. Please try again.")
                }
            } catch (e: TimeoutCancellationException) {
                _state.value = _state.value.copy(error = "Gaz AI took too long to answer. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message ?: "The model could not answer.")
            } finally {
                _state.value = _state.value.copy(busy = false)
            }
        }
    }

    override fun onCleared() {
        scope.launch {
            try {
                conversation?.close()
                conversation = null
                engine?.close()
                engine = null
            } catch (_: Exception) {
                // Best-effort cleanup when Android destroys the ViewModel.
            } finally {
                executor.shutdown()
            }
        }.invokeOnCompletion { scope.cancel() }
    }
}
