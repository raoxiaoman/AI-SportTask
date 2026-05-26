package cloud

import data.LoginResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 认证服务 — 管理登录状态
 *
 * 连接自托管后端 (VPS 23.94.233.92:3456)。
 * 支持注册、登录、登出，成功后保存 token 到 ApiClient。
 */
object AuthService {

    sealed class AuthState {
        data object SignedOut : AuthState()
        data object Loading : AuthState()
        data class SignedIn(val email: String, val userId: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isSignedIn: Boolean get() = _authState.value is AuthState.SignedIn

    /**
     * 注册新用户 → 调用后端 API
     */
    suspend fun signUp(email: String, password: String): LoginResult {
        _authState.value = AuthState.Loading

        return try {
            val response = ApiClient.signup(email, password)
            if (response.success && response.token != null) {
                ApiClient.authToken = response.token
                _authState.value = AuthState.SignedIn(
                    email = email,
                    userId = response.user?.id?.toString() ?: "unknown"
                )
                LoginResult(true, null)
            } else {
                val err = response.message ?: "注册失败"
                _authState.value = AuthState.Error(err)
                LoginResult(false, err)
            }
        } catch (e: Exception) {
            val err = e.message ?: "网络错误，请检查连接"
            _authState.value = AuthState.Error(err)
            LoginResult(false, err)
        }
    }

    /**
     * 登录 → 调用后端 API
     */
    suspend fun signIn(email: String, password: String): LoginResult {
        _authState.value = AuthState.Loading

        return try {
            val response = ApiClient.signin(email, password)
            if (response.success && response.token != null) {
                ApiClient.authToken = response.token
                _authState.value = AuthState.SignedIn(
                    email = email,
                    userId = response.user?.id?.toString() ?: "unknown"
                )
                LoginResult(true, null)
            } else {
                val err = response.message ?: "登录失败"
                _authState.value = AuthState.Error(err)
                LoginResult(false, err)
            }
        } catch (e: Exception) {
            val err = e.message ?: "网络错误，请检查连接"
            _authState.value = AuthState.Error(err)
            LoginResult(false, err)
        }
    }

    /**
     * 退出登录
     */
    suspend fun signOut() {
        _authState.value = AuthState.Loading
        ApiClient.authToken = null
        _authState.value = AuthState.SignedOut
    }

    /** 当前登录用户的邮箱 */
    val currentEmail: String? get() {
        val state = _authState.value
        return if (state is AuthState.SignedIn) state.email else null
    }
}
