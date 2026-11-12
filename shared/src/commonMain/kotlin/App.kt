import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import data.Achievement
import data.AchievementManager
import data.SportTaskRepository
import data.remote.AuthService
import data.remote.OfflineMode
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import pages.*
import ui.AchievementDialog
import ui.AppIcon
import ui.AppIcons
import ui.SystemBarAppearanceEffect
import ui.theme.*

private data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun App() {
    // 主题偏好在入口读取并持久化，切换后重启仍保持
    var isDarkMode by remember { mutableStateOf(ThemePreference.isDark()) }

    SportTaskTheme(darkTheme = isDarkMode) {
        // 状态栏/导航栏颜色跟随主题，图标深浅随主题切换
        SystemBarAppearanceEffect(
            isDark = isDarkMode,
            statusBarColor = AppTheme.colors.background,
            navigationBarColor = AppTheme.colors.surface
        )
        val authService = remember { AuthService() }
        // 首屏同步判定：已登录，或此前选择过「离线使用」，都直接进入主界面。
        // 离线标记只影响登录拦截，不伪造登录态，设置页仍显示「未登录」。
        var showMainApp by remember {
            mutableStateOf(authService.restoreSession() || OfflineMode.isEnabled())
        }

        if (!showMainApp) {
            LoginScreen(
                authService = authService,
                onLoginSuccess = {
                    OfflineMode.setEnabled(false)
                    showMainApp = true
                },
                onOfflineStart = {
                    OfflineMode.setEnabled(true)
                    showMainApp = true
                }
            )
        } else {
            MainAppScreen(
                authService = authService,
                onThemeChange = { enabled ->
                    ThemePreference.setDark(enabled)
                    isDarkMode = enabled
                },
                onLogout = {
                    authService.logout()
                    OfflineMode.setEnabled(false)
                    showMainApp = false
                },
                onRequestLogin = {
                    OfflineMode.setEnabled(false)
                    showMainApp = false
                }
            )
        }
    }
}

/**
 * 主应用界面（登录后）。
 * 顶部/底部一律使用中性色，品牌色只出现在选中态、进度与主操作上，
 * 蓝色覆盖面积控制在 10% 以内。
 */
@Composable
fun MainAppScreen(
    authService: AuthService,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onRequestLogin: () -> Unit
) {
    var showAchievements by remember { mutableStateOf(false) }
    var newAchievements by remember { mutableStateOf<List<Achievement>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }
    // 训练执行页为沉浸式：隐藏底部导航
    var immersive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember { SportTaskRepository }

    val navItems = listOf(
        NavItem("训练", AppIcons.Training),
        NavItem("分组", AppIcons.Groups),
        NavItem("打卡", AppIcons.Checkin),
        NavItem("统计", AppIcons.Stats),
        NavItem("设置", AppIcons.Settings)
    )

    Scaffold(
        backgroundColor = AppTheme.colors.background,
        bottomBar = {
            if (!immersive) {
                BottomNavigation(
                    backgroundColor = AppTheme.colors.surface,
                    contentColor = AppTheme.colors.inkSub,
                    elevation = AppDimens.divider
                ) {
                    navItems.forEachIndexed { index, item ->
                        val selected = selectedIndex == index
                        BottomNavigationItem(
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (selected) AppTheme.colors.brandSoft
                                            else AppTheme.colors.surface,
                                            shape = AppShapes.chip
                                        )
                                        .padding(
                                            horizontal = AppDimens.s2,
                                            vertical = AppDimens.s1
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppIcon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (selected) AppTheme.colors.brandStrong
                                        else AppTheme.colors.inkMuted,
                                        size = AppDimens.icon
                                    )
                                }
                            },
                            label = {
                                Text(text = item.label, style = MaterialTheme.typography.caption)
                            },
                            selected = selected,
                            onClick = { selectedIndex = index },
                            selectedContentColor = AppTheme.colors.brandStrong,
                            unselectedContentColor = AppTheme.colors.inkMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedIndex) {
                0 -> TrainingScreen(
                    onTrainingComplete = {
                        scope.launch {
                            val achs = AchievementManager.checkNewAchievements(repository)
                            if (achs.isNotEmpty()) {
                                newAchievements = achs
                                showAchievements = true
                            }
                        }
                    },
                    onImmersiveChange = { immersive = it }
                )
                1 -> GroupScreen()
                2 -> CheckinScreen()
                3 -> StatisticsScreen()
                4 -> SettingsScreen(
                    onThemeChange = onThemeChange,
                    authService = authService,
                    onLogout = onLogout,
                    onRequestLogin = onRequestLogin
                )
                else -> TrainingScreen(onImmersiveChange = { immersive = it })
            }
        }
    }

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

expect fun getPlatformName(): String
