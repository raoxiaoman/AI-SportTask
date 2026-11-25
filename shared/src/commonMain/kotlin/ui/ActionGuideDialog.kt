package ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import data.ActionItem
import data.formatTime
import pages.openExternalUrl
import ui.theme.*

/**
 * 动作详情（图解）两种容器：
 * - [ActionGuideDialog] 全屏弹窗：分组页预览用，独立窗口。
 * - [ActionGuidePanel] 内嵌面板：训练执行页「展开图解」用，与原页共生死。
 *
 * 两者的图文内容复用 [ActionGuideBody]，保证分组预览与训练中看到的图解一致。
 * 训练内嵌版在顶部常驻「倒计时 + 暂停/继续」控制条——这是本次重设计的重点：
 * 看图解时不必退出就能读时间、控播放，解决「详情页看不到倒计时、控制按钮埋在底部」的问题。
 */

/**
 * 全屏详情弹窗（分组页预览动作）。
 */
@Composable
fun ActionGuideDialog(
    action: ActionItem,
    guide: ExerciseGuide,
    onDismiss: () -> Unit,
    showTimingHint: Boolean = false,
    isRunning: Boolean = true,
    onToggleRunning: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DialogSystemBarAppearanceEffect(
            isDark = !AppTheme.colors.isLight,
            statusBarColor = AppTheme.colors.background,
            navigationBarColor = AppTheme.colors.surface
        )
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppTheme.colors.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                ActionGuideHeader(action = action, guide = guide, onBack = onDismiss)
                ActionGuideBody(action = action, guide = guide)

                // 兼容旧的调用方：若未来有页面在弹窗里训练，仍可在底部显示计时提示
                if (showTimingHint) {
                    Spacer(modifier = Modifier.height(AppDimens.s2))
                    Text(
                        text = if (onToggleRunning == null)
                            "查看详情时计时仍在继续，需要可先按暂停"
                        else if (isRunning) "查看详情时可在此暂停计时"
                        else "计时已暂停，看完详情可继续",
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (onToggleRunning != null) {
                        Spacer(modifier = Modifier.height(AppDimens.s3))
                        Button(
                            onClick = onToggleRunning,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .height(AppDimens.s10),
                            contentPadding = PaddingValues(
                                horizontal = AppDimens.s8,
                                vertical = 0.dp
                            ),
                            shape = AppShapes.pill
                        ) {
                            Text(
                                text = if (isRunning) "暂停计时" else "继续计时",
                                style = MaterialTheme.typography.subtitle2
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppDimens.s6))
            }
        }
    }
}

/**
 * 训练页内嵌图解面板：与倒计时环互斥显示，顶部常驻计时控制条。
 *
 * @param remainingSeconds 当前阶段剩余秒数（来自训练页的唯一计时源）
 * @param isRunning 计时是否运行中
 * @param isResting 是否休息阶段（决定倒计时配色）
 */
@Composable
fun ActionGuidePanel(
    action: ActionItem,
    guide: ExerciseGuide,
    remainingSeconds: Int,
    isRunning: Boolean,
    isResting: Boolean,
    onCollapse: () -> Unit,
    onToggleRunning: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部：收起图解 + 动作名（与主页面同一行，切换不跳视觉锚点）
        ActionGuideHeader(action = action, guide = guide, onBack = onCollapse)

        // 常驻计时条：把倒计时和控制放在视线第一屏，滚动看图也不丢失
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.screenPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.s2)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = AppShapes.card,
                color = if (isResting) AppTheme.colors.warningSoft else AppTheme.colors.brandSoft
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(remainingSeconds),
                        style = MaterialTheme.typography.h6,
                        fontWeight = AppTypeWeight.emphasis,
                        color = when {
                            remainingSeconds <= 3 -> AppTheme.colors.danger
                            isResting -> AppTheme.colors.warning
                            else -> AppTheme.colors.brandStrong
                        }
                    )
                    Spacer(modifier = Modifier.width(AppDimens.s2))
                    Column {
                        Text(
                            text = if (isResting) "休息剩余" else "动作剩余",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.inkSub
                        )
                        Text(
                            text = if (isRunning) "计时中" else "已暂停",
                            style = MaterialTheme.typography.caption,
                            color = AppTheme.colors.inkMuted
                        )
                    }
                }
            }
            Button(
                onClick = onToggleRunning,
                modifier = Modifier.height(AppDimens.touchTarget),
                shape = AppShapes.pill,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.colors.brandStrong,
                    contentColor = AppTheme.colors.onBrand
                )
            ) {
                Text(
                    text = if (isRunning) "暂停" else "继续",
                    style = MaterialTheme.typography.subtitle2
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimens.s3))

        ActionGuideBody(action = action, guide = guide)

        Spacer(modifier = Modifier.height(AppDimens.s6))
    }
}

