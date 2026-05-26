package cloud

/**
 * 同步管理器 — 负责本地 ↔ 后端数据同步
 *
 * 连接自托管后端 (VPS 23.94.233.92:3456)
 * 策略：每次数据操作后自动推送变更 (Push)
 * 登录/手动触发时拉取云端变更 (Pull)
 */
object SyncManager {

    sealed class SyncState {
        data object Idle : SyncState()
        data object Syncing : SyncState()
        data class Success(val timestamp: String) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private var _state: SyncState = SyncState.Idle
    val state: SyncState get() = _state
    val isSyncing: Boolean get() = _state is SyncState.Syncing
    val lastSyncSuccess: Boolean get() = _state is SyncState.Success

    private var lastSyncTimestamp: String = ""
    private var _lastSyncSuccess: Boolean = false

    /**
     * 完整同步：先 Push 再 Pull
     * 每次数据操作后自动触发此方法
     */
    suspend fun sync() {
        if (isSyncing || !AuthService.isSignedIn) return
        _state = SyncState.Syncing

        try {
            // Push: 上传本地 pending 数据 (TODO: 从 pending_operations 表读取)
            pushLocalChanges()

            // Pull: 拉取云端增量
            val serverData = pullRemoteChanges()
            _lastSyncSuccess = true

            val msg = "同步成功 (${serverData.count} 条变更)"
            _state = SyncState.Success(msg)
            lastSyncTimestamp = serverData.serverTimestamp
        } catch (e: Exception) {
            _lastSyncSuccess = false
            _state = SyncState.Error(e.message ?: "同步失败")
        }
    }

    /**
     * 上传本地变更
     */
    private suspend fun pushLocalChanges() {
        // v0.3.1: 遍历 pending_operations 表
        // 当前发送空数据保持连接活跃
        ApiClient.push(emptyList(), emptyList(), emptyList())
    }

    /**
     * 拉取云端增量
     */
    private suspend fun pullRemoteChanges(): SyncData {
        val since = if (lastSyncTimestamp.isNotEmpty()) lastSyncTimestamp else null
        return ApiClient.pull(since)
    }

    /**
     * 标记本地记录需要同步
     * TODO: v0.3.1 写入 pending_operations 表
     */
    suspend fun markDirty(table: String, recordId: Long, operationType: String) {
        // 同时触发同步
        sync()
    }

    /**
     * 首次全量同步
     * 登录成功后调用
     */
    suspend fun initialSync() {
        if (!AuthService.isSignedIn) return
        _state = SyncState.Syncing

        try {
            val fullData = ApiClient.getFullData()
            lastSyncTimestamp = fullData.serverTimestamp
            _lastSyncSuccess = true
            _state = SyncState.Success("首次同步完成")
        } catch (e: Exception) {
            _state = SyncState.Error(e.message ?: "首次同步失败")
        }
    }
}
