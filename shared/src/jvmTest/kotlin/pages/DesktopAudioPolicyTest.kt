package pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 桌面端语音与秒针冲突的回归测试。
 *
 * 用户反馈：Mac 上语音播报（尤其「时间过半」中间提醒）会和倒计时钟声/秒针叠响、
 * 且播报被拦腰截断。根因是秒针与语音共用互斥通道，下一秒秒针清场时 destroy 了
 * 正在说话的 say 进程。这里锁死修复后的调度策略。
 */
class DesktopAudioPolicyTest {

    @Test
    fun `秒针事件被识别为独立通道`() {
        assertTrue(DesktopAudioPolicy.isTickEvent(TrainingAudioEvent.TICK))
        assertTrue(DesktopAudioPolicy.isTickEvent(TrainingAudioEvent.TICK_ACCENT))
        assertFalse(DesktopAudioPolicy.isTickEvent(TrainingAudioEvent.HALFWAY))
        assertFalse(DesktopAudioPolicy.isTickEvent(TrainingAudioEvent.START))
    }

    @Test
    fun `暂停反馈不受语音静音窗口影响`() {
        assertTrue(DesktopAudioPolicy.isPauseEvent(TrainingAudioEvent.PAUSE))
        assertFalse(DesktopAudioPolicy.isPauseEvent(TrainingAudioEvent.TICK))
    }

    @Test
    fun `含长钟声的事件把语音延后到钟声播完`() {
        // START 原为 chime(0ms) + 语音(260ms)，chime 实测 1.0s，必须让语音延后
        val steps = DesktopAudioPolicy.stepsFor(TrainingAudioEvent.START, voiceAllowed = true)
        val chime = steps.first { it.clip == "chime" }
        val voice = steps.first { it.isVoice }
        assertEquals(0L, chime.delayMs)
        assertTrue(
            voice.delayMs >= DesktopAudioPolicy.CHIME_TAIL_MS,
            "语音应在 1s 钟声结束后才开始，实际 delay=${voice.delayMs}"
        )
    }

    @Test
    fun `关闭语音时不再有语音步骤`() {
        val steps = DesktopAudioPolicy.stepsFor(TrainingAudioEvent.COMPLETE, voiceAllowed = false)
        assertTrue(steps.none { it.isVoice })
        assertEquals(1, steps.size)
    }

    @Test
    fun `无钟声的中间提醒保持语音立即播放`() {
        val steps = DesktopAudioPolicy.stepsFor(TrainingAudioEvent.HALFWAY, voiceAllowed = true)
        assertEquals(1, steps.size)
        assertTrue(steps[0].isVoice)
        assertEquals(0L, steps[0].delayMs)
    }

    @Test
    fun `语音静音窗口覆盖下一秒秒针`() {
        // 「时间过半」实测约 0.89s + say 启动延迟，窗口必须 > 1s，
        // 否则下一秒的秒针会在播报中途响起并把它掐断
        val halfMute = DesktopAudioPolicy.voiceMuteMs(
            DesktopAudioPolicy.stepsFor(TrainingAudioEvent.HALFWAY, true).first()
        )
        assertTrue(halfMute > 1000L, "中间提醒的静音窗口应跨过下一秒 tick，实际=$halfMute")

        val now = 10_000L
        val muteUntil = now + halfMute
        assertTrue(
            DesktopAudioPolicy.tickSuppressed(now + 1000L, muteUntil),
            "下一秒秒针应让路"
        )
        assertFalse(
            DesktopAudioPolicy.tickSuppressed(now + halfMute + 1L, muteUntil),
            "窗口过后秒针应恢复"
        )
    }

    @Test
    fun `最后5秒数字播报窗口也跨过下一秒`() {
        val mute = DesktopAudioPolicy.voiceMuteMs(
            DesktopAudioPolicy.stepsFor(TrainingAudioEvent.NUM_5, true).first()
        )
        assertTrue(mute > 1000L, "数字播报窗口应跨过下一秒 tick，实际=$mute")
    }

    @Test
    fun `静音窗口结束后秒针恢复`() {
        assertFalse(DesktopAudioPolicy.tickSuppressed(20_000L, 15_000L))
        assertTrue(DesktopAudioPolicy.tickSuppressed(14_999L, 15_000L))
    }

    @Test
    fun `所有阶段事件语音都在钟声之后`() {
        listOf(
            TrainingAudioEvent.START,
            TrainingAudioEvent.RESUME,
            TrainingAudioEvent.REST_END,
            TrainingAudioEvent.NEXT_ACTION,
            TrainingAudioEvent.COMPLETE
        ).forEach { event ->
            val steps = DesktopAudioPolicy.stepsFor(event, voiceAllowed = true)
            val chime = steps.firstOrNull { it.clip == "chime" }
            val voice = steps.firstOrNull { it.isVoice }
            if (chime != null && voice != null) {
                assertTrue(
                    voice.delayMs > chime.delayMs + 1000L,
                    "$event 的语音未延后到钟声之后"
                )
            }
        }
    }
}
