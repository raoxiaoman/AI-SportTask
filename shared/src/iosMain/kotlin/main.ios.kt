import androidx.compose.ui.window.ComposeUIViewController
import data.remote.IosPlatformStorage
import data.remote.PlatformStorageProvider
import platform.UIKit.UIViewController

actual fun getPlatformName(): String = "iOS"

fun MainViewController(): UIViewController {
    // 对齐 Android MainActivity：入口处初始化平台存储（NSUserDefaults）。
    // 之前缺失导致 App 一进主界面就抛 IllegalStateException 闪退。
    PlatformStorageProvider.init(IosPlatformStorage())
    return ComposeUIViewController { App() }
}
