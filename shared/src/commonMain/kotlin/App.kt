import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import pages.*

@Composable
fun App() {
    // 深色模式状态管理
    var isDarkMode by remember { mutableStateOf(false) }

    val colors = if (isDarkMode) {
        darkColors(
            primary = androidx.compose.ui.graphics.Color(0xFF90CAF9),
            secondary = androidx.compose.ui.graphics.Color(0xFFCE93D8),
            background = androidx.compose.ui.graphics.Color(0xFF121212),
            surface = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
            onPrimary = androidx.compose.ui.graphics.Color.Black,
            onSecondary = androidx.compose.ui.graphics.Color.Black,
            onBackground = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color.White
        )
    } else {
        lightColors(
            primary = androidx.compose.ui.graphics.Color(0xFF1976D2),
            secondary = androidx.compose.ui.graphics.Color(0xFF7B1FA2),
            background = androidx.compose.ui.graphics.Color(0xFFFAFAFA),
            surface = androidx.compose.ui.graphics.Color.White,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            onBackground = androidx.compose.ui.graphics.Color.Black,
            onSurface = androidx.compose.ui.graphics.Color.Black
        )
    }

    MaterialTheme(colors = colors) {
        val navTitles = listOf("训练", "分组", "打卡", "统计", "设置")

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
                                4 -> "⚙️"
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
                    0 -> TrainingScreen(onTrainingComplete = { /* 成就提示 */ })
                    1 -> GroupScreen()
                    2 -> CheckinScreen()
                    3 -> StatisticsScreen()
                    4 -> SettingsScreen(onThemeChange = { isDarkMode = it })
                    else -> TrainingScreen()
                }
            }
        }
    }
}

expect fun getPlatformName(): String
