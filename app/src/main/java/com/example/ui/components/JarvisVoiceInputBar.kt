package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.VoiceState
import com.example.ui.theme.ArcAlertRed
import com.example.ui.theme.ArcCardBg
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcElectricBlue
import com.example.ui.theme.ArcEnergyAmber
import com.example.ui.theme.ArcSurfaceBorder
import com.example.ui.theme.ArcTextMuted
import com.example.ui.theme.ArcTextPrimary
import com.example.ui.theme.ArcTextSecondary
import kotlin.math.sin

@Composable
fun JarvisVoiceInputBar(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    voiceState: VoiceState,
    audioRms: Float,
    onToggleListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState == VoiceState.LISTENING
    val isSpeaking = voiceState == VoiceState.SPEAKING

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(ArcCardBg.copy(alpha = 0.95f))
            .border(1.dp, ArcSurfaceBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Live Audio Waveform when Listening or Speaking
        AnimatedVisibility(visible = isListening || isSpeaking) {
            AudioWaveformBanner(
                audioRms = audioRms,
                isSpeaking = isSpeaking,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Glowing Mic / Stop Action Button
            val micColor by animateColorAsState(
                targetValue = when (voiceState) {
                    VoiceState.LISTENING -> ArcElectricBlue
                    VoiceState.SPEAKING -> ArcEnergyAmber
                    VoiceState.PROCESSING -> ArcCyan
                    VoiceState.ERROR -> ArcAlertRed
                    VoiceState.IDLE -> ArcCyan
                },
                label = "mic_color"
            )

            val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
            val pulseSize by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "mic_pulse_size"
            )

            Box(
                modifier = Modifier
                    .size(if (isListening) 52.dp * pulseSize else 52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                micColor.copy(alpha = if (isListening) 0.6f else 0.25f),
                                micColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = if (isListening) 2.dp else 1.dp,
                        color = micColor,
                        shape = CircleShape
                    )
                    .clickable {
                        if (isSpeaking) {
                            onStopSpeaking()
                        } else {
                            onToggleListening()
                        }
                    }
                    .testTag("btn_mic_toggle"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isSpeaking -> Icons.Default.Stop
                        isListening -> Icons.Default.Mic
                        else -> Icons.Default.Mic
                    },
                    contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
                    tint = micColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Sci-Fi Holographic Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = if (isListening) "Listening to audio…" else "Command or query, sir…",
                        color = ArcTextMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_command_text"),
                textStyle = TextStyle(
                    color = ArcTextPrimary,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcCyan,
                    unfocusedBorderColor = ArcSurfaceBorder,
                    focusedContainerColor = Color(0xFF040E1B),
                    unfocusedContainerColor = Color(0xFF040E1B),
                    cursorColor = ArcCyan
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            IconButton(
                onClick = onSendMessage,
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) ArcCyan.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        1.dp,
                        if (inputText.isNotBlank()) ArcCyan else ArcSurfaceBorder.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .testTag("btn_send_command")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = if (inputText.isNotBlank()) ArcCyan else ArcTextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AudioWaveformBanner(
    audioRms: Float,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    val barColor = if (isSpeaking) ArcEnergyAmber else ArcElectricBlue
    val count = 28
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_anim")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF020914))
            .border(1.dp, barColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until count) {
            val sinVal = (sin((i.toFloat() * 0.4f) + waveOffset) + 1f) / 2f
            val baseHeight = 4f
            val dynamicHeight = if (isSpeaking) {
                (baseHeight + (sinVal * 18f))
            } else {
                (baseHeight + (audioRms * 20f * (0.4f + (sinVal * 0.6f))))
            }.coerceIn(4f, 22f)

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(dynamicHeight.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor.copy(alpha = 0.85f))
            )
        }
    }
}
