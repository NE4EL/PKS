# ИС интернет-провайдера — КР3

Spring Boot REST API и веб-клиент для учёта заявок абонентов. Предметная область и виды заявок продолжают КР1/КР2. JavaFX-клиент с ветки `pr2` сохранён отдельно в `desktop/`; он работает через JDBC с прежними таблицами. Веб-клиент КР3 и будущий Android-клиент используют новый REST API.

## Запуск через Docker

Нужен запущенный Docker Desktop с Docker Compose. Java и Maven на компьютере для этого способа не нужны.

```sh
python3 init-env.py                  # Windows: python init-env.py
# Создаёт .env со случайными секретами, существующий файл не перезаписывает.
docker compose up -d --build --wait
docker compose ps
```

Windows: `powershell -ExecutionPolicy Bypass -File backend/start-compose.ps1` (создаёт `.env` средствами PowerShell, Python не нужен).
macOS/Linux: `./backend/start-compose.sh`.
Альтернативная команда из методички: `docker compose --env-file .env -f backend/docker-compose.yml up -d --build --wait`.
Если Python не установлен, скопируйте `.env.example` в `.env` и заполните три секрета самостоятельно. JWT-секрет — Base64 минимум 32 случайных байтов. `.env` в Git не попадает.

| Назначение | Адрес |
|---|---|
| Веб-клиент | http://localhost:8080/ |
| Health | http://localhost:8080/actuator/health |
| Swagger (после входа администратора) | http://localhost:8080/swagger-ui.html |
| OpenAPI | http://localhost:8080/v3/api-docs |
| PostgreSQL | `jdbc:postgresql://localhost:55432/isp` |

Первый администратор: логин `BOOTSTRAP_ADMIN_EMAIL` и временный пароль `BOOTSTRAP_ADMIN_PASSWORD` из локального `.env`. После входа обязательно установите постоянный пароль. Пароль — от 12 символов, верхний и нижний регистр, цифра, специальный символ, не более 72 байтов UTF-8, без имени логина. Для Swagger после смены пароля web-клиент автоматически обновляет сессию.

Первая сборка скачивает образы и Maven-зависимости и выполняет тесты. При пустой БД миграция добавляет демонстрационного абонента и заявку. Backend и PostgreSQL имеют healthcheck, порты опубликованы только на localhost. Backend работает от непривилегированного пользователя.

```sh
docker compose logs --tail=100 backend
docker compose down                 # данные сохраняются в named volume
```

## Архитектура и технологии

Java 21, Spring Boot 3.5.5, Spring Data JPA, PostgreSQL 17, Flyway, Spring Security, BCrypt, JJWT, Bean Validation, Commons CSV, springdoc OpenAPI. Веб-клиент — HTML/CSS/JavaScript без отдельного сервера сборки.

`backend/src/main/java/ru/mirea/isp/` содержит пакеты `controller`, `service`, `repository`, `domain`, `dto`, `security`, `filter`, `error`, `config`, `web`. SQL лежит в `backend/src/main/resources/db/migration`, веб-клиент — в `static`. Контроллеры возвращают DTO, транзакции и правила находятся в сервисах, поиск выполняется параметризованными Criteria API, сортировка — только по разрешённым полям. Hibernate использует `ddl-auto: validate`.

Основная сущность `ConnectionRequest`: UUID, уникальный номер `externalId`, название, описание, адрес, контакты, абонент, тип заявки и тариф из КР2, статус, срок исполнения, трудозатраты 0–100 часов, срочность, теги, вычисляемый приоритет, архив, авторы, даты и optimistic `version`.

Приоритет закрытых заявок — `NOT_APPLICABLE`. Для открытых: срочная заявка с оценкой от 8 часов — `CRITICAL`, другая срочная — `HIGH`, несрочная от 8 часов или без оценки — `MEDIUM`, остальные — `LOW`. Клиент не задаёт приоритет вручную.

## Возможности

