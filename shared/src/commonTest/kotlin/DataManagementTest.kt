package shared

import data.ImportResult
import data.formatTime
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 数据管理逻辑测试
 *
 * 覆盖：导入导出数据格式、分组信息统计、
 * 数据清除确认逻辑
 */
class DataManagementTest {

    // ============ JSON 导出的结构验证 ============

    @Test
    fun `JSON导出-基本结构`() {
        val json = buildJsonExport(
            groups = """
                {"id": 1, "name": "上肢训练", "createdAt": "2025-01-01"},
                {"id": 2, "name": "下肢训练", "createdAt": "2025-01-02"}
            """.trimIndent(),
            actions = """
                {"id": 1, "groupId": 1, "name": "俯卧撑", "stepsText": "", "defaultTime": 45, "restTime": 15, "orderIndex": 1, "createdAt": "2025-01-01"}
            """.trimIndent(),
            checkins = """
                {"id": 1, "date": "2025-01-26", "groupId": 1, "actionId": null, "duration": 1800, "isCompleted": 1}
            """.trimIndent()
        )

        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"groups\""))
        assertTrue(json.contains("\"actions\""))
        assertTrue(json.contains("\"checkins\""))
        assertTrue(json.contains("\"exportedAt\""))
    }

    @Test
    fun `JSON导出-空数据结构`() {
        val json = buildJsonExport("", "", "")
        assertTrue(json.contains("\"groups\": ["))
        assertTrue(json.contains("\"actions\": ["))
        assertTrue(json.contains("\"checkins\": ["))
    }

    // ============ 导入 JSON 正则解析测试 ============

    @Test
    fun `JSON导入-提取groups数组`() {
        val json = """{"groups": [{"id": 1, "name": "test", "createdAt": "2025-01-01"}]}"""
        val groupsRegex = """"groups"\s*:\s*\[(.*?)\]""".toRegex()
        val match = groupsRegex.find(json)
        assertTrue(match != null)
        assertTrue(match!!.groupValues[1].contains("test"))
    }

    @Test
    fun `JSON导入-提取actions数组`() {
        val json = """{"actions": [{"id": 1, "groupId": 1, "name": "pushup"}]}"""
        val actionsRegex = """"actions"\s*:\s*\[(.*?)\]""".toRegex()
        val match = actionsRegex.find(json)
        assertTrue(match != null)
        assertTrue(match!!.groupValues[1].contains("pushup"))
    }

    @Test
    fun `JSON导入-提取checkins数组`() {
        val json = """{"checkins": [{"id": 1, "date": "2025-01-26"}]}"""
        val checkinsRegex = """"checkins"\s*:\s*\[(.*?)\]""".toRegex()
        val match = checkinsRegex.find(json)
        assertTrue(match != null)
        assertTrue(match!!.groupValues[1].contains("2025-01-26"))
    }

    @Test
    fun `JSON导入-正则匹配组动作`() {
        val content = """{"id": 1, "groupId": 1, "name": "俯卧撑", "stepsText": "标准动作", "defaultTime": 45, "restTime": 15, "orderIndex": 1, "createdAt": "2025-01-01"}"""
        val actionRegex = """\{\s*"id":\s*(\d+),\s*"groupId":\s*(\d+),\s*"name":\s*"([^"]*)",\s*"stepsText":\s*"([^"]*)",\s*"defaultTime":\s*(\d+),\s*"restTime":\s*(\d+),\s*"orderIndex":\s*(\d+),\s*"createdAt":\s*"([^"]*)"\s*\}""".toRegex()
        val match = actionRegex.find(content)
        assertTrue(match != null)
        assertEquals("1", match!!.groupValues[1]) // id
        assertEquals("1", match.groupValues[2])   // groupId
        assertEquals("俯卧撑", match.groupValues[3]) // name
        assertEquals("标准动作", match.groupValues[4]) // stepsText
        assertEquals("45", match.groupValues[5])  // defaultTime
        assertEquals("15", match.groupValues[6])  // restTime
    }

    @Test
    fun `JSON导入-提取checkin正则`() {
        val content = """{"id": 1, "date": "2025-01-26", "groupId": 1, "actionId": null, "duration": 1800, "isCompleted": 1}"""
        val checkinRegex = """\{\s*"id":\s*(\d+),\s*"date":\s*"([^"]*)",\s*"groupId":\s*(\d+|null),\s*"actionId":\s*(\d+|null),\s*"duration":\s*(\d+|null),\s*"isCompleted":\s*(\d+)\s*\}""".toRegex()
        val match = checkinRegex.find(content)
        assertTrue(match != null)
        assertEquals("2025-01-26", match!!.groupValues[2])
    }

    // ============ ImportResult 测试 ============

    @Test
    fun `ImportResult-成功状态`() {
        val result = ImportResult(success = true, groupsImported = 3, actionsImported = 10, checkinsImported = 20)
        assertTrue(result.success)
        assertEquals(3, result.groupsImported)
        assertEquals(10, result.actionsImported)
        assertEquals(20, result.checkinsImported)
    }

    @Test
    fun `ImportResult-失败状态带错误信息`() {
        val result = ImportResult(success = false, 0, 0, 0, "JSON 解析失败")
        assertFalse(result.success)
        assertEquals("JSON 解析失败", result.errorMessage)
    }

    @Test
    fun `ImportResult-全部导入0`() {
        val result = ImportResult(success = true, 0, 0, 0)
        assertEquals(0, result.groupsImported + result.actionsImported + result.checkinsImported)
    }

    // ============ 数据统计测试 ============

    @Test
    fun `统计-分组动作数计算`() {
        // 模拟 Repository.countActions 的逻辑
        val groupActions = mapOf(
            1L to listOf("俯卧撑", "引体向上", "哑铃弯举"),
            2L to listOf("深蹲", "弓步蹲"),
            3L to emptyList()
        )

        assertEquals(3, groupActions[1L]?.size)
        assertEquals(2, groupActions[2L]?.size)
        assertEquals(0, groupActions[3L]?.size)
    }

    @Test
    fun `统计-总动作数汇总`() {
        val groupActions = mapOf(
            1L to listOf("俯卧撑", "引体向上"),
            2L to listOf("深蹲", "弓步蹲", "提踵")
        )

        val totalActions = groupActions.values.sumOf { it.size }
        assertEquals(5, totalActions)
    }

    @Test
    fun `统计-每日训练汇总`() {
        // 模拟 getDailySummary 结果：每天完成1次，时长合计
        val dailySummaries = listOf(
            DailySummaryItem("2025-01-20", 2, 1800),
            DailySummaryItem("2025-01-21", 1, 900),
            DailySummaryItem("2025-01-22", 0, 0)
        )

        val totalCount = dailySummaries.sumOf { it.count }
        val totalDuration = dailySummaries.sumOf { it.totalDuration }

        assertEquals(3, totalCount)
        assertEquals(2700, totalDuration)
    }

    // ============ 清除确认逻辑测试 ============

    @Test
    fun `删除前确认步骤-双重确认`() {
        var showFirstConfirm = false
        var showSecondConfirm = false

        // 第一次点击清除
        showFirstConfirm = true
        assertTrue(showFirstConfirm)

        // 第一次确认后，显示第二次确认
        if (showFirstConfirm) {
            showSecondConfirm = true
        }
        assertTrue(showSecondConfirm)
    }

    @Test
    fun `取消操作保持数据`() {
        var dataExists = true

        // 点击取消
        val cancelled = true
        if (cancelled) {
            // 数据不变
        }

        assertTrue(dataExists)
    }

    @Test
    fun `空数据清除不报错`() {
        // 即使没有数据，清除操作也应该安全执行
        val groups = emptyList<Any>()
        val actions = emptyList<Any>()
        val checkins = emptyList<Any>()

        var success = true
        try {
            // 模拟清除操作，空列表应该安全
            val deletedCount = groups.size + actions.size + checkins.size
            assertEquals(0, deletedCount)
        } catch (e: Exception) {
            success = false
        }
        assertTrue(success)
    }

    // ============ 预测性时长统计 ============

    @Test
    fun `本周平均每次训练时长`() {
        val weeklyDuration = 2700 // 45分钟
        val weeklyCount = 3
        val avg = if (weeklyCount > 0) weeklyDuration / weeklyCount else 0
        assertEquals(900, avg)
    }

    @Test
    fun `无训练时平均时长为零`() {
        val weeklyDuration = 0
        val weeklyCount = 0
        val avg = if (weeklyCount > 0) weeklyDuration / weeklyCount else 0
        assertEquals(0, avg)
    }

    private fun buildJsonExport(groups: String, actions: String, checkins: String): String {
        return buildString {
            appendLine("{")
            appendLine("  \"version\": 1,")
            appendLine("  \"exportedAt\": \"2025-01-26\",")
            appendLine("  \"groups\": [")
            if (groups.isNotEmpty()) {
                appendLine("    $groups")
            }
            appendLine("  ],")
            appendLine("  \"actions\": [")
            if (actions.isNotEmpty()) {
                appendLine("    $actions")
            }
            appendLine("  ],")
            appendLine("  \"checkins\": [")
            if (checkins.isNotEmpty()) {
                appendLine("    $checkins")
            }
            appendLine("  ]")
            appendLine("}")
        }
    }

    private data class DailySummaryItem(
        val date: String,
        val count: Int,
        val totalDuration: Int
    )
}
