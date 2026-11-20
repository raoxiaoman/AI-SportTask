package ui

import data.plan.builtInTemplates
import kotlin.test.Test
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
}
