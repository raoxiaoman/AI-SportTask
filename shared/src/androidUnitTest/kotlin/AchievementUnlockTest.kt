package data

import data.remote.PlatformStorage
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 成就解锁回归测试。
 *
 * 背景：曾出现「每次训练完成都弹第一次打卡成就」——
 * checkNewAchievements 只按当前统计筛选达标成就、不记录已解锁状态。
 * 这里验证解锁记录写入后，相同统计不再返回任何成就。
 */
class AchievementUnlockTest {

    private class FakeStorage : PlatformStorage {
        private val map = mutableMapOf<String, String>()
        override fun save(key: String, value: String) { map[key] = value }
        override fun load(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
    }

    private lateinit var storage: FakeStorage

    @Before
    fun setUp() {
        storage = FakeStorage()
        AchievementManager.setStorageProviderForTest { storage }
    }

    @After
    fun tearDown() {
        AchievementManager.setStorageProviderForTest(null)
    }

    @Test
    fun `第一次训练解锁首次打卡成就`() {
        val first = AchievementManager.computeNewAchievements(
            totalCount = 1, streakDays = 1, totalDuration = 600L, maxDuration = 600L
        )
        assertEquals(1, first.size)
        assertEquals("first_checkin", first.first().id)
    }

    @Test
    fun `同样统计再次检查不再返回已解锁成就`() {
        val stats = Triple(1, 1, 600L)
        val first = AchievementManager.computeNewAchievements(stats.first, stats.second, stats.third, stats.third)
        val second = AchievementManager.computeNewAchievements(stats.first, stats.second, stats.third, stats.third)

        assertEquals(1, first.size)
        assertTrue(second.isEmpty(), "已解锁的成就不应重复返回（否则每次完成都弹窗）")
    }

    @Test
    fun `多次完成训练仍只返回本次新达成的成就`() {
        val r1 = AchievementManager.computeNewAchievements(1, 1, 600L, 600L)
        assertEquals(setOf("first_checkin"), r1.map { it.id }.toSet())

        val r2 = AchievementManager.computeNewAchievements(3, 3, 1200L, 600L)
        assertEquals(setOf("three_streak"), r2.map { it.id }.toSet())

        val r3 = AchievementManager.computeNewAchievements(10, 10, 3600L, 3600L)
        assertEquals(
            setOf("seven_streak", "ten_total", "hour_total", "first_hour_session"),
            r3.map { it.id }.toSet()
        )

        // 再次训练 11 次：无新成就
        val r4 = AchievementManager.computeNewAchievements(11, 11, 4000L, 3600L)
        assertTrue(r4.isEmpty())
    }

    @Test
    fun `解锁记录在存储重建后仍生效`() {
        AchievementManager.computeNewAchievements(1, 1, 600L, 600L)
        val snapshot = storage.load("sporttask_unlocked_achievements")
        assertTrue(snapshot!!.contains("first_checkin"))

        val other = FakeStorage()
        other.save("sporttask_unlocked_achievements", snapshot)
        AchievementManager.setStorageProviderForTest { other }

        val again = AchievementManager.computeNewAchievements(1, 1, 600L, 600L)
        assertTrue(again.isEmpty(), "重启后也不应重复弹出")
    }
}
