package pages

/**
 * 训练倒计时的语音播报（5 / 4 / 3 / 2 / 1）。
 * 进入执行页时预热，离开时释放；引擎初始化是异步的，未就绪时静默跳过，不影响计时。
 */
expect fun prepareCountdownVoice()

/** 播报一个倒数数字，跟随媒体音量 */
expect fun speakCountdownNumber(number: Int)

/** 离开执行页时释放语音引擎 */
expect fun releaseCountdownVoice()
