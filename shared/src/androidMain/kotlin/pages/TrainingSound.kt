package pages

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import db.appContext
import db.isDbContextReady

private const val AUDIO_TAG = "SportTaskAudio"

/**
 * 提示音 / 震动底层实现。
 * Context 复用 MainActivity.onCreate 中 initDb() 写入的 appContext，懒初始化。
 * 音频走 STREAM_MUSIC（跟随媒体音量）；带诊断日志，便于用 logcat 确认播放是否真的执行。
 * 秒针声用独立的低音量 ToneGenerator，避免长时间每秒响一次过吵。
 */
private object PlatformSound {
    private var toneGenerator: ToneGenerator? = null
    private var tickGenerator: ToneGenerator? = null
    private var audioManager: AudioManager? = null
    private var vibrator: Vibrator? = null
    private var initAttempted = false

    private fun ensureInit() {
        if (initAttempted || !isDbContextReady()) return
        initAttempted = true
        val ctx: Context = appContext
        try {
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // 媒体音量流：训练时跟随用户调节的媒体音量，且不受勿扰模式屏蔽通知流影响
            // 两个实例独立创建，秒针实例失败不影响提示音
            toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
                .onFailure { Log.w(AUDIO_TAG, "tone init failed: ${it.message}") }.getOrNull()
            tickGenerator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }
                .onFailure { Log.w(AUDIO_TAG, "tick init failed: ${it.message}") }.getOrNull()
        } catch (e: Exception) {
            Log.w(AUDIO_TAG, "audio init failed: ${e.message}")
        }
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        } catch (_: Exception) {
            // 设备不支持震动时忽略
        }
        Log.i(
            AUDIO_TAG,
            "init tone=${toneGenerator != null} tick=${tickGenerator != null} " +
                "vibrator=${vibrator != null} " +
                "musicVolume=${audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)}/" +
                "${audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}"
        )
    }

    fun playTone(toneType: Int, durationMs: Int, quiet: Boolean = false) {
        ensureInit()
        try {
            val tg = if (quiet) tickGenerator else toneGenerator
            if (tg == null) {
                Log.w(AUDIO_TAG, "playTone skipped: generator is null quiet=$quiet")
                return
            }
            tg.startTone(toneType, durationMs)
            Log.i(
                AUDIO_TAG,
                "playTone type=$toneType durationMs=$durationMs quiet=$quiet " +
                    "musicVolume=${audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC)}"
            )
        } catch (e: Exception) {
            Log.w(AUDIO_TAG, "playTone failed type=$toneType quiet=$quiet: ${e.message}")
            // 实例可能已失效，释放后下次重新创建
            runCatching { toneGenerator?.release() }
            runCatching { tickGenerator?.release() }
            toneGenerator = null
            tickGenerator = null
            initAttempted = false
        }
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
            Log.i(AUDIO_TAG, "vibrate durationMs=$durationMs")
        } catch (_: Exception) {
            // 未授予 VIBRATE 权限或震动服务不可用时忽略
        }
    }
}

actual fun playStartSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_BEEP, 200)
}

actual fun playEndSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_ACK, 300)
}

/** 最后一秒：更明显的提示音 */
actual fun playCountdownSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_BEEP2, 180)
}

/** 倒计时全程每秒一声「秒针」：短促 + 低音量 */
actual fun playCountdownTickSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_BEEP, 45, quiet = true)
}

actual fun playPauseSound() {
    PlatformSound.playTone(ToneGenerator.TONE_PROP_NACK, 120)
}

actual fun vibrateShort() {
    PlatformSound.vibrate(100)
}

actual fun vibrateMedium() {
    PlatformSound.vibrate(300)
}

actual fun vibrateLong() {
    PlatformSound.vibrate(500)
}
