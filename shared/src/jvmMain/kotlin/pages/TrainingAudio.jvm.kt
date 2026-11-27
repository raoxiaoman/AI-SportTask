package pages

import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.FloatControl
import javax.sound.sampled.LineEvent

/**
 * 桌面端训练音频播放器。
 *
 * 与 Android / iOS 完全一致：全部片段都是打包进 App 的离线音频
 * （秒针、钟声为 CC0 音效，中文播报为构建期用 Tingting 预生成的语音 wav），
 * 运行时只做「预加载 + 播放」，**不调用系统 TTS**。
 * 早期版本用 macOS `say` 实时合成，进程启动/销毁会带来播报卡顿与截断；
 * v1.1.2 起改为直接播放与手机端同一批离线语音。
 *
 * 调度规则同样对齐 Android 的 SoundPool：
 * - 每个事件只把自己的片段按 delay 排程叠加播放，事件之间不互斥、不清场，
 *   所以秒针不会掐断正在播的语音，长钟声也不会被后续事件停掉；
 * - 只有暂停 / 退出（stop）才统一作废旧排程并停止正在播放的片段。
 */
private object DesktopAudioPlayer {
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "sporttask-audio").apply { isDaemon = true }
    }

    /** 预加载的 PCM 字节，消除首次播放时的资源查找与解码开销 */
    private val clipBytes = ConcurrentHashMap<String, ByteArray>()

    /** 每次 stop 递增；已排程但未执行的任务据此自行作废，不需要维护 future 列表 */
    private val epoch = AtomicLong(0)

    private val activeClips = mutableListOf<Clip>()

    private fun loadBytes(name: String): ByteArray? = clipBytes[name] ?: runCatching {
        javaClass.classLoader.getResourceAsStream("audio/$name.wav")?.use { it.readBytes() }
    }.getOrNull()?.also { clipBytes[name] = it }

    fun prepare() {
        // 进入执行页时把所有片段读进内存；失败也不影响播放侧的懒加载兜底
        trainingAudioClips.forEach { loadBytes(it) }
    }

    private fun playClip(step: TrainingAudioStep) {
        val bytes = loadBytes(step.clip) ?: return
        runCatching {
            val clip = AudioSystem.getClip()
            AudioSystem.getAudioInputStream(ByteArrayInputStream(bytes)).use { raw ->
                clip.open(raw)
            }
            // 音量：volume 0..1 线性映射到该混音器支持的分贝范围
            runCatching {
                val gain = clip.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
                val target = -40f + 40f * step.volume.coerceIn(0f, 1f)
                gain.value = target.coerceIn(gain.minimum, gain.maximum)
            }
            // 变速（部分混音器不支持，失败则忽略）
            if (step.rate != 1f) {
                runCatching {
                    val rate = clip.getControl(FloatControl.Type.SAMPLE_RATE) as FloatControl
                    rate.value = (rate.value * step.rate).toFloat()
                }
            }
            synchronized(this) { activeClips.add(clip) }
            clip.addLineListener { evt ->
                if (evt.type == LineEvent.Type.STOP) {
                    synchronized(this) { activeClips.remove(clip) }
                    runCatching { clip.close() }
                }
            }
            clip.start()
        }
    }

    fun play(event: TrainingAudioEvent, voiceAllowed: Boolean) {
        val myEpoch = epoch.get()
        trainingAudioSteps(event, voiceAllowed).forEach { step ->
            scheduler.schedule({
                if (epoch.get() == myEpoch) playClip(step)
            }, step.delayMs, TimeUnit.MILLISECONDS)
        }
    }

    fun stop() {
        epoch.incrementAndGet()
        synchronized(this) {
            activeClips.toList().forEach { runCatching { it.stop() } }
            activeClips.clear()
        }
    }

    fun release() = stop()
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
