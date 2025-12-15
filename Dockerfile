# Этап 1: Сборка с официальным Gradle + JDK 21
FROM gradle:8.9-jdk21 AS build-stage

WORKDIR /app

# Копируем файлы для зависимостей (оптимизация кэша)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Загружаем зависимости
RUN gradle dependencies --no-daemon

# Копируем исходный код и собираем
COPY . .
RUN gradle bootJar --no-daemon

# Этап 2: Минимальный финальный образ
FROM eclipse-temurin:21-jre

WORKDIR /app

# Создаем непривилегированного пользователя
RUN addgroup --system spring && \
    adduser --system --ingroup spring spring

# Копируем JAR и настраиваем права
COPY --from=build-stage /app/build/libs/app.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]