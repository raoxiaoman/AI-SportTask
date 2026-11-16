package pages

/** 训练音频的「事件 → 播放片段」映射，三端共用。
 *
 * 单独成文件是为了避免与平台侧 TrainingAudio.kt 的 actual 实现
 * 生成同名 JVM 类（pages/TrainingAudioKt）而冲突。
 */
/**
 * 一个语义事件展开后的单个播放步骤。
 *
 * 三端共用同一份展开结果，避免「Android 调好了、iOS 还是旧行为」这类漂移。
 */
internal data class TrainingAudioStep(
    /** 与打包进 App 的音频文件名（不含扩展名）一一对应 */
    val clip: String,
    /** 播放速率，1f 为原速 */
    val rate: Float = 1f,
    val volume: Float = 1f,
    /** 相对事件触发时刻的延迟，用于「音效先响、语音随后」 */
    val delayMs: Long = 0L,
    /** 语音片段：关闭语音播报时整段跳过 */
    val isVoice: Boolean = false
)

/** 打包进 App 的全部音频片段（Android res/raw、iOS App bundle/Audio） */
internal val trainingAudioClips: List<String> = listOf(
    "tick", "chime", "v1", "v2", "v3", "v4", "v5",
    "v_start", "v_resume", "v_rest", "v_rest_end",
    "v_next", "v_half", "v_complete", "v_skip"
)

/** 把一个事件展开成有序的播放步骤；voiceAllowed=false 时剔除语音片段只留音效 */
internal fun trainingAudioSteps(
    event: TrainingAudioEvent,
    voiceAllowed: Boolean
): List<TrainingAudioStep> {
    val all: List<TrainingAudioStep> = when (event) {
        TrainingAudioEvent.TICK -> listOf(TrainingAudioStep("tick", volume = 0.62f))
        TrainingAudioEvent.TICK_ACCENT -> listOf(TrainingAudioStep("tick", rate = 1.35f, volume = 1f))
        TrainingAudioEvent.NUM_5 -> listOf(TrainingAudioStep("v5", isVoice = true))
        TrainingAudioEvent.NUM_4 -> listOf(TrainingAudioStep("v4", isVoice = true))
        TrainingAudioEvent.NUM_3 -> listOf(TrainingAudioStep("v3", isVoice = true))
        TrainingAudioEvent.NUM_2 -> listOf(TrainingAudioStep("v2", isVoice = true))
        TrainingAudioEvent.NUM_1 -> listOf(TrainingAudioStep("v1", isVoice = true))
        TrainingAudioEvent.START -> listOf(
            TrainingAudioStep("chime", volume = 0.8f),
            TrainingAudioStep("v_start", delayMs = 260, isVoice = true)
        )
        TrainingAudioEvent.RESUME -> listOf(
            TrainingAudioStep("chime", rate = 1.05f, volume = 0.6f),
            TrainingAudioStep("v_resume", delayMs = 220, isVoice = true)
        )
        TrainingAudioEvent.PAUSE -> listOf(TrainingAudioStep("tick", rate = 0.78f, volume = 0.7f))
        TrainingAudioEvent.SKIP -> listOf(
            TrainingAudioStep("tick", rate = 1.1f, volume = 0.85f),
            TrainingAudioStep("v_skip", delayMs = 120, isVoice = true)
        )
        TrainingAudioEvent.ENTER_REST -> listOf(
            TrainingAudioStep("tick", rate = 1.2f, volume = 0.9f),
            TrainingAudioStep("v_rest", delayMs = 140, isVoice = true)
        )
        TrainingAudioEvent.REST_END -> listOf(
            TrainingAudioStep("chime", rate = 1.12f, volume = 0.7f),
            TrainingAudioStep("v_rest_end", delayMs = 260, isVoice = true)
        )
        TrainingAudioEvent.NEXT_ACTION -> listOf(
            TrainingAudioStep("chime", rate = 1.12f, volume = 0.7f),
            TrainingAudioStep("v_next", delayMs = 260, isVoice = true)
        )
        TrainingAudioEvent.HALFWAY -> listOf(TrainingAudioStep("v_half", isVoice = true))
        TrainingAudioEvent.COMPLETE -> listOf(
            TrainingAudioStep("chime", volume = 0.95f),
            TrainingAudioStep("v_complete", delayMs = 420, isVoice = true)
        )
    }
    return if (voiceAllowed) all else all.filterNot { it.isVoice }
}
