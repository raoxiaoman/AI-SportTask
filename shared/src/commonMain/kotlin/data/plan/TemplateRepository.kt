package data.plan

import data.ImportResult
import data.SportTaskRepository
import data.remote.ApiClient
import pages.todayDateString

/** 模板列表来源：来自服务端表示拿到了最新版本，否则用的是 App 内置数据 */
data class TemplateLoadResult(
    val templates: List<TrainingTemplate>,
    val fromServer: Boolean
)

/**
 * 方案模板仓库：服务端作为可选的分发渠道，内置模板保证离线与未登录时也能导入。
 * 导入采用「追加为新分组」语义，不触碰已有的计划状态与其他数据。
 */
object TemplateRepository {

    private val api = ApiClient()

    /** 优先取服务端列表；超时、未配置服务器、返回空列表时一律回退内置 */
    suspend fun load(): TemplateLoadResult {
        val remote = api.templates().getOrNull()?.templates
            ?.filter { it.phases.isNotEmpty() && it.phases.all { phase -> phase.exercises.isNotEmpty() } }
        return if (!remote.isNullOrEmpty()) {
            TemplateLoadResult(remote, fromServer = true)
        } else {
            TemplateLoadResult(builtInTemplates(), fromServer = false)
        }
    }

    /**
     * 把模板落成新的分组 + 动作。重名分组自动加 "(2)" 后缀，避免与用户已有数据混淆。
     */
    suspend fun importTemplate(
        template: TrainingTemplate,
        startDate: String = todayDateString()
    ): ImportResult = runCatching {
        val usedNames = SportTaskRepository.getGroups().map { it.name }.toMutableSet()
        var groupsImported = 0
        var actionsImported = 0

        for (phase in template.phases) {
            val baseName = template.groupName(phase)
            var groupName = baseName
            var seq = 2
            while (groupName in usedNames) {
                groupName = "$baseName ($seq)"
                seq++
            }
            usedNames.add(groupName)

            SportTaskRepository.addGroup(groupName, startDate)
            val groupId = SportTaskRepository.getGroups().maxByOrNull { it.id }?.id
                ?: continue

            phase.exercises.forEachIndexed { index, exercise ->
                SportTaskRepository.addAction(
                    groupId = groupId,
                    name = exercise.name,
                    stepsText = exercise.detail,
                    defaultTime = exercise.seconds.toLong(),
                    restTime = exercise.restSeconds.toLong(),
                    orderIndex = (index + 1).toLong(),
                    createdAt = startDate
                )
                actionsImported++
            }
            groupsImported++
        }

        ImportResult(
            success = true,
            groupsImported = groupsImported,
            actionsImported = actionsImported,
            checkinsImported = 0
        )
    }.getOrElse { error ->
        ImportResult(
            success = false,
            groupsImported = 0,
            actionsImported = 0,
            checkinsImported = 0,
            errorMessage = error.message
        )
    }
}
