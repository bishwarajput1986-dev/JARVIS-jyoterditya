package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class JarvisVoiceManager(
    private val context: Context,
    private val onTranscriptReady: (String) -> Unit,
    private val onStateChange: (VoiceState) -> Unit,
    private val onRmsChange: (Float) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val TAG = "JarvisVoiceManager"
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private var currentSpeechRate = 1.0f
    private var currentSpeechPitch = 0.95f
    private var currentLanguage = "en"

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            applyTtsSettings()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    updateState(VoiceState.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    updateState(VoiceState.IDLE)
                }

                override fun onError(utteranceId: String?) {
                    updateState(VoiceState.IDLE)
                }
            })
        } else {
            Log.e(TAG, "TTS initialization failed with status $status")
        }
    }

    private fun applyTtsSettings() {
        val engine = tts ?: return
        if (!isTtsInitialized) return

        engine.setSpeechRate(currentSpeechRate)
        engine.setPitch(currentSpeechPitch)

        val locale = when (currentLanguage) {
            "hi" -> Locale.forLanguageTag("hi-IN")
            "en-in" -> Locale.forLanguageTag("en-IN")
            else -> Locale.UK // Authentic British J.A.R.V.I.S. flair
        }

        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback to default US English
            engine.setLanguage(Locale.US)
        }
    }

    fun updateSettings(rate: Float, pitch: Float, language: String) {
        currentSpeechRate = rate
        currentSpeechPitch = pitch
        currentLanguage = language
        applyTtsSettings()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            updateState(VoiceState.LISTENING)
                        }

                        override fun onBeginningOfSpeech() {
                            updateState(VoiceState.LISTENING)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // Normalize rmsdB (-2 to 10 typical) to 0.0 - 1.0
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            onRmsChange(normalized)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            updateState(VoiceState.PROCESSING)
                        }

                        override fun onError(error: Int) {
                            Log.w(TAG, "Speech recognition error code: $error")
                            updateState(VoiceState.IDLE)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull().orEmpty().trim()
                            if (recognizedText.isNotBlank()) {
                                onTranscriptReady(recognizedText)
                            } else {
                                updateState(VoiceState.IDLE)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed creating SpeechRecognizer", e)
            }
        }
    }

    fun startListening() {
        stopSpeaking()
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            val langTag = if (currentLanguage == "hi") "hi-IN" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            updateState(VoiceState.LISTENING)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed starting speech recognition", e)
            updateState(VoiceState.ERROR)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping listener", e)
        }
    }

    fun speak(text: String) {
        if (!isTtsInitialized || text.isBlank()) return
        stopSpeaking()
        // Strip markdown asterisks and code blocks for crisp voice delivery
        val cleanText = text
            .replace(Regex("```[\\s\\S]*?```"), "Code block omitted.")
            .replace(Regex("[*#_`~>]"), "")
            .trim()

        val utteranceId = "JARVIS_${System.currentTimeMillis()}"
        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
            updateState(VoiceState.IDLE)
        }
    }

    fun updateState(state: VoiceState) {
        _voiceState.value = state
        onStateChange(state)
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "Error in destroy", e)
        }
    }
}
