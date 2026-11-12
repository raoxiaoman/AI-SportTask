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
import pages.openExternalUrl
import ui.theme.*

/**
 * 动作详情弹窗（全屏）：示意图 + 分步操作 + 质量要点 + 疼痛信号。
 *
 * 数据来源：内置动作指导库 [guideFor]，未收录动作由调用方传入
 * [buildFallbackGuide] 的兜底结果。视频入口：videoUrl 非空时直达演示视频，
 * 否则按动作名打开 [videoSearchUrl] 生成的示范视频检索。
 */
@Composable
fun ActionGuideDialog(
    action: ActionItem,
    guide: ExerciseGuide,
    onDismiss: () -> Unit,
    /** 训练执行中打开时提示计时仍在继续 */
    showTimingHint: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Dialog 自带独立 Window：把它的系统栏同步到当前主题，
        // 否则全屏详情页会出现深灰状态栏/导航栏，与主页面的沉浸式表现割裂。
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
                // 头部：返回 + 动作名 + 一句话概括
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimens.s1, vertical = AppDimens.s2),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        AppIcon(
                            imageVector = AppIcons.Back,
                            contentDescription = "关闭详情",
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

                Spacer(modifier = Modifier.height(AppDimens.s4))

                // 训练中打开的提示 + 底部收尾
                if (showTimingHint) {
                    Text(
                        text = "查看详情时计时仍在继续，需要可先按暂停",
                        style = MaterialTheme.typography.caption,
                        color = AppTheme.colors.inkMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(AppDimens.s2))
                }
                Spacer(modifier = Modifier.height(AppDimens.s6))
            }
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
