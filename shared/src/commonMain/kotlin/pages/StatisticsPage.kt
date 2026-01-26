package pages

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

// 统计屏幕
@Composable
fun StatisticsScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var weeklyCount by remember { mutableStateOf(0) }
    var weeklyDuration by remember { mutableStateOf(0) }
    var consecutiveDays by remember { mutableStateOf(0) }
    val repository = SportTaskRepository()

    LaunchedEffect(Unit) {
        val today = LocalDate.now().toString()
        val weekAgo = LocalDate.now().minusDays(7).toString()
        val summary = withContext(Dispatchers.Default) {
            repository.getDailySummary(weekAgo, today)
        }
        weeklyCount = summary.sumOf { it.count.toInt() }
        weeklyDuration = summary.sumOf { it.total_duration.toInt() }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计数据") }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text(
                    text = "本周统计",
                    style = MaterialTheme.typography.h6
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        title = "训练次数",
                        value = "$weeklyCount",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    StatCard(
                        title = "总时长",
                        value = "${weeklyDuration / 60}分",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "连续打卡",
                            style = MaterialTheme.typography.subtitle1
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$consecutiveDays 天",
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.h5
            )
        }
    }
}