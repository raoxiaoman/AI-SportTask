package pages
import ui.AppIcon
import ui.AppIcons
import ui.theme.*


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import ui.IconBadge
import ui.LiteCard
import ui.PageHeader
import ui.SectionHeader
import ui.FullScreenLoading
import data.calculateConsecutiveDays
import data.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// 统计屏幕
@Composable
fun StatisticsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var weeklyCount by remember { mutableStateOf(0) }
    var weeklyDuration by remember { mutableStateOf(0) }
    var consecutiveDays by remember { mutableStateOf(0) }
    var totalTrainingCount by remember { mutableStateOf(0) }
    var dailyStats by remember { mutableStateOf<List<DailyStat>>(emptyList()) }
    var groupCount by remember { mutableStateOf(0) }
    var actionCount by remember { mutableStateOf(0) }
    var weeklyRate by remember { mutableStateOf(0) }
    val repository = SportTaskRepository

    LaunchedEffect(Unit) {
        val today = todayDateString()
        // 本周 = 周一 ~ 周日（dayOfWeekFromDate 返回 1=周一）
        val mondayOffset = dayOfWeekFromDate(today) - 1
        val weekStart = daysAgoDateString(mondayOffset)
        val weekEnd = daysAgoDateString(mondayOffset - 6)

        // 获取本周数据
        val weeklySummary = withContext(Dispatchers.Default) {
            repository.getDailySummary(weekStart, weekEnd)
        }
        weeklyCount = weeklySummary.sumOf { it.count.toInt() }
        weeklyDuration = weeklySummary.sumOf { it.total_duration.toInt() }

        // 获取每日数据用于图表
        dailyStats = weeklySummary.map { summary ->
            DailyStat(
                date = summary.date,
                count = summary.count.toInt(),
                duration = summary.total_duration.toInt()
            )
        }

        // 计算当前连续打卡天数（从今天往前推）
        val allCheckins = withContext(Dispatchers.Default) {
            repository.getCheckinsByDateRange("2024-01-01", today)
        }
        val uniqueDates = allCheckins.map { it.date }.distinct().sorted()
        consecutiveDays = calculateCurrentStreak(uniqueDates, today)

        // 总训练次数
        totalTrainingCount = allCheckins.size

        groupCount = repository.getGroups().size
        actionCount = repository.getAllActions().size

        // 本周完成率：计划训练日中已完成的比例（无计划时为 0）
        weeklyRate = runCatching {
            val week = data.plan.PlanRepository.weekRhythm() ?: emptyList()
            val planned = week.count { it.inPlan && it.isTrainingDay }
            if (planned > 0) (week.count { it.isCompleted } * 100 / planned) else 0
        }.getOrDefault(0)

        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        if (isLoading) {
            FullScreenLoading()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.s2)
            ) {
                // 页头
                item {
                    PageHeader(
                        title = "训练数据",
                        eyebrow = null,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "本周",
                                    style = MaterialTheme.typography.caption,
                                    color = AppTheme.colors.inkSub
                                )
                                AppIcon(
                                    imageVector = AppIcons.ChevronDown,
                                    contentDescription = "切换周期",
                                    tint = AppTheme.colors.inkMuted,
                                    size = AppDimens.iconSmall
                                )
                            }
                        }
                    )
                }

                // 连续打卡
                item {
                    ConsecutiveDaysCard(
                        consecutiveDays = consecutiveDays,
                        modifier = Modifier.padding(horizontal = AppDimens.screenPadding)
                    )
                }

                // 本周概览
                item { SectionHeader(text = "本周概览") }
                item {
                    WeekOverviewCard(
                        count = weeklyCount,
                        duration = weeklyDuration,
                        rate = weeklyRate,
                        modifier = Modifier.padding(horizontal = AppDimens.screenPadding)
                    )
                }

                // 训练趋势
                item { SectionHeader(text = "训练趋势") }
                item {
                    WeeklyChartCard(
                        dailyStats = dailyStats,
                        modifier = Modifier.padding(horizontal = AppDimens.screenPadding)
                    )
                }

                // 累计
                item { SectionHeader(text = "累计") }
                item {
                    TotalStatsCard(
                        totalTrainingCount = totalTrainingCount,
                        totalGroups = groupCount,
                        totalActions = actionCount,
                        modifier = Modifier.padding(horizontal = AppDimens.screenPadding)
                    )
                }

                item { Spacer(modifier = Modifier.height(AppDimens.s6)) }
            }
        }
    }
}

