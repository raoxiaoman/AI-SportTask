package data

import com.raohui.sporttask.db.SportTaskDatabase
import com.raohui.sporttask.db.SporttaskQueries
import data.remote.SyncAction
import data.remote.SyncCheckin
import data.remote.SyncGroup
import db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pages.todayDateString

/** 云端数据落地统计 */
data class CloudApplyResult(
    val groups: Int,
    val actions: Int,
    val checkins: Int
)

object SportTaskRepository {
    private val db: SportTaskDatabase by lazy { DatabaseProvider.db }
    private val q by lazy { db.sporttaskQueries }

    suspend fun getGroups() = withContext(Dispatchers.Default) {
        q.getAllActionGroups().executeAsList()
    }

    suspend fun getGroup(id: Long) = withContext(Dispatchers.Default) {
        q.getActionGroupById(id).executeAsOneOrNull()
    }

    suspend fun addGroup(name: String, createdAt: String) = withContext(Dispatchers.Default) {
        q.insertActionGroup(name, createdAt)
    }

    suspend fun updateGroupName(id: Long, name: String) = withContext(Dispatchers.Default) {
        q.updateActionGroupName(name, id)
    }

    suspend fun deleteGroup(id: Long) = withContext(Dispatchers.Default) {
        q.deleteActionGroupById(id)
    }

    suspend fun countActions(groupId: Long): Long = withContext(Dispatchers.Default) {
        q.countActionsInGroup(groupId).executeAsOne()
    }

    suspend fun getNextOrderIndex(groupId: Long): Long = withContext(Dispatchers.Default) {
        val count = q.countActionsInGroup(groupId).executeAsOne()
        count + 1
    }

    suspend fun getActions(groupId: Long) = withContext(Dispatchers.Default) {
        q.getActionsByGroup(groupId).executeAsList()
    }

    /**
     * 首帧同步读取：数据量很小（最多几十条），用于让页面第一帧就渲染真实内容。
     * 此前依赖 LaunchedEffect 加载 + FullScreenLoading，冷启动时窗口未拿到焦点、
     * 帧调度未启动会一直停在「白屏 + 一个转圈」，直到用户点一下屏幕才刷新。
     */
    fun loadGroupItemsSync(): List<GroupItem> = runCatching {
        q.getAllActionGroups().executeAsList().map { g ->
            GroupItem(
                id = g.id,
                name = g.name,
                actionCount = q.countActionsInGroup(g.id).executeAsOne().toInt()
            )
        }
    }.getOrDefault(emptyList())

    fun loadActionItemsSync(groupId: Long): List<ActionItem> = runCatching {
        q.getActionsByGroup(groupId).executeAsList().map { a ->
            ActionItem(
                id = a.id,
                name = a.name,
                stepsText = a.steps_text ?: "",
                defaultTime = a.default_time.toInt(),
                restTime = a.rest_time.toInt(),
                orderIndex = a.order_index.toInt()
            )
        }
    }.getOrDefault(emptyList())

    fun countActionsSync(groupId: Long): Int = runCatching {
        q.countActionsInGroup(groupId).executeAsOne().toInt()
    }.getOrDefault(0)

    fun hasCompletedSync(groupId: Long, today: String): Boolean = runCatching {
        q.getCheckinsByDateRange(today, today).executeAsList()
            .any { it.group_id == groupId && it.is_completed == 1L }
    }.getOrDefault(false)

    /** 区间内完成打卡的日期集合（同步读取，供首页本周节奏使用） */
    fun completedDatesSync(startDate: String, endDate: String): Set<String> = runCatching {
        q.getCheckinsByDateRange(startDate, endDate).executeAsList()
            .filter { it.is_completed == 1L }
            .map { it.date }
            .toSet()
    }.getOrDefault(emptySet())

    suspend fun getAllActions() = withContext(Dispatchers.Default) {
        // 获取所有分组的所有动作
        val groups = q.getAllActionGroups().executeAsList()
        groups.flatMap { group ->
            q.getActionsByGroup(group.id).executeAsList()
        }
    }

    suspend fun getAction(id: Long) = withContext(Dispatchers.Default) {
        q.getActionById(id).executeAsOneOrNull()
    }

