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

    suspend fun getActions(groupId: Long) = withContext(Dispatchers.Default) {
        q.getActionsByGroup(groupId).executeAsList()
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
}