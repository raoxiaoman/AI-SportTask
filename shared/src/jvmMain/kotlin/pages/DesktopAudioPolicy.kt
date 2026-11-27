package pages

/**
 * 桌面端音频调度策略（纯逻辑，可单测）。
 *
 * 背景：桌面端中文语音由 macOS `say` 实时合成，进程启动 + 播报通常持续
 * 0.4~1.0s，而倒计时秒针每秒触发一次。旧实现里秒针与语音共用「新事件清场」
 * 通道，导致下一秒的秒针 destroy 掉正在说话的 say 进程，播报被拦腰截断；
 * 同时阶段事件的 1s 长钟声与仅延迟 260ms 的语音会叠在一起。
 *
 * 三条策略：
 * 1. 秒针事件不参与语音互斥，且在语音播报窗口内自动让路；
 * 2. 含 chime 的事件把语音延后到钟声播完，避免长钟声与播报叠响；
 * 3. 播报类事件之间仍互斥（新播报掐掉旧播报），避免多句叠播。
 *
 * 只用于 JVM 桌面端；Android/iOS 播放的是预生成短音频，行为不受影响。
 */
internal object DesktopAudioPolicy {
    /** chime.wav 实测 1.0s，留 50ms 间隙 */
    const val CHIME_TAIL_MS = 1050L

    /** say 进程启动到出声的经验延迟 */
    const val SAY_STARTUP_MS = 300L

    /** 播报结束后的静音余量，用于覆盖下一秒秒针的触发点 */
    const val VOICE_GUARD_MS = 420L

    /** 各语音片段实测播报时长（say -v Tingting -r 235），略留冗余 */
    private val voiceDurations = mapOf(
        "v1" to 400L, "v2" to 400L, "v3" to 400L, "v4" to 400L, "v5" to 400L,
        "v_skip" to 500L,
        "v_start" to 950L, "v_resume" to 950L, "v_rest" to 950L,
        "v_rest_end" to 950L, "v_half" to 950L,
        "v_next" to 1000L, "v_complete" to 1050L
    )

    /** 纯秒针事件：不参与播报互斥，且播报期间应让路 */
    fun isTickEvent(event: TrainingAudioEvent): Boolean =
        event == TrainingAudioEvent.TICK || event == TrainingAudioEvent.TICK_ACCENT

    /** 暂停反馈：短音效，不受播报静音窗口影响（暂停时本就没有倒计时） */
    fun isPauseEvent(event: TrainingAudioEvent): Boolean =
        event == TrainingAudioEvent.PAUSE

    /** 事件重排：钟声未播完时把语音延后到钟声结束，避免叠响 */
    fun stepsFor(event: TrainingAudioEvent, voiceAllowed: Boolean): List<TrainingAudioStep> {
        val steps = trainingAudioSteps(event, voiceAllowed)
        if (steps.none { it.clip == "chime" } || steps.none { it.isVoice }) return steps
        return steps.map { step ->
            if (step.isVoice) step.copy(delayMs = maxOf(step.delayMs, CHIME_TAIL_MS)) else step
        }
    }

    /** 单条语音需要的静音窗口长度（从事件触发时刻算起） */
    fun voiceMuteMs(step: TrainingAudioStep): Long =
        step.delayMs + SAY_STARTUP_MS + (voiceDurations[step.clip] ?: 950L) + VOICE_GUARD_MS

    /** 此刻秒针是否应让路 */
    fun tickSuppressed(nowMs: Long, muteUntilMs: Long): Boolean = nowMs < muteUntilMs
}
