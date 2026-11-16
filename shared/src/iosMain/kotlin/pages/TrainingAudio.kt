@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package pages

import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private const val AUDIO_TAG = "SportTaskAudio"

/**
 * iOS 端用 AVAudioPlayer 播放 App 内置的短音频，行为与 Android 的 SoundPool 对齐：
 * - 进入执行页时一次性把 15 个片段读进内存并 prepareToPlay，播放零初始化延迟；
 * - 一个片段一个播放器实例，音效与语音可以同时出声（chime + 播报）；
 * - 同一片段重复触发时从头重播（秒针每秒一次因此不会堆积）；
 * - 音效通过 rate / volume 做变调与音量层次，不依赖系统 TTS。
 *
 * 音频资源放在 App bundle 的 `Audio/` 目录（Xcode 里以文件夹引用方式加入），
 * 与 androidApp/src/androidMain/res/raw 由同一个脚本生成，保证双端一致。
 *
 * 会话策略：`.playback` + `.mixWithOthers`
 * - 手机静音键打开时仍能播报（训练场景几乎必然静音）；
 * - 不打断用户正在听的音乐，也不申请独占（与 Android 不抢音频焦点一致）。
 *
 * 线程约定：所有调用都来自 Compose 主线程，延迟片段也调度到主队列，
 * 因此内部可变状态无需额外加锁。
 */
private object TrainingAudioPlayer {
    private var sessionReady = false
    private val players = mutableMapOf<String, AVAudioPlayer>()
    private val missingClips = mutableSetOf<String>()

    /** 每次 stop/release 递增，让仍在队列里的延迟片段自动失效 */
    private var generation = 0L

    private fun log(message: String) = println("$AUDIO_TAG $message")

    private fun ensureSession() {
        if (sessionReady) return
        sessionReady = true
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                withOptions = AVAudioSessionCategoryOptionMixWithOthers,
                error = null
            )
            session.setActive(true, error = null)
            log("session ready category=playback mixWithOthers")
        }.onFailure { log("session setup failed: ${it.message}") }
    }

    /** 懒加载单个片段；找不到文件时只记一次日志，不反复尝试 */
    private fun player(clip: String): AVAudioPlayer? {
        players[clip]?.let { return it }
        if (clip in missingClips) return null

        val path = NSBundle.mainBundle.pathForResource(clip, ofType = "mp3", inDirectory = "Audio")
        if (path == null) {
            missingClips.add(clip)
            log("clip missing: Audio/$clip.mp3")
            return null
        }
        return runCatching {
            val url = NSURL.fileURLWithPath(path)
            // AVAudioPlayer(contentsOfURL:error:)：用 memScoped 接住 NSError，便于定位坏文件
            val player = memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                AVAudioPlayer(url, error.ptr)
            }
            player.enableRate = true
            player.numberOfLoops = 0
            player.prepareToPlay()
            players[clip] = player
            player
        }.onFailure { log("load failed Audio/$clip.mp3: ${it.message}") }.getOrNull()
    }

    private fun schedule(delayMs: Long, block: () -> Unit) {
        if (delayMs <= 0L) {
            block()
            return
        }
        val token = generation
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, delayMs * 1_000_000L),
            dispatch_get_main_queue()
        ) {
            if (token == generation) block()
        }
    }

    private fun playStep(step: TrainingAudioStep) {
        val player = player(step.clip) ?: return
        runCatching {
            player.currentTime = 0.0
            player.volume = step.volume
            player.enableRate = true
            player.rate = step.rate
            val started = player.play()
            // 部分 iOS 版本要等 play() 之后才应用 rate，补一次确保变调真的生效
            if (step.rate != 1f && started) player.rate = step.rate
            log("play clip=${step.clip} started=$started rate=${step.rate} vol=${step.volume}")
        }.onFailure { log("play failed ${step.clip}: ${it.message}") }
    }

    fun prepare() {
        ensureSession()
        trainingAudioClips.forEach { player(it) }
        log("ready clips=${players.size}/${trainingAudioClips.size} missing=${missingClips.size}")
    }

    fun play(event: TrainingAudioEvent, voiceAllowed: Boolean) {
        ensureSession()
        trainingAudioSteps(event, voiceAllowed).forEach { step ->
            schedule(step.delayMs) { playStep(step) }
        }
        log("event=$event voiceAllowed=$voiceAllowed")
    }

    fun stop() {
        generation++
        players.forEach { (clip, player) ->
            runCatching {
                if (player.playing) player.stop()
                player.currentTime = 0.0
            }.onFailure { log("stop failed $clip: ${it.message}") }
        }
    }

    fun release() {
        stop()
        players.clear()
        missingClips.clear()
        runCatching {
            AVAudioSession.sharedInstance().setActive(
                false,
                withOptions = AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation,
                error = null
            )
        }
        sessionReady = false
        log("released")
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
