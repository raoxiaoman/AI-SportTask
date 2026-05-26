package cloud

import data.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 认证服务 — 管理登录状态、Token 持久化
 *
 * v0.3.0：本地模拟版，Supabase 集成后替换为真实的 Auth API。
 * 当前实现纯本地状态，不依赖网络，方便并行开发 UI。
 */
object AuthService {

    /** 登录状态 */
    sealed class AuthState {
        /** 未登录 */
        data object SignedOut : AuthState()

        /** 登录中 */
        data object Loading : AuthState()

        /** 已登录 */
        data class SignedIn(val email: String, val userId: String) : AuthState()

        /** 错误 */
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** 是否已登录 */
    val isSignedIn: Boolean get() = _authState.value is AuthState.SignedIn

    /**
     * 注册新用户
     * TODO: v0.3.1 接入 supabase-kt Auth
     */
    suspend fun signUp(email: String, password: String): LoginResult {
        _authState.value = AuthState.Loading

        // 模拟网络延迟
        kotlinx.coroutines.delay(800)

        return if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("邮箱和密码不能为空")
            LoginResult(false, "邮箱和密码不能为空")
        } else if (password.length < 6) {
            _authState.value = AuthState.Error("密码长度至少 6 位")
            LoginResult(false, "密码长度至少 6 位")
        } else {
            _authState.value = AuthState.SignedIn(
                email = email,
                userId = "local_${email.hashCode()}"
            )
            LoginResult(true, null)
        }
    }

    /**
     * 登录
     * TODO: v0.3.1 接入 supabase-kt Auth
     */
    suspend fun signIn(email: String, password: String): LoginResult {
        _authState.value = AuthState.Loading

        // 模拟网络延迟
        kotlinx.coroutines.delay(600)

        return if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("邮箱和密码不能为空")
            LoginResult(false, "邮箱和密码不能为空")
        } else {
            _authState.value = AuthState.SignedIn(
                email = email,
                userId = "local_${email.hashCode()}"
            )
            LoginResult(true, null)
        }
    }

    /**
     * 退出登录
     */
    suspend fun signOut() {
        _authState.value = AuthState.Loading
        kotlinx.coroutines.delay(300)
        _authState.value = AuthState.SignedOut
    }
}
