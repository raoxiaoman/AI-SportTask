package data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ========== 请求/响应模型 ==========

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val user: UserInfo? = null,
    val message: String? = null,
    val error: String? = null
)

@Serializable
data class UserInfo(
    val id: Long? = null,
    val email: String? = null
)

@Serializable
data class SyncPushRequest(
    val groups: List<SyncGroup>? = null,
    val actions: List<SyncAction>? = null,
    val checkins: List<SyncCheckin>? = null
)

@Serializable
data class SyncPullRequest(
    val lastSyncTimestamp: String? = null
)

@Serializable
data class SyncPullResponse(
    val groups: List<SyncGroup> = emptyList(),
    val actions: List<SyncAction> = emptyList(),
    val checkins: List<SyncCheckin> = emptyList(),
    val serverTimestamp: String = "",
    val count: Int = 0
)

@Serializable
data class SyncPushResponse(
    val success: Boolean = false,
    val groupsImported: Int = 0,
    val actionsImported: Int = 0,
    val checkinsImported: Int = 0,
    val serverTimestamp: String = ""
)

@Serializable
data class SyncFullResponse(
    val groups: List<SyncGroup> = emptyList(),
    val actions: List<SyncAction> = emptyList(),
    val checkins: List<SyncCheckin> = emptyList(),
    val serverTimestamp: String = ""
)

@Serializable
data class SyncGroup(
    val id: String? = null,
    @SerialName("local_id") val localId: String? = null,
    val name: String = "",
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SyncAction(
    val id: String? = null,
    @SerialName("group_id") val groupId: String = "",
    val name: String = "",
    val unit: String = "次",
    @SerialName("sort_order") val sortOrder: Int = 0,
    val target: Int = 0,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SyncCheckin(
    val id: String? = null,
    @SerialName("action_id") val actionId: String = "",
    val count: Int = 0,
    val date: String = "",
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class HealthResponse(
    val status: String = "",
    val version: String = ""
)

@Serializable
data class ErrorResponse(
    val error: String? = null,
    val message: String? = null
)

// ========== ApiClient ==========

class ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun url(path: String): String = "${ServerConfig.baseUrl}$path"

    // ===== Auth =====

    suspend fun health(): Result<HealthResponse> = runCatching {
        client.get(url("/api/health")).body<HealthResponse>()
    }

    suspend fun signup(email: String, password: String): Result<AuthResponse> = runCatching {
        val response = client.post(url("/api/auth/signup")) {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, password))
        }
        if (response.status.value !in 200..299) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw Exception(err?.error ?: err?.message ?: "注册失败 (${response.status})")
        }
        response.body<AuthResponse>()
    }

    suspend fun signin(email: String, password: String): Result<AuthResponse> = runCatching {
        val response = client.post(url("/api/auth/signin")) {
            contentType(ContentType.Application.Json)
            setBody(AuthRequest(email, password))
        }
        if (response.status.value !in 200..299) {
            val err = try { response.body<ErrorResponse>() } catch (_: Exception) { null }
            throw Exception(err?.error ?: err?.message ?: "登录失败 (${response.status})")
        }
        response.body<AuthResponse>()
    }

    // ===== Sync =====

    suspend fun pull(token: String, lastSyncTimestamp: String? = null): Result<SyncPullResponse> = runCatching {
        val response = client.post(url("/api/sync/pull")) {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(SyncPullRequest(lastSyncTimestamp))
        }
        if (response.status.value !in 200..299) {
            throw Exception("同步拉取失败 (${response.status})")
        }
        response.body<SyncPullResponse>()
    }

    suspend fun push(token: String, request: SyncPushRequest): Result<SyncPushResponse> = runCatching {
        val response = client.post(url("/api/sync/push")) {
            contentType(ContentType.Application.Json)
            bearerAuth(token)
            setBody(request)
        }
        if (response.status.value !in 200..299) {
            throw Exception("同步推送失败 (${response.status})")
        }
        response.body<SyncPushResponse>()
    }

    suspend fun fullSync(token: String): Result<SyncFullResponse> = runCatching {
        val response = client.get(url("/api/sync/full")) {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("全量同步失败 (${response.status})")
        }
        response.body<SyncFullResponse>()
    }

    suspend fun getMe(token: String): Result<UserInfo> = runCatching {
        val response = client.get(url("/api/user")) {
            bearerAuth(token)
        }
        if (response.status.value !in 200..299) {
            throw Exception("获取用户信息失败 (${response.status})")
        }
        response.body<UserInfo>()
    }

    fun close() {
        client.close()
    }
}
