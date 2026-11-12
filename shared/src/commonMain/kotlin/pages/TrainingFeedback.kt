package pages

import data.remote.PlatformStorageProvider

/**
 * 训练过程的声音 / 震动反馈总控。
 *
 * 设置页写入开关，执行页只读；存储不可用时回退默认值，不影响计时。
 * 音频全部走 [playTrainingAudio] 播放本地预置片段，不依赖系统 TTS。
 */
object TrainingFeedback {
    private const val KEY_SOUND = "training_sound_enabled"
    private const val KEY_VIBRATION = "training_vibration_enabled"
    private const val KEY_VOICE = "training_voice_enabled"
    private const val KEY_TICK = "training_countdown_tick_enabled"

    fun isSoundEnabled(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY_SOUND) != "false"
    }.getOrDefault(true)

    fun setSoundEnabled(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY_SOUND, enabled.toString()) }
    }

    fun isVibrationEnabled(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY_VIBRATION) == "true"
    }.getOrDefault(false)

    fun setVibrationEnabled(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY_VIBRATION, enabled.toString()) }
    }

    /** 是否播报中文语音（倒数数字 + 阶段提示），默认开启 */
    fun isVoiceEnabled(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY_VOICE) != "false"
    }.getOrDefault(true)

    fun setVoiceEnabled(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY_VOICE, enabled.toString()) }
    }

    /** 倒计时全程每秒一声木质秒针，默认开启 */
    fun isTickEnabled(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY_TICK) != "false"
    }.getOrDefault(true)

    fun setTickEnabled(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY_TICK, enabled.toString()) }
    }

    private fun play(event: TrainingAudioEvent, voice: Boolean = false) {
        if (!isSoundEnabled()) return
        runCatching { playTrainingAudio(event, voice && isVoiceEnabled()) }
    }

    private fun vibrate(block: () -> Unit) {
        if (isVibrationEnabled()) runCatching(block)
    }

    /** 首次进入训练：提示音 + 「开始训练」 */
    fun onStart() {
        play(TrainingAudioEvent.START, voice = true)
        vibrate { vibrateShort() }
    }

    /** 暂停后恢复：提示音 + 「继续训练」 */
    fun onResume() {
        play(TrainingAudioEvent.RESUME, voice = true)
        vibrate { vibrateShort() }
    }

    /** 暂停：先掐掉正在响的语音 / 排队片段，再给一声短反馈 */
    fun onPause() {
        runCatching { stopTrainingAudio() }
        play(TrainingAudioEvent.PAUSE)
        vibrate { vibrateShort() }
    }

    /** 手动跳过当前动作 / 休息 */
    fun onSkip() {
        play(TrainingAudioEvent.SKIP, voice = true)
        vibrate { vibrateShort() }
    }

    /**
     * 倒计时每过一秒调用一次，贯穿动作与休息阶段。
     * 秒针全程每秒一声，最后 3 秒换成强调音；最后 5 秒叠加中文数字播报。
     */
    fun onSecondElapsed(secondsLeft: Int) {
        if (isSoundEnabled()) {
            if (isTickEnabled()) {
                play(
                    if (secondsLeft <= 3) TrainingAudioEvent.TICK_ACCENT
                    else TrainingAudioEvent.TICK
                )
            }
            if (secondsLeft in 1..5 && isVoiceEnabled()) {
                val numberEvent = when (secondsLeft) {
                    5 -> TrainingAudioEvent.NUM_5
                    4 -> TrainingAudioEvent.NUM_4
                    3 -> TrainingAudioEvent.NUM_3
                    2 -> TrainingAudioEvent.NUM_2
                    else -> TrainingAudioEvent.NUM_1
                }
                play(numberEvent, voice = true)
            }
        }
        if (secondsLeft == 1) vibrate { vibrateShort() }
    }

    /** 兼容入口：等价于对应秒的 onSecondElapsed */
    fun onCountdown(secondsLeft: Int) = onSecondElapsed(secondsLeft)

    /** 兼容入口：等价于倒数到 1 */
    fun onFinalTick() = onSecondElapsed(1)

    /** 长动作过半提示（每个动作只触发一次） */
    fun onHalfway() = play(TrainingAudioEvent.HALFWAY, voice = true)

    /** 动作结束 → 休息 */
    fun onEnterRest() {
        play(TrainingAudioEvent.ENTER_REST, voice = true)
        vibrate { vibrateMedium() }
    }

    /** 休息结束 → 下一个动作 */
    fun onRestEnd() {
        play(TrainingAudioEvent.REST_END, voice = true)
        vibrate { vibrateMedium() }
    }

    /** 无休息，直接切下一个动作 */
    fun onNextAction() {
        play(TrainingAudioEvent.NEXT_ACTION, voice = true)
        vibrate { vibrateMedium() }
    }

    /** 兼容入口：单个阶段结束（只响提示音，避免与新流程的语音重复） */
    fun onPhaseEnd() {
        play(TrainingAudioEvent.NEXT_ACTION, voice = false)
        vibrate { vibrateMedium() }
    }

    /** 整套训练完成 */
    fun onComplete() {
        play(TrainingAudioEvent.COMPLETE, voice = true)
        vibrate { vibrateLong() }
    }

    /** 设置页试听：走与首次开始完全相同的音频组合 */
    fun preview() = play(TrainingAudioEvent.START, voice = true)
}
