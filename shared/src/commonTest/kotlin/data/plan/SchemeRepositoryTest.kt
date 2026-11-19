package data.plan

import data.GroupItem
import data.remote.PlatformStorage
import data.remote.PlatformStorageProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        val a = SchemeRepository.register("久坐三关节恢复", "关节", "2026-11-17", listOf(1L, 2L, 3L), "joint-office").scheme
        assertNotNull(SchemeRepository.current())
        assertEquals(a.id, SchemeRepository.current()?.id)
        assertEquals("久坐三关节恢复", a.name)
        assertEquals(3, a.phaseCount)
        assertEquals(1, SchemeRepository.all().size)
    }

    @Test
    fun register_twoSchemes_switchAndBackToAll() {
        val a = SchemeRepository.register("方案A", "A", "2026-01-01", listOf(1L, 2L), "tpl-a").scheme
        val b = SchemeRepository.register("方案B", "B", "2026-01-02", listOf(3L, 4L), "tpl-b").scheme
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
        val a = SchemeRepository.register("方案A", "A", "2026-01-01", listOf(1L), "tpl-a").scheme
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
        val emptied = SchemeRepository.register("空方案", "空", "2026-01-02", listOf(777L), "tpl-empty").scheme
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
        val rehab = SchemeRepository.register("8 周康复计划", "康复", "2026-01-01", listOf(1L), "rehab").scheme
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

    @Test
    fun register_sameSource_replacesAndReturnsOldGroupIds() {
        val first = SchemeRepository.register("办公室", "办公室", "2026-01-01", listOf(10L, 11L, 12L), "office-8w")
        assertEquals(emptyList<Long>(), first.replacedGroupIds)

        // 重复导入同一模板：同一方案槽位，返回旧分组 id 供清理
        val second = SchemeRepository.register("办公室", "办公室", "2026-01-02", listOf(20L, 21L, 22L), "office-8w")
        assertEquals(first.scheme.id, second.scheme.id)
        assertEquals(listOf(10L, 11L, 12L), second.replacedGroupIds)
        assertEquals(1, SchemeRepository.all().size)
        assertEquals(listOf(20L, 21L, 22L), SchemeRepository.current()?.groupIds)
    }

    @Test
    fun todayTask_followsCurrentScheme_notRehabHardcode() {
        // 今日卡片必须来自当前方案的 phases/name，而不是写死的「8 周康复计划」
        val phases = listOf(
            PlanPhase(1, "脚踝段", 21, false, listOf(PlanExercise("踝泵", "", 60, 15))),
            PlanPhase(2, "膝盖段", 21, false, listOf(PlanExercise("伸膝", "", 60, 15))),
            PlanPhase(3, "髋关节段", 21, false, listOf(PlanExercise("外展", "", 60, 15)))
        )
        SchemeRepository.register("久坐三关节恢复", "关节", "2026-11-01", listOf(1L, 2L, 3L), "joint-office", phases)

        // 第 2 天 -> 阶段 1「脚踝段」；标题用方案名
        val task = SchemeRepository.todayTask("2026-11-02")
        assertNotNull(task)
        if (task == null) return
        assertEquals("久坐三关节恢复", task.planName)
        assertEquals("脚踝段", task.phaseTitle)
        assertEquals(1, task.phaseIndex)
        assertTrue(task.isTrainingDay)
        assertEquals(1L, task.groupId)

        // 起始日之前 -> 未开始，但方案名仍正确
        val before = SchemeRepository.todayTask("2026-10-31")
        assertNotNull(before)
        if (before == null) return
        assertTrue(before.notStarted)
        assertEquals("久坐三关节恢复", before.planName)
    }

    @Test
    fun todayTask_nullWhenNoCurrentScheme() {
        // 全部分组视图（未选方案）不展示今日卡片
        assertNull(SchemeRepository.todayTask("2026-11-02"))
        assertNull(SchemeRepository.weekRhythm("2026-11-02"))
    }

    @Test
    fun register_keepsPhasesForHomeCard() {
        val phases = listOf(PlanPhase(1, "脚踝段", 21, false, emptyList()))
        val result = SchemeRepository.register("关节", "关节", "2026-11-01", listOf(1L), "joint-office", phases)
        assertEquals(phases, result.scheme.phases)
        SchemeRepository.resetCache()
        assertEquals(phases, SchemeRepository.current()?.phases)
    }

    @Test
    fun createCustomScheme_thenAddGroups() {
        val scheme = SchemeRepository.create("我的训练", "2026-01-01")
        assertTrue(scheme.isCustom)
        assertEquals(scheme.id, SchemeRepository.current()?.id)
        assertEquals(emptyList<Long>(), SchemeRepository.current()?.groupIds)

        SchemeRepository.addGroupToScheme(scheme.id, 5L)
        SchemeRepository.addGroupToScheme(scheme.id, 6L)
        assertEquals(listOf(5L, 6L), SchemeRepository.current()?.groupIds)
        assertEquals(listOf(5L, 6L), SchemeRepository.groupIdsOf(scheme.id))

        SchemeRepository.removeGroupFromScheme(5L)
        assertEquals(listOf(6L), SchemeRepository.current()?.groupIds)
    }
}
