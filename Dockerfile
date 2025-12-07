# Многоступенчатая сборка для уменьшения размера образа
# Этап сборки
FROM gradle:8.10-jdk21-alpine AS builder

# Установка рабочей директории
WORKDIR /app

# Копирование Gradle файлов для кэширования зависимостей
COPY build.gradle settings.gradle* gradle.properties gradlew ./
COPY gradle ./gradle

# Копирование конфигурационных файлов checkstyle
COPY config ./config

# Установка прав на выполнение для gradlew
RUN chmod +x gradlew

# Скачивание зависимостей (кэшируется отдельно)
RUN ./gradlew dependencies --no-daemon || true

# Копирование исходного кода
COPY src ./src

# Сборка приложения без тестов
RUN ./gradlew clean build -x test --no-daemon

# Финальный этап для минимального образа
FROM eclipse-temurin:21-jre-alpine

# Установка рабочей директории
WORKDIR /app

# Создание пользователя для безопасного запуска
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Копирование JAR файла из этапа сборки
COPY --from=builder /app/build/libs/*.jar app.jar

# Настройка Health Check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Открытие порта
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# Аргументы по умолчанию
CMD ["--spring.profiles.active=docker"]