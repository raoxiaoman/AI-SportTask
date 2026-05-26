/**
 * 同步模块 — 数据拉取/推送/首次全量同步
 */
const db = require('./db');
const crypto = require('crypto');

/**
 * 拉取云端增量数据
 */
function pull(userId, lastSyncTimestamp) {
  const groups = db.getGroups(userId, lastSyncTimestamp || undefined);
  const actions = db.getActions(userId, lastSyncTimestamp || undefined);
  const checkins = db.getCheckins(userId, lastSyncTimestamp || undefined);

  const serverTime = db.updateSyncState(userId);

  return {
    groups,
    actions,
    checkins,
    serverTimestamp: serverTime,
    count: groups.length + actions.length + checkins.length
  };
}

/**
 * 推送本地变更到云端
 */
function push(userId, groups, actions, checkins) {
  let groupsImported = 0, actionsImported = 0, checkinsImported = 0;

  if (groups && Array.isArray(groups)) {
    // 为每个分组分配云端 ID
    const enriched = groups.map(g => ({
      ...g,
      id: g.id || crypto.randomUUID(),
      updated_at: new Date().toISOString()
    }));
    groupsImported = db.upsertGroups(userId, enriched);

    // 返回 local_id → cloud_id 映射
    const idMap = {};
    for (const g of enriched) {
      if (g.local_id) idMap[`group_${g.local_id}`] = g.id;
    }
  }

  if (actions && Array.isArray(actions)) {
    const enriched = actions.map(a => ({
      ...a,
      id: a.id || crypto.randomUUID(),
      updated_at: new Date().toISOString()
    }));
    actionsImported = db.upsertActions(userId, enriched);
  }

  if (checkins && Array.isArray(checkins)) {
    const enriched = checkins.map(c => ({
      ...c,
      id: c.id || crypto.randomUUID(),
      updated_at: new Date().toISOString()
    }));
    checkinsImported = db.upsertCheckins(userId, enriched);
  }

  const serverTime = db.updateSyncState(userId);

  return {
    success: true,
    groupsImported,
    actionsImported,
    checkinsImported,
    serverTimestamp: serverTime
  };
}

/**
 * 获取用户全量数据（首次同步用）
 */
function getFullData(userId) {
  const groups = db.getGroups(userId);
  const actions = db.getActions(userId);
  const checkins = db.getCheckins(userId);
  const lastSyncAt = db.getSyncState(userId) || new Date().toISOString();

  return {
    groups,
    actions,
    checkins,
    serverTimestamp: lastSyncAt
  };
}

module.exports = { pull, push, getFullData };
