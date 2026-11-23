package ui

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/**
 * 把 App 内置的全部图解导出为服务端分发文件 backend/data/guides.json。
 *
 * 这是「单一数据源」：内置 Kotlin 定义 → 导出 → 服务端下发。
 * 以后修正图解只需改服务端 guides.json（改完即生效、不用发版），
 * 内置库继续作为离线兜底。需要重新生成全量基线时手动跑一次本测试的 exportGuides()。
 */
class ExportGuidesJson {

    @Test
    fun exportGuides() {
        val payload = GuideListResponse(
            guides = allBuiltInGuides().sortedBy { it.name },
            version = 1
        )
        val prettyJson = Json { prettyPrint = true; encodeDefaults = true }
        val text = prettyJson.encodeToString(payload)

        // androidUnitTest 的工作目录是 shared/，回退一级到项目根
        val workingDir = File(System.getProperty("user.dir") ?: ".")
        val projectRoot = if (workingDir.name == "shared") workingDir.parentFile ?: workingDir else workingDir
        val out = File(projectRoot, "backend/data/guides.json")
        out.parentFile?.mkdirs()
        out.writeText(text)
        println("[export] wrote ${payload.guides.size} guides to ${out.absolutePath}")
    }
}
