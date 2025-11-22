# Build stage
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace/app

COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test

# Production stage
FROM eclipse-temurin:21-jre
RUN addgroup --system spring && adduser --system --group spring
USER spring:spring

WORKDIR /app
COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]