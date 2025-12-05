import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*

@Composable
fun App() {
    MaterialTheme {
        val navTitles = listOf("训练", "分组", "打卡", "统计")

        var selectedIndex by remember { mutableStateOf(0) }

        Scaffold(
            bottomBar = {
                BottomNavigation {
                    navTitles.forEachIndexed { index, title ->
                        BottomNavigationItem(
                            icon = { Text(when (index) {
                                0 -> "🏋️"
                                1 -> "📁"
                                2 -> "✅"
                                3 -> "📊"
                                else -> "?"
                            }) },
                            label = { Text(title) },
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedIndex) {
                    0 -> TrainingScreen()
                    1 -> GroupScreen()
                    2 -> CheckinScreen()
                    3 -> StatisticsScreen()
                    else -> TrainingScreen()
                }
            }
        }
    }
}

// 训练屏幕
@Composable
fun TrainingScreen() {
    CenterText("训练界面")
}

// 分组屏幕
@Composable
fun GroupScreen() {
    CenterText("分组界面")
}

// 打卡屏幕
@Composable
fun CheckinScreen() {
    CenterText("打卡界面")
}

// 统计屏幕
@Composable
fun StatisticsScreen() {
    CenterText("统计界面")
}

// 辅助组件：居中显示文本
@Composable
fun CenterText(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text)
    }
}

expect fun getPlatformName(): String