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
# Limit JVM memory usage to fit within Render's 512MB free tier limit
ENTRYPOINT ["java", "-Xmx300m", "-Xss512k", "-XX:MaxMetaspaceSize=128m", "-jar", "app.jar"]
