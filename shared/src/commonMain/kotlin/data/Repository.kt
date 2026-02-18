package data

import com.raohui.sporttask.db.SportTaskDatabase
import db.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SportTaskRepository(private val db: SportTaskDatabase = DatabaseProvider.db) {
    private val q = db.sporttaskQueries

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
            appendLine("  \"exportedAt\": \"${java.time.LocalDate.now()}\",")

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
                    val createdAt = match.groupValues[3].ifEmpty { java.time.LocalDate.now().toString() }
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
                    val createdAt = match.groupValues[8].ifEmpty { java.time.LocalDate.now().toString() }
                    
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