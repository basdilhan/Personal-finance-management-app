FROM gradle:8.4-jdk17 AS builder
WORKDIR /app
COPY . /app
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
