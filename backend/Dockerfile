FROM gradle:8.4-jdk17 AS builder
WORKDIR /app
COPY . /app
RUN chmod +x ./gradlew
ENV GRADLE_OPTS="-Xmx256m -Dorg.gradle.daemon=false"
RUN ./gradlew build -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
