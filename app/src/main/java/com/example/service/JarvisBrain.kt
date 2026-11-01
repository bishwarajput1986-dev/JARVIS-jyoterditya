package com.example.service

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.ChatMessage
import com.example.data.JarvisPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class BrainResponse {
    data class Success(val text: String, val actionType: String? = null, val actionDetails: String? = null) : BrainResponse()
    data class Error(val message: String) : BrainResponse()
}

class JarvisBrain(
    private val context: Context,
    private val preferences: JarvisPreferences,
    private val deviceController: JarvisDeviceController
) {
    private val TAG = "JarvisBrain"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun processQuery(
        userInput: String,
        recentHistory: List<ChatMessage>
    ): BrainResponse = withContext(Dispatchers.IO) {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) {
            return@withContext BrainResponse.Error("Command was empty, sir.")
        }

        // 1. Check for immediate device hardware/system commands (works online AND offline)
        val deviceCommand = tryExecuteDeviceCommand(trimmed)
        if (deviceCommand != null) {
            return@withContext deviceCommand
        }

        // 2. Resolve API Key & Endpoint
        val userTitle = preferences.userTitle.value.ifBlank { "sir" }
        val customKey = preferences.apiKey.value.trim()
        val buildConfigOpenAi = try {
            val keyField = BuildConfig::class.java.getField("OPENAI_API_KEY")
            val v = (keyField.get(null) as? String ?: "").trim()
            if (v == "YOUR_OPENAI_API_KEY") "" else v
        } catch (e: Throwable) { "" }

        val buildConfigGemini = try {
            val keyField = BuildConfig::class.java.getField("GEMINI_API_KEY")
            val v = (keyField.get(null) as? String ?: "").trim()
            if (v == "YOUR_GEMINI_API_KEY") "" else v
        } catch (e: Throwable) { "" }

        val effectiveApiKey = when {
            customKey.isNotBlank() -> customKey
            buildConfigOpenAi.isNotBlank() -> buildConfigOpenAi
            buildConfigGemini.isNotBlank() -> buildConfigGemini
            else -> ""
        }

        // 3. If no API key configured, provide polite intelligent offline response with guidance
        if (effectiveApiKey.isBlank()) {
            return@withContext handleOfflineOrMissingKey(trimmed, userTitle)
        }

        // 4. Send to AI Brain
        try {
            val systemPrompt = buildSystemPrompt(userTitle)

            // Detect if this is a Gemini API key (starts with AIza or explicitly selected)
            val isGeminiKey = effectiveApiKey.startsWith("AIza") || effectiveApiKey == buildConfigGemini

            if (isGeminiKey) {
                // Call Google Gemini API
                val geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$effectiveApiKey"
                val contentsArray = JSONArray()

                // Context history
                val contextSlice = recentHistory.takeLast(6)
                for (msg in contextSlice) {
                    val role = if (msg.isJarvis) "model" else "user"
                    contentsArray.put(JSONObject().apply {
                        put("role", role)
                        put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                    })
                }

                // Current user query
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", trimmed)))
                })

                val geminiBodyJson = JSONObject().apply {
                    put("system_instruction", JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                    })
                    put("contents", contentsArray)
                }

                val request = Request.Builder()
                    .url(geminiUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(geminiBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    val errorCode = response.code
                    return@withContext when (errorCode) {
                        400, 401, 403 -> BrainResponse.Error("API key authentication rejected ($errorCode), $userTitle. Please verify or update your key in Settings.")
                        429 -> BrainResponse.Error("Quota limit reached, $userTitle. Please verify your API billing tier or rate limits.")
                        else -> BrainResponse.Error("Neural core returned status $errorCode. Operating on local protocols.")
                    }
                }

                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val contentObj = cand.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val replyText = parts?.optJSONObject(0)?.optString("text")?.trim() ?: "Systems active, $userTitle."
                    return@withContext BrainResponse.Success(text = replyText)
                } else {
                    return@withContext BrainResponse.Error("Received empty response from neural core, $userTitle.")
                }
            } else {
                // Call OpenAI compatible endpoint
                val baseUrl = preferences.baseUrl.value.trim().let {
                    if (it.isBlank()) JarvisPreferences.DEFAULT_BASE_URL else if (!it.endsWith("/")) "$it/" else it
                }
                val model = preferences.model.value.ifBlank { JarvisPreferences.DEFAULT_MODEL }

                val messagesArray = JSONArray()

                // System instructions
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })

                // Context window (up to last 8 messages)
                val contextSlice = recentHistory.takeLast(8)
                for (msg in contextSlice) {
                    messagesArray.put(JSONObject().apply {
                        put("role", if (msg.isJarvis) "assistant" else "user")
                        put("content", msg.text)
                    })
                }

                // Current prompt
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", trimmed)
                })

                val requestBodyJson = JSONObject().apply {
                    put("model", model)
                    put("messages", messagesArray)
                    put("temperature", 0.7)
                    put("max_tokens", 800)
                }

                val request = Request.Builder()
                    .url("${baseUrl}chat/completions")
                    .addHeader("Authorization", "Bearer $effectiveApiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody == null) {
                    val errorCode = response.code
                    Log.w(TAG, "API call failed with status $errorCode: $responseBody")
                    return@withContext when (errorCode) {
                        401 -> BrainResponse.Error("Authentication failed, $userTitle. Please verify or change your API key in Settings.")
                        429 -> BrainResponse.Error("Quota limit reached, $userTitle. Please verify your API billing tier or rate limits.")
                        else -> BrainResponse.Error("Neural core returned status $errorCode. Operating on local backup protocols.")
                    }
                }

                val json = JSONObject(responseBody)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val messageObj = choices.getJSONObject(0).optJSONObject("message")
                    val content = messageObj?.optString("content")?.trim() ?: "Systems active, $userTitle."
                    BrainResponse.Success(text = content)
                } else {
                    BrainResponse.Error("Received empty response from neural core, $userTitle.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception communicating with AI backend", e)
            // Fallback response with offline knowledge
            BrainResponse.Success(
                text = "Network connection is currently unstable, $userTitle. However, all local protocols, battery monitoring, alarm control, and flashlight systems remain fully operational.",
                actionType = "OFFLINE_FALLBACK"
            )
        }
    }

    private fun buildSystemPrompt(userTitle: String): String {
        val timeStr = SimpleDateFormat("h:mm a, EEEE, MMMM d, yyyy", Locale.US).format(Date())
        val name = preferences.userName.value.ifBlank { "Jyoterditya Singh" }
        val hobbies = preferences.userHobbies.value.ifBlank { "Karate and Coding" }
        return """
            You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the legendary AI assistant originally conceived by Tony Stark.
            Current device time and date: $timeStr.
            
            User Dossier:
            - Full Name: $name
            - Primary Disciplines & Hobbies: $hobbies
            - Preferred Salutation: $userTitle
            
            Guidelines:
            1. Persona: Refined, extraordinarily knowledgeable, courteous, British wit, elegant cadence.
            2. Address the user with highest respect as '$userTitle', 'Mr. Singh', or '$name'.
            3. Acknowledge and support their passions ($hobbies — martial arts discipline and software engineering) whenever appropriate or asked.
            4. Multilingual: You are fully fluent in English, Hindi, and Hinglish. Answer naturally in whatever language or mix the user communicates with.
            5. Capabilities: General intelligence, coding, mathematics, educational explanations, device management, and planning.
            6. Conciseness: Keep responses crisp and voice-friendly. Avoid unnecessarily lengthy preambles when speaking.
            7. Character quote flair: Feel free to use authentic touches like "Right away, $userTitle", "At your service, $userTitle", "Running diagnostics", etc.
        """.trimIndent()
    }

    private fun tryExecuteDeviceCommand(input: String): BrainResponse? {
        val lower = input.lowercase()
        val title = preferences.userTitle.value.ifBlank { "sir" }

        // 1. Flashlight / Torch
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = lower.contains("on") || lower.contains("activate") || lower.contains("start") || lower.contains("chalu")
            val turnOff = lower.contains("off") || lower.contains("deactivate") || lower.contains("stop") || lower.contains("band")
            
            val targetState = if (turnOff) false else if (turnOn) true else !deviceController.isFlashlightOn()
            val result = deviceController.setFlashlight(targetState)
            return if (result.isSuccess) {
                val stateText = if (targetState) "illuminating workspace. Flashlight engaged" else "flashlight deactivated"
                BrainResponse.Success(
                    text = "Right away, $title. $stateText.",
                    actionType = "FLASHLIGHT",
                    actionDetails = if (targetState) "ON" else "OFF"
                )
            } else {
                BrainResponse.Success(
                    text = "Apologies, $title. ${result.exceptionOrNull()?.message ?: "Flashlight hardware is unavailable."}",
                    actionType = "FLASHLIGHT_ERROR"
                )
            }
        }

        // 2. Battery query
        if (lower.contains("battery") || lower.contains("power level") || lower.contains("charge") || lower.contains("charging")) {
            val battery = deviceController.getBatteryInfo()
            val chargingNote = if (battery.isCharging) "Core is currently recharging." else "Operating on internal power reserves."
            return BrainResponse.Success(
                text = "Power grid status, $title: Battery is at ${battery.percentage}%. $chargingNote Arc reactor health is ${battery.health.lowercase()}.",
                actionType = "BATTERY",
                actionDetails = "${battery.percentage}%"
            )
        }

        // 3. Time / Date
        if (lower.contains("what time") || lower.contains("current time") || lower.contains("samay kya hai") || lower.contains("date today") || lower.contains("what is the date")) {
            val time = SimpleDateFormat("h:mm a", Locale.US).format(Date())
            val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(Date())
            return BrainResponse.Success(
                text = "The current time is $time, $title. Today is $date.",
                actionType = "TIME",
                actionDetails = time
            )
        }

        // 4. Open Application
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("kholo ") || lower.contains("open app")) {
            val appQuery = lower.replace("open app", "")
                .replace("open", "")
                .replace("launch", "")
                .replace("kholo", "")
                .trim()
            if (appQuery.isNotBlank()) {
                val res = deviceController.openApp(appQuery)
                return if (res.isSuccess) {
                    BrainResponse.Success(
                        text = "Accessing interface for '$appQuery' now, $title.",
                        actionType = "APP_LAUNCH",
                        actionDetails = appQuery
                    )
                } else {
                    BrainResponse.Success(
                        text = "I could not locate an installed app matching '$appQuery' on this device, $title.",
                        actionType = "APP_NOT_FOUND"
                    )
                }
            }
        }

        // 5. Alarms
        if (lower.contains("set alarm") || lower.contains("alarm lagao") || lower.contains("wake me up")) {
            val timePattern = Pattern.compile("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
            val matcher = timePattern.matcher(lower)
            if (matcher.find()) {
                var hour = matcher.group(1)?.toIntOrNull() ?: 7
                val minute = matcher.group(3)?.toIntOrNull() ?: 0
                val amPm = matcher.group(4)?.lowercase()

                if (amPm == "pm" && hour < 12) hour += 12
                if (amPm == "am" && hour == 12) hour = 0

                val res = deviceController.setAlarm(hour, minute, "JARVIS Scheduled Alert")
                return if (res.isSuccess) {
                    val formatted = String.format("%02d:%02d", hour, minute)
                    BrainResponse.Success(
                        text = "Alarm confirmed for $formatted, $title.",
                        actionType = "ALARM",
                        actionDetails = formatted
                    )
                } else {
                    BrainResponse.Success(
                        text = "Unable to set alarm, $title: ${res.exceptionOrNull()?.message}",
                        actionType = "ALARM_ERROR"
                    )
                }
            }
        }

        // 6. Web Search
        if (lower.startsWith("search for ") || lower.startsWith("google ") || lower.startsWith("look up ")) {
            val query = lower.replace("search for", "")
                .replace("google", "")
                .replace("look up", "")
                .trim()
            if (query.isNotBlank()) {
                deviceController.openWebSearch(query)
                return BrainResponse.Success(
                    text = "Initiating web scan for '$query', $title.",
                    actionType = "SEARCH",
                    actionDetails = query
                )
            }
        }

        // 7. System Diagnostics
        if (lower.contains("diagnostics") || lower.contains("system status") || lower.contains("health check")) {
            val diag = deviceController.getSystemDiagnostics()
            return BrainResponse.Success(
                text = "System diagnostics complete, $title. Device: ${diag.deviceModel}. Uptime: ${diag.uptimeFormatted}. Battery: ${diag.batteryInfo.percentage}% (${diag.batteryInfo.statusText}). Memory reserves: ${diag.availableMemoryMb}MB available. Systems nominal.",
                actionType = "DIAGNOSTICS"
            )
        }

        // 8. Offline Math calculations
        val mathResult = tryEvaluateMath(lower)
        if (mathResult != null) {
            return BrainResponse.Success(
                text = "Calculation complete, $title: $mathResult",
                actionType = "CALCULATION"
            )
        }

        // 9. Standard greetings offline
        if (lower == "hello" || lower == "hi" || lower == "hey" || lower == "hello jarvis" || lower == "hey jarvis") {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 4..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                else -> "Good evening"
            }
            return BrainResponse.Success(
                text = "$greeting, $title. J.A.R.V.I.S. systems are online and at your service. How may I assist you today?"
            )
        }

        if (lower.contains("who are you") || lower.contains("what is your name")) {
            return BrainResponse.Success(
                text = "I am J.A.R.V.I.S.—Just A Rather Very Intelligent System. Your dedicated personal AI assistant, $title."
            )
        }

        // 10. User identity and hobbies recognition
        if (lower.contains("who am i") || lower.contains("what is my name") || lower.contains("mera naam")) {
            val name = preferences.userName.value.ifBlank { "Jyoterditya Singh" }
            val hobbies = preferences.userHobbies.value.ifBlank { "Karate and Coding" }
            return BrainResponse.Success(
                text = "You are $name, $title. Master of martial discipline in karate and creator of intelligent code. It is an honor to serve you."
            )
        }

        if (lower.contains("my hobby") || lower.contains("my hobbies") || lower.contains("what do i like")) {
            val hobbies = preferences.userHobbies.value.ifBlank { "Karate and Coding" }
            return BrainResponse.Success(
                text = "Your recognized disciplines are $hobbies, $title. Combining martial focus with software engineering excellence."
            )
        }

        return null
    }

    private fun tryEvaluateMath(input: String): String? {
        val clean = input.replace("calculate", "")
            .replace("what is", "")
            .replace("solve", "")
            .trim()
        val regex = Regex("^(\\d+(?:\\.\\d+)?)\\s*([+\\-*/xX^%])\\s*(\\d+(?:\\.\\d+)?)$")
        val match = regex.find(clean) ?: return null

        val (num1Str, op, num2Str) = match.destructured
        val n1 = num1Str.toDoubleOrNull() ?: return null
        val n2 = num2Str.toDoubleOrNull() ?: return null

        val result = when (op) {
            "+" -> n1 + n2
            "-" -> n1 - n2
            "*", "x", "X" -> n1 * n2
            "/" -> if (n2 != 0.0) n1 / n2 else return "Cannot divide by zero, $n1 / 0 is undefined."
            "%" -> (n1 * n2) / 100.0
            "^" -> Math.pow(n1, n2)
            else -> return null
        }
        val formattedRes = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.2f", result)
        return "$n1 $op $n2 = $formattedRes"
    }

    private fun handleOfflineOrMissingKey(userInput: String, title: String): BrainResponse {
        return BrainResponse.Success(
            text = "Welcome, $title. Local protocols are fully active (Flashlight, Battery, App Launcher, Alarms, Diagnostics). To connect my global neural network, please configure your OpenAI API Key in the top-right Settings console.",
            actionType = "SETUP_GUIDE"
        )
    }
}
