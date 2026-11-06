Контрольная работа №3. Разработка backend и frontend информационной системы

Важно. Работа выполняется по теме, закрепленной за командой. Student Hub из архива используется как пример структуры и реализации. Защита проекта (по командам) - пояснение кода и ответы на вопросы, в том числе и лайвкодинг.
Архив с готовой системой прикреплен в самом низу страницы Контрольной работы №3. Серверный пример находится в каталоге backend. В нем находятся Spring Boot REST API, PostgreSQL, миграции Flyway, Spring Security, JWT, Swagger, Docker Compose и статический web-интерфейс для проверки работы системы.

При разработке клиентской части информационной системы может быть использована библиотека React либо иное альтернативное программное решение (фреймворк).

Связь с предыдущими контрольными работами
КР №3 продолжает ту же информационную систему, которая была начата в КР №1. Предметная область и основная сущность не меняются. База данных из КР №1 используется как основа серверной базы. Если для backend требуются дополнительные таблицы и поля, они добавляются миграциями Flyway.

JavaFX-приложение из КР №2 является отдельным настольным клиентом. Backend из КР №3 должен предоставить REST API, которым затем будет пользоваться Android-приложение из КР №4.

Как перенести Student Hub на свою тему
В архиве В проекте команды
Student Основная сущность выбранной темы.
StudyGroup Справочная сущность, связанная с основной записью.
StudentStatus Статус основной сущности.
RiskLevel Расчетный показатель: уровень риска, приоритет, состояние или другой показатель по теме.
StudentNote Заметка или комментарий к основной сущности.
student_tag Теги основной сущности.
UserAccount Учетная запись пользователя.
RefreshSession Серверная refresh-сессия.
AuditEvent Запись журнала аудита.
AnalyticsResponse Сводные показатели аналитической панели.
Названия Student, StudyGroup и другие термины Student Hub в своей работе оставлять не нужно. Их следует заменить понятиями выбранной темы. Набор функций при этом сохраняется.

Подготовка и запуск готового проекта
Docker обязателен. Backend из архива рассчитан на запуск PostgreSQL и Spring Boot через Docker Compose.

Установка Docker Desktop для Windows
Необходимое программное обеспечение
Docker Desktop.
JDK 21.
Maven 3.9.11 или другая совместимая версия Maven 3.9.
IntelliJ IDEA. Через IntelliJ IDEA проще импортировать Maven-проект, просматривать Spring-конфигурацию и запускать отдельные классы.
PostgreSQL отдельно устанавливать не требуется, если база запускается через Docker.
Если при первом запуске возникают проблемы с Docker Compose, JDK, Maven или импортом проекта в IntelliJ IDEA, можно обратиться к преподавателю. Преподаватель поможет с первоначальным развертыванием.
Порядок запуска
Распакуйте архив с проектом.
Проверьте, что файл .env находится в корне проекта.
Запустите Docker Desktop.
Откройте PowerShell в корне проекта.
Запустите .\backend\start-compose.ps1.
Дождитесь запуска PostgreSQL и Spring Boot.
Проверьте web-интерфейс, Swagger и health.
После этого приступайте к разработке backend своей темы.
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
Вариант прямого запуска через Docker Compose:

docker compose --env-file .env -f backend/docker-compose.yml up -d --wait
Адреса готового примера
Назначение Адрес
Web-интерфейс http://localhost:8080/
Swagger UI http://localhost:8080/swagger-ui.html
REST API http://localhost:8080/api
OpenAPI JSON http://localhost:8080/v3/api-docs
Health http://localhost:8080/actuator/health
PostgreSQL jdbc:postgresql://localhost:55432/student_hub
Данные готового примера
Параметр Значение
Логин администратора admin@example.org
Временный пароль администратора Adm1!750DF9E52689A0B47E4DE85BC5C8AAC0
База данных student_hub
Пользователь PostgreSQL student_hub_app
Пароль PostgreSQL Db1!D90F176A0758B82E72A92FC9BE3F8652
Внешний порт PostgreSQL 55432
После первого входа система может потребовать заменить временный пароль администратора.

Обязательная структура backend
Пакет Назначение
controller REST-контроллеры.
service Бизнес-логика и транзакции.
repository Spring Data JPA и запросы к PostgreSQL.
domain JPA-сущности и enum.
dto Входные и выходные модели REST API.
security JWT, роли, текущий пользователь, пароли.
filter Поиск и безопасная фильтрация.
error Единая обработка ошибок.
config Spring Security, OpenAPI и параметры приложения.
db/migration SQL-миграции Flyway.
Функциональные требования для всех тем

1. Авторизация и учетные записи
   Вход по логину и паролю.
   Три уровня доступа: просмотр, редактирование и администрирование. Названия ролей можно подобрать под предметную область.
   Короткоживущий access JWT.
   Отдельный refresh-токен с серверной сессией.
   Обновление access JWT по refresh-токену.
   Выход с отзывом refresh-сессии.
   Получение данных текущего пользователя.
   Смена пароля.
   Обязательная смена временного пароля после создания учетной записи.
   Хеширование паролей BCrypt.
   Защита от большого количества неудачных попыток входа.
   Пароли не хранятся и не возвращаются в открытом виде.
