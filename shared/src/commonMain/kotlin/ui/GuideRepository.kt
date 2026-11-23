package ui

import data.remote.ApiClient
import kotlinx.serialization.Serializable

/** GET /api/guides 响应：图解资源由服务端统一下发，内置库仅作离线兜底 */
@Serializable
data class GuideListResponse(
    val guides: List<ExerciseGuide> = emptyList(),
    val version: Int = 1
)

/**
 * 动作图解仓库。
 *
 * 设计要点（与模板库 TemplateRepository 同范式，但更强调「无空态」）：
 * - 内存里始终持有一份可用图解：启动时用内置库初始化，永远不为 null。
 * - [load] 异步拉取服务端版本，成功后整份替换内存 map（服务端为准）；
 *   失败/离线时静默保留内置库，UI 全程有图解，不会出现短暂空白。
 * - [find] 同步读取内存 map，签名与原 guideFor 完全一致，三个调用点无需改动。
 * - 服务端可只下发它想覆盖/新增的图解；未下发的动作继续用内置项（合并而非替换）。
 */
object GuideRepository {

    private val api = ApiClient()

    /** 当前生效的图解表：内置初始化，load 成功后合并服务端数据 */
    @Volatile
    private var current: Map<String, ExerciseGuide> = allBuiltInGuides().associateBy { it.name }

    /** 最近一次数据来源，便于调试/UI 标识 */
    @Volatile
    var fromServer: Boolean = false
        private set

    @Volatile
    var version: Int = 1
        private set

    /**
     * 拉取服务端图解并与内置库合并。服务端项覆盖同名内置项，内置项保留作为兜底。
     * 返回是否真的合并了服务端数据（成功且非空）。
     */
    suspend fun load(): Boolean {
        val response = api.guides().getOrNull() ?: return false
        if (response.guides.isEmpty()) return false
        val remote = response.guides
            .filter { it.name.isNotBlank() }
            .associateBy { it.name }
        if (remote.isEmpty()) return false
        // 合并：内置为底，服务端覆盖同名项 + 补齐新动作
        current = current + remote
        version = response.version
        fromServer = true
        return true
    }

    /** 同步查找：先精确匹配，再做去空格归一化匹配（对 key 也归一化，兼容两侧空格差异） */
    fun find(name: String): ExerciseGuide? {
        current[name]?.let { return it }
        val normalized = name.replace(" ", "")
        current[normalized]?.let { return it }
        return current.entries.firstOrNull { it.key.replace(" ", "") == normalized }?.value
    }

    /** 仅供测试：用一组图解重置内存仓库 */
    internal fun resetForTest(guides: List<ExerciseGuide>, server: Boolean = false) {
        current = guides.associateBy { it.name }
        fromServer = server
    }
}
