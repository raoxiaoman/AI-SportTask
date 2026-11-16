package pages

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.Foundation.NSBundle

/**
 * 在 iOS 模拟器上真跑音频层：验证 NSBundle 能从 App 包的 Audio/ 目录取到片段、
 * AVAudioPlayer 能全部创建成功。此测试的可执行文件需与 Audio/ 目录同层
 * （本地验证时手动把 iosApp/iosApp/Audio 拷到 test.kexe 旁边）。
 */
class TrainingAudioIosTest {

    @Test
    fun stepsMappingCoversAllEvents() {
        TrainingAudioEvent.entries.forEach { event ->
            val steps = trainingAudioSteps(event, voiceAllowed = true)
            assertTrue(steps.isNotEmpty(), "事件 $event 展开为空")
            steps.forEach { step ->
                assertTrue(step.clip in trainingAudioClips, "事件 $event 引用了未知片段 ${step.clip}")
            }
        }
    }

    @Test
    fun voiceDisabledStripsVoiceSteps() {
        val withVoice = trainingAudioSteps(TrainingAudioEvent.START, voiceAllowed = true)
        val withoutVoice = trainingAudioSteps(TrainingAudioEvent.START, voiceAllowed = false)
        assertEquals(listOf("chime"), withoutVoice.map { it.clip })
        assertTrue(withVoice.size > withoutVoice.size)
    }

    @Test
    fun audioBundleLookupResolvesAudioDirectory() {
        // 测试可执行文件的 bundle 根目录（Audio/ 需被拷到同一层）
        val bundlePath = NSBundle.mainBundle.bundlePath
        val found = trainingAudioClips.count { clip ->
            NSBundle.mainBundle.pathForResource(clip, ofType = "mp3", inDirectory = "Audio") != null
        }
        println("SportTaskAudio bundlePath=$bundlePath found=$found/${trainingAudioClips.size}")
        assertEquals(trainingAudioClips.size, found, "Audio/ 目录中有片段无法通过 NSBundle 找到")
    }

    @Test
    fun prepareAndPlayAllEventsDoesNotThrow() {
        prepareTrainingAudio()
        TrainingAudioEvent.entries.forEach { event ->
            playTrainingAudio(event, voiceAllowed = true)
        }
        stopTrainingAudio()
        releaseTrainingAudio()
    }
}
