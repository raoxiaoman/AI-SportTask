package pages

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
 */
@Composable
fun LoginScreen(
    authService: AuthService,
    onLoginSuccess: () -> Unit
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
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / 标题
            Text(
                text = "🏋️",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "AI SportTask",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "智能运动任务管理",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 邮箱输入
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("邮箱") },
                placeholder = { Text("your@email.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 密码输入
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("密码") },
                placeholder = { Text("至少 6 位") },
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
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("确认密码") },
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 提交按钮
            Button(
                onClick = { submit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colors.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isLoginMode) "登 录" else "注 册",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            // 服务器配置（折叠的）
            TextButton(
                onClick = { showServerConfig = !showServerConfig }
            ) {
                Text(
                    text = if (showServerConfig) "收起服务器设置 ▲" else "服务器设置 ▼",
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                )
            }

            if (showServerConfig) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://IP:3456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        ServerConfig.setBaseUrl(serverUrl)
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colors.secondary
                    )
                ) {
                    Text("保存服务器地址")
                }
            }
        }
    }
}
