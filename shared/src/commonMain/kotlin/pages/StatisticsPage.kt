package pages

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
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import ui.EmptyState
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
    var monthlyCount by remember { mutableStateOf(0) }
    var monthlyDuration by remember { mutableStateOf(0) }
    var consecutiveDays by remember { mutableStateOf(0) }
    var totalTrainingCount by remember { mutableStateOf(0) }
    var dailyStats by remember { mutableStateOf<List<DailyStat>>(emptyList()) }
    var groupCount by remember { mutableStateOf(0) }
    val repository = SportTaskRepository

    LaunchedEffect(Unit) {
        val today = todayDateString()
        val weekStart = daysAgoDateString(6)
        val weekEnd = today
        val monthStart = daysAgoDateString(29)
        val monthEnd = today

        // 获取本周数据
        val weeklySummary = withContext(Dispatchers.Default) {
            repository.getDailySummary(weekStart, weekEnd)
        }
        weeklyCount = weeklySummary.sumOf { it.count.toInt() }
        weeklyDuration = weeklySummary.sumOf { it.total_duration.toInt() }

        // 获取本月数据
        val monthlySummary = withContext(Dispatchers.Default) {
            repository.getDailySummary(monthStart, monthEnd)
        }
        monthlyCount = monthlySummary.sumOf { it.count.toInt() }
        monthlyDuration = monthlySummary.sumOf { it.total_duration.toInt() }

        // 获取每日数据用于图表
        dailyStats = weeklySummary.map { summary ->
            DailyStat(
                date = summary.date,
                count = summary.count.toInt(),
                duration = summary.total_duration.toInt()
            )
        }

        // 计算连续打卡天数
        val allCheckins = withContext(Dispatchers.Default) {
            repository.getCheckinsByDateRange("2024-01-01", today.toString())
        }
        val uniqueDates = allCheckins.map { it.date }.distinct().sorted()
        consecutiveDays = calculateConsecutiveDays(uniqueDates)

        // 总训练次数
        totalTrainingCount = allCheckins.size

        groupCount = repository.getGroups().size
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 统计数据") }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            FullScreenLoading(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // 连续打卡卡片
                    ConsecutiveDaysCard(consecutiveDays = consecutiveDays)
                }

                item {
                    // 本周统计卡片
                    WeeklyStatsCard(
                        weeklyCount = weeklyCount,
                        weeklyDuration = weeklyDuration
                    )
                }

                item {
                    // 本月统计卡片
                    MonthlyStatsCard(
                        monthlyCount = monthlyCount,
                        monthlyDuration = monthlyDuration
                    )
                }

                item {
                    // 每日训练图表
                    WeeklyChartCard(dailyStats = dailyStats)
                }

                item {
                    // 总统计
                    TotalStatsCard(totalTrainingCount = totalTrainingCount, totalGroups = groupCount)
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ConsecutiveDaysCard(consecutiveDays: Int) {
    val isHighStreak = consecutiveDays >= 7
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = if (isHighStreak) {
            MaterialTheme.colors.primary
        } else {
            MaterialTheme.colors.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "🔥 连续打卡",
                    style = MaterialTheme.typography.subtitle1,
                    color = if (isHighStreak) Color.White else Color.Gray
                )
                Text(
                    text = "保持习惯，继续加油！",
                    style = MaterialTheme.typography.body2,
                    color = if (isHighStreak) Color.White.copy(alpha = 0.8f) else Color.Gray
                )
            }
            Text(
                text = "$consecutiveDays 天",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
                color = if (isHighStreak) Color.White else MaterialTheme.colors.primary
            )
        }
    }
}

@Composable
fun WeeklyStatsCard(weeklyCount: Int, weeklyDuration: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📅 本周训练",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$weeklyCount",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "训练次数",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(weeklyDuration),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "总时长",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (weeklyCount > 0) weeklyDuration / weeklyCount else 0}",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = "平均分钟",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyStatsCard(monthlyCount: Int, monthlyDuration: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📆 本月训练",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$monthlyCount",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                    Text(
                        text = "训练次数",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(monthlyDuration),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                    Text(
                        text = "总时长",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val daysInMonth = 30
                    val progress = (monthlyCount.toFloat() / daysInMonth * 100).toInt()
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.secondary
                    )
                    Text(
                        text = "完成率",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyChartCard(dailyStats: List<DailyStat>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📈 本周训练趋势",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (dailyStats.isEmpty()) {
                Text(
                    text = "本周暂无训练数据",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else {
                // 简易柱状图
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val maxCount = dailyStats.maxOfOrNull { it.count } ?: 1

                    val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

                    // 生成完整的7天数据
                    val fullWeek = (0..6).map { offset ->
                        val dateStr = daysAgoDateString(6 - offset)
                        dailyStats.find { it.date == dateStr }
                            ?: DailyStat(date = dateStr, count = 0, duration = 0)
                    }

                    fullWeek.forEach { stat ->
                        val dayOfWeek = dayOfWeekFromDate(stat.date)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            // 柱子
                            val height = (stat.count.toFloat() / maxCount * 80).dp.coerceAtLeast(4.dp)
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(height)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        if (stat.count > 0) MaterialTheme.colors.primary
                                        else Color.LightGray
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = dayLabels.getOrElse(dayOfWeek - 1) { "日" }.take(1),
                                style = MaterialTheme.typography.caption,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TotalStatsCard(totalTrainingCount: Int, totalGroups: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        backgroundColor = MaterialTheme.colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalTrainingCount",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "累计训练",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalGroups",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "训练分组",
                    style = MaterialTheme.typography.body2,
                    color = Color.Gray
                )
            }
        }
    }
}

data class DailyStat(
    val date: String,
    val count: Int,
    val duration: Int
)