2. Основная сущность
   UUID.
   Отдельный код, номер или внешний идентификатор.
   Основные текстовые поля по теме.
   Связь со справочной сущностью.
   Статус.
   Хотя бы один числовой показатель.
   Хотя бы один логический признак.
   Набор тегов.
   Вычисляемый показатель, например риск, приоритет или состояние.
   Дата создания и дата изменения.
   Поле version для контроля конкурентного изменения.
   Признак архива.
3. CRUD и архив
   Получение списка.
   Получение одной записи по UUID.
   Создание.
   Изменение.
   Архивирование вместо обычного удаления.
   Восстановление из архива.
   Окончательное удаление только администратором.
   Проверка version при изменении записи.
4. Поиск, фильтрация, сортировка и пагинация
   Свободный текстовый поиск.
   Фильтр по справочной сущности.
   Фильтр по статусу.
   Фильтр по логическому признаку.
   Фильтр по вычисляемому показателю.
   Фильтр по тегу.
   Фильтр по архивным и активным записям.
   Фильтр по числовому показателю с операциями сравнения.
   Сортировка минимум по трем полям.
   Серверная пагинация.
   Ограничение максимального размера страницы.
   Запрещается собирать SQL или JPQL конкатенацией из пользовательской строки.
5. Справочная сущность
   Получение списка.
   Создание.
   Изменение.
   Включение и отключение без физического удаления.
   Изменение справочника доступно администратору.
6. Заметки или комментарии
   История заметок для основной записи.
   Создание заметки.
   Изменение заметки.
   Удаление заметки.
   Хранение автора и времени.
   Закрепленная заметка или другой аналогичный признак.
7. Массовые операции
   Работа с набором UUID.
   Архивирование выбранных записей.
   Восстановление.
   Массовое изменение статуса.
   Добавление тега.
   Удаление тега.
   Отдельный результат по успешным операциям и ошибкам.
8. Импорт и экспорт CSV
   Экспорт основной сущности в CSV.
   Экспорт с учетом текущего фильтра.
   Импорт CSV.
   Предварительная проверка файла отдельным запросом.
   Возврат количества корректных и ошибочных строк.
   Отдельный запрос применения импорта после проверки.
   Два режима импорта: только новые записи и обновление по внешнему идентификатору.
9. Аналитика
   Общее количество записей.
   Количество активных.
   Количество архивных.
   Количество записей по логическому признаку.
   Среднее значение числового показателя.
   Группировка по справочной сущности.
   Группировка по статусу.
   Группировка по вычисляемому показателю.
10. Администрирование пользователей
    Просмотр пользователей с пагинацией.
    Создание пользователя.
    Изменение имени, логина, ролей и признака активности.
    Сброс пароля с созданием временного пароля.
    Отключенная учетная запись не может войти.
11. Журнал аудита
    Запись входов и важных изменений данных.
    Хранение пользователя, типа события, типа сущности, UUID, времени и краткого описания.
    Correlation ID для HTTP-запросов.
    Просмотр аудита только администратором.
    Поиск, сортировка и пагинация аудита.
12. Валидация и ошибки
    Bean Validation для входных DTO.
    Проверка обязательных полей, длины, диапазонов и форматов.
    JPA-сущности не возвращаются напрямую из контроллеров.
    Единый JSON-формат ошибок.
    Stack trace клиенту не возвращается.
    HTTP 400 для неверных данных, 401 для отсутствия авторизации, 403 для недостатка прав, 404 для отсутствующей записи, 409 для конфликта версий.
    Обязательный набор REST API
    Назначение Метод и адрес
    Вход POST /api/auth/login
    Refresh POST /api/auth/refresh
    Выход POST /api/auth/logout
    Текущий пользователь GET /api/auth/me
    Список основной сущности GET /api/items
    Карточка GET /api/items/{id}
    Создание POST /api/items
    Изменение PUT /api/items/{id}
    Архив DELETE /api/items/{id}
    Восстановление POST /api/items/{id}/restore
    Окончательное удаление DELETE /api/items/{id}/permanent
    Массовые операции POST /api/items/bulk
    Проверка CSV POST /api/items/import/preview
    Применение CSV POST /api/items/import/apply
    Экспорт CSV GET /api/items/export
    Заметки GET /api/items/{id}/notes
    Справочник GET /api/references
    Аналитика GET /api/analytics/summary
    Пользователи GET /api/admin/users
    Аудит GET /api/admin/audit
    Слова items и references являются условными. В проекте используются названия сущностей выбранной темы.
    PostgreSQL и Flyway
    Основная СУБД: PostgreSQL.
    Схема создается и изменяется через Flyway.
    Для Hibernate используется ddl-auto: validate.
    Минимально нужны таблицы пользователей, ролей, refresh-сессий, основной сущности, справочника, тегов, заметок и аудита.
    Для полей поиска создаются индексы.
    Внешние ключи и ограничения целостности задаются на уровне PostgreSQL.
    Готовый pom.xml Maven для backend
    Конфигурация соответствует backend из архива. В блоке ниже сохранены значения из архива. В собственной работе groupId, artifactId, name, description и package необходимо заменить под свою тему.