    suspend fun addAction(
        groupId: Long,
        name: String,
        stepsText: String?,
        defaultTime: Long,
        restTime: Long,
        orderIndex: Long,
        createdAt: String?
    ) = withContext(Dispatchers.Default) {
        q.insertAction(groupId, name, stepsText, defaultTime, restTime, orderIndex, createdAt)
    }

    suspend fun updateActionOrder(id: Long, orderIndex: Long) = withContext(Dispatchers.Default) {
        q.updateActionOrder(orderIndex, id)
    }

    suspend fun updateAction(
        id: Long,
        groupId: Long,
        name: String,
        stepsText: String?,
        defaultTime: Long,
        restTime: Long,
        orderIndex: Long,
        createdAt: String?
    ) = withContext(Dispatchers.Default) {
        q.updateAction(groupId, name, stepsText, defaultTime, restTime, orderIndex, createdAt, id)
    }

    suspend fun deleteAction(id: Long) = withContext(Dispatchers.Default) {
        q.deleteActionById(id)
    }

    /** 删除分组下全部动作（重置计划时用） */
    suspend fun deleteActionsInGroup(groupId: Long) = withContext(Dispatchers.Default) {
        q.deleteActionsByGroup(groupId)
    }

    suspend fun addCheckin(
        date: String,
        groupId: Long?,
        actionId: Long?,
        duration: Long?,
        isCompleted: Long
    ) = withContext(Dispatchers.Default) {
        q.insertCheckin(date, groupId, actionId, duration, isCompleted)
    }

    suspend fun getDailySummary(startDate: String, endDate: String) = withContext(Dispatchers.Default) {
        q.getDailySummary(startDate, endDate).executeAsList()
    }

    suspend fun getCheckinsByDateRange(startDate: String, endDate: String) = withContext(Dispatchers.Default) {
        q.getCheckinsByDateRange(startDate, endDate).executeAsList()
    }

    suspend fun updateCheckin(id: Long, duration: Long, isCompleted: Long) = withContext(Dispatchers.Default) {
        q.updateCheckin(duration, isCompleted, id)
    }

    suspend fun deleteCheckin(id: Long) = withContext(Dispatchers.Default) {
        q.deleteCheckinById(id)
    }

    // Data management
    suspend fun deleteAllCheckins() = withContext(Dispatchers.Default) {
        q.deleteAllCheckins()
    }

    suspend fun deleteAllActions() = withContext(Dispatchers.Default) {
        q.deleteAllActions()
    }

    suspend fun deleteAllActionGroups() = withContext(Dispatchers.Default) {
        q.deleteAllActionGroups()
    }

    suspend fun clearAllData() = withContext(Dispatchers.Default) {
        q.deleteAllCheckins()
        q.deleteAllActions()
        q.deleteAllActionGroups()
    }

    /**
     * 用云端全量快照覆盖本地（登录自动同步 / 手动同步的"下载落地"）。
     *
     * 关键点：
     * - 云端 action.group_id / checkin.group_id、action_id 存的是**云端 id**，
     *   落地时必须先建 cloudGroupId → 本地 id 的映射，否则动作会挂到错误分组。
     * - 恢复时使用云端保存的 local_id 作为本地主键，让方案(Scheme.groupIds)引用不错位。
     * - 云端三张表全空时直接返回、不清本地：避免用户刚换设备时把本地数据误删。
     */
    suspend fun replaceAllFromCloud(
        groups: List<SyncGroup>,
        actions: List<SyncAction>,
        checkins: List<SyncCheckin>
    ): CloudApplyResult = withContext(Dispatchers.Default) {
        applyCloudSnapshot(db, q, groups, actions, checkins)
    }

