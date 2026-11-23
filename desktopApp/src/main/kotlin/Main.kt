import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import data.remote.JvmPlatformStorage
import data.remote.PlatformStorageProvider

/** 记住初始为竖屏手机比例的窗口尺寸 */
@Composable
private fun rememberDesktopWindowState(): WindowState =
    remember { WindowState(width = 460.dp, height = 860.dp) }

fun main() {
    // 与 Android MainActivity / iOS MainViewController 一致：入口先初始化平台存储
    PlatformStorageProvider.init(JvmPlatformStorage())

    application {
        // 应用沿用手机比例布局，用接近竖屏手机的窗口尺寸保证 UI 不变形；
        // 设最小尺寸避免被缩到内容截断。用户可自由放大窗口。
        val windowState = rememberDesktopWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "运动任务 · 康复训练"
        ) {
            App()
        }
    }
}
