package com.jarvis.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val ACTIVE_MODEL_ID     = stringPreferencesKey("active_model_id")
        val WAKE_WORD_ENABLED   = booleanPreferencesKey("wake_word_enabled")
        val TTS_SPEED           = floatPreferencesKey("tts_speed")
        val TTS_PITCH           = floatPreferencesKey("tts_pitch")
        val AUTO_LISTEN         = booleanPreferencesKey("auto_listen")
        val SCREEN_ACCESS       = booleanPreferencesKey("screen_access")
        val SYSTEM_PROMPT       = stringPreferencesKey("system_prompt")
        val LLM_TEMPERATURE     = floatPreferencesKey("llm_temperature")
        val LLM_MAX_TOKENS      = intPreferencesKey("llm_max_tokens")
        val JARVIS_NAME         = stringPreferencesKey("jarvis_name")
        val VOICE_FEEDBACK      = booleanPreferencesKey("voice_feedback")
        val HAPTIC_FEEDBACK     = booleanPreferencesKey("haptic_feedback")
    }

    val activeModelId: Flow<String> = context.dataStore.data
        .map { it[Keys.ACTIVE_MODEL_ID] ?: "" }

    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.WAKE_WORD_ENABLED] ?: false }

    val ttsSpeed: Flow<Float> = context.dataStore.data
        .map { it[Keys.TTS_SPEED] ?: 1.0f }

    val ttsPitch: Flow<Float> = context.dataStore.data
        .map { it[Keys.TTS_PITCH] ?: 0.85f }

    val autoListen: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.AUTO_LISTEN] ?: false }

    val screenAccess: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.SCREEN_ACCESS] ?: false }

    val systemPrompt: Flow<String> = context.dataStore.data
        .map { it[Keys.SYSTEM_PROMPT] ?: DEFAULT_SYSTEM_PROMPT }

    val llmTemperature: Flow<Float> = context.dataStore.data
        .map { it[Keys.LLM_TEMPERATURE] ?: 0.7f }

    val llmMaxTokens: Flow<Int> = context.dataStore.data
        .map { it[Keys.LLM_MAX_TOKENS] ?: 512 }

    val jarvisName: Flow<String> = context.dataStore.data
        .map { it[Keys.JARVIS_NAME] ?: "JARVIS" }

    val voiceFeedback: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.VOICE_FEEDBACK] ?: true }

    val hapticFeedback: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.HAPTIC_FEEDBACK] ?: true }

    suspend fun setActiveModelId(id: String) =
        context.dataStore.edit { it[Keys.ACTIVE_MODEL_ID] = id }

    suspend fun setWakeWordEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }

    suspend fun setTtsSpeed(speed: Float) =
        context.dataStore.edit { it[Keys.TTS_SPEED] = speed }

    suspend fun setTtsPitch(pitch: Float) =
        context.dataStore.edit { it[Keys.TTS_PITCH] = pitch }

    suspend fun setAutoListen(enabled: Boolean) =
        context.dataStore.edit { it[Keys.AUTO_LISTEN] = enabled }

    suspend fun setScreenAccess(enabled: Boolean) =
        context.dataStore.edit { it[Keys.SCREEN_ACCESS] = enabled }

    suspend fun setSystemPrompt(prompt: String) =
        context.dataStore.edit { it[Keys.SYSTEM_PROMPT] = prompt }

    suspend fun setLlmTemperature(temp: Float) =
        context.dataStore.edit { it[Keys.LLM_TEMPERATURE] = temp }

    suspend fun setLlmMaxTokens(tokens: Int) =
        context.dataStore.edit { it[Keys.LLM_MAX_TOKENS] = tokens }

    suspend fun setJarvisName(name: String) =
        context.dataStore.edit { it[Keys.JARVIS_NAME] = name }

    suspend fun setVoiceFeedback(enabled: Boolean) =
        context.dataStore.edit { it[Keys.VOICE_FEEDBACK] = enabled }

    suspend fun setHapticFeedback(enabled: Boolean) =
        context.dataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT = """You are JARVIS, an advanced AI assistant running locally on this Android device.
You are helpful, intelligent, and concise. You can control the phone, read the screen, and assist with any task.
Keep responses brief and conversational when using voice. Use clear, direct language.
When asked to perform phone actions, respond with what you are doing, then the action tag.
For phone control, use: [ACTION:OPEN_APP:AppName], [ACTION:CLICK:description], [ACTION:SCROLL:direction], [ACTION:TYPE:text], [ACTION:BACK], [ACTION:HOME], [ACTION:RECENT_APPS]
Always be helpful and proactive. You are the user's personal AI companion."""
    }
}
