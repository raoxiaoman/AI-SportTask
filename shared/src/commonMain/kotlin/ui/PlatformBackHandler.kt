package ui

import androidx.compose.runtime.Composable

/**
 * 拦截系统返回键，让它和页面左上角的返回按钮走同一套逻辑。
 * enabled=false 时不拦截（例如动作说明弹窗打开时，先让弹窗自行关闭）。
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
