package ui

import androidx.compose.runtime.Composable

@Suppress("UNUSED_PARAMETER")
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS 无物理返回键，暂不处理
}
