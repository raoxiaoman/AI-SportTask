package pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.SportTaskRepository
import data.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// 打卡屏幕
@Composable
fun CheckinScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var checkins by remember { mutableStateOf<List<CheckinItemWithGroup>>(emptyList()) }
    val repository = SportTaskRepository()

    LaunchedEffect(Unit) {
        val today = LocalDate.now()
        val monthAgo = today.minusDays(30)
        val dbCheckins = withContext(Dispatchers.Default) {
            repository.getCheckinsByDateRange(monthAgo.toString(), today.toString())
        }

        // 获取所有分组名称
        val groups = withContext(Dispatchers.Default) {
            repository.getGroups()
        }
        val groupMap = groups.associateBy { it.id }

        checkins = dbCheckins.map { checkin ->
            CheckinItemWithGroup(
                id = checkin.id,
                date = checkin.date,
                duration = checkin.duration?.toInt() ?: 0,
                isCompleted = checkin.is_completed == 1L,
                groupName = checkin.group_id?.let { groupMap[it]?.name } ?: "自由训练"
            )
        }.sortedByDescending { it.date }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("✅ 打卡记录") }
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
        } else if (checkins.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.h1
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无打卡记录",
                        style = MaterialTheme.typography.body1,
                        color = Color.Gray
                    )
                    Text(
                        text = "开始训练来完成你的第一次打卡吧！",
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "最近30天",
                        style = MaterialTheme.typography.subtitle2,
                        color = Color.Gray
                    )
                }

                // 按日期分组显示
                val groupedCheckins = checkins.groupBy { it.date }
                groupedCheckins.forEach { (date, dayCheckins) ->
                    item {
                        DateSection(
                            date = date,
                            checkins = dayCheckins
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DateSection(date: String, checkins: List<CheckinItemWithGroup>) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
    val parsedDate = java.time.LocalDate.parse(date)
    val formattedDate = parsedDate.format(dateFormatter)
    val dayOfWeek = parsedDate.format(dayFormatter)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dayOfWeek,
                        style = MaterialTheme.typography.body2,
                        color = Color.Gray
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${checkins.size} 次训练",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary
                    )
                    Text(
                        text = formatTime(checkins.sumOf { it.duration }),
                        style = MaterialTheme.typography.body1,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            checkins.forEach { checkin ->
                CheckinItemRow(checkin = checkin)
                if (checkin != checkins.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun CheckinItemRow(checkin: CheckinItemWithGroup) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 状态指示器
        Box(
            modifier = Modifier
                .size(8.dp)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (checkin.isCompleted) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .padding(2.dp)
                ) {
                    // 完成状态 - 实心圆
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 训练类型
        Text(
            text = checkin.groupName,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.weight(1f)
        )

        // 时长
        Text(
            text = formatTime(checkin.duration),
            style = MaterialTheme.typography.body2,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 完成状态标签
        if (checkin.isCompleted) {
            Text(
                text = "完成",
                style = MaterialTheme.typography.caption,
                color = Color(0xFF4CAF50),
                modifier = Modifier
                    .background(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

data class CheckinItemWithGroup(
    val id: Long,
    val date: String,
    val duration: Int,
    val isCompleted: Boolean,
    val groupName: String
)