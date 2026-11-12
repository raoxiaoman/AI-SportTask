package pages
import ui.AppIcon
import ui.AppIcons
import ui.AppTextField
import ui.theme.*


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.remote.AuthService
import data.remote.ServerConfig
import kotlinx.coroutines.launch

/**
 * 登录/注册页面
 *
 * @param authService 认证服务实例
 * @param onLoginSuccess 登录成功回调
 * @param onOfflineStart 选择「先不登录 · 离线使用」回调（服务端未部署时使用本地功能）
 */
@Composable
fun LoginScreen(
    authService: AuthService,
    onLoginSuccess: () -> Unit,
    onOfflineStart: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showServerConfig by remember { mutableStateOf(false) }
    var serverUrl by remember { mutableStateOf(ServerConfig.baseUrl) }

    val scope = rememberCoroutineScope()

    // 启动时尝试恢复登录态和服务器配置
    LaunchedEffect(Unit) {
        ServerConfig.restoreFromStorage()
        serverUrl = ServerConfig.baseUrl
        if (authService.restoreSession()) {
            onLoginSuccess()
        }
    }

    fun validate(): String? {
        if (email.isBlank()) return "请输入邮箱"
        if (!email.contains("@")) return "邮箱格式不正确"
        if (password.isBlank()) return "请输入密码"
        if (password.length < 6) return "密码长度至少 6 位"
        if (!isLoginMode && password != confirmPassword) return "两次输入的密码不一致"
        return null
    }

    fun submit() {
        val err = validate()
        if (err != null) {
            errorMessage = err
            return
        }
        isLoading = true
        errorMessage = null

        scope.launch {
            val result = if (isLoginMode) {
                authService.signin(email.trim(), password)
            } else {
                authService.signup(email.trim(), password)
            }

            result.fold(
                onSuccess = {
                    isLoading = false
                    onLoginSuccess()
                },
                onFailure = { e ->
                    isLoading = false
                    errorMessage = e.message ?: "操作失败"
                }
            )
        }
    }

    Scaffold(
        backgroundColor = MaterialTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.s8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / 标题
            AppIcon(
                imageVector = AppIcons.Training,
                contentDescription = null,
                size = AppDimens.iconHero,
                tint = AppTheme.colors.brand
            )
            Spacer(modifier = Modifier.height(AppDimens.s2))
            Text(
                text = "AI SportTask",
                style = MaterialTheme.typography.h4,
                fontWeight = AppTypeWeight.emphasis
            )
            Text(
                text = "智能运动任务管理",
                style = MaterialTheme.typography.body2,
                color = AppTheme.colors.inkSub
            )

            Spacer(modifier = Modifier.height(AppDimens.s10))

            // 邮箱输入
            AppTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = "邮箱",
                placeholder = "your@email.com",
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(AppDimens.s3))

            // 密码输入
            AppTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = "密码",
                placeholder = "至少 6 位",
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isLoginMode) ImeAction.Done else ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // 注册模式下的确认密码
            if (!isLoginMode) {
                Spacer(modifier = Modifier.height(AppDimens.s3))
                AppTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = "确认密码",
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }

            // 错误提示
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(AppDimens.s2))
                Text(
                    text = errorMessage!!,
                    color = AppTheme.colors.danger,
                    style = MaterialTheme.typography.body2
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.s6))

            // 提交按钮
            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.buttonHeight),
                enabled = !isLoading,
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.colors.brandStrong,
                    contentColor = AppTheme.colors.onBrand
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(AppDimens.s6),
                        color = AppTheme.colors.onBrand,
                        strokeWidth = AppDimens.hairline
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "登 录" else "注 册",
                        style = MaterialTheme.typography.subtitle1
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.s4))

            // 切换登录/注册
            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    errorMessage = null
                },
                enabled = !isLoading
            ) {
                Text(
                    text = if (isLoginMode) "没有账号？点击注册"
                    else "已有账号？点击登录"
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.s4))

            // 离线使用：服务端未部署时，本地功能无需账号即可使用
            OutlinedButton(
                onClick = onOfflineStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.buttonHeight),
                enabled = !isLoading,
                shape = AppShapes.button,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = AppTheme.colors.surface,
                    contentColor = AppTheme.colors.brand
                )
            ) {
                Text(
                    text = "先不登录 · 离线使用",
                    style = MaterialTheme.typography.subtitle2
                )
            }
            Spacer(modifier = Modifier.height(AppDimens.s1))
            Text(
                text = "本地功能无需账号；云同步可稍后在设置页开启",
                style = MaterialTheme.typography.caption,
                color = AppTheme.colors.inkMuted
            )

            Spacer(modifier = Modifier.height(AppDimens.s2))

            // 服务器配置（折叠的）
            TextButton(
                onClick = { showServerConfig = !showServerConfig }
            ) {
                Text(
                    text = if (showServerConfig) "收起服务器设置" else "服务器设置",
                    style = MaterialTheme.typography.caption,
                    color = AppTheme.colors.inkMuted
                )
            }

            if (showServerConfig) {
                Spacer(modifier = Modifier.height(AppDimens.s2))
                AppTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = "服务器地址",
                    placeholder = "http://IP:3456",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(AppDimens.s2))
                Button(
                    onClick = {
                        ServerConfig.setBaseUrl(serverUrl)
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.buttonHeight),
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.colors.brandStrong,
                        contentColor = AppTheme.colors.onBrand
                    )
                ) {
                    Text("保存服务器地址", style = MaterialTheme.typography.subtitle2)
                }
            }
        }
    }
}
