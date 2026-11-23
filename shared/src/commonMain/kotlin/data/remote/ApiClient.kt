package data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import data.plan.TemplateListResponse
import ui.GuideListResponse

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
    val checkins: List<SyncCheckin>? = null,
    /** 客户端本地状态（方案 SchemeState 等），键见 SyncKeys */
    val kv: Map<String, String>? = null
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
    val kv: Map<String, String> = emptyMap(),
    val serverTimestamp: String = "",
    val count: Int = 0
)

/** 同步的 KV 键：方案状态 */
object SyncKeys {
    const val SCHEME_STATE = "scheme_state_v1"
    /** 标记某账号已在本设备完成过首次自动落地，避免每次启动都用云端覆盖本地新数据 */
    fun autoSyncedFlag(userId: String) = "auto_synced_$userId"
}

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

/**
 * 云端分组。字段与 backend/db.js 的 cloud_groups 表一一对应：
 * id 是云端主键（由客户端按本地 id 稳定生成），local_id 保存本地自增 id 以便回溯。
 */
@Serializable
data class SyncGroup(
    val id: String? = null,
    @SerialName("local_id") val localId: Long? = null,
    val name: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SyncAction(
    val id: String? = null,
    @SerialName("local_id") val localId: Long? = null,
    /** 所属分组的**云端 id**，不是本地 id */
    @SerialName("group_id") val groupId: String? = null,
    val name: String = "",
    @SerialName("steps_text") val stepsText: String = "",
    @SerialName("default_time") val defaultTime: Int = 30,
    @SerialName("rest_time") val restTime: Int = 10,
    @SerialName("order_index") val orderIndex: Int = 1,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SyncCheckin(
    val id: String? = null,
    @SerialName("local_id") val localId: Long? = null,
    val date: String = "",
    /** 云端 id（分组/动作） */
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("action_id") val actionId: String? = null,
    val duration: Int = 0,
    @SerialName("is_completed") val isCompleted: Int = 1,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class HealthResponse(
    val status: String = "",
    val version: String = ""
)

/** 模板 DTO 定义在 data.plan.TrainingTemplate，网络层直接复用，避免两套结构漂移 */

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
        // 未配置服务器或离线时快速失败，让模板库能立即回退内置数据
        install(HttpTimeout) {
            connectTimeoutMillis = 2_000
            requestTimeoutMillis = 5_000
            socketTimeoutMillis = 5_000
        }
    }

    private fun url(path: String): String = "${ServerConfig.baseUrl}$path"

    // ===== Auth =====

    suspend fun health(): Result<HealthResponse> = runCatching {
        client.get(url("/api/health")).body<HealthResponse>()
    }

    /** 公开接口，无需登录；离线时调用方回退到内置模板 */
    suspend fun templates(): Result<TemplateListResponse> = runCatching {
        val response = client.get(url("/api/templates"))
        if (response.status.value !in 200..299) {
            throw Exception("获取模板列表失败 (${response.status})")
        }
        response.body<TemplateListResponse>()
    }

    /** 公开接口，无需登录；离线/失败时调用方回退到内置图解 */
    suspend fun guides(): Result<GuideListResponse> = runCatching {
        val response = client.get(url("/api/guides"))
        if (response.status.value !in 200..299) {
            throw Exception("获取动作图解失败 (${response.status})")
        }
        response.body<GuideListResponse>()
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
