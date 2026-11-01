package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.JarvisPreferences
import com.example.ui.theme.ArcAlertRed
import com.example.ui.theme.ArcCardBg
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcDarkBg
import com.example.ui.theme.ArcElectricBlue
import com.example.ui.theme.ArcEnergyAmber
import com.example.ui.theme.ArcSurfaceBorder
import com.example.ui.theme.ArcTextMuted
import com.example.ui.theme.ArcTextPrimary
import com.example.ui.theme.ArcTextSecondary

@Composable
fun JarvisSettingsDialog(
    preferences: JarvisPreferences,
    onDismiss: () -> Unit,
    onTestVoice: (rate: Float, pitch: Float, language: String) -> Unit,
    onClearHistory: () -> Unit,
    onExportHistory: () -> Unit,
    onOpenBackendGuide: () -> Unit
) {
    val currentApiKey by preferences.apiKey.collectAsState()
    val currentBaseUrl by preferences.baseUrl.collectAsState()
    val currentModel by preferences.model.collectAsState()
    val currentUserName by preferences.userName.collectAsState()
    val currentUserHobbies by preferences.userHobbies.collectAsState()
    val currentUserTitle by preferences.userTitle.collectAsState()
    val currentAssistantName by preferences.assistantName.collectAsState()
    val currentSpeechRate by preferences.speechRate.collectAsState()
    val currentSpeechPitch by preferences.speechPitch.collectAsState()
    val currentAutoSpeak by preferences.autoSpeak.collectAsState()
    val currentWakeWord by preferences.wakeWordEnabled.collectAsState()
    val currentLangMode by preferences.languageMode.collectAsState()

    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var baseUrlInput by remember { mutableStateOf(currentBaseUrl) }
    var modelInput by remember { mutableStateOf(currentModel) }
    var userNameInput by remember { mutableStateOf(currentUserName) }
    var userHobbiesInput by remember { mutableStateOf(currentUserHobbies) }
    var userTitleInput by remember { mutableStateOf(currentUserTitle) }
    var assistantNameInput by remember { mutableStateOf(currentAssistantName) }
    var speechRateInput by remember { mutableFloatStateOf(currentSpeechRate) }
    var speechPitchInput by remember { mutableFloatStateOf(currentSpeechPitch) }
    var showApiKey by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, ArcCyan, RoundedCornerShape(20.dp))
                .testTag("settings_dialog_modal"),
            color = ArcDarkBg
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ArcCyan,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM CONFIGURATION",
                            color = ArcCyan,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = ArcTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 1: AI BRAIN & API
                SettingsSectionHeader(title = "NEURAL CORE (OPENAI / BACKEND)", icon = Icons.Default.Key)

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        preferences.setApiKey(it)
                    },
                    label = { Text("OpenAI API Key / Bearer Token", color = ArcTextSecondary, fontSize = 12.sp) },
                    placeholder = { Text("sk-proj-...", color = ArcTextMuted, fontSize = 12.sp) },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Text(
                            text = if (showApiKey) "HIDE" else "SHOW",
                            color = ArcCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showApiKey = !showApiKey }
                                .padding(8.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_api_key"),
                    textStyle = androidx.compose.ui.text.TextStyle(color = ArcTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = ArcSurfaceBorder,
                        focusedContainerColor = ArcCardBg,
                        unfocusedContainerColor = ArcCardBg
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Paste and Clear Quick Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                            if (!clipText.isNullOrBlank()) {
                                apiKeyInput = clipText
                                preferences.setApiKey(clipText)
                                Toast.makeText(context, "API Key pasted & saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyan)
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PASTE KEY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = {
                            apiKeyInput = ""
                            preferences.setApiKey("")
                            Toast.makeText(context, "API Key cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcAlertRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcAlertRed)
                    ) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CLEAR KEY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = {
                        baseUrlInput = it
                        preferences.setBaseURL(it)
                    },
                    label = { Text("Base URL (OpenAI / Custom Backend Proxy)", color = ArcTextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = ArcTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = ArcSurfaceBorder,
                        focusedContainerColor = ArcCardBg,
                        unfocusedContainerColor = ArcCardBg
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Model Selection Chips
                Text("Selected Model:", color = ArcTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("gpt-4o-mini", "gpt-4o", "gemini-1.5-flash").forEach { modelOption ->
                        val isSelected = modelInput == modelOption
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ArcCyan.copy(alpha = 0.25f) else ArcCardBg)
                                .border(1.dp, if (isSelected) ArcCyan else ArcSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    modelInput = modelOption
                                    preferences.setModel(modelOption)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = modelOption,
                                color = if (isSelected) ArcCyan else ArcTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 2: PERSONALITY & SALUTATION
                SettingsSectionHeader(title = "USER DOSSIER & PROTOCOLS", icon = Icons.Default.Psychology)

                // User Full Name and Hobbies
                OutlinedTextField(
                    value = userNameInput,
                    onValueChange = {
                        userNameInput = it
                        preferences.setUserName(it)
                    },
                    label = { Text("User Full Name", color = ArcTextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = ArcTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = ArcSurfaceBorder,
                        focusedContainerColor = ArcCardBg,
                        unfocusedContainerColor = ArcCardBg
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userHobbiesInput,
                    onValueChange = {
                        userHobbiesInput = it
                        preferences.setUserHobbies(it)
                    },
                    label = { Text("Disciplines & Hobbies (e.g. Karate, Coding)", color = ArcTextSecondary, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = ArcTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = ArcSurfaceBorder,
                        focusedContainerColor = ArcCardBg,
                        unfocusedContainerColor = ArcCardBg
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assistantNameInput,
                        onValueChange = {
                            assistantNameInput = it
                            preferences.setAssistantName(it)
                        },
                        label = { Text("Assistant Name", color = ArcTextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = ArcTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcCyan,
                            unfocusedBorderColor = ArcSurfaceBorder,
                            focusedContainerColor = ArcCardBg,
                            unfocusedContainerColor = ArcCardBg
                        )
                    )

                    OutlinedTextField(
                        value = userTitleInput,
                        onValueChange = {
                            userTitleInput = it
                            preferences.setUserTitle(it)
                        },
                        label = { Text("Salutation Title", color = ArcTextSecondary, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(color = ArcTextPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ArcCyan,
                            unfocusedBorderColor = ArcSurfaceBorder,
                            focusedContainerColor = ArcCardBg,
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 3: VOICE & SPEECH
                SettingsSectionHeader(title = "VOICE RECOGNITION & TTS", icon = Icons.Default.RecordVoiceOver)

                // Speech Rate Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Velocity: ${String.format("%.1fx", speechRateInput)}", color = ArcTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = speechRateInput,
                    onValueChange = {
                        speechRateInput = it
                        preferences.setSpeechRate(it)
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = ArcCyan,
                        activeTrackColor = ArcCyan,
                        inactiveTrackColor = ArcSurfaceBorder
                    )
                )

                // Speech Pitch Slider
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vocal Pitch: ${String.format("%.2f", speechPitchInput)}", color = ArcTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = speechPitchInput,
                    onValueChange = {
                        speechPitchInput = it
                        preferences.setSpeechPitch(it)
                    },
                    valueRange = 0.5f..1.5f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = ArcElectricBlue,
                        activeTrackColor = ArcElectricBlue,
                        inactiveTrackColor = ArcSurfaceBorder
                    )
                )

                // Language Mode
                Text("Dialect / Language:", color = ArcTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("en" to "English (UK)", "en-in" to "English (IN)", "hi" to "Hindi").forEach { (code, label) ->
                        val isSelected = currentLangMode == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) ArcElectricBlue.copy(alpha = 0.25f) else ArcCardBg)
                                .border(1.dp, if (isSelected) ArcElectricBlue else ArcSurfaceBorder, RoundedCornerShape(6.dp))
                                .clickable { preferences.setLanguageMode(code) }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) ArcElectricBlue else ArcTextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Test Voice Button
                OutlinedButton(
                    onClick = {
                        onTestVoice(speechRateInput, speechPitchInput, currentLangMode)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyan)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TEST JARVIS VOICE SYNTHESIS", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Speak Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Speak Responses", color = ArcTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Switch(
                        checked = currentAutoSpeak,
                        onCheckedChange = { preferences.setAutoSpeak(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArcCyan,
                            checkedTrackColor = ArcCyan.copy(alpha = 0.5f),
                            uncheckedThumbColor = ArcTextMuted,
                            uncheckedTrackColor = ArcCardBg
                        )
                    )
                }

                // Wake word toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Wake Word ('Hey JARVIS')", color = ArcTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("Enables mic standby trigger", color = ArcTextMuted, fontSize = 11.sp)
                    }
                    Switch(
                        checked = currentWakeWord,
                        onCheckedChange = { preferences.setWakeWordEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ArcCyan,
                            checkedTrackColor = ArcCyan.copy(alpha = 0.5f),
                            uncheckedThumbColor = ArcTextMuted,
                            uncheckedTrackColor = ArcCardBg
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 4: DATA & BACKEND GUIDE
                SettingsSectionHeader(title = "LOGS & BACKEND ARCHITECTURE", icon = Icons.Default.Code)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onExportHistory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcElectricBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcElectricBlue)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export Log", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcAlertRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcAlertRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Purge Log", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // View Backend Proxy Server Source Code & Guide
                Button(
                    onClick = onOpenBackendGuide,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcCyan.copy(alpha = 0.2f),
                        contentColor = ArcCyan
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ArcCyan)
                ) {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("BACKEND PROXY SERVER SOURCE CODE", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcCyan,
                        contentColor = Color(0xFF020710)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("SAVE & INITIALIZE", fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ArcCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = ArcCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}
