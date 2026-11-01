package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ChatMessage
import com.example.data.ChatMessageDao
import com.example.data.JarvisDatabase
import com.example.data.JarvisPreferences
import com.example.service.BatteryInfo
import com.example.service.BrainResponse
import com.example.service.JarvisBrain
import com.example.service.JarvisDeviceController
import com.example.service.JarvisVoiceManager
import com.example.service.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JarvisUiState(
    val voiceState: VoiceState = VoiceState.IDLE,
    val inputText: String = "",
    val audioRms: Float = 0f,
    val batteryInfo: BatteryInfo? = null,
    val isFlashlightOn: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isHistoryOpen: Boolean = false,
    val isBackendGuideOpen: Boolean = false,
    val statusMessage: String = "SYSTEMS ONLINE",
    val errorMessage: String? = null
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    val database = JarvisDatabase.getDatabase(application)
    private val dao: ChatMessageDao = database.chatMessageDao()
    val preferences = JarvisPreferences(application)
    val deviceController = JarvisDeviceController(application)

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val brain = JarvisBrain(application, preferences, deviceController)

    val voiceManager = JarvisVoiceManager(
        context = application,
        onTranscriptReady = { transcript ->
            handleVoiceTranscript(transcript)
        },
        onStateChange = { newState ->
            _uiState.value = _uiState.value.copy(
                voiceState = newState,
                statusMessage = when (newState) {
                    VoiceState.IDLE -> "SYSTEMS READY"
                    VoiceState.LISTENING -> "LISTENING TO COMMAND…"
                    VoiceState.PROCESSING -> "PROCESSING NEURAL QUERY…"
                    VoiceState.SPEAKING -> "TRANSMITTING VOCAL DATA…"
                    VoiceState.ERROR -> "SYSTEM RECOVERY"
                }
            )
        },
        onRmsChange = { rms ->
            _uiState.value = _uiState.value.copy(audioRms = rms)
        }
    )

    init {
        refreshBattery()
        // Sync voice manager with preferences
        voiceManager.updateSettings(
            rate = preferences.speechRate.value,
            pitch = preferences.speechPitch.value,
            language = preferences.languageMode.value
        )
        // Insert initial greeting message if database is empty
        viewModelScope.launch {
            if (dao.getMessageCount() == 0) {
                val name = preferences.userName.value.ifBlank { "Jyoterditya Singh" }
                val initialGreeting = "Good day, Mr. $name. J.A.R.V.I.S. neural interface is fully operational. Coding environments and martial arts protocols are primed. How may I assist you today, sir?"
                dao.insertMessage(
                    ChatMessage(
                        text = initialGreeting,
                        sender = "JARVIS",
                        actionType = "ONLINE"
                    )
                )
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendTextMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isNotBlank()) {
            _uiState.value = _uiState.value.copy(inputText = "")
            processUserQuery(text)
        }
    }

    private fun handleVoiceTranscript(transcript: String) {
        if (transcript.isNotBlank()) {
            processUserQuery(transcript)
        }
    }

    fun processUserQuery(query: String) {
        viewModelScope.launch {
            // 1. Insert user message into Room
            dao.insertMessage(
                ChatMessage(
                    text = query,
                    sender = "USER"
                )
            )

            // 2. Set processing state
            _uiState.value = _uiState.value.copy(
                voiceState = VoiceState.PROCESSING,
                statusMessage = "PROCESSING NEURAL QUERY…"
            )

            // 3. Process with Brain
            val history = chatMessages.value
            when (val response = brain.processQuery(query, history)) {
                is BrainResponse.Success -> {
                    // Insert JARVIS message into Room
                    dao.insertMessage(
                        ChatMessage(
                            text = response.text,
                            sender = "JARVIS",
                            actionType = response.actionType,
                            actionDetails = response.actionDetails
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        isFlashlightOn = deviceController.isFlashlightOn(),
                        statusMessage = "TRANSMISSION COMPLETE"
                    )
                    // Speak if autoSpeak is enabled
                    if (preferences.autoSpeak.value) {
                        voiceManager.speak(response.text)
                    } else {
                        _uiState.value = _uiState.value.copy(voiceState = VoiceState.IDLE)
                    }
                }
                is BrainResponse.Error -> {
                    dao.insertMessage(
                        ChatMessage(
                            text = response.message,
                            sender = "JARVIS",
                            actionType = "ERROR"
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        voiceState = VoiceState.ERROR,
                        errorMessage = response.message,
                        statusMessage = "NEURAL ERROR"
                    )
                    if (preferences.autoSpeak.value) {
                        voiceManager.speak(response.message)
                    }
                }
            }
            refreshBattery()
        }
    }

    fun startListening() {
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun stopSpeaking() {
        voiceManager.stopSpeaking()
    }

    fun speakMessage(text: String) {
        voiceManager.speak(text)
    }

    fun toggleFlashlight() {
        val result = deviceController.toggleFlashlight()
        if (result.isSuccess) {
            val isOn = deviceController.isFlashlightOn()
            _uiState.value = _uiState.value.copy(isFlashlightOn = isOn)
            val title = preferences.userTitle.value.ifBlank { "sir" }
            val reply = if (isOn) "Illuminating workspace. Flashlight engaged, $title." else "Flashlight turned off, $title."
            viewModelScope.launch {
                dao.insertMessage(ChatMessage(text = reply, sender = "JARVIS", actionType = "FLASHLIGHT"))
            }
            if (preferences.autoSpeak.value) {
                voiceManager.speak(reply)
            }
        }
    }

    fun refreshBattery() {
        val battery = deviceController.getBatteryInfo()
        _uiState.value = _uiState.value.copy(
            batteryInfo = battery,
            isFlashlightOn = deviceController.isFlashlightOn()
        )
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearAll()
            val title = preferences.userTitle.value.ifBlank { "sir" }
            dao.insertMessage(
                ChatMessage(
                    text = "Memory logs purged. Neural cores initialized for fresh session, $title.",
                    sender = "JARVIS"
                )
            )
        }
    }

    fun exportChatHistory(context: Context) {
        val messages = chatMessages.value
        val sb = StringBuilder("=== J.A.R.V.I.S. MISSION LOG ===\n\n")
        for (m in messages) {
            val time = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(m.timestamp))
            sb.append("[$time] ${m.sender}: ${m.text}\n\n")
        }
        deviceController.shareText(sb.toString())
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsOpen = isOpen)
    }

    fun setHistoryOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isHistoryOpen = isOpen)
    }

    fun setBackendGuideOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isBackendGuideOpen = isOpen)
    }

    fun updateVoiceSettings(rate: Float, pitch: Float, language: String) {
        preferences.setSpeechRate(rate)
        preferences.setSpeechPitch(pitch)
        preferences.setLanguageMode(language)
        voiceManager.updateSettings(rate, pitch, language)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
