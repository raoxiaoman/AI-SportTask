package ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

/** 可变回调持有者：Composable 重组时原地更新，不改变在栈中的位置 */
private class BackHolder(var enabled: Boolean, var onBack: () -> Unit)

/**
 * 桌面端全局返回分发器：用 AWT KeyEventDispatcher 拦截 ESC。
 * 维护一个页面栈，只有栈顶（最上层页面）且 enabled 时响应，
 * 与 Android BackHandler 的语义一致。
 */
private object DesktopBackDispatcher {
    private val stack = ArrayDeque<BackHolder>()
    private var installed = false

    fun push(holder: BackHolder) {
        stack.addLast(holder)
        install()
    }

    fun remove(holder: BackHolder) {
        stack.remove(holder)
    }

    private fun install() {
        if (installed) return
        installed = true
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher { event ->
            if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE) {
                val top = stack.lastOrNull()
                if (top != null && top.enabled) {
                    top.onBack()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }
}

/**
 * 桌面端把 ESC 键当作系统返回键。
 * enabled=false 时不拦截（例如动作说明弹窗打开时，先让弹窗自行关闭）。
 */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val holder = remember { BackHolder(enabled, onBack) }
    holder.enabled = enabled
    holder.onBack = onBack
    DisposableEffect(Unit) {
        DesktopBackDispatcher.push(holder)
        onDispose { DesktopBackDispatcher.remove(holder) }
    }
}
