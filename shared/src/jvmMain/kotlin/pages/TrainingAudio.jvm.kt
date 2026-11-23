package pages

import java.io.BufferedInputStream
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl

/**
 * 桌面端训练音频播放器。
 *
 * 复用 commonMain 的 [trainingAudioSteps] 事件→片段映射：
 * - 音效（tick / chime）用打包进 resources/audio 的 wav，通过 JVM 原生 Clip 播放；
 * - 中文语音（v*）用 macOS 内置 `say` 实时合成播报，无需分发语音资源。
 *
 * 语音是否播放遵循上层 voiceAllowed（设置页的语音开关）；
 * 非 macOS 平台没有可用的系统 TTS，自动降级为只播音效。
 */
private object DesktopVoice {
    /** 与 tools/generate_training_audio.sh 的语音文案保持一致 */
    private val texts = mapOf(
        "v5" to "五",
        "v4" to "四",
        "v3" to "三",
        "v2" to "二",
        "v1" to "一",
        "v_start" to "开始训练",
        "v_resume" to "继续训练",
        "v_rest" to "休息一下",
        "v_rest_end" to "休息结束",
        "v_next" to "下一个动作",
        "v_half" to "时间过半",
        "v_complete" to "训练完成",
        "v_skip" to "跳过"
    )

    private val isMac = System.getProperty("os.name", "")
        .contains("Mac", ignoreCase = true)

    /** 返回 false 表示该片段没有可播报的语音或平台不支持 TTS */
    fun speak(clip: String, myGen: Int, isCurrent: () -> Boolean): Boolean {
        val text = texts[clip] ?: return false
        if (!isMac) return false
        Thread {
            runCatching {
                if (!isCurrent()) return@runCatching
                // GUI 启动的 app PATH 可能很精简，用绝对路径确保找到系统 TTS
                val process = ProcessBuilder("/usr/bin/say", "-v", "Tingting", "-r", "235", text)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start()
                DesktopAudioPlayer.registerSpeech(process, myGen)
                try {
                    process.waitFor()
                } finally {
                    DesktopAudioPlayer.unregisterSpeech(process)
                }
            }
        }.apply { isDaemon = true }.start()
        return true
    }
}

private object DesktopAudioPlayer {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "sporttask-audio").apply { isDaemon = true }
    }
    @Volatile
    private var generation = 0
    private val pending = mutableListOf<ScheduledFuture<*>>()
    private val activeClips = mutableListOf<Clip>()
    private val activeSpeech = mutableListOf<Pair<Process, Int>>()

    /** 按上层语音开关展开事件：voiceAllowed=true 时语音片段与音效一起排程 */
    private fun stepsFor(event: TrainingAudioEvent, voiceAllowed: Boolean): List<TrainingAudioStep> =
        trainingAudioSteps(event, voiceAllowed)

    fun registerSpeech(process: Process, myGen: Int) {
        synchronized(this) {
            if (myGen == generation) activeSpeech.add(process to myGen)
            else runCatching { process.destroy() }
        }
    }

    fun unregisterSpeech(process: Process) {
        synchronized(this) { activeSpeech.removeAll { it.first === process } }
    }

    private fun playClip(step: TrainingAudioStep, myGen: Int) {
        if (myGen != generation) return
        runCatching {
            val inStream = javaClass.classLoader.getResourceAsStream("audio/${step.clip}.wav")
                ?: return
            val bin = BufferedInputStream(inStream)
            val raw: AudioInputStream = AudioSystem.getAudioInputStream(bin)
            val clip = AudioSystem.getClip()
            clip.open(raw)
            // 音量：volume 0..1 线性映射到该混音器支持的分贝范围
            runCatching {
                val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val min = gain.javaClass.getMethod("getMinimumValue").invoke(gain) as Float
                val max = gain.javaClass.getMethod("getMaximumValue").invoke(gain) as Float
                val target = -40f + 40f * step.volume.coerceIn(0f, 1f)
                gain.value = target.coerceIn(min, max)
            }
            // 变速（部分混音器不支持，失败则忽略）
            if (step.rate != 1f) {
                runCatching {
                    val rate = clip.getControl(FloatControl.Type.SAMPLE_RATE) as FloatControl
                    rate.value = (rate.value * step.rate).toFloat()
                }
            }
            synchronized(this) { activeClips.add(clip) }
            clip.start()
            clip.addLineListener { evt ->
                if (evt.type == javax.sound.sampled.LineEvent.Type.STOP) {
                    synchronized(this) { activeClips.remove(clip) }
                    runCatching { clip.close() }
                }
            }
        }
    }

    fun play(event: TrainingAudioEvent, voiceAllowed: Boolean) {
        val myGen = ++generation
        stopInternal(clearGeneration = false)
        stepsFor(event, voiceAllowed).forEach { step ->
            val future: ScheduledFuture<*> = scheduler.schedule({
                if (step.isVoice) {
                    DesktopVoice.speak(step.clip, myGen) { generation == myGen }
                } else {
                    playClip(step, myGen)
                }
            }, step.delayMs, TimeUnit.MILLISECONDS)
            synchronized(this) { pending.add(future) }
        }
    }

    fun prepare() {
        // JVM Clip 无需预热，首次播放加载开销可忽略；保留空实现对齐接口
    }

    fun stop() {
        generation++
        stopInternal(clearGeneration = true)
    }

    fun release() {
        stop()
    }

    private fun stopInternal(clearGeneration: Boolean) {
        synchronized(this) {
            pending.forEach { it.cancel(false) }
            pending.clear()
            activeClips.toList().forEach { runCatching { it.stop() } }
            activeClips.clear()
            activeSpeech.toList().forEach { runCatching { it.first.destroy() } }
            activeSpeech.clear()
        }
    }
}

actual fun prepareTrainingAudio() {
    runCatching { DesktopAudioPlayer.prepare() }
}

actual fun playTrainingAudio(event: TrainingAudioEvent, voiceAllowed: Boolean) {
    runCatching { DesktopAudioPlayer.play(event, voiceAllowed) }
}

actual fun stopTrainingAudio() {
    runCatching { DesktopAudioPlayer.stop() }
}

actual fun releaseTrainingAudio() {
    runCatching { DesktopAudioPlayer.release() }
}
