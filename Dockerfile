FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем собранный JAR файл
COPY build/libs/UserService-0.0.1-SNAPSHOT.jar app.jar

# Создаем пользователя для безопасности
RUN adduser --system --group spring
USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]