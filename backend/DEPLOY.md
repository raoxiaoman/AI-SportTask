# 部署手册（Ubuntu + systemd + Caddy）

> 生产环境形态：Node 进程只监听 `127.0.0.1:3456`，由 Caddy 反代并自动签发 HTTPS 证书。
> 数据库落在 `/var/lib/sporttask/sporttask.db`，每天 02:10 自动备份到 `/var/backups/sporttask`。

```
App ──HTTPS──> Caddy :443 ──HTTP──> 127.0.0.1:3456 (systemd: sporttask)
                                          └── SQLite /var/lib/sporttask/sporttask.db
```

## 一、前置条件

| 项 | 要求 |
| --- | --- |
| 系统 | Ubuntu 22.04 / 24.04 |
| Node | ≥ 20（`node -v` 可查） |
| Caddy | 已运行并占用 80/443，且 `caddy validate` 通过 |
| 域名 | 一个指向该服务器的 A 记录；没有域名可用 `<IP>.sslip.io`（无需任何 DNS 配置） |

> 若 80/443 已被 nginx / 其它反代占用，改用它做反代即可，后端只认 `127.0.0.1:3456`。

## 二、一键部署

```bash
# 在本机仓库根目录执行
tar czf - --exclude=node_modules --exclude='*.db*' -C backend . \
  | ssh root@<SERVER_IP> 'mkdir -p /opt/sporttask/backend && tar xzf - -C /opt/sporttask/backend'
```

然后在服务器上：

```bash
set -e

# 1) 独立系统用户，无登录权限
id sporttask >/dev/null 2>&1 || useradd --system --no-create-home --shell /usr/sbin/nologin sporttask
mkdir -p /opt/sporttask/backend /var/lib/sporttask /etc/sporttask

# 2) 依赖（npm 11 会拦 install 脚本，better-sqlite3 自带预编译包，无需放行）
cd /opt/sporttask/backend && rm -f ._* && npm install --omit=dev --no-audit --no-fund
chown -R sporttask:sporttask /opt/sporttask

# 3) 运行参数（密钥随机生成，权限 600）
cat > /etc/sporttask/backend.env <<EOF
NODE_ENV=production
PORT=3456
HOST=127.0.0.1
TRUST_PROXY=1
DB_PATH=/var/lib/sporttask/sporttask.db
JWT_SECRET=$(openssl rand -hex 32)
EOF
chmod 600 /etc/sporttask/backend.env

# 4) 常驻服务
cp /opt/sporttask/backend/deploy/sporttask.service /etc/systemd/system/
systemctl daemon-reload && systemctl enable --now sporttask
systemctl is-active sporttask && curl -s http://127.0.0.1:3456/api/health
```

## 三、接入 Caddy

```bash
cp /etc/caddy/Caddyfile /etc/caddy/Caddyfile.bak.$(date +%Y%m%d-%H%M%S)
cat >> /etc/caddy/Caddyfile <<'EOF'

# ===== AI SportTask 同步服务（systemd: sporttask.service → 127.0.0.1:3456） =====
sporttask.<SERVER_IP>.sslip.io {
	encode zstd gzip
	reverse_proxy 127.0.0.1:3456
}
EOF
caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
systemctl reload caddy
journalctl -u caddy --since "5 min ago" | grep tls.obtain
```

证书通常在 10 秒内签发完成；日志出现 `certificate obtained successfully` 即成功。

## 四、运行参数

| 变量 | 默认 | 说明 |
| --- | --- | --- |
| `PORT` | 3456 | 监听端口 |
| `HOST` | 127.0.0.1 | 生产**不要**改成 0.0.0.0，交给反代 |
| `DB_PATH` | `./sporttask.db` | 建议指到 `/var/lib/sporttask/` |
| `JWT_SECRET` | 内置占位值 | **必须覆盖**，否则 token 可被伪造 |
| `SIGNUP_CODE` | 空 | 设置后注册必须带同样的 `code`，防止公网被刷号（App 暂不发该字段，开启后需同步改客户端） |
| `TRUST_PROXY` | 1 | 反代层数，限流依赖真实 IP |

## 五、日常运维

```bash
systemctl status sporttask          # 状态
systemctl restart sporttask         # 重启
journalctl -u sporttask -f          # 实时日志
journalctl -u sporttask -n 100 --no-pager   # 最近 100 行
```

备份与恢复：

```bash
/opt/sporttask/backup.sh                                  # 手动备份（在线热备，不锁表）
ls -lh /var/backups/sporttask/                            # 默认保留 14 份
gunzip -c /var/backups/sporttask/sporttask-YYYY-MM-DD.db.gz > /tmp/restore.db
systemctl stop sporttask && cp /tmp/restore.db /var/lib/sporttask/sporttask.db && systemctl start sporttask
```

定时任务在 `/etc/cron.d/sporttask-backup`（每天 02:10）。

## 六、换成自己的域名

```bash
# 1) DNS 加一条 A 记录指向服务器，**只开 DNS，不要开 Cloudflare 橙色云**
#    （橙云会让源站证书签发走 CDN，需要额外配置 DNS-01 或源站证书）
# 2) 替换 Caddy 站点块里的域名后热加载
systemctl reload caddy
# 3) App 登录页「服务器设置」改成新地址
```

同一个域名同时被其他服务使用时，不要重复写站点块，改为在已有站点里加 `handle /api/* { reverse_proxy 127.0.0.1:3456 }`。

## 七、安全清单

- [x] 进程只监听 `127.0.0.1`，公网只能经 Caddy 的 HTTPS 进来
- [x] `JWT_SECRET` 随机生成，存 `/etc/sporttask/backend.env`（600，root only）
- [x] `.env`、`*.db` 已在 `.gitignore` 中，不会随代码提交
- [x] 注册/登录接口限流：同一 IP 10 分钟 20 次，超限返回 429
- [x] `systemd` 加固：`ProtectSystem=strict`、`PrivateTmp`、`NoNewPrivileges`，仅可写 `/var/lib/sporttask`
- [x] 统一错误处理，不把堆栈回传给客户端
- [ ] 需要更高安全性时：开启 `SIGNUP_CODE`、把 JWT 有效期从 30 天调短、加 fail2ban 规则

## 八、排错

| 现象 | 排查 |
| --- | --- |
| 502 Bad Gateway | `systemctl status sporttask`；多半是进程没起来或端口不对 |
| 证书签发失败 | 域名是否真的解析到本机（`dig +short <域名>`）；Cloudflare 是否开了橙云 |
| `SQLITE_BUSY` | 确认 `journal_mode = WAL` 未被改动；备份脚本用的是 `.backup`，不会锁表 |
| 401 一直不通过 | 客户端 token 与服务器 `JWT_SECRET` 不匹配（换过密钥需重新登录） |
| 429 | 限流触发，等窗口过期或重启服务（内存计数会清零） |
