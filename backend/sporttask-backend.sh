#!/bin/bash
# AI SportTask 后端管理脚本
# 用法: ./sporttask-backend.sh {start|stop|restart|status|logs}

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="/tmp/sporttask-backend.log"
PID_FILE="/tmp/sporttask-backend.pid"

case "$1" in
  start)
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
      echo "后端已在运行 (PID: $(cat $PID_FILE))"
      exit 0
    fi
    cd "$APP_DIR"
    nohup node server.js > "$LOG_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    sleep 1
    echo "✅ 后端已启动 (PID: $!)"
    ;;
  stop)
    if [ -f "$PID_FILE" ]; then
      kill $(cat "$PID_FILE") 2>/dev/null
      rm -f "$PID_FILE"
      echo "✅ 后端已停止"
    else
      echo "后端未运行"
    fi
    ;;
  restart)
    $0 stop
    sleep 1
    $0 start
    ;;
  status)
    if [ -f "$PID_FILE" ] && kill -0 $(cat "$PID_FILE") 2>/dev/null; then
      echo "✅ 后端运行中 (PID: $(cat $PID_FILE))"
      curl -s http://localhost:3456/api/health | python3 -m json.tool 2>/dev/null
    else
      echo "❌ 后端未运行"
    fi
    ;;
  logs)
    tail -f "$LOG_FILE"
    ;;
  *)
    echo "用法: $0 {start|stop|restart|status|logs}"
    exit 1
    ;;
esac
