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
}