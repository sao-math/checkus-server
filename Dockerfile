# Dockerfile
FROM gradle:8.13-jdk21 AS builder

WORKDIR /app
COPY . .
RUN ./gradlew build -x test

FROM openjdk:21-jdk-slim

WORKDIR /app
COPY --from=builder /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]