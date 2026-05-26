package cloud

/**
 * Supabase 配置常量
 *
 * 首次使用时替换为实际的 Supabase URL 和 anon key：
 * 1. 在 https://supabase.com 注册并创建项目
 * 2. 项目 Settings → API → Project URL & anon public key
 * 3. 替换下面的占位值
 */
object SupabaseConfig {
    /** Supabase 项目 URL */
    const val SUPABASE_URL = "https://your-project-id.supabase.co"

    /** Supabase anon/public key（非 service_role key） */
    const val SUPABASE_ANON_KEY = "your-anon-key-here"
}
