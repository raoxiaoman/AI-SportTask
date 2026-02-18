package pages

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

// 音效和震动辅助类
private object PlatformSound {
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    
    fun init(ctx: Context) {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (e: Exception) {
            // 忽略初始化错误
        }
    }
    
    fun playTone(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            // 忽略播放错误
        }
    }
    
    fun vibrate(durationMs: Long) {
        try {
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            // 忽略震动错误
        }
    }
    
    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}

fun playStartSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_BEEP, 200)
}

fun playEndSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_ACK, 300)
}

fun playCountdownSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_BEEP2, 100)
}

fun vibrateShort() {
    PlatformSound.vibrate(100)
}

fun vibrateMedium() {
    PlatformSound.vibrate(300)
}

fun vibrateLong() {
    PlatformSound.vibrate(500)
}
