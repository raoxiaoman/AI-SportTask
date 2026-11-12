package pages

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import db.appContext
import db.isDbContextReady

private const val VIBRATION_TAG = "SportTaskAudio"

/** 震动与音频解耦：静音或关闭声音时仍可独立开启 */
private object PlatformVibration {
    private var vibrator: Vibrator? = null
    private var initAttempted = false

    private fun ensureInit() {
        if (initAttempted || !isDbContextReady()) return
        initAttempted = true
        val ctx: Context = appContext
        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }.onFailure { Log.w(VIBRATION_TAG, "vibrator init failed: ${it.message}") }
    }

    fun vibrate(durationMs: Long) {
        ensureInit()
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) {
            // 未授予 VIBRATE 权限或设备不支持时忽略
        }
    }
}

actual fun vibrateShort() = PlatformVibration.vibrate(90L)

actual fun vibrateMedium() = PlatformVibration.vibrate(260L)

actual fun vibrateLong() = PlatformVibration.vibrate(450L)
