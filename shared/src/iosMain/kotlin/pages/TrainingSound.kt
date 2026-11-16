package pages

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

/**
 * iOS 震动反馈。
 *
 * iOS 没有「震动 XX 毫秒」的公开 API，只能用 Taptic Engine 的触感生成器，
 * 因此按手感而不是时长与 Android 对齐：
 * - short（Android 90ms）  → 轻触感：开始、暂停、倒数最后一拍；
 * - medium（Android 260ms）→ 中等触感：进入休息、休息结束、切换动作；
 * - long（Android 450ms）  → 重触感 + 成功提示：整套训练完成，最“有分量”。
 *
 * 生成器在首次使用时创建并常驻；每次触发前再 prepare 一次，把延迟压到最低。
 * 静音键、低电量模式下系统会自行抑制触感，这里不需要也不应该做额外判断；
 * iPad 与模拟器没有 Taptic Engine，调用会被系统安静忽略（不报错）。
 */
private object PlatformHaptics {
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val heavy = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private val notification = UINotificationFeedbackGenerator()

    private fun fire(block: () -> Unit) {
        runCatching(block)
    }

    fun short() = fire {
        light.prepare()
        light.impactOccurred()
    }

    fun medium() = fire {
        medium.prepare()
        medium.impactOccurred()
    }

    /** 先一记重触感，再补系统「成功」双拍，凑出比 medium 明显更长、更重的完成感 */
    fun long() = fire {
        heavy.prepare()
        heavy.impactOccurred()
        dispatch_after(
            dispatch_time(DISPATCH_TIME_NOW, 160L * 1_000_000L),
            dispatch_get_main_queue()
        ) {
            fire {
                notification.prepare()
                notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
            }
        }
    }
}

actual fun vibrateShort() = PlatformHaptics.short()

actual fun vibrateMedium() = PlatformHaptics.medium()

actual fun vibrateLong() = PlatformHaptics.long()
