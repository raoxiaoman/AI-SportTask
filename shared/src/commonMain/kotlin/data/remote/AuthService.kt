package data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 认证服务 — 管理登录状态和 Token 持久化
 */
@Serializable
data class AuthState(
    val email: String = "",
    val token: String = "",
    val isLoggedIn: Boolean = false,
    val userId: Long? = null
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private const val PREFS_KEY = "sporttask_auth_state"

        fun fromJson(jsonStr: String): AuthState {
            return try {
                json.decodeFromString(jsonStr)
            } catch (_: Exception) {
                AuthState()
            }
        }

        fun toJson(state: AuthState): String = json.encodeToString(state)
    }
}

class AuthService(private val apiClient: ApiClient = ApiClient()) {

    private val storage by lazy { PlatformStorageProvider.get() }

    // 内存中的当前认证状态
    var currentState: AuthState = AuthState()
        private set

    /**
     * 尝试从本地存储恢复登录状态
     */
    fun restoreSession(): Boolean {
        val raw = storage.load("sporttask_auth_state") ?: return false
        currentState = AuthState.fromJson(raw)
        return currentState.isLoggedIn
    }

    /**
     * 注册
     */
    suspend fun signup(email: String, password: String): Result<AuthState> {
        return withContext(Dispatchers.Default) {
            apiClient.signup(email, password).map { response ->
                val state = AuthState(
                    email = response.user?.email ?: email,
                    token = response.token ?: "",
                    isLoggedIn = true,
                    userId = response.user?.id
                )
                saveState(state)
                state
            }
        }
    }

    /**
     * 登录
     */
    suspend fun signin(email: String, password: String): Result<AuthState> {
        return withContext(Dispatchers.Default) {
            apiClient.signin(email, password).map { response ->
                val state = AuthState(
                    email = response.user?.email ?: email,
                    token = response.token ?: "",
                    isLoggedIn = true,
                    userId = response.user?.id
                )
                saveState(state)
                state
            }
        }
    }

    /**
     * 退出登录
     */
    fun logout() {
        currentState = AuthState()
        storage.remove("sporttask_auth_state")
    }

    val isLoggedIn: Boolean get() = currentState.isLoggedIn
    val token: String get() = currentState.token
    val userEmail: String get() = currentState.email

    private fun saveState(state: AuthState) {
        currentState = state
        storage.save("sporttask_auth_state", AuthState.toJson(state))
    }
}
