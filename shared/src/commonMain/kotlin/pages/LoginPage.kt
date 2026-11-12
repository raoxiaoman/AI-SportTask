package pages
import ui.AppIcon
import ui.AppIcons
import ui.theme.*


import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cloud.AuthService
import cloud.SyncManager
import kotlinx.coroutines.launch

/**
 * 登录/注册页面
 *
 * v0.3.0: 使用本地 AuthService，后续接入 Supabase Auth。
 * 支持邮箱+密码注册/登录，含表单验证和加载状态。
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val authState by AuthService.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 监听登录成功
    LaunchedEffect(authState) {
        if (authState is cloud.AuthService.AuthState.SignedIn) {
            // 首次登录执行初始同步
            scope.launch {
                SyncManager.initialSync()
            }
            onLoginSuccess()
        }
        if (authState is cloud.AuthService.AuthState.Error) {
            errorMessage = (authState as cloud.AuthService.AuthState.Error).message
        }
    }

    Scaffold(
        backgroundColor = MaterialTheme.colors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = AppDimens.s8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 标题图标
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

            Spacer(modifier = Modifier.height(AppDimens.s1))

            Text(
                text = "运动训练打卡助手",
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(AppDimens.s10))

            // 邮箱输入
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    errorMessage = null
                },
                label = { Text("邮箱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is cloud.AuthService.AuthState.Loading
            )

            Spacer(modifier = Modifier.height(AppDimens.s3))

            // 密码输入
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = authState !is cloud.AuthService.AuthState.Loading
            )

            // 错误提示
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(AppDimens.s2))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colors.error,
                    style = MaterialTheme.typography.body2
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.s6))

            // 登录/注册按钮
            Button(
                onClick = {
                    scope.launch {
                        errorMessage = null
                        if (isRegisterMode) {
                            AuthService.signUp(email, password)
                        } else {
                            AuthService.signIn(email, password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimens.s12),
                enabled = authState !is cloud.AuthService.AuthState.Loading
            ) {
                if (authState is cloud.AuthService.AuthState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colors.onPrimary,
                        strokeWidth = AppDimens.hairline,
                        modifier = Modifier.size(AppDimens.s6)
                    )
                } else {
                    Text(
                        text = if (isRegisterMode) "注  册" else "登  录",
                        style = MaterialTheme.typography.button
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.s4))

            // 切换登录/注册
            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    errorMessage = null
                },
                enabled = authState !is cloud.AuthService.AuthState.Loading
            ) {
                Text(
                    text = if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册",
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}
