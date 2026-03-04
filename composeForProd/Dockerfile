# 1) 빌드 (Gradle 포함 이미지 사용)
FROM gradle:8-jdk17-jammy AS builder
WORKDIR /app

ENV GRADLE_USER_HOME=/app/.gradle

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon || true

COPY src ./src
RUN gradle bootJar --no-daemon

# 2) 실행
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=builder /app/build/libs/search-service-*.jar app.jar

ENV SERVER_PORT=8081
EXPOSE 8081

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
