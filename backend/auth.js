/**
 * 认证模块 — 注册/登录/JWT
 */
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('./db');

const JWT_SECRET = process.env.JWT_SECRET || 'sporttask-secret-key-change-in-production';
const SALT_ROUNDS = 10;

function signup(email, password) {
  if (!email || !password) {
    throw new Error('邮箱和密码不能为空');
  }
  if (password.length < 6) {
    throw new Error('密码长度至少 6 位');
  }
  if (!email.includes('@')) {
    throw new Error('邮箱格式不正确');
  }

  const existing = db.getUserByEmail(email);
  if (existing) {
    throw new Error('该邮箱已注册');
  }

  const hash = bcrypt.hashSync(password, SALT_ROUNDS);
  const userId = db.createUser(email, hash);

  const token = jwt.sign({ userId, email }, JWT_SECRET, { expiresIn: '30d' });

  return {
    success: true,
    token,
    user: { id: userId, email },
    message: '注册成功'
  };
}

function signin(email, password) {
  if (!email || !password) {
    throw new Error('邮箱和密码不能为空');
  }

  const user = db.getUserByEmail(email);
  if (!user) {
    throw new Error('邮箱或密码错误');
  }

  const valid = bcrypt.compareSync(password, user.password_hash);
  if (!valid) {
    throw new Error('邮箱或密码错误');
  }

  const token = jwt.sign({ userId: user.id, email: user.email }, JWT_SECRET, { expiresIn: '30d' });

  return {
    success: true,
    token,
    user: { id: user.id, email: user.email },
    message: '登录成功'
  };
}

// 公开路由列表（不需要 token）
const PUBLIC_PATHS = ['/api/health', '/api/auth/signup', '/api/auth/signin'];

// JWT 中间件 — 解析 token，挂在 req.userId 上
function middleware(req, res, next) {
  // 公开路径跳过验证
  if (PUBLIC_PATHS.some(p => req.path === p || req.path.startsWith(p))) {
    return next();
  }

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    // 不需要认证的继续，需要认证的路由自己检查
    return next();
  }

  const token = authHeader.split(' ')[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.userId = decoded.userId;
    req.userEmail = decoded.email;
  } catch (e) {
    // token 无效，但不阻止请求（requireAuth 会拦截）
  }
  next();
}

// 需要认证的路由守卫
function requireAuth(req, res, next) {
  if (!req.userId) {
    return res.status(401).json({ error: '未登录或 token 已过期' });
  }
  next();
}

module.exports = { signup, signin, middleware, requireAuth };
