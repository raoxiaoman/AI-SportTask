package data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * 云端 id 的稳定性是「重复同步不产生重复数据」的前提：
 * 服务端 push 走 ON CONFLICT(id) DO UPDATE，id 一变就会插新行。
 */
class SyncServiceCloudIdTest {

    @Test
    fun `同一记录多次计算结果一致`() {
        val first = SyncService.cloudId("2", "group", 1)
        val again = SyncService.cloudId("2", "group", 1)
        assertEquals(first, again)
    }

    @Test
    fun `不同本地 id 得到不同云端 id`() {
        assertNotEquals(
            SyncService.cloudId("2", "group", 1),
            SyncService.cloudId("2", "group", 2)
        )
    }

    @Test
    fun `不同类型之间不冲突`() {
        // 本地分组 1 与本地动作 1 的 id 都是 1，云端不能撞成同一行
        assertNotEquals(
            SyncService.cloudId("2", "group", 1),
            SyncService.cloudId("2", "action", 1)
        )
    }

    @Test
    fun `不同用户的相同本地 id 互相隔离`() {
        // 服务端 upsert 不更新 user_id，主键撞车会覆盖别人的数据
        assertNotEquals(
            SyncService.cloudId("2", "group", 1),
            SyncService.cloudId("3", "group", 1)
        )
    }

    @Test
    fun `云端 id 为 32 位十六进制`() {
        val id = SyncService.cloudId("user@example.com", "checkin", 42)
        assertEquals(32, id.length)
        assertEquals(true, id.all { it in '0'..'9' || it in 'a'..'f' })
    }
}
