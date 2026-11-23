package data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.raohui.sporttask.db.SportTaskDatabase
import data.remote.SyncAction
import data.remote.SyncCheckin
import data.remote.SyncGroup
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * 云端落地测试：验证云端 id → 本地 id 映射、动作归属分组、以及云端全空时的保护。
 * 用内存 SQLite，不依赖 Android context。
 */
class CloudSyncApplyTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: SportTaskDatabase

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        SportTaskDatabase.Schema.create(driver)
        database = SportTaskDatabase(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun apply(
        groups: List<SyncGroup>,
        actions: List<SyncAction>,
        checkins: List<SyncCheckin>
    ) = SportTaskRepository.applyCloudSnapshot(database, database.sporttaskQueries, groups, actions, checkins)

    @Test
    fun `云端数据按 local_id 恢复本地主键并映射动作归属`() {
        val groups = listOf(
            SyncGroup(id = "cg1", localId = 10L, name = "阶段1"),
            SyncGroup(id = "cg2", localId = 20L, name = "阶段2")
        )
        // 动作的 groupId 是云端 id，必须映射回本地 id（10/20），否则挂错分组
        val actions = listOf(
            SyncAction(id = "ca1", localId = 100L, groupId = "cg2", name = "靠墙天使"),
            SyncAction(id = "ca2", localId = 101L, groupId = "cg1", name = "踝泵")
        )
        val checkins = listOf(
            SyncCheckin(id = "cc1", localId = 1L, date = "2026-11-20", groupId = "cg2", actionId = "ca1", isCompleted = 1)
        )

        val result = apply(groups, actions, checkins)

        assertEquals(2, result.groups)
        assertEquals(2, result.actions)
        assertEquals(1, result.checkins)

        val dbGroups = database.sporttaskQueries.getAllActionGroups().executeAsList()
        assertEquals(listOf(10L, 20L), dbGroups.map { it.id })

        val stage2Actions = database.sporttaskQueries.getActionsByGroup(20).executeAsList()
        assertEquals(listOf("靠墙天使"), stage2Actions.map { it.name })
        val stage1Actions = database.sporttaskQueries.getActionsByGroup(10).executeAsList()
        assertEquals(listOf("踝泵"), stage1Actions.map { it.name })

        val checkin = database.sporttaskQueries.getCheckinsByDateRange("2026-11-20", "2026-11-20").executeAsList().single()
        assertEquals(20L, checkin.group_id)
        assertEquals(100L, checkin.action_id)
    }

    @Test
    fun `缺失 local_id 时按序分配本地主键`() {
        val groups = listOf(
            SyncGroup(id = "cg1", localId = null, name = "A"),
            SyncGroup(id = "cg2", localId = null, name = "B")
        )
        apply(groups, emptyList(), emptyList())
        assertEquals(listOf(1L, 2L), database.sporttaskQueries.getAllActionGroups().executeAsList().map { it.id })
    }

    @Test
    fun `云端全空时不覆盖本地数据`() {
        // 先放一条本地数据
        database.sporttaskQueries.insertActionGroup("本地分组", "2026-11-23")
        assertEquals(1, database.sporttaskQueries.getAllActionGroups().executeAsList().size)

        val result = apply(emptyList(), emptyList(), emptyList())

        assertEquals(0, result.groups)
        assertEquals(1, database.sporttaskQueries.getAllActionGroups().executeAsList().size)
    }

    @Test
    fun `引用未知分组的动作被跳过而非挂错`() {
        val actions = listOf(SyncAction(id = "ca1", localId = 1L, groupId = "不存在", name = "孤儿动作"))
        val result = apply(emptyList(), actions, emptyList())
        assertEquals(0, database.sporttaskQueries.getActionById(1).executeAsOneOrNull()?.let { 1 } ?: 0)
        assertEquals(1, result.actions)
    }
}
