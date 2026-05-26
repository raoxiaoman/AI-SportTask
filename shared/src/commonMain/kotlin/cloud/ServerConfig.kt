package cloud

/**
 * 服务器配置
 *
 * API_BASE_URL — 后端服务地址
 * ============================
 *
 * 开发阶段指向 VPS IP:
 *   http://23.94.233.92:3456
 *
 * 上线前请改为正式域名:
 *   https://api.sporttask.xyz
 *   (并配置 HTTPS 证书)
 *
 * ⚠️ 公开仓库会暴露 IP 地址，但当前仅限开发/内测阶段使用。
 *    产品上线前务必换成域名。
 */
object ServerConfig {
    const val API_BASE_URL = "http://23.94.233.92:3456"
}
