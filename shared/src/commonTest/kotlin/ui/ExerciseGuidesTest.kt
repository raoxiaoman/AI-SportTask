package ui

import data.plan.builtInTemplates
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 图解覆盖测试：三套内置方案的全部动作都必须有专属示意图，
 * 不能回退到通用站立小人（否则示意图与动作不符）。
 */
class ExerciseGuidesTest {

    @Test
    fun `所有内置方案动作都有专属图解`() {
        val templates = builtInTemplates()
        val missing = mutableListOf<String>()
        templates.forEach { template ->
            template.phases.forEach { phase ->
                phase.exercises.forEach { ex ->
                    if (guideFor(ex.name) == null) missing.add("${template.name}/${ex.name}")
                }
            }
        }
        assertTrue(missing.isEmpty(), "以下动作缺少专属图解：${missing.joinToString()}")
        assertTrue(templates.isNotEmpty())
    }

    @Test
    fun `每个图解都有姿势和数据`() {
        val templates = builtInTemplates()
        templates.forEach { template ->
            template.phases.forEach { phase ->
                phase.exercises.forEach { ex ->
                    val guide = guideFor(ex.name)
                    assertNotNull(guide, "${ex.name} 应有图解")
                    assertTrue(guide.steps.isNotEmpty(), "${ex.name} 应有动作步骤")
                    assertTrue(guide.tips.isNotEmpty(), "${ex.name} 应有质量要点")
                    assertTrue(guide.cautions.isNotEmpty(), "${ex.name} 应有注意信号")
                }
            }
        }
    }

    @Test
    fun `办公室与关节专属动作图解可命中`() {
        // 这两套方案的动作名带场景限定词，早期精确匹配不到会回退通用图
        assertNotNull(guideFor("椅背胸椎伸展"))
        assertNotNull(guideFor("靠墙天使"))
        assertNotNull(guideFor("坐姿踝泵"))
        assertNotNull(guideFor("空中画字母"))
        assertNotNull(guideFor("坐姿髌骨松动"))
        assertNotNull(guideFor("扶桌单腿站"))
        assertNotNull(guideFor("扶桌单腿站立"))
    }

    @Test
    fun `全部内置图解可 JSON 序列化并完整往返`() {
        val json = Json { encodeDefaults = true }
        val originals = allBuiltInGuides()
        assertTrue(originals.isNotEmpty())
        originals.forEach { g ->
            val decoded = json.decodeFromString<ExerciseGuide>(json.encodeToString(g))
            assertEquals(g.name, decoded.name)
            assertEquals(g.pose.arrows.size, decoded.pose.arrows.size, "${g.name} 箭头数量应一致")
            assertEquals(g.pose.head, decoded.pose.head, "${g.name} 头部坐标应一致")
            assertEquals(g.steps, decoded.steps, "${g.name} 步骤应一致")
        }
    }

    @Test
    fun `仓库合并后服务端图解覆盖同名内置项且保留其余`() {
        try {
            val base = allBuiltInGuides()
            val patched = base.first().copy(summary = "服务端修正版")
            // 模拟 load 的合并语义：内置为底，服务端覆盖同名
            val merged = base.associateBy { it.name } + mapOf(patched.name to patched)
            GuideRepository.resetForTest(merged.values.toList(), server = true)
            assertEquals("服务端修正版", guideFor(patched.name)?.summary)
            // 未覆盖的动作仍在
            val another = base.last()
            assertNotNull(guideFor(another.name))
        } finally {
            // 还原为内置库，避免测试间相互污染
            GuideRepository.resetForTest(allBuiltInGuides())
        }
    }

    @Test
    fun `仓库查找支持去空格归一化匹配`() {
        try {
            GuideRepository.resetForTest(allBuiltInGuides())
            // 动作名含空格的动作，去掉空格后仍能命中
            val spaced = allBuiltInGuides().firstOrNull { it.name.contains(" ") }
            if (spaced != null) {
                assertNotNull(guideFor(spaced.name.replace(" ", "")))
            }
        } finally {
            GuideRepository.resetForTest(allBuiltInGuides())
        }
    }
}
