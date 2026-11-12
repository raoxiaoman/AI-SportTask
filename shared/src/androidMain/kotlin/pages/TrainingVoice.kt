package pages

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import db.appContext
import db.isDbContextReady
import java.util.Locale

private const val VOICE_TAG = "SportTaskAudio"

/**
 * Android 端用系统 TextToSpeech 朗读倒数数字。
 * Context 复用 MainActivity.onCreate 中 initDb() 写入的 appContext。
 * 音频走 USAGE_MEDIA，跟随媒体音量，不被勿扰模式的通知流屏蔽。
 * 引擎初始化是异步的：未就绪时记下最后一个数字，就绪后补播，
 * 避免冷启动后前几个数字整段静默。
 */
private object CountdownVoice {
    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    @Volatile
    private var pendingNumber: Int? = null

    fun prepare() {
        if (tts != null || !isDbContextReady()) return
        val ctx: Context = appContext
        // 部分 ROM（MIUI / 黑鲨）未写入默认引擎，默认构造会初始化失败；
        // 此时显式指定一个已安装的 TTS 引擎包名。
        val engineName = defaultEngine(ctx) ?: availableEngine(ctx)
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val speech = tts
                if (speech != null) {
                    val result = runCatching { speech.setLanguage(Locale.CHINA) }
                        .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        // 设备缺中文语音包时退回英文，仍能读出数字
                        runCatching { speech.setLanguage(Locale.US) }
                    }
                    runCatching {
                        speech.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                    }
                    ready = true
                    Log.i(VOICE_TAG, "TTS ready engine=$engineName langResult=$result")
                    pendingNumber?.let { number ->
                        pendingNumber = null
                        speakNow(number)
                    }
                }
            } else {
                Log.w(VOICE_TAG, "TTS init failed status=$status engine=$engineName")
                runCatching { tts?.shutdown() }
                tts = null
                ready = false
            }
        }
        runCatching {
            Log.i(VOICE_TAG, "TTS prepare engine=$engineName")
            tts = if (engineName.isNullOrBlank()) {
                TextToSpeech(ctx, listener)
            } else {
                TextToSpeech(ctx, listener, engineName)
            }
        }.onFailure {
            Log.w(VOICE_TAG, "TTS constructor failed: ${it.message}")
            tts = null
        }
    }

    private fun defaultEngine(ctx: Context): String? = runCatching {
        Settings.Secure.getString(ctx.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH)
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun availableEngine(ctx: Context): String? = runCatching {
        @Suppress("DEPRECATION")
        val services = ctx.packageManager.queryIntentServices(
            Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE),
            PackageManager.MATCH_DEFAULT_ONLY
        )
        services.firstOrNull()?.serviceInfo?.packageName
    }.getOrNull()

    private fun speakNow(number: Int): Boolean {
        val engine = tts ?: return false
        if (!ready) return false
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        }
        return runCatching {
            val result = engine.speak(
                number.toString(),
                TextToSpeech.QUEUE_FLUSH,
                params,
                "countdown_$number"
            )
            Log.i(VOICE_TAG, "TTS speak number=$number result=$result")
            result == TextToSpeech.SUCCESS
        }.getOrDefault(false)
    }

    fun speak(number: Int) {
        prepare()
        if (!speakNow(number)) {
            pendingNumber = number
            Log.i(VOICE_TAG, "TTS not ready, pending number=$number")
        }
    }

    fun release() {
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
        pendingNumber = null
    }
}

actual fun prepareCountdownVoice() {
    runCatching { CountdownVoice.prepare() }
}

actual fun speakCountdownNumber(number: Int) {
    runCatching { CountdownVoice.speak(number) }
}

actual fun releaseCountdownVoice() {
    runCatching { CountdownVoice.release() }
}
