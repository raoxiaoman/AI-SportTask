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

// 打卡屏幕
@Composable
fun CheckinScreen() {
    var isLoading by remember { mutableStateOf(true) }
    var checkins by remember { mutableStateOf<List<CheckinItem>>(emptyList()) }
    val repository = SportTaskRepository()

    LaunchedEffect(Unit) {
        val today = LocalDate.now().toString()
        val weekAgo = LocalDate.now().minusDays(7).toString()
        val dbCheckins = withContext(Dispatchers.Default) {
            repository.getCheckinsByDateRange(weekAgo, today)
        }
        checkins = dbCheckins.map { checkin ->
            CheckinItem(
                id = checkin.id,
                date = checkin.date,
                duration = checkin.duration?.toInt() ?: 0,
                isCompleted = checkin.is_completed == 1L
            )
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("打卡记录") }
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
                Text(
                    text = "暂无打卡记录",
                    style = MaterialTheme.typography.body1,
                    color = Color.Gray
                )
            }
        } else {
            Text(
                text = "打卡记录功能开发中",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        }
    }
}

data class CheckinItem(
    val id: Long,
    val date: String,
    val duration: Int,
    val isCompleted: Boolean
)