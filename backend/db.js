/**
 * 数据库层 — SQLite 存储
 */
const Database = require('better-sqlite3');
const fs = require('fs');
const path = require('path');

// 生产环境由 systemd 传 DB_PATH（如 /var/lib/sporttask/sporttask.db），
// 本地开发默认落在 backend 目录下，行为与旧版一致
const DB_PATH = process.env.DB_PATH || path.join(__dirname, 'sporttask.db');
let db;

function init() {
  fs.mkdirSync(path.dirname(DB_PATH), { recursive: true });
  db = new Database(DB_PATH);
  db.pragma('journal_mode = WAL');
  db.pragma('foreign_keys = ON');

  // 用户表
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      created_at TEXT DEFAULT (datetime('now'))
    )
  `);

  // 云端分组表
  db.exec(`
    CREATE TABLE IF NOT EXISTS cloud_groups (
      id TEXT PRIMARY KEY,
      user_id INTEGER NOT NULL,
      local_id INTEGER,
      name TEXT NOT NULL,
      created_at TEXT,
      updated_at TEXT DEFAULT (datetime('now')),
      deleted_at TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id)
    )
  `);

  // 云端动作表
  db.exec(`
    CREATE TABLE IF NOT EXISTS cloud_actions (
      id TEXT PRIMARY KEY,
      user_id INTEGER NOT NULL,
      local_id INTEGER,
      group_id TEXT,
      name TEXT NOT NULL,
      steps_text TEXT DEFAULT '',
      default_time INTEGER DEFAULT 30,
      rest_time INTEGER DEFAULT 10,
      order_index INTEGER DEFAULT 1,
      created_at TEXT,
      updated_at TEXT DEFAULT (datetime('now')),
      deleted_at TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id),
      FOREIGN KEY (group_id) REFERENCES cloud_groups(id)
    )
  `);

  // 云端打卡表
  db.exec(`
    CREATE TABLE IF NOT EXISTS cloud_checkins (
      id TEXT PRIMARY KEY,
      user_id INTEGER NOT NULL,
      local_id INTEGER,
      date TEXT NOT NULL,
      group_id TEXT,
      action_id TEXT,
      duration INTEGER DEFAULT 0,
      is_completed INTEGER DEFAULT 1,
      created_at TEXT,
      updated_at TEXT DEFAULT (datetime('now')),
      deleted_at TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id)
    )
  `);

  // 同步时间戳表
  db.exec(`
    CREATE TABLE IF NOT EXISTS sync_state (
      user_id INTEGER PRIMARY KEY,
      last_sync_at TEXT,
      FOREIGN KEY (user_id) REFERENCES users(id)
    )
  `);

  // 通用 KV：客户端本地状态（如方案 SchemeState）随同步上云，
  // 让新设备登录后能拿到与旧设备一致的方案结构，而不只是分组/动作。
  db.exec(`
    CREATE TABLE IF NOT EXISTS cloud_kv (
      user_id INTEGER NOT NULL,
      key TEXT NOT NULL,
      value TEXT,
      updated_at TEXT DEFAULT (datetime('now')),
      PRIMARY KEY (user_id, key),
      FOREIGN KEY (user_id) REFERENCES users(id)
    )
  `);

  return db;
}

// ========== 用户 ==========

function createUser(email, passwordHash) {
  const stmt = db.prepare('INSERT INTO users (email, password_hash) VALUES (?, ?)');
  const result = stmt.run(email, passwordHash);
  return result.lastInsertRowid;
}

function getUserByEmail(email) {
  return db.prepare('SELECT * FROM users WHERE email = ?').get(email);
}

function getUser(id) {
  return db.prepare('SELECT * FROM users WHERE id = ?').get(id);
}

// ========== 云端数据 ==========

function getGroups(userId, since) {
  if (since) {
    return db.prepare('SELECT * FROM cloud_groups WHERE user_id = ? AND updated_at > ? ORDER BY updated_at').all(userId, since);
  }
  return db.prepare('SELECT * FROM cloud_groups WHERE user_id = ? AND deleted_at IS NULL').all(userId);
}

function getActions(userId, since) {
  if (since) {
    return db.prepare('SELECT * FROM cloud_actions WHERE user_id = ? AND updated_at > ? ORDER BY updated_at').all(userId, since);
  }
  return db.prepare('SELECT * FROM cloud_actions WHERE user_id = ? AND deleted_at IS NULL').all(userId);
}

function getCheckins(userId, since) {
  if (since) {
    return db.prepare('SELECT * FROM cloud_checkins WHERE user_id = ? AND updated_at > ? ORDER BY updated_at').all(userId, since);
  }
  return db.prepare('SELECT * FROM cloud_checkins WHERE user_id = ? AND deleted_at IS NULL').all(userId);
}

// ========== 批量写入 ==========

function upsertGroups(userId, groups) {
  const upsert = db.prepare(`
    INSERT INTO cloud_groups (id, user_id, local_id, name, created_at, updated_at, deleted_at)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      name = excluded.name,
      local_id = excluded.local_id,
      updated_at = excluded.updated_at,
      deleted_at = excluded.deleted_at
  `);

  const txn = db.transaction((items) => {
    let count = 0;
    for (const g of items) {
      upsert.run(
        g.id || require('crypto').randomUUID(),
        userId,
        g.local_id || null,
        g.name,
        g.created_at || new Date().toISOString(),
        g.updated_at || new Date().toISOString(),
        g.deleted_at || null
      );
      count++;
    }
    return count;
  });

  return txn(groups);
}

function upsertActions(userId, actions) {
  const upsert = db.prepare(`
    INSERT INTO cloud_actions (id, user_id, local_id, group_id, name, steps_text, default_time, rest_time, order_index, created_at, updated_at, deleted_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      name = excluded.name, steps_text = excluded.steps_text,
      default_time = excluded.default_time, rest_time = excluded.rest_time,
      order_index = excluded.order_index,
      updated_at = excluded.updated_at, deleted_at = excluded.deleted_at
  `);

  const txn = db.transaction((items) => {
    let count = 0;
    for (const a of items) {
      upsert.run(
        a.id || require('crypto').randomUUID(),
        userId, a.local_id || null, a.group_id || null,
        a.name, a.steps_text || '', a.default_time || 30, a.rest_time || 10,
        a.order_index || 1,
        a.created_at || new Date().toISOString(),
        a.updated_at || new Date().toISOString(),
        a.deleted_at || null
      );
      count++;
    }
    return count;
  });

  return txn(actions);
}

function upsertCheckins(userId, checkins) {
  const upsert = db.prepare(`
    INSERT INTO cloud_checkins (id, user_id, local_id, date, group_id, action_id, duration, is_completed, created_at, updated_at, deleted_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    ON CONFLICT(id) DO UPDATE SET
      date = excluded.date, duration = excluded.duration,
      is_completed = excluded.is_completed,
      updated_at = excluded.updated_at, deleted_at = excluded.deleted_at
  `);

  const txn = db.transaction((items) => {
    let count = 0;
    for (const c of items) {
      upsert.run(
        c.id || require('crypto').randomUUID(),
        userId, c.local_id || null,
        c.date, c.group_id || null, c.action_id || null,
        c.duration || 0, c.is_completed ?? 1,
        c.created_at || new Date().toISOString(),
        c.updated_at || new Date().toISOString(),
        c.deleted_at || null
      );
      count++;
    }
    return count;
  });

  return txn(checkins);
}

function getKv(userId) {
  const rows = db.prepare('SELECT key, value FROM cloud_kv WHERE user_id = ?').all(userId);
  const map = {};
  for (const r of rows) map[r.key] = r.value;
  return map;
}

function upsertKv(userId, kv) {
  if (!kv || typeof kv !== 'object' || Array.isArray(kv)) return 0;
  const upsert = db.prepare(`
    INSERT INTO cloud_kv (user_id, key, value, updated_at) VALUES (?, ?, ?, ?)
    ON CONFLICT(user_id, key) DO UPDATE SET value = excluded.value, updated_at = excluded.updated_at
  `);
  const now = new Date().toISOString();
  const txn = db.transaction((pairs) => {
    let count = 0;
    for (const [k, v] of pairs) {
      if (typeof k !== 'string' || !k) continue;
      upsert.run(userId, k, v == null ? null : String(v), now);
      count++;
    }
    return count;
  });
  return txn(Object.entries(kv));
}

function getSyncState(userId) {
  const row = db.prepare('SELECT last_sync_at FROM sync_state WHERE user_id = ?').get(userId);
  return row ? row.last_sync_at : null;
}

function updateSyncState(userId) {
  const now = new Date().toISOString();
  db.prepare(`
    INSERT INTO sync_state (user_id, last_sync_at) VALUES (?, ?)
    ON CONFLICT(user_id) DO UPDATE SET last_sync_at = excluded.last_sync_at
  `).run(userId, now);
  return now;
}

// 初始化数据库
init();

module.exports = {
  createUser, getUserByEmail, getUser,
  getGroups, getActions, getCheckins,
  upsertGroups, upsertActions, upsertCheckins,
  getSyncState, updateSyncState,
  getKv, upsertKv,
  /** 供优雅退出调用，确保 WAL 落盘 */
  close: () => db && db.close()
};
