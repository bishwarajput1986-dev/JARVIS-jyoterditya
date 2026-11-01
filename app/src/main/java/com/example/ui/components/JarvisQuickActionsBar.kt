package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArcCardBg
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcElectricBlue
import com.example.ui.theme.ArcEnergyAmber
import com.example.ui.theme.ArcSurfaceBorder
import com.example.ui.theme.ArcTextPrimary

@Composable
fun JarvisQuickActionsBar(
    isFlashlightOn: Boolean,
    onToggleFlashlight: () -> Unit,
    onQueryBattery: () -> Unit,
    onSetAlarm: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onLaunchApp: () -> Unit,
    onWebSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flashlight Chip
        val flashColor by animateColorAsState(
            targetValue = if (isFlashlightOn) ArcEnergyAmber else ArcCyan,
            label = "torch_color"
        )
        HudActionChip(
            label = if (isFlashlightOn) "TORCH ON" else "TORCH",
            icon = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
            isActive = isFlashlightOn,
            activeColor = flashColor,
            testTag = "action_torch",
            onClick = onToggleFlashlight
        )

        // Battery Diagnostic Chip
        HudActionChip(
            label = "BATTERY",
            icon = Icons.Default.BatteryChargingFull,
            testTag = "action_battery",
            onClick = onQueryBattery
        )

        // Diagnostics Chip
        HudActionChip(
            label = "DIAGNOSTICS",
            icon = Icons.Default.HealthAndSafety,
            testTag = "action_diagnostics",
            onClick = onRunDiagnostics
        )

        // Alarm Chip
        HudActionChip(
            label = "ALARM",
            icon = Icons.Default.Alarm,
            testTag = "action_alarm",
            onClick = onSetAlarm
        )

        // Launch App Chip
        HudActionChip(
            label = "LAUNCH APP",
            icon = Icons.Default.Apps,
            testTag = "action_launch_app",
            onClick = onLaunchApp
        )

        // Search Chip
        HudActionChip(
            label = "SEARCH",
            icon = Icons.Default.Search,
            testTag = "action_search",
            onClick = onWebSearch
        )
    }
}

@Composable
fun HudActionChip(
    label: String,
    icon: ImageVector,
    testTag: String,
    isActive: Boolean = false,
    activeColor: androidx.compose.ui.graphics.Color = ArcCyan,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) activeColor else ArcSurfaceBorder
    val bgColor = if (isActive) activeColor.copy(alpha = 0.2f) else ArcCardBg.copy(alpha = 0.7f)
    val contentColor = if (isActive) activeColor else ArcTextPrimary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }
    }
}
