package pages

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import db.appContext
import db.isDbContextReady

private const val AUDIO_TAG = "SportTaskAudio"

/**
 * Android 端用 SoundPool 预加载 App 内置的短音频。
 *
 * 与旧的 ToneGenerator + TextToSpeech 相比：
 * - 全部片段在进入执行页时解码到内存，播放零初始化延迟；
 * - 中文播报是离线预生成的真人语音 mp3，不受设备 TTS 引擎/语音包影响；
 * - 音效与语音按事件组合排队，暂停时统一取消排队并停止流。
 *
 * 音频走 USAGE_MEDIA，跟随媒体音量；不申请音频焦点，不打断背景音乐。
 * 「事件 → 片段」的映射见 commonMain 的 [trainingAudioSteps]。
 */
private object TrainingAudioPlayer {
    private val handler = Handler(Looper.getMainLooper())
    private var pool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private val loaded = mutableSetOf<String>()
    private val activeStreams = mutableSetOf<Int>()
    private val scheduled = mutableSetOf<Runnable>()

    /** 所有状态读写都收敛到主线程，避免 SoundPool 回调与 UI 线程竞争 */
    private fun ensureInit(): SoundPool? {
        pool?.let { return it }
        if (!isDbContextReady()) return null
        return runCatching {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val sp = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(attributes)
                .build()
            sp.setOnLoadCompleteListener { _, sampleId, status ->
                handler.post {
                    if (status == 0) {
                        soundIds.entries.firstOrNull { it.value == sampleId }?.key
                            ?.let { loaded.add(it) }
                    }
                }
            }
            pool = sp
            val resources = appContext.resources
            val packageName = appContext.packageName
            trainingAudioClips.forEach { name ->
                val resId = resources.getIdentifier(name, "raw", packageName)
                if (resId == 0) {
                    Log.w(AUDIO_TAG, "raw resource missing: $name")
                } else {
                    soundIds[name] = sp.load(appContext, resId, 1)
                }
            }
            Log.i(AUDIO_TAG, "SoundPool ready clips=${soundIds.size} package=$packageName")
            sp
        }.onFailure { Log.w(AUDIO_TAG, "SoundPool init failed: ${it.message}") }.getOrNull()
    }

    private fun schedule(delayMs: Long, block: () -> Unit) {
        lateinit var runnable: Runnable
        runnable = Runnable {
            scheduled.remove(runnable)
            block()
        }
        scheduled.add(runnable)
        handler.postDelayed(runnable, delayMs)
    }

    /** 资源尚未解码完成时短暂重试，保证冷启动 / 设置页试听都不静默 */
    private fun playStep(sp: SoundPool, step: TrainingAudioStep, attempt: Int = 0) {
        if (!loaded.contains(step.clip)) {
            if (attempt < 8) {
                schedule(90) { playStep(sp, step, attempt + 1) }
            } else {
                Log.w(AUDIO_TAG, "clip not loaded: ${step.clip}")
            }
            return
        }
        val soundId = soundIds[step.clip] ?: return
        val streamId = runCatching {
            sp.play(soundId, step.volume, step.volume, 1, 0, step.rate)
        }.onFailure { Log.w(AUDIO_TAG, "play failed ${step.clip}: ${it.message}") }
            .getOrDefault(0)
        if (streamId != 0) {
            activeStreams.add(streamId)
            Log.i(
                AUDIO_TAG,
                "play clip=${step.clip} stream=$streamId rate=${step.rate} vol=${step.volume}"
            )
        }
    }

    fun prepare() {
        handler.post { ensureInit() }
    }

    fun play(event: TrainingAudioEvent, voiceAllowed: Boolean) {
        handler.post {
            val sp = ensureInit() ?: return@post
            trainingAudioSteps(event, voiceAllowed).forEach { step ->
                schedule(step.delayMs) { playStep(sp, step) }
            }
            Log.i(AUDIO_TAG, "event=$event voiceAllowed=$voiceAllowed")
        }
    }

    fun stop() {
        handler.post {
            scheduled.toList().forEach { handler.removeCallbacks(it) }
            scheduled.clear()
            activeStreams.forEach { streamId -> runCatching { pool?.stop(streamId) } }
            activeStreams.clear()
        }
    }

    fun release() {
        handler.post {
            scheduled.toList().forEach { handler.removeCallbacks(it) }
            scheduled.clear()
            activeStreams.clear()
            runCatching { pool?.release() }
            pool = null
            soundIds.clear()
            loaded.clear()
            Log.i(AUDIO_TAG, "SoundPool released")
        }
    }
}

actual fun prepareTrainingAudio() {
    runCatching { TrainingAudioPlayer.prepare() }
}

actual fun playTrainingAudio(event: TrainingAudioEvent, voiceAllowed: Boolean) {
    runCatching { TrainingAudioPlayer.play(event, voiceAllowed) }
}

actual fun stopTrainingAudio() {
    runCatching { TrainingAudioPlayer.stop() }
}

actual fun releaseTrainingAudio() {
    runCatching { TrainingAudioPlayer.release() }
}
