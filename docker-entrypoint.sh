#!/bin/sh
# 轻阅读 Docker 启动入口

CONF=/app/conf.yml
JAR=/app/read.jar

# 用环境变量覆写 conf.yml
if [ -n "$ADMIN_USERNAME" ]; then
  sed -i "s/^  username: \".*\"/  username: \"$ADMIN_USERNAME\"/" $CONF
fi
if [ -n "$ADMIN_PASSWORD" ]; then
  sed -i "s/^  password: \".*\"/  password: \"$ADMIN_PASSWORD\"/" $CONF
fi
if [ -n "$ADMIN_GONGGAO" ]; then
  sed -i "s/^  gonggao: \".*\"/  gonggao: \"$ADMIN_GONGGAO\"/" $CONF
fi
if [ -n "$ADMIN_UPDATE" ]; then
  sed -i "s/^  update: .*/  update: $ADMIN_UPDATE/" $CONF
fi
if [ -n "$USER_SOURCE" ]; then
  sed -i "s/^  source: .*/  source: $USER_SOURCE/" $CONF
fi
if [ -n "$USER_ALLOWCHANGE" ]; then
  sed -i "s/^  allowchange: .*/  allowchange: $USER_ALLOWCHANGE/" $CONF
fi
if [ -n "$USER_PROXYPNG" ]; then
  sed -i "s/^  proxypng: .*/  proxypng: $USER_PROXYPNG/" $CONF
fi
if [ -n "$USER_INDEX" ]; then
  sed -i "s/^  index: .*/  index: $USER_INDEX/" $CONF
fi
if [ -n "$AUTO_CRAWL" ]; then
  sed -i "s/^  crawl: .*/  crawl: $AUTO_CRAWL/" $CONF
fi
if [ -n "$AUTO_UPDATE" ]; then
  sed -i "s/^  update: true/  update: $AUTO_UPDATE/" $CONF
fi

# 环境变量未设置时，保持默认值不做修改

exec java -jar $JAR
