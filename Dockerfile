# Dockerfile
FROM gradle:8.13-jdk21 AS builder

WORKDIR /app
COPY . .
RUN ./gradlew build -x test
# JAR 파일명 확인 및 복사 최적화
RUN ls -la /app/build/libs/

FROM openjdk:21-jdk-slim

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]