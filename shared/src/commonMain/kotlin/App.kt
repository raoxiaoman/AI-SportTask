import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import data.Achievement
import data.AchievementManager
import data.SportTaskRepository
import data.remote.AuthService
import kotlinx.coroutines.launch
import pages.*
import ui.AchievementDialog

@Composable
fun App() {
    // 认证服务 — 全局单例
    val authService = remember { AuthService() }

    // 登录状态
    var isLoggedIn by remember { mutableStateOf(false) }
    var isCheckingLogin by remember { mutableStateOf(true) }

    // 检查本地是否已有登录态
    LaunchedEffect(Unit) {
        isLoggedIn = authService.restoreSession()
        isCheckingLogin = false
    }

    if (isCheckingLogin) {
        // 启动加载页
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!isLoggedIn) {
        LoginScreen(
            authService = authService,
            onLoginSuccess = {
                isLoggedIn = true
            }
        )
        return
    }

    // ===== 已登录，显示主界面 =====
    MainAppScreen(
        authService = authService,
        onLogout = {
            authService.logout()
            isLoggedIn = false
        }
    )
}

/**
 * 主应用界面（登录后）
 */
@Composable
fun MainAppScreen(
    authService: AuthService,
    onLogout: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(false) }

    // 成就系统状态
    var showAchievements by remember { mutableStateOf(false) }
    var newAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val repository = remember { SportTaskRepository }

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
                    0 -> TrainingScreen(
                        onTrainingComplete = {
                            // 训练完成，检查成就
                            scope.launch {
                                val achs = AchievementManager.checkNewAchievements(repository)
                                if (achs.isNotEmpty()) {
                                    newAchievements = achs
                                    showAchievements = true
                                }
                            }
                        }
                    )
                    1 -> GroupScreen()
                    2 -> CheckinScreen()
                    3 -> StatisticsScreen()
                    4 -> SettingsScreen(
                        onThemeChange = { isDarkMode = it },
                        authService = authService,
                        onLogout = onLogout
                    )
                    else -> TrainingScreen()
                }
            }
        }

        // 成就解锁弹窗
        if (showAchievements && newAchievements.isNotEmpty()) {
            AchievementDialog(
                achievements = newAchievements,
                onDismiss = {
                    showAchievements = false
                    newAchievements = emptyList()
                }
            )
        }
    }
}

expect fun getPlatformName(): String
