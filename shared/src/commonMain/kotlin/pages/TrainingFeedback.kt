package pages

import data.remote.PlatformStorageProvider

/**
 * 训练过程的声音 / 震动反馈总控。
 * 设置页写入开关，执行页只读；存储不可用时回退到默认值，不影响计时。
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

    /** 最后 5 秒是否用语音读出数字，默认开启；受「声音提示」总开关约束 */
    fun isVoiceEnabled(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY_VOICE) != "false"
    }.getOrDefault(true)

    fun setVoiceEnabled(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY_VOICE, enabled.toString()) }
    }

    /** 最后 5 秒每秒一声「时钟滴答」，默认开启；与语音互不影响 */
    fun isTickEnabled(): Boolean = runCatching {
        PlatformStorageProvider.get().load(KEY_TICK) != "false"
    }.getOrDefault(true)

    fun setTickEnabled(enabled: Boolean) {
        runCatching { PlatformStorageProvider.get().save(KEY_TICK, enabled.toString()) }
    }

    private fun sound(block: () -> Unit) {
        if (isSoundEnabled()) runCatching(block)
    }

    private fun vibrate(block: () -> Unit) {
        if (isVibrationEnabled()) runCatching(block)
    }

    /** 开始 / 恢复 / 重来 */
    fun onStart() {
        sound { playStartSound() }
        vibrate { vibrateShort() }
    }

    /** 暂停 */
    fun onPause() {
        sound { playPauseSound() }
        vibrate { vibrateShort() }
    }

    /**
     * 倒计时每过一秒调用一次，贯穿整个动作 / 休息阶段。
     * 秒针声全程每秒一声；最后 5 秒额外用语音读出数字；
     * 最后一秒换成更明显的提示音并震动。
     * 秒针与语音彼此独立，TTS 引擎未就绪也不影响秒针声。
     */
    fun onSecondElapsed(secondsLeft: Int) {
        val lastSecond = secondsLeft == 1
        if (isSoundEnabled()) {
            if (isTickEnabled()) {
                runCatching {
                    if (lastSecond) playCountdownSound() else playCountdownTickSound()
                }
            }
            if (secondsLeft in 1..5 && isVoiceEnabled()) {
                runCatching { speakCountdownNumber(secondsLeft) }
            }
        }
        if (lastSecond) vibrate { vibrateShort() }
    }

    /** 兼容入口：等价于对应秒的 onSecondElapsed */
    fun onCountdown(secondsLeft: Int) = onSecondElapsed(secondsLeft)

    /** 兼容入口：等价于倒数到 1 */
    fun onFinalTick() = onSecondElapsed(1)

    /** 单个动作或休息结束 */
    fun onPhaseEnd() {
        sound { playEndSound() }
        vibrate { vibrateMedium() }
    }

    /** 整套训练完成 */
    fun onComplete() {
        sound { playEndSound() }
        vibrate { vibrateLong() }
    }
}