    /** 纯逻辑：接收数据库实例，便于用内存库做单测 */
    internal fun applyCloudSnapshot(
        database: SportTaskDatabase,
        queries: SporttaskQueries,
        groups: List<SyncGroup>,
        actions: List<SyncAction>,
        checkins: List<SyncCheckin>
    ): CloudApplyResult {
        if (groups.isEmpty() && actions.isEmpty() && checkins.isEmpty()) {
            return CloudApplyResult(0, 0, 0)
        }
        database.transaction {
            queries.deleteAllCheckins()
            queries.deleteAllActions()
            queries.deleteAllActionGroups()

            // 云端 group id -> 本地 group id（优先用来源 local_id 保住方案引用）
            val groupIdMap = mutableMapOf<String, Long>()
            var nextGroupId = 1L
            groups.forEach { g ->
                val localId = g.localId?.takeIf { it > 0 } ?: nextGroupId++
                if (localId >= nextGroupId) nextGroupId = localId + 1
                g.id?.let { groupIdMap[it] = localId }
                queries.insertActionGroupWithId(localId, g.name, g.createdAt)
            }

            // 云端 action id -> 本地 action id
            val actionIdMap = mutableMapOf<String, Long>()
            var nextActionId = 1L
            actions.forEach { a ->
                val localGroupId = a.groupId?.let { groupIdMap[it] } ?: return@forEach
                val localId = a.localId?.takeIf { it > 0 } ?: nextActionId++
                if (localId >= nextActionId) nextActionId = localId + 1
                a.id?.let { actionIdMap[it] = localId }
                queries.insertActionWithId(
                    localId, localGroupId, a.name, a.stepsText,
                    a.defaultTime.toLong(), a.restTime.toLong(),
                    a.orderIndex.toLong(), a.createdAt
                )
            }

            checkins.forEach { c ->
                val localGroupId = c.groupId?.let { groupIdMap[it] }
                val localActionId = c.actionId?.let { actionIdMap[it] }
                val duration = c.duration.toLong()
                val completed = c.isCompleted.toLong()
                val localId = c.localId?.takeIf { it > 0 }
                if (localId != null) {
                    queries.insertCheckinWithId(localId, c.date, localGroupId, localActionId, duration, completed)
                } else {
                    queries.insertCheckin(c.date, localGroupId, localActionId, duration, completed)
                }
            }
        }
        return CloudApplyResult(groups.size, actions.size, checkins.size)
    }

