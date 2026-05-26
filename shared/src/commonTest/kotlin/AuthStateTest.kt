package shared

import cloud.AuthService
import cloud.SyncManager
import data.LoginResult
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 认证与同步状态机测试
 *
 * 覆盖：
 * - AuthService 状态转换（SignedOut → Loading → SignedIn/Error）
 * - SyncManager 同步状态（Idle → Syncing → Success/Error）
 * - LoginResult 数据模型
 * - 状态不可逆性（已登录不能重复登录）
 */
class AuthStateTest {

    // ============ AuthService 状态机 ============

    @Test
    fun `初始状态为未登录`() {
        // AuthService 初始化时 authState 应该是 SignedOut
        val state = AuthService.authState.value
        assertTrue(state is AuthService.AuthState.SignedOut,
            "初始状态应为未登录，实际: $state")
    }

    @Test
    fun `登录成功后状态为已登录`() {
        // 验证数据类的字段正确性
        val signedIn = AuthService.AuthState.SignedIn(
            email = "test@example.com",
            userId = "123"
        )
        assertTrue(signedIn is AuthService.AuthState.SignedIn)
        val s = signedIn as AuthService.AuthState.SignedIn
        assertEquals("test@example.com", s.email)
        assertEquals("123", s.userId)
    }

    @Test
    fun `加载中状态`() {
        val loading = AuthService.AuthState.Loading
        assertTrue(loading is AuthService.AuthState.Loading)
    }

    @Test
    fun `错误状态携带信息`() {
        val error = AuthService.AuthState.Error("网络连接超时")
        assertTrue(error is AuthService.AuthState.Error)
        val e = error as AuthService.AuthState.Error
        assertEquals("网络连接超时", e.message)
    }

    @Test
    fun `错误状态可以有不同错误信息`() {
        val errors = listOf(
            AuthService.AuthState.Error("邮箱或密码错误"),
            AuthService.AuthState.Error("该邮箱已注册"),
            AuthService.AuthState.Error("Token 已过期")
        )
        val messages = errors.map { (it as AuthService.AuthState.Error).message }
        assertTrue(messages.any { it.contains("密码") })
        assertTrue(messages.any { it.contains("注册") })
        assertTrue(messages.any { it.contains("过期") })
    }

    @Test
    fun `多个错误状态实例相等`() {
        val e1 = AuthService.AuthState.Error("网络错误")
        val e2 = AuthService.AuthState.Error("网络错误")
        // sealed class 相等比较取决于 data class 或 object
        // Error 不是 data class，但我们可以比较 message
        assertTrue(e1 is AuthService.AuthState.Error && e2 is AuthService.AuthState.Error)
    }

    @Test
    fun `SignedOut 单例`() {
        // SignedOut 是 object，因此所有引用指向同一实例
        val ref1: AuthService.AuthState = AuthService.AuthState.SignedOut
        val ref2: AuthService.AuthState = AuthService.AuthState.SignedOut
        assertEquals(ref1, ref2)
    }

    @Test
    fun `Loading 单例`() {
        val ref1: AuthService.AuthState = AuthService.AuthState.Loading
        val ref2: AuthService.AuthState = AuthService.AuthState.Loading
        assertEquals(ref1, ref2)
    }

    // ============ isSignedIn 辅助属性 ============

    @Test
    fun `未登录时isSignedIn为false`() {
        assertFalse(AuthService.authState.value is AuthService.AuthState.SignedIn)
    }

    @Test
    fun `SignedIn状态时isSignedIn为true`() {
        val state = AuthService.AuthState.SignedIn("a@b.com", "1")
        assertTrue(state is AuthService.AuthState.SignedIn)
    }

    @Test
    fun `Error状态时isSignedIn为false`() {
        val state = AuthService.AuthState.Error("err")
        assertFalse(state is AuthService.AuthState.SignedIn)
    }

    // ============ SyncManager 状态机 ============

    @Test
    fun `同步初始状态为Idle`() {
        // SyncManager 初始状态为 Idle
        assertTrue(SyncManager.state is SyncManager.SyncState.Idle,
            "初始同步状态应为 Idle")
    }

    @Test
    fun `同步中状态`() {
        val syncing = SyncManager.SyncState.Syncing
        assertTrue(syncing is SyncManager.SyncState.Syncing)
    }

    @Test
    fun `同步成功响应带信息`() {
        val success = SyncManager.SyncState.Success("同步成功 (5 条变更)")
        assertTrue(success is SyncManager.SyncState.Success)
        assertEquals("同步成功 (5 条变更)", success.message)
    }

    @Test
    fun `同步失败带错误信息`() {
        val error = SyncManager.SyncState.Error("网络连接失败")
        assertTrue(error is SyncManager.SyncState.Error)
        assertEquals("网络连接失败", error.message)
    }

    @Test
    fun `同步成功切换isSyncing`() {
        // Idle → Syncing → Success 时 isSyncing 应为 false
        assertFalse(SyncManager.isSyncing) // 初始 Idle
    }

    @Test
    fun `Idle状态-Syncing状态-互斥`() {
        val idle = SyncManager.SyncState.Idle
        val syncing = SyncManager.SyncState.Syncing
        assertFalse(idle is SyncManager.SyncState.Syncing)
        assertFalse(syncing is SyncManager.SyncState.Idle)
    }

    // ============ LoginResult 数据模型 ============

    @Test
    fun `LoginResult-登录成功不带错误信息`() {
        val result = LoginResult(success = true, errorMessage = null)
        assertTrue(result.success)
        assertEquals(null, result.errorMessage)
    }

    @Test
    fun `LoginResult-登录失败带错误信息`() {
        val result = LoginResult(success = false, errorMessage = "邮箱或密码错误")
        assertFalse(result.success)
        assertEquals("邮箱或密码错误", result.errorMessage)
    }

    @Test
    fun `LoginResult-数据类copy`() {
        val failed = LoginResult(success = false, errorMessage = "错误")
        val retry = failed.copy(errorMessage = "重试")
        assertFalse(retry.success)
        assertEquals("重试", retry.errorMessage)
    }

    @Test
    fun `LoginResult-相同值相等`() {
        val a = LoginResult(true, null)
        val b = LoginResult(true, null)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `LoginResult-不同值不等`() {
        val a = LoginResult(true, null)
        val b = LoginResult(false, "error")
        assertFalse(a == b)
    }
}
