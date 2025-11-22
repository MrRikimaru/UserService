# Build stage
FROM eclipse-temurin:21-jdk as builder
WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew clean build -x test -x spotlessJavaCheck --no-daemon

# Production stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]