# ИС интернет-провайдера — КР2 (JavaFX + JDBC)

Настольное приложение для учёта абонентов интернет-провайдера и их заявок (подключение, смена тарифа,
техподдержка, отключение, замена оборудования). Продолжение КР1: та же предметная область, та же
база данных PostgreSQL, те же модель, репозитории и сервисы. Поверх них добавлен графический интерфейс на JavaFX.

- Полное ТЗ КР1 и распределение ролей — [`PKS.md`](PKS.md), ER-диаграмма — [`diagram.md`](diagram.md).
- Подготовка к защите КР2 (ответы на вопросы, сценарий демонстрации) — [`KR2.md`](KR2.md).
- Подробно, что изменилось по сравнению с КР1, — [`difference.md`](difference.md).

## Технологии

Java 21 · JavaFX 21 (FXML + CSS) · Maven (есть Maven Wrapper) · PostgreSQL + JDBC (DAO/Repository) ·
Apache POI (.xlsx) · slf4j · JUnit 5 + AssertJ · Docker Compose (БД)

## Архитектура

```
View (FXML + CSS)      src/main/resources/ru/mirea/ispteam/fx/view/*.fxml, fx/css/app.css
      │  fx:id, onAction
Controller             fx/controller/*  — действия пользователя, связь окна с сервисами
      │
Service                service/*        — бизнес-правила, валидация, поиск/фильтр, статистика, экспорт
      │
Repository / DAO       repository/*     — SQL через JDBC + PreparedStatement
      │
Database               PostgreSQL       — таблицы subscribers, connection_requests, PK/FK/UNIQUE/CHECK
```

Правила: в контроллерах нет SQL; сервисы не знают про JavaFX; пароль БД не хранится в коде
(файл `database.properties` не коммитится); все объекты собираются в одном месте — `AppContext`
(контроллеры получают готовые сервисы). Консольный интерфейс КР1 удалён — его заменил JavaFX.

## Быстрый старт (Windows)

Нужно: **JDK 21** (переменная `JAVA_HOME`), **Docker Desktop** (запущен). Maven ставить не обязательно —
в проекте есть `mvnw.cmd`.

```powershell
# 1. База данных КР1 в Docker (первый запуск сам выполнит schema.sql и seed-data.sql)
copy .env.example .env                 # впишите свой пароль в ISP_DB_PASSWORD
docker compose up -d --wait
docker compose ps                      # isp-db должен быть в состоянии healthy

# 2. Настройки JDBC-подключения
copy src\main\resources\database.properties.example src\main\resources\database.properties
#    db.url=jdbc:postgresql://localhost:55433/isp
#    db.user=isp_app
#    db.password=<тот же пароль, что в .env>

# 3. Запуск JavaFX-приложения
.\mvnw.cmd javafx:run
#    или: powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Остановка БД: `docker compose down` (данные сохраняются в volume; не добавляйте `-v`).

**Без Docker** (локальный PostgreSQL): создайте БД `CREATE DATABASE isp;`, в `database.properties` укажите
`jdbc:postgresql://localhost:5432/isp` и своего пользователя/пароль. При `db.initOnStartup=true`
приложение само создаст таблицы и наполнит пустую БД тестовыми данными.

### Другие команды

| Команда | Что делает |
|---|---|
| `.\mvnw.cmd clean compile` | компиляция |
| `.\mvnw.cmd test` | 16 unit-тестов (+186 проверок бизнес-правил B) — без БД |
| `.\mvnw.cmd javafx:run` | запуск JavaFX-клиента (КР2) |
| `.\mvnw.cmd clean package` → `java -jar target\isp-team.jar` | сборка и запуск jar с GUI |

В IntelliJ IDEA: открыть папку как Maven-проект, Project SDK = 21, запустить `ru.mirea.ispteam.fx.Launcher`
(или Maven → Plugins → javafx → `javafx:run`).

## Демонстрационная система из архива (Docker)

