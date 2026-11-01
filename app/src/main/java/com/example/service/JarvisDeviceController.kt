package com.example.service

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val statusText: String,
    val health: String
)

data class SystemDiagnostics(
    val batteryInfo: BatteryInfo,
    val totalMemoryMb: Long,
    val availableMemoryMb: Long,
    val uptimeFormatted: String,
    val osVersion: String,
    val deviceModel: String
)

class JarvisDeviceController(private val context: Context) {
    private val TAG = "JarvisDeviceController"
    private var isTorchOn = false
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    fun getBatteryInfo(): BatteryInfo {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 100

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Operating on Core Power"
        }

        val health = when (batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            else -> "Normal"
        }

        return BatteryInfo(
            percentage = pct,
            isCharging = isCharging,
            statusText = statusText,
            health = health
        )
    }

    fun isFlashlightSupported(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    fun isFlashlightOn(): Boolean = isTorchOn

    fun toggleFlashlight(): Result<Boolean> {
        return setFlashlight(!isTorchOn)
    }

    fun setFlashlight(enable: Boolean): Result<Boolean> {
        if (cameraManager == null || !isFlashlightSupported()) {
            return Result.failure(Exception("Torch hardware is unavailable on this device."))
        }
        return try {
            val cameraIds = cameraManager.cameraIdList
            var selectedId: String? = null
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    selectedId = id
                    break
                }
            }
            if (selectedId == null && cameraIds.isNotEmpty()) {
                selectedId = cameraIds[0]
            }

            if (selectedId != null) {
                cameraManager.setTorchMode(selectedId, enable)
                isTorchOn = enable
                Result.success(enable)
            } else {
                Result.failure(Exception("No camera flash hardware detected."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting flashlight", e)
            Result.failure(e)
        }
    }

    fun openApp(appName: String): Result<String> {
        val pm = context.packageManager
        val query = appName.trim().lowercase()

        // Known aliases
        val targetAlias = when {
            query.contains("youtube") -> "youtube"
            query.contains("chrome") || query.contains("browser") -> "chrome"
            query.contains("map") -> "maps"
            query.contains("setting") -> "settings"
            query.contains("camera") -> "camera"
            query.contains("clock") || query.contains("alarm") -> "clock"
            query.contains("calculator") -> "calculator"
            query.contains("music") || query.contains("spotify") -> "music"
            query.contains("photo") || query.contains("gallery") -> "gallery"
            else -> query
        }

        if (targetAlias == "settings") {
            return openSettings()
        }

        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(launchIntent, 0)

        // Find match by app label or package
        var bestMatchPkg: String? = null
        var bestMatchLabel: String? = null

        for (resolveInfo in activities) {
            val label = resolveInfo.loadLabel(pm).toString().lowercase()
            val pkg = resolveInfo.activityInfo.packageName.lowercase()

            if (label == targetAlias || pkg.contains(targetAlias)) {
                bestMatchPkg = resolveInfo.activityInfo.packageName
                bestMatchLabel = resolveInfo.loadLabel(pm).toString()
                break
            } else if (label.contains(targetAlias) && bestMatchPkg == null) {
                bestMatchPkg = resolveInfo.activityInfo.packageName
                bestMatchLabel = resolveInfo.loadLabel(pm).toString()
            }
        }

        return if (bestMatchPkg != null) {
            val intent = pm.getLaunchIntentForPackage(bestMatchPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Result.success("Opening $bestMatchLabel")
            } else {
                Result.failure(Exception("Cannot start application intent for $bestMatchLabel"))
            }
        } else {
            Result.failure(Exception("Application '$appName' not found on this device."))
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "JARVIS Alarm"): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format("%02d:%02d", hour, minute)
            Result.success("Alarm initialized for $timeStr")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set alarm", e)
            Result.failure(e)
        }
    }

    fun setTimer(seconds: Int, message: String = "JARVIS Timer"): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Timer set for $seconds seconds")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer", e)
            Result.failure(e)
        }
    }

    fun openWebSearch(query: String): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Searching web for '$query'")
        } catch (e: Exception) {
            // Fallback to browser URL
            openUrl("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun openUrl(url: String): Result<String> {
        return try {
            val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opening $cleanUrl")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openSettings(): Result<String> {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Accessing system settings")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareText(text: String): Result<String> {
        return try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "JARVIS Transmission").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(shareIntent)
            Result.success("Shared successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSystemDiagnostics(): SystemDiagnostics {
        val battery = getBatteryInfo()
        val runtime = Runtime.getRuntime()
        val totalMem = (runtime.totalMemory() / (1024 * 1024))
        val freeMem = (runtime.freeMemory() / (1024 * 1024))
        val maxMem = (runtime.maxMemory() / (1024 * 1024))

        val uptimeMillis = SystemClock.elapsedRealtime()
        val hours = uptimeMillis / (1000 * 60 * 60)
        val minutes = (uptimeMillis / (1000 * 60)) % 60
        val uptimeStr = "${hours}h ${minutes}m"

        return SystemDiagnostics(
            batteryInfo = battery,
            totalMemoryMb = maxMem,
            availableMemoryMb = freeMem,
            uptimeFormatted = uptimeStr,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}"
        )
    }
}
