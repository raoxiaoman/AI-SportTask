package data.plan

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun builtInTemplates_haveTwoDistinctIds() {
        val templates = builtInTemplates()
        assertEquals(2, templates.size)
        assertEquals(2, templates.map { it.id }.toSet().size)
        assertTrue(templates.any { it.id == "rehab-8w" })
        assertTrue(templates.any { it.id == "office-8w" })
    }

    @Test
    fun rehabTemplate_hasThreePhasesAnd23Actions() {
        val template = rehabTemplate()
        assertEquals(3, template.phases.size)
        assertEquals(23, template.actionCount)
        assertEquals(56, template.totalDays)
        assertEquals("康复 · 阶段1 激活", template.groupName(template.phases.first()))
    }

    @Test
    fun officeTemplate_hasThreePhasesAnd25Actions() {
        val template = officeTemplate()
        assertEquals(3, template.phases.size)
        assertEquals(25, template.actionCount)
        assertEquals(56, template.totalDays)
        assertEquals("办公室 · 阶段1 唤醒", template.groupName(template.phases.first()))
        assertTrue(template.phases[2].everyOtherDay)
    }

    @Test
    fun everyExercise_hasValidDurationsAndDetail() {
        builtInTemplates().forEach { template ->
            template.phases.forEach { phase ->
                assertTrue(phase.exercises.isNotEmpty(), "${template.id}/${phase.index} 无动作")
                assertTrue(phase.days > 0)
                phase.exercises.forEach { exercise ->
                    assertTrue(exercise.name.isNotBlank())
                    assertTrue(exercise.detail.isNotBlank(), "${exercise.name} 缺少步骤说明")
                    assertTrue(exercise.seconds > 0, "${exercise.name} 训练时长必须为正")
                    assertTrue(exercise.restSeconds >= 0, "${exercise.name} 休息时长不能为负")
                }
            }
        }
    }

    @Test
    fun templateJson_roundTripKeepsAllFields() {
        val original = officeTemplate()
        val encoded = json.encodeToString(TemplateListResponse.serializer(), TemplateListResponse(listOf(original)))
        val decoded = json.decodeFromString(TemplateListResponse.serializer(), encoded).templates.single()

        assertEquals(original.id, decoded.id)
        assertEquals(original.groupPrefix, decoded.groupPrefix)
        assertEquals(original.phases.size, decoded.phases.size)
        assertEquals(original.actionCount, decoded.actionCount)
        assertEquals(original.phases[1].exercises[3].detail, decoded.phases[1].exercises[3].detail)
    }
}
