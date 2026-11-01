package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.VoiceState
import com.example.ui.components.ArcReactorView
import com.example.ui.components.JarvisBackendGuideDialog
import com.example.ui.components.JarvisChatView
import com.example.ui.components.JarvisHistoryDialog
import com.example.ui.components.JarvisHudHeader
import com.example.ui.components.JarvisQuickActionsBar
import com.example.ui.components.JarvisSettingsDialog
import com.example.ui.components.JarvisVoiceInputBar
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
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val autoSpeak by viewModel.preferences.autoSpeak.collectAsState()
    val userTitle by viewModel.preferences.userTitle.collectAsState()

    var showChatOverlay by remember { mutableStateOf(false) }

    // Microphone permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required for vocal commands, $userTitle.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun handleMicClick() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            if (uiState.voiceState == VoiceState.LISTENING) {
                viewModel.stopListening()
            } else {
                viewModel.startListening()
            }
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("jarvis_main_scaffold"),
        containerColor = ArcDarkBg,
        topBar = {
            JarvisHudHeader(
                voiceState = uiState.voiceState,
                batteryInfo = uiState.batteryInfo,
                isAutoSpeak = autoSpeak,
                onToggleAutoSpeak = { viewModel.preferences.setAutoSpeak(!autoSpeak) },
                onOpenHistory = { viewModel.setHistoryOpen(true) },
                onOpenSettings = { viewModel.setSettingsOpen(true) }
            )
        },
        bottomBar = {
            JarvisVoiceInputBar(
                inputText = uiState.inputText,
                onInputTextChange = { viewModel.onInputTextChange(it) },
                onSendMessage = { viewModel.sendTextMessage() },
                voiceState = uiState.voiceState,
                audioRms = uiState.audioRms,
                onToggleListening = { handleMicClick() },
                onStopSpeaking = { viewModel.stopSpeaking() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ArcDarkBg,
                            Color(0xFF030E1F),
                            ArcDarkBg
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Quick Hardware Action Chips
                JarvisQuickActionsBar(
                    isFlashlightOn = uiState.isFlashlightOn,
                    onToggleFlashlight = { viewModel.toggleFlashlight() },
                    onQueryBattery = { viewModel.processUserQuery("Check battery status") },
                    onSetAlarm = { viewModel.processUserQuery("Set alarm for 7:00 AM") },
                    onRunDiagnostics = { viewModel.processUserQuery("Run system diagnostics") },
                    onLaunchApp = { viewModel.processUserQuery("Open YouTube") },
                    onWebSearch = { viewModel.processUserQuery("Search for latest technology news") }
                )

                // View Mode Toggle (Cockpit Arc Reactor vs Full Conversation Stream)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CORE TELEMETRY",
                        color = ArcCyan.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ArcCardBg)
                            .border(1.dp, ArcSurfaceBorder, RoundedCornerShape(8.dp))
                            .clickable { showChatOverlay = !showChatOverlay }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = ArcCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showChatOverlay) "SHOW REACTOR" else "EXPAND CHAT",
                            color = ArcCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = if (showChatOverlay) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = ArcCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (showChatOverlay) {
                    // Full chat stream mode
                    JarvisChatView(
                        messages = chatMessages,
                        onSpeakMessage = { viewModel.speakMessage(it) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp)
                    )
                } else {
                    // Futuristic Cockpit Arc Reactor Mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Status Hologram Text
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.statusMessage,
                                color = when (uiState.voiceState) {
                                    VoiceState.LISTENING -> ArcElectricBlue
                                    VoiceState.PROCESSING -> ArcEnergyAmber
                                    VoiceState.ERROR -> ArcAlertRed
                                    else -> ArcCyan
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "TAP ARC REACTOR FOR VOCAL INTERACTION",
                                color = ArcTextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        // The Iconic Animated Arc Reactor Centerpiece
                        ArcReactorView(
                            voiceState = uiState.voiceState,
                            audioRms = uiState.audioRms,
                            size = 230.dp,
                            onClick = { handleMicClick() }
                        )

                        // Latest Transmission Card Preview
                        val latestJarvisMsg = chatMessages.lastOrNull { it.isJarvis }
                        if (latestJarvisMsg != null) {
                            TransmissionPreviewCard(
                                text = latestJarvisMsg.text,
                                isSpeaking = uiState.voiceState == VoiceState.SPEAKING,
                                onReplay = { viewModel.speakMessage(latestJarvisMsg.text) },
                                onClick = { showChatOverlay = true }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ArcCardBg.copy(alpha = 0.6f))
                                    .border(1.dp, ArcSurfaceBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ready for instructions, $userTitle. Ask anything or issue device commands.",
                                    color = ArcTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Modal
    if (uiState.isSettingsOpen) {
        JarvisSettingsDialog(
            preferences = viewModel.preferences,
            onDismiss = { viewModel.setSettingsOpen(false) },
            onTestVoice = { rate, pitch, lang ->
                viewModel.updateVoiceSettings(rate, pitch, lang)
                viewModel.speakMessage("Vocal calibration verified, $userTitle. Audio output nominal.")
            },
            onClearHistory = {
                viewModel.clearChatHistory()
                Toast.makeText(context, "Memory logs cleared, $userTitle.", Toast.LENGTH_SHORT).show()
            },
            onExportHistory = {
                viewModel.exportChatHistory(context)
            },
            onOpenBackendGuide = {
                viewModel.setBackendGuideOpen(true)
            }
        )
    }

    // History Modal
    if (uiState.isHistoryOpen) {
        JarvisHistoryDialog(
            messages = chatMessages,
            onDismiss = { viewModel.setHistoryOpen(false) },
            onSpeakMessage = { viewModel.speakMessage(it) },
            onClearHistory = { viewModel.clearChatHistory() },
            onExportHistory = { viewModel.exportChatHistory(context) }
        )
    }

    // Backend Proxy Guide Modal
    if (uiState.isBackendGuideOpen) {
        JarvisBackendGuideDialog(
            onDismiss = { viewModel.setBackendGuideOpen(false) }
        )
    }
}

@Composable
fun TransmissionPreviewCard(
    text: String,
    isSpeaking: Boolean,
    onReplay: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(14.dp))
            .background(ArcCardBg.copy(alpha = 0.85f))
            .border(1.dp, if (isSpeaking) ArcEnergyAmber else ArcSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("preview_transmission_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) ArcEnergyAmber else ArcCyan)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSpeaking) "VOCAL TRANSMISSION ACTIVE" else "LATEST RESPONSE",
                        color = if (isSpeaking) ArcEnergyAmber else ArcCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = text,
                    color = ArcTextPrimary,
                    fontSize = 12.sp,
                    maxLines = 3,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 17.sp
                )
            }

            IconButton(
                onClick = onReplay,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_replay_speech")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Speak response",
                    tint = ArcCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
