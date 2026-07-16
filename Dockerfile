FROM docker.1ms.run/openjdk:22-rc-oracle AS builder

WORKDIR /build

RUN set -eux; \
    apt-get update; \
    apt-get install -y unzip wget; \
    wget -q https://services.gradle.org/distributions/gradle-8.10-bin.zip -O gradle.zip; \
    unzip -q gradle.zip; \
    rm gradle.zip; \
    mv gradle-8.10 /opt/gradle

COPY . /build
RUN /opt/gradle/bin/gradle clean jar --no-daemon && \
    rm -rf /root/.gradle /opt/gradle

# ---- runtime ----
FROM docker.1ms.run/openjdk:22-rc-oracle

WORKDIR /app

COPY --from=builder /build/build/libs/solon-read-*.jar /app/read.jar
COPY --from=builder /build/conf/conf.yml /app/conf/conf.yml
COPY docker-entrypoint.sh /app/entrypoint.sh

RUN chmod +x /app/entrypoint.sh

EXPOSE 8080

ENV TZ=Asia/Shanghai

ENTRYPOINT ["/app/entrypoint.sh"]
