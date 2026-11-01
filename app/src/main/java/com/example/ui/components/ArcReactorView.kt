package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.service.VoiceState
import com.example.ui.theme.ArcAlertRed
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcCyanGlow
import com.example.ui.theme.ArcElectricBlue
import com.example.ui.theme.ArcEnergyAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorView(
    voiceState: VoiceState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor_anim")

    // Slow continuous rotation for idle
    val rotationSlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_slow"
    )

    // Fast counter rotation for processing/active
    val rotationFast by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_fast"
    )

    // Core breathing glow pulse
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    // Speaking rhythmic wave pulse
    val speakingPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaking_pulse"
    )

    val coreColor = when (voiceState) {
        VoiceState.IDLE -> ArcCyan
        VoiceState.LISTENING -> ArcElectricBlue
        VoiceState.PROCESSING -> ArcEnergyAmber
        VoiceState.SPEAKING -> ArcCyan
        VoiceState.ERROR -> ArcAlertRed
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, color = coreColor),
                onClick = onClick
            )
            .testTag("arc_reactor_core"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2f

            val currentRotation = when (voiceState) {
                VoiceState.PROCESSING -> rotationFast
                VoiceState.LISTENING -> rotationFast * 1.5f
                else -> rotationSlow
            }

            val scaleMultiplier = when (voiceState) {
                VoiceState.SPEAKING -> speakingPulse
                VoiceState.LISTENING -> 1f + (audioRms * 0.35f)
                else -> pulseGlow
            }

            // 1. Outermost Background Glow Halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColor.copy(alpha = 0.35f * scaleMultiplier),
                        coreColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius,
                center = center
            )

            // 2. Outermost Segmented Ring (Stark HUD Segments)
            drawOuterSegmentedRing(
                center = center,
                radius = baseRadius * 0.95f,
                rotation = currentRotation,
                color = coreColor.copy(alpha = 0.8f)
            )

            // 3. Middle Concentric Ring with Dashed HUD Ticks
            drawDashedHudRing(
                center = center,
                radius = baseRadius * 0.78f,
                rotation = -currentRotation * 0.8f,
                color = ArcCyan.copy(alpha = 0.65f)
            )

            // 4. Inner Ring with 10 Mechanical Arc Segments
            drawReactorSegments(
                center = center,
                radius = baseRadius * 0.62f,
                segmentCount = 10,
                rotation = currentRotation * 1.2f,
                color = coreColor
            )

            // 5. Central Triangular Reactor Core
            drawCentralReactorCore(
                center = center,
                radius = baseRadius * 0.38f,
                scale = scaleMultiplier,
                color = coreColor,
                voiceState = voiceState,
                audioRms = audioRms
            )
        }
    }
}

private fun DrawScope.drawOuterSegmentedRing(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color
) {
    val strokeWidth = 3.dp.toPx()
    // 3 Major curved arcs around the perimeter
    val sweepAngle = 75f
    val gap = 45f
    for (i in 0 until 3) {
        val startAngle = rotation + (i * (sweepAngle + gap))
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

    // Outer thin guideline
    drawCircle(
        color = color.copy(alpha = 0.25f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawDashedHudRing(
    center: Offset,
    radius: Float,
    rotation: Float,
    color: Color
) {
    val strokeWidth = 1.8.dp.toPx()
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 16f), rotation)
        )
    )

    // Inner subtle guideline
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = radius * 0.92f,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawReactorSegments(
    center: Offset,
    radius: Float,
    segmentCount: Int,
    rotation: Float,
    color: Color
) {
    val step = (2 * Math.PI / segmentCount).toFloat()
    val radOffset = Math.toRadians(rotation.toDouble()).toFloat()

    for (i in 0 until segmentCount) {
        val angle = (i * step) + radOffset
        val innerR = radius * 0.72f
        val outerR = radius * 0.98f

        val startX = center.x + (cos(angle) * innerR)
        val startY = center.y + (sin(angle) * innerR)
        val endX = center.x + (cos(angle) * outerR)
        val endY = center.y + (sin(angle) * outerR)

        drawLine(
            color = color.copy(alpha = 0.85f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        color = color.copy(alpha = 0.7f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

private fun DrawScope.drawCentralReactorCore(
    center: Offset,
    radius: Float,
    scale: Float,
    color: Color,
    voiceState: VoiceState,
    audioRms: Float
) {
    val scaledRadius = radius * (0.85f + (scale * 0.15f))

    // Inner bright gradient core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                color.copy(alpha = 0.9f),
                color.copy(alpha = 0.3f),
                Color.Transparent
            ),
            center = center,
            radius = scaledRadius
        ),
        radius = scaledRadius,
        center = center
    )

    // Central Triangular Palladium Core (The iconic Stark chest piece shape)
    val triPath = Path()
    val triRadius = scaledRadius * 0.58f
    for (i in 0 until 3) {
        val angle = Math.toRadians((i * 120.0) - 90.0).toFloat()
        val x = center.x + (cos(angle) * triRadius)
        val y = center.y + (sin(angle) * triRadius)
        if (i == 0) {
            triPath.moveTo(x, y)
        } else {
            triPath.lineTo(x, y)
        }
    }
    triPath.close()

    drawPath(
        path = triPath,
        color = Color.White.copy(alpha = 0.95f),
        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    )

    // Triangular inner glowing fill
    drawPath(
        path = triPath,
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.85f), color.copy(alpha = 0.35f)),
            center = center,
            radius = triRadius
        )
    )

    // Center micro core point
    drawCircle(
        color = Color.White,
        radius = 3.5.dp.toPx(),
        center = center
    )
}
