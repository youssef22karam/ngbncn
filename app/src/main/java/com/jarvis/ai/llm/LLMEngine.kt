package com.jarvis.ai.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class LLMState {
    object Idle        : LLMState()
    object Loading     : LLMState()
    object Ready       : LLMState()
    data class Error(val message: String) : LLMState()
}

sealed class InferenceResult {
    data class Token(val text: String, val done: Boolean) : InferenceResult()
    data class Complete(val fullText: String)              : InferenceResult()
    data class Error(val message: String)                  : InferenceResult()
}

class LLMEngine(private val context: Context) {

    private var llmInference: LlmInference? = null
    private var currentModelId: String? = null
    var state: LLMState = LLMState.Idle
        private set

    suspend fun loadModel(
        modelPath: String,
        modelId: String,
        maxTokens: Int = 1024,
        temperature: Float = 0.7f
    ): Boolean = withContext(Dispatchers.IO) {
        if (currentModelId == modelId && state == LLMState.Ready) {
            return@withContext true
        }

        try {
            state = LLMState.Loading
            llmInference?.close()
            llmInference = null

            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(maxTokens)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            currentModelId = modelId
            state = LLMState.Ready
            Log.i(TAG, "Model loaded: $modelId")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            state = LLMState.Error(e.message ?: "Unknown error")
            llmInference = null
            false
        }
    }

    /**
     * Stream tokens as they are generated. Emits InferenceResult.Token for each
     * partial response and InferenceResult.Complete when done.
     */
    fun generateStream(
        prompt: String,
        systemPrompt: String = ""
    ): Flow<InferenceResult> = callbackFlow {
        val engine = llmInference
        if (engine == null) {
            trySend(InferenceResult.Error("No model loaded"))
            close()
            return@callbackFlow
        }

        val fullPrompt = buildPrompt(systemPrompt, prompt)
        val fullResponse = StringBuilder()

        try {
            engine.generateResponseAsync(fullPrompt) { partial, done ->
                fullResponse.append(partial)
                trySend(InferenceResult.Token(partial, done))
                if (done) {
                    trySend(InferenceResult.Complete(fullResponse.toString()))
                    close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference error", e)
            trySend(InferenceResult.Error(e.message ?: "Inference failed"))
            close()
        }

        awaitClose { /* nothing to cancel — MediaPipe handles lifecycle */ }
    }.flowOn(Dispatchers.Default)

    /**
     * Blocking (suspend) version — returns full response at once.
     */
    suspend fun generate(
        prompt: String,
        systemPrompt: String = ""
    ): String = withContext(Dispatchers.Default) {
        val engine = llmInference
            ?: return@withContext "ERROR: No model loaded."

        try {
            val fullPrompt = buildPrompt(systemPrompt, prompt)
            engine.generateResponse(fullPrompt)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            "I encountered an error: ${e.message}"
        }
    }

    /**
     * Build prompt in a chat-friendly format compatible with Gemma / Phi-2 instruction style.
     */
    private fun buildPrompt(systemPrompt: String, userMessage: String): String {
        return if (systemPrompt.isNotBlank()) {
            "<start_of_turn>system\n$systemPrompt<end_of_turn>\n<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
        } else {
            "<start_of_turn>user\n$userMessage<end_of_turn>\n<start_of_turn>model\n"
        }
    }

    fun isReady() = state == LLMState.Ready && llmInference != null

    fun unload() {
        llmInference?.close()
        llmInference = null
        currentModelId = null
        state = LLMState.Idle
    }

    fun parseActions(response: String): List<PhoneAction> {
        val actions = mutableListOf<PhoneAction>()
        val actionRegex = Regex("\\[ACTION:(\\w+)(?::(.+?))?]")
        actionRegex.findAll(response).forEach { match ->
            val type = match.groupValues[1]
            val param = match.groupValues[2].takeIf { it.isNotBlank() }
            actions.add(PhoneAction(type, param))
        }
        return actions
    }

    companion object {
        private const val TAG = "LLMEngine"
    }
}

data class PhoneAction(
    val type: String,    // OPEN_APP, CLICK, SCROLL, TYPE, BACK, HOME, RECENT_APPS
    val param: String?   // Optional parameter (app name, text to type, etc.)
)
