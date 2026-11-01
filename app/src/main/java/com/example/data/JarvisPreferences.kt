package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JarvisPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_API_KEY = "api_key"
        const val KEY_BASE_URL = "base_url"
        const val KEY_MODEL = "model"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_HOBBIES = "user_hobbies"
        const val KEY_USER_TITLE = "user_title"
        const val KEY_ASSISTANT_NAME = "assistant_name"
        const val KEY_SPEECH_RATE = "speech_rate"
        const val KEY_SPEECH_PITCH = "speech_pitch"
        const val KEY_AUTO_SPEAK = "auto_speak"
        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_WAKE_WORD = "wake_word_enabled"
        const val KEY_LANGUAGE = "language_mode"

        const val DEFAULT_BASE_URL = "https://api.openai.com/v1/"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_USER_NAME = "Jyoterditya Singh"
        const val DEFAULT_USER_HOBBIES = "Karate and Coding"
        const val DEFAULT_USER_TITLE = "Sir Jyoterditya"
        const val DEFAULT_ASSISTANT_NAME = "J.A.R.V.I.S."
    }

    private val defaultKey: String = try {
        val keyField = com.example.BuildConfig::class.java.getField("OPENAI_API_KEY")
        val v = (keyField.get(null) as? String ?: "").trim()
        if (v == "YOUR_OPENAI_API_KEY") "" else v
    } catch (e: Throwable) { "" }

    private val _apiKey = MutableStateFlow(
        prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() } ?: defaultKey
    )
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _baseUrl = MutableStateFlow(prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _model = MutableStateFlow(prefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL)
    val model: StateFlow<String> = _model.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userHobbies = MutableStateFlow(prefs.getString(KEY_USER_HOBBIES, DEFAULT_USER_HOBBIES) ?: DEFAULT_USER_HOBBIES)
    val userHobbies: StateFlow<String> = _userHobbies.asStateFlow()

    private val _userTitle = MutableStateFlow(prefs.getString(KEY_USER_TITLE, DEFAULT_USER_TITLE) ?: DEFAULT_USER_TITLE)
    val userTitle: StateFlow<String> = _userTitle.asStateFlow()

    private val _assistantName = MutableStateFlow(prefs.getString(KEY_ASSISTANT_NAME, DEFAULT_ASSISTANT_NAME) ?: DEFAULT_ASSISTANT_NAME)
    val assistantName: StateFlow<String> = _assistantName.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_SPEECH_RATE, 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat(KEY_SPEECH_PITCH, 0.95f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _autoSpeak = MutableStateFlow(prefs.getBoolean(KEY_AUTO_SPEAK, true))
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _darkTheme = MutableStateFlow(prefs.getBoolean(KEY_DARK_THEME, true))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _wakeWordEnabled = MutableStateFlow(prefs.getBoolean(KEY_WAKE_WORD, false))
    val wakeWordEnabled: StateFlow<Boolean> = _wakeWordEnabled.asStateFlow()

    private val _languageMode = MutableStateFlow(prefs.getString(KEY_LANGUAGE, "en") ?: "en")
    val languageMode: StateFlow<String> = _languageMode.asStateFlow()

    fun setApiKey(value: String) {
        prefs.edit().putString(KEY_API_KEY, value).apply()
        _apiKey.value = value
    }

    fun setBaseURL(value: String) {
        val trimmed = if (value.isBlank()) DEFAULT_BASE_URL else if (!value.endsWith("/")) "$value/" else value
        prefs.edit().putString(KEY_BASE_URL, trimmed).apply()
        _baseUrl.value = trimmed
    }

    fun setModel(value: String) {
        prefs.edit().putString(KEY_MODEL, value).apply()
        _model.value = value
    }

    fun setUserTitle(value: String) {
        prefs.edit().putString(KEY_USER_TITLE, value).apply()
        _userTitle.value = value
    }

    fun setUserName(value: String) {
        prefs.edit().putString(KEY_USER_NAME, value).apply()
        _userName.value = value
    }

    fun setUserHobbies(value: String) {
        prefs.edit().putString(KEY_USER_HOBBIES, value).apply()
        _userHobbies.value = value
    }

    fun setAssistantName(value: String) {
        prefs.edit().putString(KEY_ASSISTANT_NAME, value).apply()
        _assistantName.value = value
    }

    fun setSpeechRate(value: Float) {
        prefs.edit().putFloat(KEY_SPEECH_RATE, value).apply()
        _speechRate.value = value
    }

    fun setSpeechPitch(value: Float) {
        prefs.edit().putFloat(KEY_SPEECH_PITCH, value).apply()
        _speechPitch.value = value
    }

    fun setAutoSpeak(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply()
        _autoSpeak.value = value
    }

    fun setDarkTheme(value: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()
        _darkTheme.value = value
    }

    fun setWakeWordEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_WAKE_WORD, value).apply()
        _wakeWordEnabled.value = value
    }

    fun setLanguageMode(value: String) {
        prefs.edit().putString(KEY_LANGUAGE, value).apply()
        _languageMode.value = value
    }
}
