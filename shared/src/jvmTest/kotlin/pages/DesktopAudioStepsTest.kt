package pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 桌面端音频回归测试。
 *
 * v1.1.1 曾把语音延后到钟声之后、并在播报期间屏蔽秒针，结果语音卡顿、
 * 「到时间」的长钟声又被后续事件的清场停掉。v1.1.2 起桌面端与手机端完全一致：
 * 所有片段都是打包进 App 的离线 wav，运行时不调用系统 TTS；
 * 事件只按 commonMain 的原始步骤叠加排程，不做额外延后或静音窗口。
 */
class DesktopAudioStepsTest {

    @Test
    fun `开始事件保持钟声与原有的语音延迟`() {
        val steps = trainingAudioSteps(TrainingAudioEvent.START, voiceAllowed = true)
        assertEquals(2, steps.size)
        assertEquals("chime", steps[0].clip)
        assertEquals(0L, steps[0].delayMs)
        assertTrue(steps[1].isVoice)
        assertEquals("v_start", steps[1].clip)
        assertEquals(260L, steps[1].delayMs, "语音不应被延后到钟声之后")
    }

    @Test
    fun `完成事件语音仍为420ms`() {
        val steps = trainingAudioSteps(TrainingAudioEvent.COMPLETE, voiceAllowed = true)
        assertEquals("chime", steps[0].clip)
        assertTrue(steps.last().isVoice)
        assertEquals(420L, steps.last().delayMs)
    }

    @Test
    fun `中间提醒只有离线语音且立即播放`() {
        val steps = trainingAudioSteps(TrainingAudioEvent.HALFWAY, voiceAllowed = true)
        assertEquals(1, steps.size)
        assertEquals("v_half", steps[0].clip)
        assertTrue(steps[0].isVoice)
        assertEquals(0L, steps[0].delayMs)
    }

    @Test
    fun `秒针与暂停事件只有音效不含语音`() {
        listOf(
            TrainingAudioEvent.TICK,
            TrainingAudioEvent.TICK_ACCENT,
            TrainingAudioEvent.PAUSE
        ).forEach { event ->
            val steps = trainingAudioSteps(event, voiceAllowed = true)
            assertTrue(steps.none { it.isVoice }, "$event 不应携带语音")
            assertTrue(steps.all { it.clip == "tick" }, "$event 应只播秒针音效")
        }
    }

    @Test
    fun `关闭语音时只保留音效`() {
        val steps = trainingAudioSteps(TrainingAudioEvent.COMPLETE, voiceAllowed = false)
        assertTrue(steps.none { it.isVoice })
        assertEquals(1, steps.size)
        assertEquals("chime", steps[0].clip)
    }

    @Test
    fun `所有语音片段都在离线资源清单内`() {
        val voices = enumValues<TrainingAudioEvent>()
            .flatMap { trainingAudioSteps(it, voiceAllowed = true) }
            .filter { it.isVoice }
            .map { it.clip }
            .toSet()
        assertEquals(13, voices.size)
        assertTrue(
            trainingAudioClips.containsAll(voices),
            "以下语音缺少本地资源：${voices - trainingAudioClips.toSet()}"
        )
    }
}