@Composable
fun ConsecutiveDaysCard(consecutiveDays: Int, modifier: Modifier = Modifier) {
    LiteCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(
                horizontal = AppDimens.s3,
                vertical = AppDimens.s3
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(
                icon = AppIcons.Streak,
                contentDescription = null,
                tint = AppTheme.colors.success,
                container = AppTheme.colors.successSoft
            )
            Spacer(modifier = Modifier.width(AppDimens.s3))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "连续打卡",
                    style = MaterialTheme.typography.body1,
                    fontWeight = AppTypeWeight.medium,
                    color = AppTheme.colors.ink
                )
                Text(
                    text = "保持习惯，继续加油",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$consecutiveDays",
                    style = MaterialTheme.typography.h3,
                    fontWeight = AppTypeWeight.emphasis,
                    color = AppTheme.colors.brandStrong
                )
                Text(
                    text = " 天",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkSub
                )
            }
        }
    }
}

@Composable
fun WeekOverviewCard(count: Int, duration: Int, rate: Int, modifier: Modifier = Modifier) {
    LiteCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.s6, horizontal = AppDimens.s4),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(value = "$count", label = "训练次数")
            StatColumn(value = formatTime(duration), label = "总时长")
            StatColumn(value = "$rate%", label = "完成率")
        }
    }
}

@Composable
fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.h2,
            fontWeight = AppTypeWeight.emphasis,
            color = AppTheme.colors.brandStrong
        )
        Spacer(modifier = Modifier.height(AppDimens.s1))
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = AppTheme.colors.inkSub
        )
    }
}

@Composable
fun WeeklyChartCard(dailyStats: List<DailyStat>, modifier: Modifier = Modifier) {
    val today = todayDateString()
    // 今天在本周中的下标：0=周一 … 6=周日
    val todayIndex = dayOfWeekFromDate(today) - 1
    LiteCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.s4)
        ) {
            // 柱状图：7 根圆角短柱常驻显示，今天用品牌绿高亮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.chartHeight),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.s2),
                verticalAlignment = Alignment.Bottom
            ) {
                val maxCount = (dailyStats.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
                val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

                (0..6).forEach { i ->
                    val dateStr = daysAgoDateString(todayIndex - i)
                    val stat = dailyStats.find { it.date == dateStr }
                        ?: DailyStat(date = dateStr, count = 0, duration = 0)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // 无数据时保留 20% 基线短柱，避免空图表
                        val fillRatio = stat.count.toFloat() / maxCount
                        val height = (AppDimens.chartHeight * (0.2f + 0.8f * fillRatio))
                            .coerceAtMost(AppDimens.chartHeight)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height)
                                .background(
                                    color = if (i == todayIndex) AppTheme.colors.success
                                    else AppTheme.colors.brandSoft,
                                    shape = AppShapes.chip
                                )
                        )
                        Spacer(modifier = Modifier.height(AppDimens.s1))
                        Text(
                            text = dayLabels[i],
                            style = MaterialTheme.typography.caption,
                            color = if (i == todayIndex) AppTheme.colors.success
                            else AppTheme.colors.inkSub
                        )
                    }
                }
            }

            if (dailyStats.all { it.count == 0 }) {
                Spacer(modifier = Modifier.height(AppDimens.s3))
                Text(
                    text = "本周暂无数据，完成首次训练后生成",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TotalStatsCard(totalTrainingCount: Int, totalGroups: Int, totalActions: Int, modifier: Modifier = Modifier) {
    LiteCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.s6, horizontal = AppDimens.s4),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatColumn(value = "$totalGroups", label = "训练分组")
            StatColumn(value = "$totalActions", label = "训练动作")
            StatColumn(value = "$totalTrainingCount", label = "累计训练")
        }
    }
}

data class DailyStat(
    val date: String,
    val count: Int,
    val duration: Int
)

/**
 * 计算从今天开始的当前连续打卡天数
 * 与 calculateConsecutiveDays（全局最大连续）不同，
 * 此函数只关注从今天起往前的连续天数。
 */
internal fun calculateCurrentStreak(sortedDates: List<String>, today: String): Int {
    if (sortedDates.isEmpty() || sortedDates.last() != today) return 0

    var count = 0
    var expected = today
    for (dateStr in sortedDates.reversed()) {
        if (dateStr == expected) {
            count++
            // 往前推一天
            val parts = expected.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt()
            val d = parts[2].toInt()
            val dayCount = data.absoluteDayCount(y, m, d) - 1
            expected = fromDayCount(dayCount)
        } else {
            break
        }
    }
    return count
}

private fun fromDayCount(totalDays: Int): String {
    val base = data.absoluteDayCount(2025, 1, 1)
    var remaining = totalDays - base
    var y = 2025
    val monthDays = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    while (true) {
        val daysInYear = if ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0) 366 else 365
        if (remaining >= daysInYear) {
            remaining -= daysInYear
            y++
        } else break
    }
    var m = 1
    while (m <= 12) {
        val dim = monthDays[m - 1] + if (m == 2 && ((y % 4 == 0 && y % 100 != 0) || y % 400 == 0)) 1 else 0
        if (remaining >= dim) {
            remaining -= dim
            m++
        } else break
    }
    val d = remaining + 1
    return "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}
