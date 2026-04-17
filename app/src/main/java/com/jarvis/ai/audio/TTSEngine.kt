package com.jarvis.ai.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class TTSEngine(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentSpeed   = 0.9f
    private var currentPitch   = 0.8f   // Slightly lower = more JARVIS-like

    private var onDoneCallback: (() -> Unit)? = null

    init {
        initTTS()
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "English TTS not supported, falling back")
                    tts?.setLanguage(Locale.getDefault())
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }
                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        if (utteranceId == UTTERANCE_CALLBACK) {
                            onDoneCallback?.invoke()
                            onDoneCallback = null
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        _isSpeaking.value = false
                    }
                })

                applyVoiceSettings()
                isReady = true
                Log.i(TAG, "TTS initialized")

            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    private fun applyVoiceSettings() {
        tts?.setSpeechRate(currentSpeed)
        tts?.setPitch(currentPitch)
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isReady || tts == null) {
            Log.w(TAG, "TTS not ready")
            onDone?.invoke()
            return
        }

        // Clean up markdown/action tags before speaking
        val cleaned = cleanForSpeech(text)
        if (cleaned.isBlank()) {
            onDone?.invoke()
            return
        }

        onDoneCallback = onDone

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UTTERANCE_CALLBACK)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_CALLBACK)
    }

    fun speakQueued(text: String) {
        if (!isReady) return
        val cleaned = cleanForSpeech(text)
        if (cleaned.isBlank()) return
        tts?.speak(cleaned, TextToSpeech.QUEUE_ADD, null, "queued_${System.currentTimeMillis()}")
    }

    suspend fun speakAndWait(text: String) = suspendCancellableCoroutine<Unit> { cont ->
        speak(text) { cont.resume(Unit) }
        cont.invokeOnCancellation { stop() }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setSpeed(speed: Float) {
        currentSpeed = speed.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(currentSpeed)
    }

    fun setPitch(pitch: Float) {
        currentPitch = pitch.coerceIn(0.5f, 1.5f)
        tts?.setPitch(currentPitch)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    fun isAvailable() = isReady && tts != null

    private fun cleanForSpeech(text: String): String {
        return text
            .replace(Regex("\\[ACTION:[^]]+]"), "") // Remove action tags
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1") // Bold markdown
            .replace(Regex("\\*(.+?)\\*"), "$1")       // Italic markdown
            .replace(Regex("`(.+?)`"), "$1")            // Code backticks
            .replace(Regex("#{1,6}\\s"), "")            // Headers
            .replace(Regex("\\n{2,}"), ". ")            // Double newlines → pause
            .replace("\n", " ")
            .trim()
    }

    companion object {
        private const val TAG              = "TTSEngine"
        private const val UTTERANCE_CALLBACK = "jarvis_callback"
    }
}
