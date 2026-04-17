package com.jarvis.ai.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle        : SpeechState()
    object Listening   : SpeechState()
    object Processing  : SpeechState()
    data class Result(val text: String) : SpeechState()
    data class Error(val message: String) : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
}

class SpeechManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private val _amplitude = MutableStateFlow(0f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private var onResultCallback: ((String) -> Unit)? = null
    private var isListening = false

    private var wakeWords = listOf("hey jarvis", "jarvis", "ok jarvis", "hello jarvis")

    fun isAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(onResult: (String) -> Unit) {
        if (!isAvailable()) {
            _state.value = SpeechState.Error("Speech recognition not available")
            return
        }
        onResultCallback = onResult
        createRecognizer()
        startSession()
    }

    fun stopListening() {
        isListening = false
        recognizer?.stopListening()
        _state.value = SpeechState.Idle
    }

    fun destroy() {
        isListening = false
        recognizer?.destroy()
        recognizer = null
    }

    private fun createRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {

                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = SpeechState.Listening
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    _state.value = SpeechState.Listening
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize amplitude 0..1 from typical -2..10 dB range
                    val norm = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _amplitude.value = norm
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _state.value = SpeechState.Processing
                    _amplitude.value = 0f
                }

                override fun onError(error: Int) {
                    val msg = errorString(error)
                    Log.w(TAG, "Recognition error: $msg ($error)")
                    _state.value = SpeechState.Idle
                    _amplitude.value = 0f

                    // Auto-restart on recoverable errors
                    if (isListening && error in listOf(
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                        )) {
                        startSession()
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?: return
                    val text = matches.firstOrNull() ?: return

                    _state.value = SpeechState.Result(text)
                    onResultCallback?.invoke(text)
                    Log.d(TAG, "Result: $text")

                    // Keep listening if in continuous mode
                    if (isListening) {
                        startSession()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: return
                    _state.value = SpeechState.PartialResult(partial)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun startSession() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }
        isListening = true
        recognizer?.startListening(intent)
    }

    fun startContinuousListening(wakeWordMode: Boolean, onWakeWord: (() -> Unit)? = null, onCommand: (String) -> Unit) {
        if (!isAvailable()) return
        isListening = true
        createRecognizer()

        if (wakeWordMode) {
            // Listen for wake word first, then command
            startListening { text ->
                val lower = text.lowercase()
                val hasWakeWord = wakeWords.any { lower.contains(it) }
                if (hasWakeWord) {
                    onWakeWord?.invoke()
                    val command = wakeWords.fold(lower) { acc, w -> acc.replace(w, "") }.trim()
                    if (command.isNotBlank()) onCommand(command)
                }
            }
        } else {
            startListening(onCommand)
        }
    }

    fun updateWakeWords(words: List<String>) {
        wakeWords = words.map { it.lowercase() }
    }

    private fun errorString(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO             -> "Audio error"
        SpeechRecognizer.ERROR_CLIENT            -> "Client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No microphone permission"
        SpeechRecognizer.ERROR_NETWORK           -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT   -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH          -> "No speech detected"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY   -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER            -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT    -> "Speech timeout"
        else                                     -> "Error $error"
    }

    companion object {
        private const val TAG = "SpeechManager"
    }
}
