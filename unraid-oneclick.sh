#!/bin/bash
# ============================================
# 轻阅读 - Unraid 一键部署脚本
# ============================================
set -e

APP_DIR="/mnt/user/appdata/qread"
RELEASE_URL="https://github.com/autobcb/read/releases/latest"

echo "=============================="
echo "  轻阅读 - Unraid 一键部署"
echo "=============================="

# 1. 创建目录
mkdir -p "$APP_DIR"
cd "$APP_DIR"

# 2. 下载最新 release（需要自行上传或从 release 下载）
echo ""
echo ">>> 请将 read.jar 放入目录：$APP_DIR"
echo "    下载地址：$RELEASE_URL"
echo ""

# 3. 下载 docker-compose.yml（如果不存在）
if [ ! -f docker-compose.yml ]; then
    echo ">>> 下载 docker-compose.yml ..."
    curl -sL "https://raw.githubusercontent.com/autobcb/read/main/docker-compose.yml" -o docker-compose.yml
fi

# 4. 检查 read.jar
if [ ! -f read.jar ]; then
    echo "!!! 未找到 read.jar，请先下载并放入 $APP_DIR"
    echo "    然后重新运行此脚本"
    exit 1
fi

# 5. 启动
echo ""
echo ">>> 启动容器..."
docker compose up -d

echo ""
echo "=============================="
echo "  部署完成！"
echo "  后台地址：http://$(hostname -I | awk '{print $1}'):8080/admin"
echo "  Web阅读：http://$(hostname -I | awk '{print $1}'):8080/"
echo "  账号：admin"
echo "  密码：adminadmin"
echo "=============================="
