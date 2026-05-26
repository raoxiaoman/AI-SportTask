/**
 * AI SportTask 云同步后端
 *
 * 轻量级 REST API 服务，提供：
 * - 用户注册/登录 (JWT 认证)
 * - 数据同步 (Pull/Push)
 * - SQLite 本地存储
 *
 * 启动: node server.js
 * 端口: 3456
 */
const express = require('express');
const cors = require('cors');
const path = require('path');
const db = require('./db');
const auth = require('./auth');
const sync = require('./sync');

const app = express();
const PORT = 3456;

// 中间件
app.use(cors());
app.use(express.json({ limit: '10mb' }));
app.use(auth.middleware); // JWT 验证（公开路由排除）

// ========== 公开路由 ==========

// 健康检查
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', version: '1.0.0', time: new Date().toISOString() });
});

// 注册
app.post('/api/auth/signup', async (req, res) => {
  try {
    const { email, password } = req.body;
    const result = auth.signup(email, password);
    res.json(result);
  } catch (e) {
    res.status(400).json({ success: false, error: e.message });
  }
});

// 登录
app.post('/api/auth/signin', async (req, res) => {
  try {
    const { email, password } = req.body;
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
  const { lastSyncTimestamp } = req.body;
  const data = sync.pull(req.userId, lastSyncTimestamp);
  res.json(data);
});

// 数据同步 - 推送本地变更
app.post('/api/sync/push', auth.requireAuth, (req, res) => {
  const { groups, actions, checkins } = req.body;
  const result = sync.push(req.userId, groups, actions, checkins);
  res.json(result);
});

// 获取所有数据（首次同步用）
app.get('/api/sync/full', auth.requireAuth, (req, res) => {
  const data = sync.getFullData(req.userId);
  res.json(data);
});

// 启动服务器
app.listen(PORT, '0.0.0.0', () => {
  console.log(`🏋️ SportTask Backend running on http://0.0.0.0:${PORT}`);
  console.log(`   Health: http://localhost:${PORT}/api/health`);
});