    // Export data as JSON
    suspend fun exportDataAsJson(): String = withContext(Dispatchers.Default) {
        val groups = q.getAllActionGroups().executeAsList()
        val actions = groups.flatMap { group ->
            q.getActionsByGroup(group.id).executeAsList()
        }
        val checkins = q.getCheckinsByDateRange("1970-01-01", "2099-12-31").executeAsList()

        buildString {
            appendLine("{")
            appendLine("  \"version\": 1,")
            appendLine("  \"exportedAt\": \"${todayDateString()}\",")

            // Export groups
            appendLine("  \"groups\": [")
            groups.forEachIndexed { index, group ->
                append("    {")
                append("\"id\": ${group.id}, ")
                append("\"name\": \"${group.name}\", ")
                append("\"createdAt\": \"${group.created_at ?: ""}\"")
                append("}")
                if (index < groups.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ],")

            // Export actions
            appendLine("  \"actions\": [")
            actions.forEachIndexed { index, action ->
                append("    {")
                append("\"id\": ${action.id}, ")
                append("\"groupId\": ${action.group_id}, ")
                append("\"name\": \"${action.name}\", ")
                append("\"stepsText\": \"${action.steps_text ?: ""}\", ")
                append("\"defaultTime\": ${action.default_time}, ")
                append("\"restTime\": ${action.rest_time}, ")
                append("\"orderIndex\": ${action.order_index}, ")
                append("\"createdAt\": \"${action.created_at ?: ""}\"")
                append("}")
                if (index < actions.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ],")

            // Export checkins
            appendLine("  \"checkins\": [")
            checkins.forEachIndexed { index, checkin ->
                append("    {")
                append("\"id\": ${checkin.id}, ")
                append("\"date\": \"${checkin.date}\", ")
                append("\"groupId\": ${checkin.group_id ?: "null"}, ")
                append("\"actionId\": ${checkin.action_id ?: "null"}, ")
                append("\"duration\": ${checkin.duration ?: "null"}, ")
                append("\"isCompleted\": ${checkin.is_completed}")
                append("}")
                if (index < checkins.size - 1) appendLine(",") else appendLine()
            }
            appendLine("  ]")

            appendLine("}")
        }
    }

    // Import data from JSON
    suspend fun importDataFromJson(json: String): ImportResult = withContext(Dispatchers.Default) {
        try {
            // 简单的 JSON 解析
            var groupsImported = 0
            var actionsImported = 0
            var checkinsImported = 0
            
            // 解析 groups
            val groupsRegex = """\"groups\"\s*:\s*\[(.*?)\]""".toRegex()
            val groupMatch = groupsRegex.find(json)
            
            if (groupMatch != null) {
                val groupsContent = groupMatch.groupValues[1]
                val groupRegex = """\{\s*"id":\s*(\d+),\s*"name":\s*"([^"]*)",\s*"createdAt":\s*"([^"]*)"\s*\}""".toRegex()
                
                groupRegex.findAll(groupsContent).forEach { match ->
                    val name = match.groupValues[2]
                    val createdAt = match.groupValues[3].ifEmpty { todayDateString() }
                    q.insertActionGroup(name, createdAt)
                    groupsImported++
                }
            }
            
            // 获取所有分组ID映射
            val groupIdMap = mutableMapOf<Long, Long>()
            val existingGroups = q.getAllActionGroups().executeAsList()
            existingGroups.forEachIndexed { index, group ->
                groupIdMap[index + 1L] = group.id
            }
            
            // 解析 actions
            val actionsRegex = """\"actions\"\s*:\s*\[(.*?)\]""".toRegex()
            val actionMatch = actionsRegex.find(json)
            
            if (actionMatch != null) {
                val actionsContent = actionMatch.groupValues[1]
                val actionRegex = """\{\s*"id":\s*(\d+),\s*"groupId":\s*(\d+),\s*"name":\s*"([^"]*)",\s*"stepsText":\s*"([^"]*)",\s*"defaultTime":\s*(\d+),\s*"restTime":\s*(\d+),\s*"orderIndex":\s*(\d+),\s*"createdAt":\s*"([^"]*)"\s*\}""".toRegex()
                
                actionRegex.findAll(actionsContent).forEach { match ->
                    val oldGroupId = match.groupValues[2].toLongOrNull() ?: return@forEach
                    val newGroupId = groupIdMap[oldGroupId] ?: return@forEach
                    val name = match.groupValues[3]
                    val stepsText = match.groupValues[4]
                    val defaultTime = match.groupValues[5].toLongOrNull() ?: 30
                    val restTime = match.groupValues[6].toLongOrNull() ?: 10
                    val orderIndex = match.groupValues[7].toLongOrNull() ?: 1
                    val createdAt = match.groupValues[8].ifEmpty { todayDateString() }
                    
                    q.insertAction(newGroupId, name, stepsText, defaultTime, restTime, orderIndex, createdAt)
                    actionsImported++
                }
            }
            
            // 解析 checkins
            val checkinsRegex = """\"checkins\"\s*:\s*\[(.*?)\]""".toRegex()
            val checkinMatch = checkinsRegex.find(json)
            
            if (checkinMatch != null) {
                val checkinsContent = checkinMatch.groupValues[1]
                val checkinRegex = """\{\s*"id":\s*(\d+),\s*"date":\s*"([^"]*)",\s*"groupId":\s*(\d+|null),\s*"actionId":\s*(\d+|null),\s*"duration":\s*(\d+|null),\s*"isCompleted":\s*(\d+)\s*\}""".toRegex()
                
                checkinRegex.findAll(checkinsContent).forEach { match ->
                    val date = match.groupValues[2]
                    val groupId = match.groupValues[3].toLongOrNull()?.let { groupIdMap[it] }
                    val actionId = match.groupValues[4].toLongOrNull()
                    val duration = match.groupValues[5].toLongOrNull()
                    val isCompleted = match.groupValues[6].toLongOrNull() ?: 0
                    
                    q.insertCheckin(date, groupId, actionId, duration, isCompleted)
                    checkinsImported++
                }
            }
            
            ImportResult(success = true, groupsImported, actionsImported, checkinsImported)
        } catch (e: Exception) {
            ImportResult(success = false, 0, 0, 0, e.message)
        }
    }
}