Методичка требует хотя бы один раз развернуть готовую систему из архива КР2 (backend + PostgreSQL в Docker,
пример `javafx-client`). Она **не входит** в этот репозиторий — архив скачивается со страницы КР2:

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1     # из корня распакованного архива
# проверка: http://localhost:8080/  и  http://localhost:8080/actuator/health
cd javafx-client; .\run.ps1
```

Демо-система публикует PostgreSQL на порту 55432, наша БД КР1 — на 55433, поэтому они не конфликтуют.

## Что реализовано (обязательный минимум КР2)

| № | Требование | Где |
|---|---|---|
| 1 | Приложение запускается | `fx/IspFxApplication`, `fx/Launcher`, `mvnw javafx:run` |
| 2–3 | Предметная область и БД КР1, отдельное JDBC-подключение | `util/DatabaseManager`, `database.properties`, `repository/*` |
| 4 | Основная сущность в `TableView` | вкладка «Заявки»: `requests-tab.fxml`, `RequestsController` |
| 5–8 | Добавление, редактирование, удаление с подтверждением | `RequestFormController`, `SubscriberFormController`, `Dialogs.confirm` |
| 9 | Связанные данные | карточка заявки с данными абонента; заявки выбранного абонента; выбор абонента в `ComboBox` |
| 10 | Поиск по нескольким полям | ФИО, телефон, описание, тариф — `service/RequestFilter`; абоненты — `SubscriberFilter` |
| 11 | Фильтрация (≥2) | статус, тип, период дат создания; «только с активными заявками» |
| 12 | Сортировка (≥2) | список «Сортировка» (5 вариантов) + щелчок по заголовкам (`SortedList` ↔ `TableView.comparator`) |
| 13–14 | Валидация и понятные ошибки | `SubscriberService`/`ConnectionRequestService` → `ValidationException(field, message)` → подсветка поля |
| 15–16 | FXML и CSS | `fx/view/*.fxml` (6 файлов), `fx/css/app.css` |
| 17 | Слои Controller / Service / DAO | см. «Архитектура» |
| 18 | Статистика (11 показателей + 2 диаграммы) | `StatisticsService.buildReport`, окно `statistics-view.fxml` |
| 19 | Экспорт `.xlsx` и `.csv` через `FileChooser` | `ExportService`, `CsvExporter`, `ExcelExporter`, `fx/util/ExportFiles` |
| 20 | Нет неработающих кнопок | кнопки без выбранной строки неактивны (`disableProperty().bind(...)`) |

## Структура

```
src/main/java/ru/mirea/ispteam/
├── AppContext.java            сборка зависимостей: util -> repository -> service
├── model/                     Subscriber, ConnectionRequest, RequestStatus, RequestType
├── repository/                CrudRepository + JDBC-реализации (DAO)
├── service/                   бизнес-логика, RequestFilter, SubscriberFilter, StatisticsService(+Report), ExportService
├── exception/                 Validation/Business/EntityNotFound/Database/ExportException
├── util/                      DatabaseManager, CsvExporter, ExcelExporter, TableData
└── fx/                        JavaFX-клиент КР2
    ├── IspFxApplication.java  точка входа (Stage, Scene)
    ├── Launcher.java          main для запуска из jar
    ├── ViewLoader.java        загрузка FXML + фабрика контроллеров
    ├── FormDialogs.java       открытие модальных форм
    ├── DataStore.java         ObservableList абонентов и заявок
    ├── model/RequestRow.java  строка таблицы: заявка + абонент
    ├── controller/            Main, Requests, Subscribers, RequestForm, SubscriberForm, Statistics (+ FormController)
    └── util/                  Dialogs, Cells, Formats, Option, ExportFiles
src/main/resources/
├── ru/mirea/ispteam/fx/view/  main-view, requests-tab, subscribers-tab, request-form, subscriber-form, statistics-view (.fxml)
├── ru/mirea/ispteam/fx/css/app.css
├── schema.sql, seed-data.sql  схема и тестовые данные (5 абонентов, 12 заявок)
└── database.properties.example
```

## Git-workflow

Ветки по ролям, `main` — только через Pull Request. Коммиты с префиксом роли: `[A] ...`, `[B] ...`.
`database.properties` и `.env` не коммитятся — у каждого участника свои.
