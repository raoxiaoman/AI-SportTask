package ui

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 全局轻提示：由 MainAppScreen 的 Scaffold 挂载 SnackbarHost，业务层调用 show() 即可。
 *
 * 关键点：用自管理的全局 scope，而非调用方的 Composable scope。
 * 否则「先关闭对话框、再弹提示」时，对话框销毁会取消其 scope，导致 Snackbar 被一起取消而不显示。
 */
object AppToast {
    private var hostState: SnackbarHostState? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun attach(state: SnackbarHostState) { hostState = state }
    fun detach() { hostState = null }

    fun show(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        val state = hostState ?: return
        scope.launch {
            state.showSnackbar(message, actionLabel, duration = duration)
        }
    }
}
