/**
 * AI SportTask 云同步后端
 *
 * 轻量级 REST API 服务，提供：
 * - 用户注册/登录 (JWT 认证)
 * - 数据同步 (Pull/Push)
 * - SQLite 本地存储
 *
 * 部署方式见 ./DEPLOY.md：生产环境由 systemd 拉起，Caddy 反代到 127.0.0.1:3456
 *
 * 环境变量：
 *   PORT         监听端口，默认 3456
 *   HOST         监听地址，默认 127.0.0.1（生产只允许本机反代访问）
 *   DB_PATH      SQLite 文件路径，默认 ./sporttask.db
 *   JWT_SECRET   签名密钥，**生产必须设置**，未设置时启动告警
 *   SIGNUP_CODE  可选；设置后注册需带同样的 code 字段，防止公网被刷注册
 *   TRUST_PROXY  反代层数，默认 1（Caddy/Nginx 在上一层）
 */
const express = require('express');
const cors = require('cors');
const fs = require('fs');
const path = require('path');
const db = require('./db');
const auth = require('./auth');
const sync = require('./sync');

const app = express();
const PORT = Number(process.env.PORT || 3456);
const HOST = process.env.HOST || '127.0.0.1';
const SIGNUP_CODE = process.env.SIGNUP_CODE || '';
const TRUST_PROXY = Number(process.env.TRUST_PROXY ?? 1);

if (!process.env.JWT_SECRET) {
  console.warn('[warn] JWT_SECRET 未设置，正在使用代码内置默认值，生产环境必须覆盖！');
}

// 位于 Caddy 之后，需要信任一层代理才能拿到真实客户端 IP（限流依赖它）
app.set('trust proxy', TRUST_PROXY);
app.disable('x-powered-by');

// 中间件
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(auth.middleware); // JWT 验证（公开路由排除）

// ========== 简易限流 ==========
// 只为挡住公网暴力破解与刷注册，不做分布式限流，重启即清零
const buckets = new Map();
const WINDOW_MS = 10 * 60 * 1000;
const MAX_HITS = 20;

function rateLimit(req, res, next) {
  const key = req.ip || 'unknown';
  const now = Date.now();
  const bucket = buckets.get(key);

  if (!bucket || now - bucket.start > WINDOW_MS) {
    buckets.set(key, { start: now, count: 1 });
    return next();
  }
  bucket.count += 1;
  if (bucket.count > MAX_HITS) {
    const retry = Math.ceil((WINDOW_MS - (now - bucket.start)) / 1000);
    res.set('Retry-After', String(retry));
    return res.status(429).json({ success: false, error: '请求过于频繁，请稍后再试' });
  }
  next();
}

// 定期清理过期桶，避免长期运行内存增长
setInterval(() => {
  const now = Date.now();
  for (const [key, bucket] of buckets) {
    if (now - bucket.start > WINDOW_MS) buckets.delete(key);
  }
}, WINDOW_MS).unref();

// ========== 公开路由 ==========

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', version: '1.0.0', time: new Date().toISOString() });
});

// 训练方案模板库（公开只读）：客户端内置同名模板作离线兜底，这里只是分发/更新渠道。
// 启动时读一次即可，数据是随代码部署的静态文件；systemd 下代码目录只读，不能运行时写入。
const TEMPLATE_FILE = path.join(__dirname, 'data', 'templates.json');
let templatePayload = { version: 1, templates: [] };
try {
  const parsed = JSON.parse(fs.readFileSync(TEMPLATE_FILE, 'utf8'));
  if (Array.isArray(parsed.templates)) {
    templatePayload = { version: Number(parsed.version) || 1, templates: parsed.templates };
    console.log(`[templates] loaded ${templatePayload.templates.length} templates (v${templatePayload.version})`);
  } else {
    console.warn(`[warn] ${TEMPLATE_FILE} 缺少 templates 数组，模板库将返回空列表`);
  }
} catch (e) {
  console.warn(`[warn] 读取模板文件失败：${e.message}`);
}

app.get('/api/templates', (req, res) => {
  res.json(templatePayload);
});

// 注册
app.post('/api/auth/signup', rateLimit, async (req, res) => {
  try {
    const { email, password, code } = req.body || {};
    if (SIGNUP_CODE && code !== SIGNUP_CODE) {
      return res.status(403).json({ success: false, error: '注册码不正确' });
    }
    const result = auth.signup(email, password);
    res.json(result);
  } catch (e) {
    res.status(400).json({ success: false, error: e.message });
  }
});

// 登录
app.post('/api/auth/signin', rateLimit, async (req, res) => {
  try {
    const { email, password } = req.body || {};
    const result = auth.signin(email, password);
    res.json(result);
  } catch (e) {
    res.status(401).json({ success: false, error: e.message });
  }
});

// ========== 需要认证的路由 ==========

// 获取用户信息
app.get('/api/user', auth.requireAuth, (req, res) => {
  const user = db.getUser(req.userId);
  if (!user) return res.status(404).json({ error: 'User not found' });
  res.json({ id: user.id, email: user.email, createdAt: user.created_at });
});

// 数据同步 - 拉取云端变更
app.post('/api/sync/pull', auth.requireAuth, (req, res) => {
  const { lastSyncTimestamp } = req.body || {};
  const data = sync.pull(req.userId, lastSyncTimestamp);
  res.json(data);
});

// 数据同步 - 推送本地变更
app.post('/api/sync/push', auth.requireAuth, (req, res) => {
  const { groups, actions, checkins } = req.body || {};
  const result = sync.push(req.userId, groups, actions, checkins);
  res.json(result);
});

// 获取所有数据（首次同步用）
app.get('/api/sync/full', auth.requireAuth, (req, res) => {
  const data = sync.getFullData(req.userId);
  res.json(data);
});

// 兜底 404，避免返回 HTML
app.use((req, res) => {
  res.status(404).json({ success: false, error: 'Not found' });
});

// 统一错误处理：不要把栈打到响应里
app.use((err, req, res, next) => {
  console.error('[error]', err.message);
  res.status(500).json({ success: false, error: 'Internal error' });
});

const server = app.listen(PORT, HOST, () => {
  console.log(`🏋️ SportTask Backend running on http://${HOST}:${PORT}`);
  console.log(`   Health: http://${HOST}:${PORT}/api/health`);
});

// 优雅退出：先停止接收新连接，再关数据库，避免 WAL 未落盘
function shutdown(signal) {
  console.log(`[${signal}] 正在关闭…`);
  server.close(() => {
    try {
      db.close();
    } catch (_) {
      // 忽略重复关闭
    }
    process.exit(0);
  });
  // 10 秒还没退干净就强制结束
  setTimeout(() => process.exit(1), 10_000).unref();
}

process.on('SIGTERM', () => shutdown('SIGTERM'));
process.on('SIGINT', () => shutdown('SIGINT'));
