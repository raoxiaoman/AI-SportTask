package cloud

import data.remote.ServerConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 后端 API 客户端 — 连接自托管后端
 * 地址运行时读取 data.remote.ServerConfig.baseUrl
 */
object ApiClient {
    private fun baseUrl(path: String): String = "${ServerConfig.baseUrl}$path"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /** 当前认证 token */
    var authToken: String? = null

    private val authHeader: String get() = "Bearer ${authToken ?: ""}"

    // ========== Auth ==========

    suspend fun signup(email: String, password: String): AuthResponse {
        return withContext(Dispatchers.Default) {
            val response = client.post(baseUrl("/api/auth/signup")) {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }
            response.body<AuthResponse>()
        }
    }

    suspend fun signin(email: String, password: String): AuthResponse {
        return withContext(Dispatchers.Default) {
            val response = client.post(baseUrl("/api/auth/signin")) {
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(email, password))
            }
            response.body<AuthResponse>()
        }
    }

    // ========== Sync ==========

    suspend fun getFullData(): SyncData {
        return withContext(Dispatchers.Default) {
            val response = client.get(baseUrl("/api/sync/full")) {
                header("Authorization", authHeader)
            }
            response.body<SyncData>()
        }
    }

    suspend fun push(groups: List<PushGroup>, actions: List<PushAction>, checkins: List<PushCheckin>): PushResult {
        return withContext(Dispatchers.Default) {
            val response = client.post(baseUrl("/api/sync/push")) {
                contentType(ContentType.Application.Json)
                header("Authorization", authHeader)
                setBody(PushRequest(groups, actions, checkins))
            }
            response.body<PushResult>()
        }
    }

    suspend fun pull(lastSyncTimestamp: String? = null): SyncData {
        return withContext(Dispatchers.Default) {
            val response = client.post(baseUrl("/api/sync/pull")) {
                contentType(ContentType.Application.Json)
                header("Authorization", authHeader)
                setBody(PullRequest(lastSyncTimestamp))
            }
            response.body<SyncData>()
        }
    }
}

// ========== 数据类 ==========

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val user: RemoteUser? = null,
    val message: String? = null
)

@Serializable
data class RemoteUser(val id: Int, val email: String)

@Serializable
data class PullRequest(val lastSyncTimestamp: String? = null)

@Serializable
data class PushRequest(
    val groups: List<PushGroup>,
    val actions: List<PushAction>,
    val checkins: List<PushCheckin>
)

@Serializable
data class PushGroup(
    val local_id: Long? = null,
    val id: String? = null,
    val name: String,
    val created_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class PushAction(
    val local_id: Long? = null,
    val id: String? = null,
    val group_id: String? = null,
    val name: String,
    val steps_text: String? = "",
    val default_time: Int? = 30,
    val rest_time: Int? = 10,
    val order_index: Int? = 1,
    val created_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class PushCheckin(
    val local_id: Long? = null,
    val id: String? = null,
    val date: String,
    val group_id: String? = null,
    val action_id: String? = null,
    val duration: Int? = 0,
    val is_completed: Int? = 1,
    val created_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class SyncData(
    val groups: List<RemoteGroup> = emptyList(),
    val actions: List<RemoteAction> = emptyList(),
    val checkins: List<RemoteCheckin> = emptyList(),
    val serverTimestamp: String = "",
    val count: Int = 0
)

@Serializable
data class RemoteGroup(
    val id: String,
    val user_id: Int? = null,
    val local_id: Long? = null,
    val name: String,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class RemoteAction(
    val id: String,
    val user_id: Int? = null,
    val local_id: Long? = null,
    val group_id: String? = null,
    val name: String,
    val steps_text: String? = "",
    val default_time: Int? = 30,
    val rest_time: Int? = 10,
    val order_index: Int? = 1,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class RemoteCheckin(
    val id: String,
    val user_id: Int? = null,
    val local_id: Long? = null,
    val date: String,
    val group_id: String? = null,
    val action_id: String? = null,
    val duration: Int? = 0,
    val is_completed: Int? = 1,
    val created_at: String? = null,
    val updated_at: String? = null,
    val deleted_at: String? = null
)

@Serializable
data class PushResult(
    val success: Boolean,
    val groupsImported: Int = 0,
    val actionsImported: Int = 0,
    val checkinsImported: Int = 0,
    val serverTimestamp: String? = null
)
