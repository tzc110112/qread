FROM eclipse-temurin:22-jdk AS builder

WORKDIR /build

COPY gradlew gradlew.bat ./
COPY gradle/wrapper/ gradle/wrapper/
COPY . /build

RUN chmod +x ./gradlew && \
    ./gradlew clean jar --no-daemon -x test && \
    rm -rf /root/.gradle

# ---- runtime ----
FROM eclipse-temurin:22-jre

WORKDIR /app

# 构建产物
COPY --from=builder /build/build/libs/solon-read-*.jar /app/read.jar
COPY --from=builder /build/conf/conf.yml /app/conf.yml
COPY docker-entrypoint.sh /app/entrypoint.sh

# 下载 Flutter Web 前端静态资源并解压到 /app/web/
# Solon 通过 StaticMappings.add("/", FileStaticRepository("web/")) 服务
ARG FLUTTER_ASSETS_URL=https://github.com/tzc110112/qread/releases/download/v1.0.0/flutter-web-static.tar.gz
RUN set -eux; \
    apt-get update; \
    apt-get install -y wget; \
    wget -q "$FLUTTER_ASSETS_URL" -O /tmp/flutter-web-static.tar.gz; \
    mkdir -p /app/web; \
    tar xzf /tmp/flutter-web-static.tar.gz -C /app/web; \
    rm -f /tmp/flutter-web-static.tar.gz; \
    apt-get remove -y wget; \
    apt-get autoremove -y; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*; \
    chmod +x /app/entrypoint.sh

EXPOSE 8080

ENV TZ=Asia/Shanghai

ENTRYPOINT ["/app/entrypoint.sh"]
