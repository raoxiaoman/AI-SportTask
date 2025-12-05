import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import pages.*

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

expect fun getPlatformName(): String