/** 详情头部：返回按钮 + 动作名 + 一句话概括（两种容器共用） */
@Composable
private fun ActionGuideHeader(
    action: ActionItem,
    guide: ExerciseGuide,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.s1, vertical = AppDimens.s2),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            AppIcon(
                imageVector = AppIcons.Back,
                contentDescription = "收起详情",
                tint = AppTheme.colors.ink
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.name,
                style = MaterialTheme.typography.h6,
                fontWeight = AppTypeWeight.emphasis,
                color = AppTheme.colors.ink
            )
            Text(
                text = guide.summary,
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.inkSub
            )
        }
    }
}

/** 图解正文：参数 chip + 示意图 + 步骤 + 要点 + 注意 + 视频入口（两种容器共用） */
@Composable
private fun ActionGuideBody(action: ActionItem, guide: ExerciseGuide) {
    // 参数行：时长 / 休息 / 视频
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.s2)
    ) {
        GuideChip(text = "时长 ${action.defaultTime} 秒")
        GuideChip(text = "组间休息 ${action.restTime} 秒")
        GuideChip(text = if (guide.videoUrl != null) "含视频演示" else "图解 + 视频")
    }

    Spacer(modifier = Modifier.height(AppDimens.s4))

    // 示意图
    LiteCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.s3),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExerciseDiagram(
                spec = guide.pose,
                modifier = Modifier.size(AppDimens.diagramSize)
            )
            Spacer(modifier = Modifier.height(AppDimens.s1))
            Text(
                text = "动作示意图（侧视）",
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.inkMuted
            )
        }
    }

    Spacer(modifier = Modifier.height(AppDimens.s4))

    // 分步操作
    SectionHeader(text = "动作步骤", modifier = Modifier.padding(horizontal = AppDimens.screenPadding))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
    ) {
        guide.steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.caption,
                    fontWeight = AppTypeWeight.emphasis,
                    color = AppTheme.colors.onBrand,
                    modifier = Modifier
                        .size(AppDimens.s6)
                        .background(AppTheme.colors.brand, CircleShape)
                        .wrapContentSize(Alignment.Center)
                )
                Spacer(modifier = Modifier.width(AppDimens.s2))
                Text(
                    text = step,
                    style = MaterialTheme.typography.body2,
                    color = AppTheme.colors.ink,
                    modifier = Modifier.padding(top = AppDimens.s1)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(AppDimens.s4))

    // 质量要点
    SectionHeader(text = "质量要点", modifier = Modifier.padding(horizontal = AppDimens.screenPadding))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
    ) {
        guide.tips.forEach { tip ->
            Row(verticalAlignment = Alignment.Top) {
                AppIcon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = AppTheme.colors.success,
                    size = AppDimens.iconSmall
                )
                Spacer(modifier = Modifier.width(AppDimens.s2))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.body2,
                    color = AppTheme.colors.inkSub
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(AppDimens.s4))

    // 疼痛信号 / 禁忌
    SectionHeader(text = "注意信号", modifier = Modifier.padding(horizontal = AppDimens.screenPadding))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding),
        shape = AppShapes.card,
        color = AppTheme.colors.warningSoft
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.s3),
            verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
        ) {
            guide.cautions.forEach { caution ->
                Row(verticalAlignment = Alignment.Top) {
                    AppIcon(
                        imageVector = AppIcons.Warning,
                        contentDescription = null,
                        tint = AppTheme.colors.warning,
                        size = AppDimens.iconSmall
                    )
                    Spacer(modifier = Modifier.width(AppDimens.s2))
                    Text(
                        text = caution,
                        style = MaterialTheme.typography.body2,
                        color = AppTheme.colors.ink
                    )
                }
            }
        }
    }

    // 视频例子：有直链时直接打开，否则按动作名检索示范视频
    val directVideo = guide.videoUrl
    Spacer(modifier = Modifier.height(AppDimens.s6))
    OutlinedButton(
        onClick = { openExternalUrl(directVideo ?: videoSearchUrl(action.name)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.screenPadding)
            .height(AppDimens.buttonHeight),
        shape = AppShapes.button,
        border = BorderStroke(AppDimens.divider, AppTheme.colors.brand),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.brand)
    ) {
        AppIcon(
            imageVector = AppIcons.Video,
            contentDescription = null,
            tint = AppTheme.colors.brand,
            size = AppDimens.icon
        )
        Spacer(modifier = Modifier.width(AppDimens.s2))
        Text(
            text = if (directVideo != null) "观看视频演示" else "查找视频示范",
            style = MaterialTheme.typography.subtitle1
        )
        if (directVideo == null) {
            Spacer(modifier = Modifier.width(AppDimens.s2))
            Text(
                text = "外部打开",
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.inkMuted
            )
        }
    }
}

/** 详情页顶部的小信息胶囊 */
@Composable
private fun GuideChip(text: String) {
    Surface(
        shape = AppShapes.pill,
        color = AppTheme.colors.surfaceAlt
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = AppTheme.colors.inkSub,
            modifier = Modifier.padding(horizontal = AppDimens.s3, vertical = AppDimens.s2)
        )
    }
}
