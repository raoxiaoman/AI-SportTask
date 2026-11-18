package data.plan

import data.GroupItem
import data.remote.PlatformStorage
import data.remote.PlatformStorageProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class SchemeRepositoryTest {

    private class FakeStorage : PlatformStorage {
        val map = mutableMapOf<String, String>()
        override fun save(key: String, value: String) { map[key] = value }
        override fun load(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
    }

    private val storage = FakeStorage()

    @BeforeTest
    fun setUp() {
        PlatformStorageProvider.init(storage)
        storage.map.clear()
        SchemeRepository.resetCache()
    }

    @Test
    fun empty_whenNoStateAndNoLegacyPlan() {
        assertEquals(emptyList(), SchemeRepository.all())
        assertNull(SchemeRepository.current())
    }

    @Test
    fun register_setsCurrentAndKeepsAllSchemes() {
        val a = SchemeRepository.register("久坐三关节恢复", "关节", "2026-11-17", listOf(1L, 2L, 3L), "joint-office")
        assertNotNull(SchemeRepository.current())
        assertEquals(a.id, SchemeRepository.current()?.id)
        assertEquals("久坐三关节恢复", a.name)
        assertEquals(3, a.phaseCount)
        assertEquals(1, SchemeRepository.all().size)
    }

    @Test
    fun register_twoSchemes_switchAndBackToAll() {
        val a = SchemeRepository.register("方案A", "A", "2026-01-01", listOf(1L, 2L), "tpl-a")
        val b = SchemeRepository.register("方案B", "B", "2026-01-02", listOf(3L, 4L), "tpl-b")
        assertEquals(2, SchemeRepository.all().size)
        assertEquals(b.id, SchemeRepository.current()?.id)

        SchemeRepository.switchTo(a.id)
        assertEquals(a.id, SchemeRepository.current()?.id)

        SchemeRepository.switchTo(null)
        assertNull(SchemeRepository.current())
        assertEquals(2, SchemeRepository.all().size)
    }

    @Test
    fun removeScheme_fallsBackToAll() {
        val a = SchemeRepository.register("方案A", "A", "2026-01-01", listOf(1L), "tpl-a")
        SchemeRepository.remove(a.id)
        assertNull(SchemeRepository.current())
        assertEquals(emptyList(), SchemeRepository.all())
    }

    @Test
    fun currentGroups_filtersByGroupIds_andFallsBackWhenMissing() {
        val all = listOf(
            GroupItem(1, "关节 · 脚踝段", 6),
            GroupItem(2, "关节 · 膝盖段", 6),
            GroupItem(99, "其它分组", 3)
        )
        SchemeRepository.register("关节方案", "关节", "2026-01-01", listOf(1L, 2L), "tpl")
        assertEquals(listOf(1L, 2L), SchemeRepository.currentGroups(all).map { it.id })

        // 分组被删光时退回全部，避免首页空白
        val emptied = SchemeRepository.register("空方案", "空", "2026-01-02", listOf(777L), "tpl-empty")
        assertEquals(emptied.id, SchemeRepository.current()?.id)
        assertEquals(all, SchemeRepository.currentGroups(all))
    }

    @Test
    fun removeBySource_clearsAllRehabSchemes_andResetsCurrent() {
        // 康复方案 id 是动态的，删除计划需按 source 一次清干净
        SchemeRepository.register("8 周康复计划", "康复", "2026-01-01", listOf(1L, 2L), "rehab")
        SchemeRepository.register("其它方案", "其它", "2026-01-02", listOf(3L), "tpl-x")
        assertEquals(2, SchemeRepository.all().size)

        SchemeRepository.removeBySource("rehab")
        assertEquals(1, SchemeRepository.all().size)
        assertEquals("其它方案", SchemeRepository.all().first().name)
        assertEquals("其它方案", SchemeRepository.current()?.name)
    }

    @Test
    fun removeBySource_whenCurrentRemoved_fallsBackToAll() {
        val rehab = SchemeRepository.register("8 周康复计划", "康复", "2026-01-01", listOf(1L), "rehab")
        assertEquals(rehab.id, SchemeRepository.current()?.id)

        SchemeRepository.removeBySource("rehab")
        assertNull(SchemeRepository.current())
        assertEquals(emptyList(), SchemeRepository.all())
    }

    @Test
    fun statePersistsAcrossReload() {
        SchemeRepository.register("持久方案", "P", "2026-01-01", listOf(1L), "tpl-p")
        // 模拟冷启动：清缓存后重新读
        SchemeRepository.resetCache()
        assertEquals("持久方案", SchemeRepository.current()?.name)
    }
}
