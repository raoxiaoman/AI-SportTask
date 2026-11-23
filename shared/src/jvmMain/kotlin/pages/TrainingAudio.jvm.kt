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
 * 复用 commonMain 的 [trainingAudioSteps] 事件→片段映射，但按桌面版策略
 * 强制 voiceAllowed=false：只播放 tick / chime 等音效，中文语音片段（v*）全部跳过。
 * 这样无需系统 TTS、无需分发语音资源，跨平台表现稳定。
 *
 * 音效为打包进 resources/audio 的 wav，用 JVM 原生 Clip 播放；
 * rate（变速）通过 Clip 的 PLAYBACK_RATE 控制，不支持时退化为原速。
 */
private object DesktopAudioPlayer {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "sporttask-audio").apply { isDaemon = true }
    }
    @Volatile
    private var generation = 0
    private val pending = mutableListOf<ScheduledFuture<*>>()
    private val activeClips = mutableListOf<Clip>()

    /** 只保留音效片段：语音在桌面端降级为静音 */
    private fun stepsFor(event: TrainingAudioEvent): List<TrainingAudioStep> =
        trainingAudioSteps(event, voiceAllowed = false)

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

    fun play(event: TrainingAudioEvent) {
        val myGen = ++generation
        stopInternal(clearGeneration = false)
        stepsFor(event).forEach { step ->
            val future: ScheduledFuture<*> = scheduler.schedule({
                playClip(step, myGen)
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
        }
    }
}

actual fun prepareTrainingAudio() {
    runCatching { DesktopAudioPlayer.prepare() }
}

actual fun playTrainingAudio(event: TrainingAudioEvent, voiceAllowed: Boolean) {
    // 桌面端忽略 voiceAllowed，始终按静音语音策略处理
    runCatching { DesktopAudioPlayer.play(event) }
}

actual fun stopTrainingAudio() {
    runCatching { DesktopAudioPlayer.stop() }
}

actual fun releaseTrainingAudio() {
    runCatching { DesktopAudioPlayer.release() }
}
