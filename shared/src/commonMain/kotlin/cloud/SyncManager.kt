package cloud

/**
 * 同步管理器 — 负责本地 ↔ 云端数据同步
 *
 * v0.3.0: 框架搭建阶段
 * - 定义了 SyncState 和同步接口
 * - 具体同步逻辑随 Supabase 集成逐步实现
 *
 * 同步策略：
 * - 每次数据操作后自动触发增量同步
 * - 先 Push 本地变更，再 Pull 云端变更
 * - 冲突采用 Last-Write-Wins（最后修改时间覆盖）
 */
object SyncManager {

    /** 同步状态 */
    sealed class SyncState {
        data object Idle : SyncState()
        data object Syncing : SyncState()
        data class Success(val timestamp: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    /** 当前同步状态 */
    private var _state: SyncState = SyncState.Idle
    val state: SyncState get() = _state

    /** 最后同步时间 */
    private var lastSyncTimestamp: String? = null

    /** 是否正在同步 */
    val isSyncing: Boolean get() = _state is SyncState.Syncing

    /** 上次同步是否成功 */
    val lastSyncSuccess: Boolean get() = _state is SyncState.Success

    /**
     * 触发增量同步
     * TODO: v0.3.2 实现具体的 Push/Pull 逻辑
     */
    suspend fun sync() {
        if (isSyncing) return
        _state = SyncState.Syncing

        try {
            // Step 1: Push 本地变更
            pushLocalChanges()

            // Step 2: Pull 云端变更
            pullRemoteChanges()

            _state = SyncState.Success(lastSyncTimestamp ?: "just now")
        } catch (e: Exception) {
            _state = SyncState.Error(e.message ?: "同步失败")
        }
    }

    /**
     * 上传本地 pending 数据到云端
     * TODO: 遍历 pending_operations 表依次上传
     */
    private suspend fun pushLocalChanges() {
        // v0.3.1: 实现上传逻辑
    }

    /**
     * 从云端拉取增量变更到本地
     * TODO: 查询 lastSyncTimestamp 后的云端变更并合并
     */
    private suspend fun pullRemoteChanges() {
        // v0.3.1: 实现下载逻辑
    }

    /**
     * 标记本地记录需要同步
     * TODO: 写入 pending_operations 表
     */
    suspend fun markDirty(table: String, recordId: Long, operationType: String) {
        // v0.3.1: 实现脏标记逻辑
    }

    /**
     * 首次全量同步
     * 注册后首次登录时执行
     */
    suspend fun initialSync() {
        // v0.3.1: 实现首次同步逻辑
    }
}