- JWT access (15 минут по умолчанию), одноразовый refresh с серверной сессией и ротацией, logout, текущий пользователь, смена пароля, обязательная замена временного пароля, блокировка перебора после 5 ошибок.
- Роли: `VIEWER` — чтение; `OPERATOR` — заявки, заметки, CSV, массовые операции; `ADMIN` — все операции, справочник абонентов, пользователи, аудит и Swagger.
- CRUD заявок, архив и восстановление, контроль версий, окончательное удаление администратором при `PERMANENT_DELETE_ENABLED=true` и заголовке `X-Confirm-Delete: PERMANENTLY_DELETE`.
- Справочник абонентов: номер договора, ФИО, контакты, адрес, год регистрации, включение/отключение; назначение неактивного абонента запрещено.
- Заметки с автором, датой, закреплением и версией. Изменение и удаление доступны автору или администратору.
- Массовое архивирование, восстановление, статус, добавление/удаление тега. Результат содержит успехи и ошибки каждого UUID; записи обрабатываются в отдельных транзакциях.
- CSV: отдельная проверка и применение, только создание / обновление по номеру, ошибки по строкам; экспорт текущего фильтра, UTF-8 BOM и защита от формул Excel.
- Аналитика: всего, активные, архивные, срочные, средние трудозатраты, распределение по абонентам, статусам и приоритетам. Группировки и среднее считаются по неархивным заявкам.
- Администрирование пользователей с пагинацией, изменение ролей и активности, назначение временного пароля, отзыв refresh-сессий при отключении/сбросе пароля.
- Аудит с автором, типом, UUID сущности, временем, описанием, correlation ID; поиск, сортировка, страницы, доступ только администратору.
- Единый JSON ошибок: `timestamp`, `status`, `code`, `message`, `path`, `correlationId`, `validationErrors`. Коды 400/401/403/404/409; stack trace не отправляется клиенту.

## REST API

Все прикладные запросы требуют `Authorization: Bearer <accessToken>`. Для Android при входе укажите `clientType: "NATIVE"` (значение по умолчанию): refresh вернётся в JSON. Для web используется `clientType: "WEB"`: refresh хранится только в HttpOnly/SameSite=Strict cookie, access — в памяти вкладки. Пароли/хеши не входят в пользовательские DTO.

| Метод | Путь | Назначение |
|---|---|---|
| POST | `/api/auth/login` | `{email,password,clientType}` |
| POST | `/api/auth/refresh` | `{refreshToken}` для native или cookie web |
| POST | `/api/auth/logout` | отзыв refresh-сессии |
| GET | `/api/auth/me` | текущий пользователь |
| POST | `/api/auth/change-initial-password` | `{newPassword}` |
| POST | `/api/auth/change-password` | `{currentPassword,newPassword}` |
| GET/POST | `/api/requests` | список / создание заявки |
| GET/PUT/DELETE | `/api/requests/{id}` | карточка / изменение / архив |
| POST | `/api/requests/{id}/restore` | восстановление |
| DELETE | `/api/requests/{id}/permanent` | окончательное удаление |
| POST | `/api/requests/bulk` | `{ids,action,status?,tag?}` |
| POST | `/api/requests/import/preview` | multipart `file`, query `mode` |
| POST | `/api/requests/import/apply` | multipart `file`, query `mode` |
| GET | `/api/requests/export?q=...` | CSV текущего фильтра |
| GET/POST | `/api/requests/{id}/notes` | история / создание заметки |
| PUT/DELETE | `/api/requests/{id}/notes/{noteId}` | изменение / удаление |
| GET/POST | `/api/subscribers` | список / создание абонента |
| PUT | `/api/subscribers/{id}` | изменение абонента |
| PATCH | `/api/subscribers/{id}/active?active=false&version=0` | отключение/включение |
| GET | `/api/analytics/summary` | аналитика |
| GET/POST | `/api/admin/users` | список / создание пользователя |
| PUT | `/api/admin/users/{id}` | изменение пользователя |
| POST | `/api/admin/users/{id}/reset-password` | `{temporaryPassword}` |
| GET | `/api/admin/audit` | аудит |

Для PUT заявки/абонента/заметки отправляйте `version`, полученную в последней карточке. Сервер возвращает актуальную версию; устаревшая даёт 409. Полная схема DTO доступна в Swagger.

Пример создания заявки (UUID абонента возьмите из `/api/subscribers`):

```json
{"externalId":"REQ-2026-001","title":"Подключение интернета","description":"Настроить роутер","subscriberId":"10000000-0000-0000-0000-000000000001","status":"NEW","type":"NEW_CONNECTION","tariffPlan":"Домашний 500","estimatedHours":4,"urgent":false,"tags":["подключение"]}
```

Статусы: `NEW`, `IN_PROGRESS`, `APPROVED`, `COMPLETED`, `REJECTED`, `CANCELLED`. Типы: `NEW_CONNECTION`, `TARIFF_CHANGE`, `TECHNICAL_SUPPORT`, `DISCONNECTION`, `EQUIPMENT_REPLACEMENT`.

