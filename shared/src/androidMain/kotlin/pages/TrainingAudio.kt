package pages

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import db.appContext
import db.isDbContextReady

private const val AUDIO_TAG = "SportTaskAudio"

/** 一个语义事件展开后的单个播放步骤 */
private data class AudioStep(
    val resName: String,
    val rate: Float = 1f,
    val volume: Float = 1f,
    val delayMs: Long = 0L,
    val isVoice: Boolean = false
)

/**
 * Android 端用 SoundPool 预加载 App 内置的短音频。
 *
 * 与旧的 ToneGenerator + TextToSpeech 相比：
 * - 全部片段在进入执行页时解码到内存，播放零初始化延迟；
 * - 中文播报是离线预生成的真人语音 mp3，不受设备 TTS 引擎/语音包影响；
 * - 音效与语音按事件组合排队，暂停时统一取消排队并停止流。
 *
 * 音频走 USAGE_MEDIA，跟随媒体音量；不申请音频焦点，不打断背景音乐。
 */
private object TrainingAudioPlayer {
    private val handler = Handler(Looper.getMainLooper())
    private var pool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private val loaded = mutableSetOf<String>()
    private val activeStreams = mutableSetOf<Int>()
    private val scheduled = mutableSetOf<Runnable>()

    /** 与 androidApp/src/androidMain/res/raw 下的 mp3 一一对应 */
    private val clips = listOf(
        "tick", "chime", "v1", "v2", "v3", "v4", "v5",
        "v_start", "v_resume", "v_rest", "v_rest_end",
        "v_next", "v_half", "v_complete", "v_skip"
    )

    private fun steps(event: TrainingAudioEvent, voiceAllowed: Boolean): List<AudioStep> {
        val all: List<AudioStep> = when (event) {
            TrainingAudioEvent.TICK -> listOf(AudioStep("tick", volume = 0.62f))
            TrainingAudioEvent.TICK_ACCENT -> listOf(AudioStep("tick", rate = 1.35f, volume = 1f))
            TrainingAudioEvent.NUM_5 -> listOf(AudioStep("v5", isVoice = true))
            TrainingAudioEvent.NUM_4 -> listOf(AudioStep("v4", isVoice = true))
            TrainingAudioEvent.NUM_3 -> listOf(AudioStep("v3", isVoice = true))
            TrainingAudioEvent.NUM_2 -> listOf(AudioStep("v2", isVoice = true))
            TrainingAudioEvent.NUM_1 -> listOf(AudioStep("v1", isVoice = true))
            TrainingAudioEvent.START -> listOf(
                AudioStep("chime", volume = 0.8f),
                AudioStep("v_start", delayMs = 260, isVoice = true)
            )
            TrainingAudioEvent.RESUME -> listOf(
                AudioStep("chime", rate = 1.05f, volume = 0.6f),
                AudioStep("v_resume", delayMs = 220, isVoice = true)
            )
            TrainingAudioEvent.PAUSE -> listOf(AudioStep("tick", rate = 0.78f, volume = 0.7f))
            TrainingAudioEvent.SKIP -> listOf(
                AudioStep("tick", rate = 1.1f, volume = 0.85f),
                AudioStep("v_skip", delayMs = 120, isVoice = true)
            )
            TrainingAudioEvent.ENTER_REST -> listOf(
                AudioStep("tick", rate = 1.2f, volume = 0.9f),
                AudioStep("v_rest", delayMs = 140, isVoice = true)
            )
            TrainingAudioEvent.REST_END -> listOf(
                AudioStep("chime", rate = 1.12f, volume = 0.7f),
                AudioStep("v_rest_end", delayMs = 260, isVoice = true)
            )
            TrainingAudioEvent.NEXT_ACTION -> listOf(
                AudioStep("chime", rate = 1.12f, volume = 0.7f),
                AudioStep("v_next", delayMs = 260, isVoice = true)
            )
            TrainingAudioEvent.HALFWAY -> listOf(AudioStep("v_half", isVoice = true))
            TrainingAudioEvent.COMPLETE -> listOf(
                AudioStep("chime", volume = 0.95f),
                AudioStep("v_complete", delayMs = 420, isVoice = true)
            )
        }
        return if (voiceAllowed) all else all.filterNot { it.isVoice }
    }

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
            clips.forEach { name ->
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
    private fun playStep(sp: SoundPool, step: AudioStep, attempt: Int = 0) {
        if (!loaded.contains(step.resName)) {
            if (attempt < 8) {
                schedule(90) { playStep(sp, step, attempt + 1) }
            } else {
                Log.w(AUDIO_TAG, "clip not loaded: ${step.resName}")
            }
            return
        }
        val soundId = soundIds[step.resName] ?: return
        val streamId = runCatching {
            sp.play(soundId, step.volume, step.volume, 1, 0, step.rate)
        }.onFailure { Log.w(AUDIO_TAG, "play failed ${step.resName}: ${it.message}") }
            .getOrDefault(0)
        if (streamId != 0) {
            activeStreams.add(streamId)
            Log.i(
                AUDIO_TAG,
                "play clip=${step.resName} stream=$streamId rate=${step.rate} vol=${step.volume}"
            )
        }
    }

    fun prepare() {
        handler.post { ensureInit() }
    }

    fun play(event: TrainingAudioEvent, voiceAllowed: Boolean) {
        handler.post {
            val sp = ensureInit() ?: return@post
            steps(event, voiceAllowed).forEach { step ->
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
