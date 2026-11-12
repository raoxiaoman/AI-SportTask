package pages

/**
 * 训练音频事件。
 *
 * 全部对应打包在 App 内的本地 mp3：秒针、提示音来自 CC0 音效，
 * 中文倒数与阶段播报为构建期离线预生成的语音文件。
 * 运行时只做「预加载 + 播放」，**不再调用系统 TTS**，避免引擎初始化卡顿、
 * 冷启动静默、机型语音包缺失等问题。
 *
 * 平台层负责把一个事件展开成一个或多个音频片段，并处理排队、去重与释放。
 */
enum class TrainingAudioEvent {
    /** 每秒一次的木质秒针 */
    TICK,

    /** 最后 3 秒的强调秒针 */
    TICK_ACCENT,
    NUM_5,
    NUM_4,
    NUM_3,
    NUM_2,
    NUM_1,

    /** 首次开始训练 */
    START,

    /** 暂停后恢复 */
    RESUME,

    /** 暂停 */
    PAUSE,

    /** 手动跳过 */
    SKIP,

    /** 动作结束，进入休息 */
    ENTER_REST,

    /** 休息结束，准备下一个动作 */
    REST_END,

    /** 无休息，直接切换下一个动作 */
    NEXT_ACTION,

    /** 动作时间过半 */
    HALFWAY,

    /** 整套训练完成 */
    COMPLETE
}

/** 进入执行页时预加载音频；未预热时播放侧也会懒加载 */
expect fun prepareTrainingAudio()

/** 播放一个事件；voiceAllowed=false 时只播音效、跳过语音片段 */
expect fun playTrainingAudio(event: TrainingAudioEvent, voiceAllowed: Boolean)

/** 暂停 / 退出时立即停止正在播放与排队中的片段 */
expect fun stopTrainingAudio()

/** 离开执行页时释放播放器 */
expect fun releaseTrainingAudio()
