package data.remote

import data.SportTaskRepository
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

    /** 先上传本地全量数据，再拉一次确认服务端时间戳 */
    suspend fun sync(): Result<SyncOutcome> = withContext(Dispatchers.Default) {
        val state = auth.currentState
        if (!state.isLoggedIn || state.token.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("未登录，无法同步"))
        }

        runCatching {
            val namespace = state.userId?.toString() ?: state.email
            val request = buildPushRequest(namespace)

            val pushed = api.push(state.token, request).getOrThrow()
            // 拉取一次：既确认 token 有效，也为后续双向合并留下服务端时间戳
            val pulled = api.pull(state.token).getOrNull()

            SyncOutcome(
                groupsUploaded = pushed.groupsImported,
                actionsUploaded = pushed.actionsImported,
                checkinsUploaded = pushed.checkinsImported,
                serverTimestamp = pushed.serverTimestamp.ifBlank { pulled?.serverTimestamp ?: "" }
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
            }
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
