package data.remote

import data.CloudApplyResult
import data.SportTaskRepository
import data.plan.SchemeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地 ↔ 云端同步。
 *
 * 之前的「手动同步」只调了 pull，一个字节都没上传，界面却显示「同步成功」。
 * 这里补上真正的推送，并保持幂等：云端主键由「用户 + 表 + 本地 id」确定性生成，
 * 重复同步只会更新同一行，不会在云端堆出一堆重复分组。
 *
 * 服务端 upsert 的 ON CONFLICT 不更新 user_id，所以主键必须按用户隔离命名空间，
 * 否则不同账号的相同本地 id 会算出同一个云端 id 而互相覆盖。
 */
class SyncService(
    private val auth: AuthService,
    private val api: ApiClient = ApiClient()
) {

    data class SyncOutcome(
        val groupsUploaded: Int,
        val actionsUploaded: Int,
        val checkinsUploaded: Int,
        val serverTimestamp: String
    ) {
        val total: Int get() = groupsUploaded + actionsUploaded + checkinsUploaded
    }

    /** 下载落地结果 */
    data class DownloadOutcome(
        val groups: Int,
        val actions: Int,
        val checkins: Int,
        val schemeRestored: Boolean
    ) {
        val total: Int get() = groups + actions + checkins
    }

    /**
     * 登录后的自动同步：每个账号在本设备只自动落地一次。
     *
     * 策略是「云端为准」——新设备/重装后本地可能是空的，直接 pull 云端全量覆盖本地，
     * 才能拿到手机端的数据。但不能每次启动都覆盖，否则用户在桌面端新增的打卡会被
     * 云端旧数据冲掉。因此用本地标记保证一个账号只自动落地一次；之后的增量同步
     * 交给设置页的手动同步（双向）。
     */
    suspend fun autoSyncOnLogin(): Result<DownloadOutcome?> = withContext(Dispatchers.Default) {
        val state = auth.currentState
        if (!state.isLoggedIn || state.token.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("未登录，无法同步"))
        }
        val accountKey = state.userId?.toString() ?: state.email
        val storage = PlatformStorageProvider.get()
        if (storage.load(SyncKeys.autoSyncedFlag(accountKey)) == "1") {
            return@withContext Result.success(null) // 已自动落地过
        }
        runCatching {
            val outcome = downloadFromCloud().getOrThrow()
            // 只有真正落地了数据才写标记。云端为空时不标记，
            // 这样手机端后续同步上来后，桌面端下次启动仍会自动拉取。
            if (outcome.total > 0) {
                storage.save(SyncKeys.autoSyncedFlag(accountKey), "1")
            }
            outcome
        }
    }

    /**
     * 从云端拉取全量并覆盖落地：分组/动作/打卡 + 方案状态。
     * 云端三张表全空时不覆盖本地（见 Repository.replaceAllFromCloud）。
     */
    suspend fun downloadFromCloud(): Result<DownloadOutcome> = withContext(Dispatchers.Default) {
        val state = auth.currentState
        if (!state.isLoggedIn || state.token.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("未登录，无法同步"))
        }
        runCatching {
            val pulled = api.pull(state.token).getOrThrow()
            val applied: CloudApplyResult = SportTaskRepository.replaceAllFromCloud(
                pulled.groups, pulled.actions, pulled.checkins
            )
            var schemeRestored = false
            pulled.kv[SyncKeys.SCHEME_STATE]?.let { schemeJson ->
                // 仅在云端方案存在时覆盖；解析失败保留本地，避免方案空白
                schemeRestored = SchemeRepository.importStateFromJson(schemeJson)
            }
            DownloadOutcome(applied.groups, applied.actions, applied.checkins, schemeRestored)
        }
    }

    /**
     * 手动同步（双向）：先把本地全量 upsert 到云端，再把合并后的云端全量落地本地。
     *
     * 顺序很关键：必须先推后拉。因为落地是全量覆盖，若先拉会先把本地新增的打卡冲掉；
     * 先推则本地新增已进云端，再拉回来时既保留了本地新增，也拿到了手机端的数据。
     * 云端主键是确定性的 cloudId，upsert 不会重复插入。
     */
    suspend fun sync(): Result<SyncOutcome> = withContext(Dispatchers.Default) {
        val state = auth.currentState
        if (!state.isLoggedIn || state.token.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("未登录，无法同步"))
        }

        runCatching {
            val namespace = state.userId?.toString() ?: state.email
            val request = buildPushRequest(namespace)
            val pushed = api.push(state.token, request).getOrThrow()
            // 推送成功后再落地：此时云端已含本地新增 + 其他设备数据，落地得到完整合并结果
            runCatching { downloadFromCloud() }
            SyncOutcome(
                groupsUploaded = pushed.groupsImported,
                actionsUploaded = pushed.actionsImported,
                checkinsUploaded = pushed.checkinsImported,
                serverTimestamp = pushed.serverTimestamp
            )
        }
    }

    private suspend fun buildPushRequest(namespace: String): SyncPushRequest {
        val groups = SportTaskRepository.getGroups()
        val actions = SportTaskRepository.getAllActions()
        val checkins = SportTaskRepository.getCheckinsByDateRange("1970-01-01", "2099-12-31")

        return SyncPushRequest(
            groups = groups.map { g ->
                SyncGroup(
                    id = cloudId(namespace, "group", g.id),
                    localId = g.id,
                    name = g.name,
                    createdAt = g.created_at
                )
            },
            actions = actions.map { a ->
                SyncAction(
                    id = cloudId(namespace, "action", a.id),
                    localId = a.id,
                    groupId = cloudId(namespace, "group", a.group_id),
                    name = a.name,
                    stepsText = a.steps_text ?: "",
                    defaultTime = a.default_time.toInt(),
                    restTime = a.rest_time.toInt(),
                    orderIndex = a.order_index.toInt(),
                    createdAt = a.created_at
                )
            },
            checkins = checkins.map { c ->
                SyncCheckin(
                    id = cloudId(namespace, "checkin", c.id),
                    localId = c.id,
                    date = c.date,
                    groupId = c.group_id?.let { cloudId(namespace, "group", it) },
                    actionId = c.action_id?.let { cloudId(namespace, "action", it) },
                    duration = c.duration?.toInt() ?: 0,
                    isCompleted = c.is_completed?.toInt() ?: 1
                )
            },
            kv = mapOf(SyncKeys.SCHEME_STATE to SchemeRepository.exportStateJson())
        )
    }

    companion object {
        /** 稳定的云端 id：同一本地记录每次同步都得到同一个值，服务端 upsert 才不会重复插入 */
        internal fun cloudId(namespace: String, kind: String, localId: Long): String {
            val seed = "$namespace|$kind|$localId"
            return fnv1a(seed, -0x340d631b7bdddcdbL).toHex16() + fnv1a(seed, 0x6a09e667f3bcc909L).toHex16()
        }

        private fun fnv1a(text: String, basis: Long): Long {
            var hash = basis
            for (ch in text) {
                hash = hash xor ch.code.toLong()
                hash *= 0x100000001b3L
            }
            return hash
        }

        private fun Long.toHex16(): String = this.toULong().toString(16).padStart(16, '0')
    }
}
