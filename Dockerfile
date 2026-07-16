# 使用 eclipse-temurin 基础镜像（带 apt）
FROM eclipse-temurin:22-jdk AS builder

WORKDIR /build

# 复制 gradle wrapper
COPY gradlew gradlew.bat ./
COPY gradle/wrapper/ gradle/wrapper/

# 复制源码
COPY . /build

# 国内镜像加速（可选，Github Actions 不需要）
# RUN sed -i 's|services.gradle.org|mirrors.tencent.com/gradle|g' gradle/wrapper/gradle-wrapper.properties

RUN chmod +x ./gradlew && \
    ./gradlew clean jar --no-daemon -x test && \
    rm -rf /root/.gradle

# ---- runtime ----
FROM eclipse-temurin:22-jre

WORKDIR /app

COPY --from=builder /build/build/libs/solon-read-*.jar /app/read.jar
COPY --from=builder /build/conf/conf.yml /app/conf.yml
COPY docker-entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENV TZ=Asia/Shanghai

ENTRYPOINT ["/app/entrypoint.sh"]
