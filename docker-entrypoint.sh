#!/bin/sh
# 轻阅读 Docker 启动入口

CONF=/app/conf/conf.yml
JAR=/app/read.jar

# admin
[ -n "$ADMIN_USERNAME" ] && sed -i "s/^  username:.*/  username: \"$ADMIN_USERNAME\"/" $CONF
[ -n "$ADMIN_PASSWORD" ] && sed -i "s/^  password:.*/  password: \"$ADMIN_PASSWORD\"/" $CONF
[ -n "$ADMIN_GONGGAO" ] && sed -i "s/^  gonggao:.*/  gonggao: \"$ADMIN_GONGGAO\"/" $CONF
[ -n "$ADMIN_UPDATE" ] && sed -i "s/^  update:.*/  update: $ADMIN_UPDATE/" $CONF

# user
[ -n "$USER_SOURCE" ] && sed -i "s/^  source: .*/  source: $USER_SOURCE/" $CONF
[ -n "$USER_ALLOWCHANGE" ] && sed -i "s/^  allowchange: .*/  allowchange: $USER_ALLOWCHANGE/" $CONF
[ -n "$USER_PROXYPNG" ] && sed -i "s/^  proxypng: .*/  proxypng: $USER_PROXYPNG/" $CONF
[ -n "$USER_INDEX" ] && sed -i "s/^  index: .*/  index: $USER_INDEX/" $CONF

# auto
[ -n "$AUTO_CRAWL" ] && sed -i "s/^  crawl: .*/  crawl: $AUTO_CRAWL/" $CONF
[ -n "$AUTO_UPDATE" ] && sed -i "s/^  update: true/  update: $AUTO_UPDATE/" $CONF

exec java -jar $JAR