<?xml version="1.0" encoding="UTF-8"?>

<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
<modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
        <relativePath/>
    </parent>

    <groupId>ru.mirea.studenthub</groupId>
    <artifactId>student-hub-backend</artifactId>
    <version>1.0.0</version>
    <name>Student Hub Backend</name>
    <description>Secure REST backend and web UI for student management</description>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.12.6</jjwt.version>
        <springdoc.version>2.8.9</springdoc.version>
        <commons-csv.version>1.14.1</commons-csv.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-csv</artifactId>
            <version>${commons-csv.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <layers>
                        <enabled>true</enabled>
                    </layers>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.14.0</version>
                <configuration>
                    <release>${java.version}</release>
                    <parameters>true</parameters>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.5.3</version>
                <configuration>
                    <useModulePath>false</useModulePath>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
Основные настройки application.yml
Полный файл примера находится в backend/src/main/resources/application.yml.

spring:
application:
name: information-system

datasource:
url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/information_system}
username: ${SPRING_DATASOURCE_USERNAME:information_system_app}
password: ${SPRING_DATASOURCE_PASSWORD:change_me}

jpa:
open-in-view: false
hibernate:
ddl-auto: validate

flyway:
enabled: true

server:
port: 8080
error:
include-message: never
include-stacktrace: never

management:
endpoints:
web:
exposure:
include: health,info

springdoc:
swagger-ui:
path: /swagger-ui.html
api-docs:
path: /v3/api-docs

app:
jwt:
secret-base64: ${JWT_SECRET_BASE64}
issuer: ${JWT_ISSUER:information-system}
access-minutes: ${ACCESS_TOKEN_MINUTES:15}
refresh-days: ${REFRESH_TOKEN_DAYS:14}

bootstrap:
email: ${BOOTSTRAP_ADMIN_EMAIL}
name: ${BOOTSTRAP_ADMIN_NAME:System Administrator}
password: ${BOOTSTRAP_ADMIN_PASSWORD}

cors:
allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:8080}
Корневой файл .env
Файл .env находится в корне проекта. Для сдачи рекомендуется также иметь .env.example без реальных секретов.

POSTGRES*DB=information_system
POSTGRES_USER=information_system_app
POSTGRES_PASSWORD=укажите*свой_пароль

JWT*SECRET_BASE64=укажите*свой*случайный_base64*секрет
JWT_ISSUER=information-system
ACCESS_TOKEN_MINUTES=15
REFRESH_TOKEN_DAYS=14

BOOTSTRAP*ADMIN_EMAIL=admin@example.org
BOOTSTRAP_ADMIN_NAME=System Administrator
BOOTSTRAP_ADMIN_PASSWORD=укажите*временный*пароль*администратора

CORS_ALLOWED_ORIGINS=http://localhost:8080
COOKIE_SECURE=false
PERMANENT_DELETE_ENABLED=false
APP_PORT=8080
POSTGRES_PORT=55432
Docker Compose
Готовая конфигурация находится в backend/docker-compose.yml. В архиве она запускает PostgreSQL 17 и Spring Boot на Java 21.

PostgreSQL запускается отдельным сервисом.
Backend запускается отдельным сервисом.
Параметры берутся из корневого .env.
Порт backend публикуется на localhost.
Порт PostgreSQL для учебной работы также публикуется только на localhost.
Для PostgreSQL должен быть healthcheck.
Для backend должен быть healthcheck.
Обычный docker compose down не должен удалять данные базы.
Swagger и тесты
Swagger UI должен быть доступен по адресу /swagger-ui.html. В примере доступ к Swagger разрешен администратору.

Smoke-тест загрузки Spring Context.
Тест бизнес-правила или вычисляемого показателя.
Тест фильтра или поиска.
Тест политики паролей или другого элемента безопасности.
Для тестов разрешается использовать H2, как в архиве.
Что показать на защите КР №3
Запуск PostgreSQL и backend через Docker.
Healthcheck.
Вход.
Различия ролей.
Создание и изменение основной сущности.
Поиск и фильтры.
Пагинацию и сортировку.
Архивирование и восстановление.
Массовую операцию.
Заметки.
Проверку CSV, импорт и экспорт.
Аналитическую сводку.
Управление пользователями.
Журнал аудита.
Swagger.
Миграции Flyway и структуру PostgreSQL.
Результат КР №3. Должен получиться Spring Boot backend по теме команды, работающий с PostgreSQL, запускаемый через Docker и предоставляющий REST API для Android-приложения из КР №4.
