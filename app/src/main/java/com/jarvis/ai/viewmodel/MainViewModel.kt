package com.jarvis.ai.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.JarvisApplication
import com.jarvis.ai.audio.SpeechManager
import com.jarvis.ai.audio.SpeechState
import com.jarvis.ai.audio.TTSEngine
import com.jarvis.ai.data.*
import com.jarvis.ai.llm.LLMEngine
import com.jarvis.ai.llm.InferenceResult
import com.jarvis.ai.llm.ModelDownloader
import com.jarvis.ai.services.JarvisAccessibilityService
import com.jarvis.ai.services.JarvisService
import com.jarvis.ai.vision.ScreenCaptureManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val prefs = AppPreferences(application)
    private val dao = app.database.modelDao()
    private val downloader = ModelDownloader(application)

    val llmEngine   = app.llmEngine
    val ttsEngine   = app.ttsEngine
    val speechMgr   = SpeechManager(application)
    val screenCapture = ScreenCaptureManager(application)

    // ── UI State ─────────────────────────────────────────────────────────────
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _isSpeakingTTS = MutableStateFlow(false)
    val isSpeakingTTS: StateFlow<Boolean> = _isSpeakingTTS.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse: StateFlow<String> = _streamingResponse.asStateFlow()

    private val _activeModelInfo = MutableStateFlow<ModelInfo?>(null)
    val activeModelInfo: StateFlow<ModelInfo?> = _activeModelInfo.asStateFlow()

    private val _modelLoadState = MutableStateFlow<ModelLoadState>(ModelLoadState.Idle)
    val modelLoadState: StateFlow<ModelLoadState> = _modelLoadState.asStateFlow()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    // Catalog merges HuggingFace list with local DB status
    private val _catalog = MutableStateFlow<List<ModelInfo>>(AvailableModels.catalog)
    val catalog: StateFlow<List<ModelInfo>> = _catalog.asStateFlow()

    val micAmplitude    = speechMgr.amplitude
    val speechState     = speechMgr.state
    val accessibilityEnabled = JarvisAccessibilityService.isConnected

    // Settings exposed for UI
    val ttsSpeed        = prefs.ttsSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.9f)
    val ttsPitch        = prefs.ttsPitch.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.8f)
    val wakeWordEnabled = prefs.wakeWordEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val autoListen      = prefs.autoListen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    val systemPrompt    = prefs.systemPrompt.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), AppPreferences.DEFAULT_SYSTEM_PROMPT)
    val llmTemperature  = prefs.llmTemperature.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.7f)
    val llmMaxTokens    = prefs.llmMaxTokens.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 512)
    val jarvisName      = prefs.jarvisName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "JARVIS")

    init {
        observeSpeech()
        observeTTS()
        syncCatalogWithDb()
        restoreActiveModel()
    }

    // ── Catalog & DB Sync ────────────────────────────────────────────────────
    private fun syncCatalogWithDb() {
        viewModelScope.launch {
            dao.getAllModels().collect { dbModels ->
                val dbMap = dbModels.associateBy { it.id }
                _catalog.value = AvailableModels.catalog.map { base ->
                    val saved = dbMap[base.id]
                    if (saved != null) {
                        // Verify file still exists
                        val fileExists = downloader.isModelDownloaded(saved.id)
                        if (!fileExists && saved.status == ModelStatus.DOWNLOADED) {
                            dao.updateStatus(saved.id, ModelStatus.NOT_DOWNLOADED)
                            saved.copy(status = ModelStatus.NOT_DOWNLOADED, localPath = "")
                        } else saved
                    } else {
                        // Seed DB with catalog defaults
                        dao.upsertModel(base)
                        base
                    }
                }
            }
        }
    }

    private fun restoreActiveModel() {
        viewModelScope.launch {
            val activeId = prefs.activeModelId.first()
            if (activeId.isNotBlank()) {
                val model = dao.getModel(activeId)
                if (model != null && downloader.isModelDownloaded(model.id)) {
                    _activeModelInfo.value = model
                    loadModel(model)
                }
            }
        }
    }

    // ── Model Management ─────────────────────────────────────────────────────
    fun downloadModel(model: ModelInfo) {
        viewModelScope.launch {
            dao.updateStatus(model.id, ModelStatus.DOWNLOADING)
            downloader.downloadModel(model.id, model.downloadUrl)
                .collect { state ->
                    val current = _downloadStates.value.toMutableMap()
                    current[model.id] = state
                    _downloadStates.value = current

                    dao.updateProgress(model.id, state.progress,
                        if (state.isComplete) ModelStatus.DOWNLOADED
                        else if (state.error != null) ModelStatus.ERROR
                        else ModelStatus.DOWNLOADING
                    )

                    if (state.isComplete) {
                        val path = downloader.getModelFile(model.id).absolutePath
                        dao.setLocalPath(model.id, path, ModelStatus.DOWNLOADED)
                        current.remove(model.id)
                        _downloadStates.value = current
                        addSystemMessage("Model '${model.name}' downloaded successfully. Tap to activate.")
                    } else if (state.error != null) {
                        addSystemMessage("Download failed: ${state.error}")
                    }
                }
        }
    }

    fun activateModel(model: ModelInfo) {
        viewModelScope.launch {
            if (!downloader.isModelDownloaded(model.id)) {
                addSystemMessage("Model not downloaded yet.")
                return@launch
            }
            dao.clearActiveModel()
            dao.setActiveModel(model.id)
            prefs.setActiveModelId(model.id)
            _activeModelInfo.value = model
            loadModel(model)
        }
    }

    private fun loadModel(model: ModelInfo) {
        viewModelScope.launch {
            _modelLoadState.value = ModelLoadState.Loading(model.name)
            val path = downloader.getModelFile(model.id).absolutePath
            val success = llmEngine.loadModel(
                modelPath   = path,
                modelId     = model.id,
                maxTokens   = llmMaxTokens.value,
                temperature = llmTemperature.value
            )
            _modelLoadState.value = if (success) {
                addSystemMessage("${model.name} loaded. I'm ready, sir.")
                speakResponse("${model.name} online. Ready.")
                ModelLoadState.Ready(model.name)
            } else {
                ModelLoadState.Error("Failed to load ${model.name}")
            }
        }
    }

    fun deleteModel(model: ModelInfo) {
        viewModelScope.launch {
            if (_activeModelInfo.value?.id == model.id) {
                llmEngine.unload()
                _activeModelInfo.value = null
                _modelLoadState.value = ModelLoadState.Idle
                prefs.setActiveModelId("")
            }
            downloader.deleteModel(model.id)
            dao.updateStatus(model.id, ModelStatus.NOT_DOWNLOADED)
            dao.setLocalPath(model.id, "", ModelStatus.NOT_DOWNLOADED)
        }
    }

    // ── Speech ───────────────────────────────────────────────────────────────
    private fun observeSpeech() {
        viewModelScope.launch {
            speechMgr.state.collect { state ->
                when (state) {
                    is SpeechState.Listening       -> _isListening.value = true
                    is SpeechState.Processing      -> _isListening.value = true
                    is SpeechState.PartialResult   -> _partialTranscript.value = state.text
                    is SpeechState.Result          -> {
                        _partialTranscript.value = ""
                        _isListening.value = false
                    }
                    is SpeechState.Idle,
                    is SpeechState.Error           -> {
                        _isListening.value = false
                        _partialTranscript.value = ""
                    }
                }
            }
        }
    }

    private fun observeTTS() {
        viewModelScope.launch {
            ttsEngine.isSpeaking.collect { _isSpeakingTTS.value = it }
        }
    }

    fun startListening() {
        if (!llmEngine.isReady()) {
            speakResponse("Please select and activate a model first.")
            return
        }
        ttsEngine.stop()
        speechMgr.startListening { text ->
            viewModelScope.launch { sendMessage(text, isVoice = true) }
        }
    }

    fun stopListening() {
        speechMgr.stopListening()
        _isListening.value = false
    }

    fun toggleContinuousListen(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoListen(enabled) }
        if (enabled) {
            speechMgr.startContinuousListening(
                wakeWordMode = wakeWordEnabled.value,
                onWakeWord   = { speakResponse("Yes, sir?") }
            ) { text ->
                viewModelScope.launch { sendMessage(text, isVoice = true) }
            }
        } else {
            speechMgr.stopListening()
        }
    }

    // ── LLM Inference ────────────────────────────────────────────────────────
    fun sendMessage(text: String, isVoice: Boolean = false) {
        viewModelScope.launch {
            if (!llmEngine.isReady()) {
                addSystemMessage("No model loaded. Please download and activate a model first.")
                return@launch
            }

            // Add user message
            addMessage(ChatMessage(content = text, isUser = true, isVoice = isVoice))
            _isThinking.value = true
            _streamingResponse.value = ""

            // Add placeholder AI message
            val thinkingMsg = ChatMessage(content = "", isUser = false, isThinking = true)
            addMessage(thinkingMsg)

            val prompt = buildContextualPrompt(text)
            val fullResponse = StringBuilder()

            llmEngine.generateStream(prompt, systemPrompt.value)
                .collect { result ->
                    when (result) {
                        is InferenceResult.Token -> {
                            fullResponse.append(result.text)
                            _streamingResponse.value = fullResponse.toString()
                            // Update the thinking message with live text
                            updateLastAiMessage(fullResponse.toString(), thinking = !result.done)
                        }
                        is InferenceResult.Complete -> {
                            _isThinking.value = false
                            _streamingResponse.value = ""
                            val response = result.fullText
                            updateLastAiMessage(response, thinking = false)

                            // Speak response if voice was used or TTS enabled
                            if (isVoice) speakResponse(response)

                            // Execute phone actions if any
                            executeActions(response)
                        }
                        is InferenceResult.Error -> {
                            _isThinking.value = false
                            _streamingResponse.value = ""
                            updateLastAiMessage("Error: ${result.message}", thinking = false)
                        }
                    }
                }
        }
    }

    private fun buildContextualPrompt(userText: String): String {
        val screenText = JarvisAccessibilityService.instance?.getScreenText() ?: ""
        val currentApp = JarvisAccessibilityService.currentApp.value

        return buildString {
            if (currentApp.isNotBlank()) {
                append("[Context: User is currently in app: $currentApp]\n")
            }
            if (screenText.isNotBlank()) {
                append("[Screen content summary: ${screenText.take(500)}]\n")
            }
            append(userText)
        }
    }

    private suspend fun executeActions(response: String) {
        val actions = llmEngine.parseActions(response)
        val accessibility = JarvisAccessibilityService.instance ?: return

        for (action in actions) {
            delay(300) // Small delay between actions
            when (action.type) {
                "OPEN_APP"      -> accessibility.openApp(action.param ?: "")
                "CLICK"         -> accessibility.clickByText(action.param ?: "")
                "SCROLL"        -> accessibility.scroll(action.param ?: "DOWN")
                "TYPE"          -> accessibility.typeText(action.param ?: "")
                "BACK"          -> accessibility.pressBack()
                "HOME"          -> accessibility.pressHome()
                "RECENT_APPS"   -> accessibility.openRecentApps()
                "SCREENSHOT"    -> accessibility.takeAccessibilityScreenshot()
                "NOTIFICATIONS" -> accessibility.openNotifications()
                "QUICK_SETTINGS"-> accessibility.openQuickSettings()
            }
            Log.d(TAG, "Executed action: ${action.type} ${action.param}")
        }
    }

    private fun speakResponse(text: String) {
        viewModelScope.launch {
            ttsEngine.setSpeed(ttsSpeed.value)
            ttsEngine.setPitch(ttsPitch.value)
            ttsEngine.speak(text)
        }
    }

    fun stopSpeaking() = ttsEngine.stop()

    // ── Message Helpers ──────────────────────────────────────────────────────
    private fun addMessage(msg: ChatMessage) {
        _messages.value = _messages.value + msg
    }

    private fun addSystemMessage(text: String) {
        addMessage(ChatMessage(
            content = text,
            isUser  = false,
            isVoice = false
        ))
    }

    private fun updateLastAiMessage(text: String, thinking: Boolean) {
        val msgs = _messages.value.toMutableList()
        val lastIdx = msgs.indexOfLast { !it.isUser }
        if (lastIdx >= 0) {
            msgs[lastIdx] = msgs[lastIdx].copy(content = text, isThinking = thinking)
            _messages.value = msgs
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
    }

    // ── Settings ─────────────────────────────────────────────────────────────
    fun setTtsSpeed(v: Float)          { viewModelScope.launch { prefs.setTtsSpeed(v); ttsEngine.setSpeed(v) } }
    fun setTtsPitch(v: Float)          { viewModelScope.launch { prefs.setTtsPitch(v); ttsEngine.setPitch(v) } }
    fun setWakeWordEnabled(v: Boolean) { viewModelScope.launch { prefs.setWakeWordEnabled(v) } }
    fun setSystemPrompt(v: String)     { viewModelScope.launch { prefs.setSystemPrompt(v) } }
    fun setLlmTemperature(v: Float)    { viewModelScope.launch { prefs.setLlmTemperature(v) } }
    fun setLlmMaxTokens(v: Int)        { viewModelScope.launch { prefs.setLlmMaxTokens(v) } }
    fun setJarvisName(v: String)       { viewModelScope.launch { prefs.setJarvisName(v) } }

    // ── Service ──────────────────────────────────────────────────────────────
    fun startBackgroundService(context: Context) = JarvisService.start(context)
    fun stopBackgroundService(context: Context)  = JarvisService.stop(context)

    override fun onCleared() {
        super.onCleared()
        speechMgr.destroy()
        ttsEngine.destroy()
        screenCapture.stopCapture()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

sealed class ModelLoadState {
    object Idle : ModelLoadState()
    data class Loading(val name: String) : ModelLoadState()
    data class Ready(val name: String)   : ModelLoadState()
    data class Error(val message: String): ModelLoadState()
}