Списки заявок, пользователей и аудита возвращают `{content,page,size,totalElements,totalPages,first,last}`; страницы с нуля, размер 1–100. Параметры: `page`, `size`, `sort=поле,asc|desc`. Сортировки заявок: `title`, `description`, `subscriber`, `status`, `hours`, `priority`, `updated`; пользователей: `email`, `name`, `created`, `updated`; аудита: `occurred`, `actor`, `event`.

Поиск заявок — параметр `q`, например:

```text
роутер абонент:ACC-001 статус:NEW срочно:да часы>=8 приоритет:critical тег:важно архив:все
```

Английские команды: `subscriber:`, `status:`, `urgent:`, `hours>=`, `priority:`, `tag:`, `archived:`. `архив:да` — архив, `архив:нет` — активные (по умолчанию), `архив:все` — оба. Числовые операции `<`, `<=`, `=`, `>=`, `>`.

CSV-колонки (запятая, теги через `|`, даты YYYY-MM-DD):

```text
externalId,description,title,address,email,phone,dueDate,subscriber,status,type,tariffPlan,estimatedHours,urgent,tags
```

`subscriber` — код договора, не UUID. Пример — `examples/requests.csv` для демонстрационной БД. Режимы: `CREATE_ONLY`, `UPSERT_BY_EXTERNAL_ID`. Лимит файла 5 MB / 5000 строк; при применении корректные строки сохраняются, ошибки возвращаются отдельно. Перед применением веб-клиент требует проверку файла; смена файла или режима сбрасывает результат проверки.

## База КР1/КР2 и миграции

`V1` создаёт прежние `subscribers` и `connection_requests`. `V2` добавляет таблицы backend, индексы и ограничения. `V3` копирует прежние записи со стабильным UUID, сохраняя связь, ФИО, контакты, адрес, тип, статус, тариф и даты. Исходные таблицы не удаляются. `V4` добавляет демонстрационные записи только в пустую базу. `V5` добавляет версию учётных данных: смена или сброс пароля немедленно делает прежние access JWT недействительными.

Новый Compose использует отдельный volume `isp-kr3_isp_postgres`, поэтому существующие базы КР2 не меняются автоматически. Для продолжения существующей базы сначала сделайте резервную копию, восстановите её **в отдельную БД КР3**, затем запустите Flyway с `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` и `SPRING_FLYWAY_BASELINE_VERSION=0`. Это отмечает исходную схему как версию 0: идемпотентный V1 проверит прежние таблицы, V2–V5 выполнят перенос. Одновременные записи JDBC-клиента и backend не синхронизируются; перенос одноразовый. JavaFX остаётся отдельным клиентом КР2, как требует контракт.

## Сборка и проверки без Docker

Нужен JDK 21. Maven Wrapper скачает Maven; локальный Maven 3.9 также подходит.

```sh
./mvnw verify                       # backend, H2-тесты, сборка jar
./mvnw -f desktop/pom.xml test      # прежние тесты КР2
./mvnw -f desktop/pom.xml javafx:run
```

Тесты backend проверяют Spring Context, расчёт приоритета, безопасный парсер, политику паролей и интеграционные сценарии API (роли, временный пароль, CRUD, версии, архив, массовые действия, заметки, CSV с BOM, аналитика, аудит, ротация refresh). Тестовый профиль использует H2 и не затрагивает PostgreSQL. Dockerfile также выполняет `mvn verify` перед упаковкой.

## Сценарий защиты

1. `docker compose ps`, health, журнал Flyway; показать таблицы и индексы PostgreSQL.
2. Вход администратором, обязательная смена временного пароля, Swagger.
3. Создать `VIEWER` и `OPERATOR`, продемонстрировать 403 на запрещённых операциях.
4. Создать абонента и заявку, изменить с `version`, повторить старый PUT → 409.
5. Показать поиск, все виды фильтров, сортировку, страницы, архив и восстановление.
6. Массовый тег/статус, заметки с автором и закреплением.
7. Экспорт, preview CSV с ошибкой, исправленный импорт, второй режим обновления.
8. Аналитика, управление пользователями, отключение пользователя, аудит и correlation ID.
9. Объяснить слои, серверный расчёт приоритета, JWT/refresh, BCrypt и миграции.
