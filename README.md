# Игра Морской бой (Backend)

Серверная часть многопользовательской игры «Морской бой».  
Обеспечивает игровую логику, управление пользователями, авторизацию и проведение матчей между игроками или против ИИ в режиме реального времени.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Gradle](https://img.shields.io/badge/Build-Gradle-06A0CE)
![WebSocket](https://img.shields.io/badge/Real--time-WebSocket-purple)

## Содержание
- [Технологический стек](#технологический-стек)
- [Команда проекта](#команда-проекта)
- [Требования](#требования)
- [Установка и настройка](#установка-и-настройка)
- [Устранение неполадок](#устранение-неполадок)

---

## Технологический стек
- **Язык**: Java 21
- **Фреймворк**: Spring Boot 3.5.7
- **Безопасность**: Spring Security + JWT (JSON Web Token)
- **База данных**: PostgreSQL
- **Взаимодействие в реальном времени**: WebSocket (STOMP)
- **Маппинг данных**: MapStruct
- **Сборка**: Gradle

---

## Команда проекта
Проект разработан командой из трёх человек в рамках лабораторного практикума:

- **[@nast1x](https://github.com/nast1x)** — бэкенд-разработчик, работа с базой данных
- **[@Jane11Al](https://github.com/Jane11Al)** — бэкенд-разработчик
- **[@shevlya](https://github.com/shevlya)** — дизайнер, фронтенд-разработчик, технический писатель

**Фронтенд-репозиторий:** [battleship-game-FRONTEND](https://github.com/shevlya/battleship-game-FRONTEND)

---

## Требования
Перед началом работы убедитесь, что на вашем компьютере установлены:
- **JDK**: версия 21 или выше
- **PostgreSQL**: версия 15 или выше
- **Среда сборки**: Gradle (или используйте встроенный `gradlew`)

---

## Установка и настройка

1. Клонирование репозитория
```bash
git clone <URL_ВАШЕГО_РЕПОЗИТОРИЯ>
cd <ИМЯ_ПАПКИ_ПРОЕКТА>
```
2. Настройка базы данных
Создайте базу данных в PostgreSQL (например, `battleship_db`) и пользователя с соответствующими правами.
3. Конфигурация приложения
Откройте файл `src/main/resources/application.properties` и укажите актуальные параметры подключения:
```properties
# Настройки подключения к PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/battleship_db
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

# Настройки Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Настройки JWT (пример, замените на свой сложный секретный ключ)
app.jwt.secret=your_super_secret_jwt_key_here_must_be_long_enough
app.jwt.expiration-ms=86400000
```

## Устранение неполадок
1. Убедитесь, что установлена Java 21 (*)
> Если у вас установлена другая версия Java по умолчанию, вы можете изменить её в настройках проекта.
> Откройте файл build.gradle и измените версию в блоке toolchain:
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21) // Измените 21 на вашу версию
    }
}
```
> <b>NB:</b> Убедитесь, что выбранная версия Java совместима с версией Spring Boot 3.5.7.
2. Ошибка подключения к базе данных:
- убедитесь, что PostgreSQL запущена;
- проверьте правильность `username`, `password` и имени базы данных в `application.properties`;
- убедитесь, что порт 5432 не заблокирован брандмауэром.
3. Порт 8080 уже используется
Если порт занят, измените его в `application.properties`:
```bash
server.port=8081
```
Не забудьте также обновить `apiUrl` во фронтенд-приложении (`environment.ts`), чтобы он указывал на новый порт.
