# Отличия КР2 от КР1 — подробный разбор

Документ для всей команды: что именно изменилось в проекте при переходе от консольного приложения (КР1)
к настольному приложению на JavaFX (КР2), **зачем** это сделано и **где** это лежит в коде.
Его стоит прочитать каждому перед защитой: преподаватель может спросить любого участника о любой части.

---

## Содержание

1. [Коротко](#1-коротко)
2. [Точка отсчёта: с чем сравниваем](#2-точка-отсчёта-с-чем-сравниваем)
3. [Сводка изменений в цифрах](#3-сводка-изменений-в-цифрах)
4. [Архитектура: было → стало](#4-архитектура-было--стало)
5. [Сборка, Java, зависимости (`pom.xml`)](#5-сборка-java-зависимости-pomxml)
6. [Подключение к БД и Docker](#6-подключение-к-бд-и-docker)
7. [Изменения по слоям](#7-изменения-по-слоям)
   - [7.1 model](#71-model--модель-предметной-области)
   - [7.2 exception](#72-exception--исключения)
   - [7.3 repository](#73-repository--доступ-к-данным-dao)
   - [7.4 service](#74-service--бизнес-логика)
   - [7.5 util](#75-util--вспомогательные-классы)
   - [7.6 ui → fx: новый интерфейс](#76-ui--fx-новый-интерфейс)
   - [7.7 Ресурсы: FXML, CSS, настройки](#77-ресурсы-fxml-css-настройки)
   - [7.8 Тесты](#78-тесты)
8. [Функции КР1 → где они в КР2](#8-функции-кр1--где-они-в-кр2)
9. [Удалённые файлы и почему](#9-удалённые-файлы-и-почему)
10. [Что НЕ изменилось](#10-что-не-изменилось)
11. [Изменения контрактов из PKS.md — что важно знать команде](#11-изменения-контрактов-из-pksmd--что-важно-знать-команде)
12. [Как всё проверено](#12-как-всё-проверено)
13. [Полный список файлов КР2](#13-полный-список-файлов-кр2)

---

## 1. Коротко

| | КР1 | КР2 |
|---|---|---|
| Интерфейс | консоль: меню из цифр, `Scanner`, таблицы из символов | окно JavaFX: вкладки, таблицы `TableView`, формы, диалоги, диаграммы |
| Описание интерфейса | Java-код (`ui/*Menu.java`) | FXML-разметка (6 файлов) + CSS (1 файл) + контроллеры |
| Java | 17 | 21 |
| Запуск | `mvn compile exec:java` | `mvnw javafx:run` (Maven ставить не нужно — есть Maven Wrapper) |
| БД | PostgreSQL, создаётся вручную | та же PostgreSQL; дополнительно — `docker compose up` |
| Настройки БД | `db.properties` | `database.properties` (как в методичке) |
| Поиск | 2 отдельных пункта меню (по абоненту / по описанию) | одно поле поиска сразу по ФИО, телефону, описанию и тарифу; поиск по абонентам |
| Фильтры | по статусу **или** по типу (по одному за раз) | статус **и** тип **и** период дат одновременно + «только с активными заявками» |
| Сортировка | 3 пункта меню | 5 готовых вариантов + щелчок по любому заголовку столбца (в т. ч. по нескольким) |
| Статистика | 6 показателей текстом | 11 показателей-плиток + круговая и столбчатая диаграммы + выбор периода |
| Экспорт | `.xlsx` всех заявок, имя файла вводится в консоли | `.xlsx` **и** `.csv` **текущей выборки** заявок или абонентов, путь — через `FileChooser` |
| Ошибки ввода | текст в консоли, повторный ввод | подсветка конкретного поля красным + текст под формой |
| Тесты | автономные проверки B (`ServiceChecks`, запуск вручную) | 16 JUnit-тестов, в т. ч. запуск всех 186 проверок B, — `mvnw test` |

**Не изменились:** схема БД, тестовые данные, репозитории (SQL), модельные классы `Subscriber` и
`ConnectionRequest`, шесть бизнес-правил из `PKS.md`.

---

## 2. Точка отсчёта: с чем сравниваем

- **КР1** — итоговое состояние ветки `origin/main` (коммит `d7aa738 Update README.md`): там есть код всех
  четырёх ролей, в том числе бизнес-логика B.
- **Ветка `pr2`** на начало работы (коммит `0fab87f`) **отставала** от `origin/main`: в ней
  `SubscriberService` и `ConnectionRequestService` ещё были заглушками (`UnsupportedOperationException("TODO(B)…")`),
  не было `diagram.md` и проверок `ServiceChecks`. Поэтому эти файлы сначала были взяты из `origin/main`
  (без merge, просто содержимым файлов), и только потом на них строилась КР2.
- Всё описанное ниже — изменения рабочей копии ветки `pr2` относительно КР1. **Ничего не закоммичено.**

---

## 3. Сводка изменений в цифрах

| Что | Количество |
|---|---|
| Новых Java-классов в `main` | 26 (из них 18 — пакет `fx`) |
| Новых FXML-файлов | 6 |
| Новых CSS-файлов | 1 (`app.css`, ~460 строк) |
| Изменённых Java-классов из КР1 | 8 (+ тестовый `ServiceChecks`) |
| Удалённых Java-классов КР1 | 10 (весь консольный интерфейс + консольный сервис поиска) |
| Новых тестовых классов | 5 (+ изменён `ServiceChecks`) |
| Новых файлов инфраструктуры | `docker-compose.yml`, `.env.example`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/…`, `run.ps1`, `run.sh` |
| Новой документации | `KR2.md` (защита), `difference.md` (этот файл); `README.md` переписан |

---

## 4. Архитектура: было → стало

### Было (КР1)

```
Main.java  ── собирает все объекты вручную ──┐
                                             ▼
ui/ConsoleApp → ui/SubscriberMenu, RequestMenu, StatisticsMenu  (Scanner, System.out)
      │              util/InputReader, ui/TablePrinter, ui/Labels
      ▼
service/SubscriberService, ConnectionRequestService      (B)
service/ConnectionRequestQueryService, StatisticsService (D)
      ▼
repository/*RepositoryImpl  (JDBC)                       (A)
      ▼
PostgreSQL
```

### Стало (КР2)

```
fx/IspFxApplication (Stage) ── AppContext.create() ── собирает util → repository → service
      │
      ├─ ViewLoader: FXML → узлы (Node) + контроллеры через фабрику
      │
View (FXML + CSS)         main-view, requests-tab, subscribers-tab, request-form, subscriber-form, statistics-view
      │  fx:id, onAction
Controller (fx/controller) Main, Requests, Subscribers, RequestForm, SubscriberForm, Statistics (+ FormController)
      │       │
      │       └─ DataStore: ObservableList<Subscriber>, ObservableList<RequestRow>
      ▼
Service (service/)        SubscriberService, ConnectionRequestService      — правила и валидация (B)
                          RequestFilter, SubscriberFilter                   — поиск/фильтр (D)
                          StatisticsService + StatisticsReport             — статистика (D)
                          ExportService (+ util/CsvExporter, ExcelExporter) — экспорт (D)
      ▼
Repository / DAO          без изменений с КР1 (A)
      ▼
PostgreSQL                та же схема; можно запустить в Docker
```

### Главные архитектурные решения КР2

1. **`AppContext` вместо ручной сборки в `Main`.** Все объекты создаются в одном месте снизу вверх.
   Контроллеры получают готовые сервисы через конструктор и ничего не знают о БД и репозиториях.
2. **Контроллеры создаёт наша фабрика** (`ViewLoader.createController`), а не FXMLLoader «как получится»:
   так в контроллер передаются зависимости, а не создаются внутри него.
3. **Общие данные интерфейса — в `DataStore`**: обе вкладки работают с одними и теми же `ObservableList`.
   После любой операции вызывается `reload()`, и все таблицы обновляются сами.
4. **Вся проверка данных — в сервисах.** Формы ничего не проверяют сами, а показывают ошибку,
   которую вернул сервис, и подсвечивают поле по ключу из `ValidationException`.
5. **Нет SQL в контроллерах, нет JavaFX в сервисах.** Это то же правило, что было в КР1 для консоли.

---

## 5. Сборка, Java, зависимости (`pom.xml`)

### Изменения в `pom.xml`

| Параметр | КР1 | КР2 | Зачем |
|---|---|---|---|
| `version` | `1.0.0` | `2.0.0` | новая версия проекта |
| `name` | `ISP Team - КР1` | `ISP Team - КР2` | |
| `maven.compiler.release` | `17` | `21` | методичка требует Java 21 |
| `postgresql` | `42.7.4` | `42.7.13` | версия из методички |
| `poi-ooxml` | `5.3.0` | `5.3.0` | без изменений, вынесена в свойство |
| `javafx-controls`, `javafx-fxml` | — | `21.0.8` | **новое**: элементы интерфейса и загрузка FXML |
| `slf4j-api`, `slf4j-simple` | — | `2.0.17` | **новое**: журнал ошибок вместо `System.out` |
| `log4j-to-slf4j` | — | `2.23.1` | **новое**: Apache POI пишет в Log4j2 — перенаправляем в slf4j (иначе в консоль сыпалось «Log4j2 could not find a logging implementation») |
| `junit-jupiter`, `assertj-core` | — | `5.13.4`, `3.27.3` | **новое**: unit-тесты |

| Плагин | КР1 | КР2 | Зачем |
|---|---|---|---|
| `maven-compiler-plugin` | по умолчанию | `3.14.0`, `<parameters>true</parameters>` | компиляция под Java 21 |
| `maven-resources-plugin` | по умолчанию | `3.3.1`, UTF-8 | копирование FXML/CSS/`database.properties` в сборку в правильной кодировке |
| `maven-surefire-plugin` | по умолчанию | `3.5.3`, `useModulePath=false` | запуск JUnit 5 в немодульном проекте |
| `javafx-maven-plugin` | — | `0.0.8`, `mainClass=ru.mirea.ispteam.fx.IspFxApplication` | **новое**: команда `mvn javafx:run` |
| `exec-maven-plugin` | запуск консоли `Main` | **удалён** | консольного приложения больше нет |
| `maven-shade-plugin` | jar с `Main` | jar с `fx.Launcher`, фильтр подписей `META-INF/*.SF` и т. п. | `java -jar target/isp-team.jar` открывает окно JavaFX |

Почему в jar главный класс `Launcher`, а не `IspFxApplication`: если главный класс наследует
`javafx.application.Application`, Java при запуске из classpath требует JavaFX в module path и выдаёт
«JavaFX runtime components are missing». Класс-обёртка `Launcher` это обходит.

Зависимости `jackson-*` из примера в методичке **не добавлены**: они нужны готовому клиенту из архива,
который ходит в REST API. Наше приложение работает с БД напрямую через JDBC, JSON ему не нужен.

### Maven Wrapper (новое)

Файлы `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`. Wrapper сам скачивает Maven 3.9.9
при первом запуске, поэтому Maven устанавливать не нужно — достаточно JDK 21 в `JAVA_HOME`:

```powershell
.\mvnw.cmd javafx:run      # вместо mvn javafx:run
.\mvnw.cmd test
.\mvnw.cmd clean package
```

### Скрипты запуска (новое)

- `run.ps1` (Windows) — проверяет, что задан `JAVA_HOME` и существует `database.properties`, затем
  `mvnw.cmd javafx:run`. Сохранён в UTF-8 с BOM, чтобы Windows PowerShell 5.1 правильно показывал русский текст.
- `run.sh` (Linux/macOS) — то же самое через `./mvnw`.

---

## 6. Подключение к БД и Docker

### Файл настроек: `db.properties` → `database.properties`

| | КР1 | КР2 |
|---|---|---|
| Файл | `src/main/resources/db.properties` | `src/main/resources/database.properties` (имя из методички) |
| Шаблон в git | `db.properties.example` | `database.properties.example` (старый шаблон удалён) |
| Ключи | `db.url`, `db.user`, `db.password`, `db.initOnStartup` | те же самые |
| Переопределение | нет | любой ключ можно задать системным свойством: `-Ddb.password=...` |
| В git | игнорируется | игнорируется; строка для старого `db.properties` в `.gitignore` оставлена, чтобы чей-то старый файл с паролем случайно не попал в коммит |

Если у вас остался `db.properties` от КР1 — просто переименуйте его в `database.properties` (содержимое то же).
Шаблон по умолчанию настроен на БД из Docker (порт 55433), локальный PostgreSQL — закомментированный вариант.

### `DatabaseManager` (роль A) — что изменилось

| | КР1 | КР2 |
|---|---|---|
| Чтение настроек | только `db.properties` | только `database.properties`; понятная ошибка, если файла нет («Скопируйте database.properties.example…») |
| Проверка `db.url` | нет | если `db.url` пустой — `DatabaseException` с понятным текстом |
| Проверка подключения при старте | нет (ошибка всплывала при первом запросе) | новый метод `checkConnection()` — открывает и закрывает соединение при запуске |
| Сообщения | `System.out.println(...)` | журнал slf4j (`log.info`) |
| `getConnection`, `runScript`, `initializeIfEnabled` | — | без изменений |

### Docker для БД КР1 (новое)

Методичка требует Docker и разрешает запускать БД КР1 в контейнере («JDBC-подключение выполняется
к опубликованному порту PostgreSQL»). Добавлено:

- **`docker-compose.yml`** — один сервис `isp-db` (`postgres:16-alpine`):
  - порт `55433` на компьютере → `5432` в контейнере (`5432` часто занят локальным PostgreSQL,
    `55432` — демо-системой из архива, поэтому выбран `55433`);
  - данные в volume `isp-db-data` — сохраняются после `docker compose down`;
  - при первом запуске на пустом volume автоматически выполняются `schema.sql` и `seed-data.sql`
    (смонтированы в `/docker-entrypoint-initdb.d/`);
  - healthcheck `pg_isready` — `docker compose up -d --wait` ждёт, пока БД реально готова.
- **`.env.example`** — имя БД, пользователь, пароль, порт. Реальный `.env` в git не попадает.
  Если пароль не задан, compose останавливается с понятной ошибкой
  (`${ISP_DB_PASSWORD:?Задайте ISP_DB_PASSWORD…}`).

```powershell
copy .env.example .env          # вписать пароль
docker compose up -d --wait     # поднять БД
docker compose ps               # isp-db: healthy
docker compose down             # остановить (данные сохраняются; не добавляйте -v)
```

---

## 7. Изменения по слоям

### 7.1 `model/` — модель предметной области

**`Subscriber`, `ConnectionRequest`** — **без изменений** (поля, конструкторы, `equals/hashCode/toString` из КР1).

**`RequestStatus`** — добавлены русская подпись и метод «активна ли заявка»:

```java
// КР1
public enum RequestStatus { NEW, IN_PROGRESS, APPROVED, COMPLETED, REJECTED, CANCELLED }

// КР2
public enum RequestStatus {
    NEW("Новая"), IN_PROGRESS("В работе"), APPROVED("Одобрена"),
    COMPLETED("Выполнена"), REJECTED("Отклонена"), CANCELLED("Отменена");

    public String getTitle() { ... }          // подпись для интерфейса
    public boolean isActive() {               // NEW, IN_PROGRESS, APPROVED — правила 4 и 5
        return this == NEW || this == IN_PROGRESS || this == APPROVED;
    }
}
```

**`RequestType`** — аналогично: `getTitle()` («Подключение», «Смена тарифа», «Техподдержка», «Отключение»,
«Замена оборудования») и `usesTariff()` — `true` только для `NEW_CONNECTION` и `TARIFF_CHANGE`
(по `PKS.md` разд. 2.2 тариф актуален только для них; форма по этому методу включает/выключает поле «Тариф»).

Почему так:

- В КР1 русские подписи жили в `ui/Labels` (консольный пакет) — JavaFX не мог их переиспользовать.
  Перенос в сами enum'ы убрал дублирование.
- `isActive()` раньше был **скопирован** в двух сервисах (`private static boolean isActive(...)` и в
  `SubscriberService`, и в `ConnectionRequestService`). Теперь он один — в enum'е.
- **`toString()` НЕ переопределён** — `name()`/`toString()` по-прежнему `NEW`, `IN_PROGRESS`… В БД хранится
  `name()`, а проверки B строят строки вида `from + ":" + to`. Переопределение `toString()` ломало эти проверки,
  поэтому в интерфейсе подписи выводятся через `getTitle()` и `StringConverter`.

### 7.2 `exception/` — исключения

**`ValidationException`** — добавлено необязательное поле `field` (ключ поля формы):

```java
// КР1
public ValidationException(String message)

// КР2
public ValidationException(String message)                 // как раньше: field = null
public ValidationException(String field, String message)   // новое
public String getField()                                   // "fullName", "phone", "email", "address",
                                                           // "registrationDate", "subscriber", "type",
                                                           // "status", "tariffPlan", "description", "period", "file"
```

Зачем: форма по ключу находит нужный элемент ввода, красит его рамку в красный (CSS-класс `field-error`)
и ставит на него фокус. Методичка: «пользователь должен получить понятное сообщение: какое поле заполнено
неверно». Старый конструктор оставлен — весь код КР1 компилируется без изменений.

**`ExportException`** — **новый**. Ошибка записи файла экспорта (нет прав, файл открыт в Excel).
Оборачивает `IOException`. В КР1 `ExcelExporter` бросал «голый» `RuntimeException`.

`BusinessException`, `EntityNotFoundException`, `DatabaseException` — **без изменений**.

### 7.3 `repository/` — доступ к данным (DAO)

**Без изменений.** `CrudRepository`, `SubscriberRepository`, `ConnectionRequestRepository` и их JDBC-реализации
на `PreparedStatement` — ровно как в КР1. Это прямое подтверждение того, что многослойная архитектура
сработала: интерфейс заменили полностью, а слой доступа к данным не тронули.

### 7.4 `service/` — бизнес-логика

#### `SubscriberService` (роль B) — изменён

| Что | КР1 | КР2 |
|---|---|---|
| Дата регистрации | любая (`null` → сегодня) | `null` → сегодня; **дата в будущем запрещена** → `ValidationException("registrationDate", "Дата регистрации не может быть позже сегодняшней")` |
| Порядок в `create` | проверка уникальности, потом дата | сначала дата (дешёвая проверка), потом уникальность (запрос к БД) |
| Ошибки ФИО/телефона/адреса/email | `ValidationException(message)` | `ValidationException(field, message)` с ключом поля |
| Текст ошибки email | «Некорректный email» | «Некорректный email (пример: name@example.com)» |
| Метод `required(...)` | `required(value, label, max)` | `required(value, field, label, max)` |
| Проверка активных заявок при удалении | свой `private static isActive(...)` | `r.getStatus().isActive()` |

Бизнес-правила 1 и 4 из `PKS.md` (формат ФИО/телефона, запрет удаления абонента с активными заявками)
и проверка уникальности телефона/email работают **как в КР1**.

#### `ConnectionRequestService` (роль B) — изменён

| Что | КР1 | КР2 |
|---|---|---|
| Таблица переходов статусов | приватный `switch` внутри `checkTransition` | **публичный** `static Set<RequestStatus> allowedTransitions(RequestStatus from)`; `checkTransition` использует его |
| Абонент не выбран | «ID абонента должен быть положительным числом» | `ValidationException("subscriber", "Выберите абонента")` |
| Ошибки типа/описания/тарифа/статуса | без поля | с ключами `type`, `description`, `tariffPlan`, `status` |
| Текст ошибки перехода | `Недопустимый переход статуса: NEW -> COMPLETED` | `Недопустимый переход статуса: «Новая» -> «Выполнена»` |
| Текст при создании не с NEW | «…должна иметь статус NEW» | «…должна иметь статус «Новая»» |
| Проверка «активная» | свой `private static isActive(...)` | `getStatus().isActive()` |

Зачем `allowedTransitions` стал публичным: форма редактирования и диалог «Сменить статус» предлагают
**только допустимые** статусы, а карточка заявки показывает «Можно перевести в: …». Таблица переходов
при этом по-прежнему хранится в одном месте — в сервисе.

Бизнес-правила 2, 3, 5, 6 из `PKS.md` работают **как в КР1** (это подтверждают 186 проверок `ServiceChecks`).

#### `RequestFilter` (роль D) — **новый**

`record RequestFilter(String query, RequestStatus status, RequestType type, LocalDate from, LocalDate to)`

- `matches(ConnectionRequest request, Subscriber subscriber)` — подходит ли заявка под **все** заданные
  условия (логическое И). Любое условие может быть `null` — тогда оно не применяется.
- Поиск `query` — подстрока **без учёта регистра** сразу по четырём полям: ФИО абонента, телефон абонента,
  описание заявки, тарифный план.
- Период — по дате создания, **границы включительно**.
- `validate()` — логическая проверка дат: «с» не позже «по», иначе `ValidationException("period", …)`.
- `RequestFilter.EMPTY` — фильтр без условий.

Используется: `RequestsController` (предикат `FilteredList`), `StatisticsService` (заявки за период).

#### `SubscriberFilter` (роль D) — **новый**

`record SubscriberFilter(String query, boolean onlyWithActive)` — поиск абонентов по ФИО, телефону, email
и адресу и фильтр «только с активными заявками». Используется во вкладке «Абоненты».

#### `ConnectionRequestQueryService` (роль D) — **удалён**

В КР1 у него было 6 методов, каждый из которых заново читал **все** заявки из БД и возвращал новый список
для печати в консоль. В JavaFX данные уже загружены в `ObservableList`, а поиск/фильтр/сортировка
выполняются «на лету» через `FilteredList` и `SortedList`. Соответствие:

| Метод КР1 | Как это делается в КР2 |
|---|---|
| `searchBySubscriber(query)` | поле «Поиск» → `RequestFilter.query` (ФИО/телефон) |
| `searchByDescription(keyword)` | то же поле «Поиск» → `RequestFilter.query` (описание/тариф) |
| `filterByStatus(status)` | список «Статус» → `RequestFilter.status` |
| `filterByType(type)` | список «Тип заявки» → `RequestFilter.type` |
| `sortByCreatedAt(ascending)` | «Сортировка» → «Сначала новые»/«Сначала старые» или щелчок по столбцу «Создана» |
| `sortByStatus()` | «Сортировка» → «По статусу» или щелчок по столбцу «Статус» |

Плюс в КР2 есть то, чего в КР1 не было: комбинация условий, период дат, сортировка по абоненту и типу,
сортировка по нескольким столбцам (Shift+щелчок).

#### `StatisticsService` (роль D) — переписан

| | КР1 | КР2 |
|---|---|---|
| API | 6 методов: `totalSubscribers()`, `totalRequests()`, `activeRequests()`, `completedRequests()`, `cancelledOrRejectedRequests()`, `countByType()` | один метод `StatisticsReport buildReport(LocalDate from, LocalDate to)` |
| Обращения к БД | каждый метод заново читал все данные (6+ запросов на экран статистики) | данные читаются **один раз**, все показатели считаются в памяти |
| Показателей | 6 | 11 + 2 распределения |

Показатели `StatisticsReport` (**новый** record):

| № | Показатель | Как считается |
|---|---|---|
| 1 | Всего абонентов | `subscribers.size()` |
| 2 | Всего заявок | `requests.size()` |
| 3 | Активных | `status.isActive()` |
| 4 | Выполнено | `status == COMPLETED` |
| 5 | Отклонено или отменено | `REJECTED` + `CANCELLED` |
| 6 | Доля выполненных, % | `completed * 100 / total` (0, если заявок нет) |
| 7 | Заявок на одного абонента | `total / subscribers` (0, если абонентов нет) |
| 8 | Среднее время выполнения | `Duration` от `createdAt` до `updatedAt` у выполненных, `DoubleSummaryStatistics.getAverage()` |
| 9 | Мин. / макс. время выполнения | `getMin()` / `getMax()` той же статистики |
| 10 | Самый популярный тариф | `groupingBy(tariff, counting())` → `max` (при равенстве — по алфавиту) |
| 11 | Заявок за период | `RequestFilter(…, from, to).matches(…)` |
| — | По статусам / по типам | `EnumMap` со **всеми** значениями enum (в т. ч. нулевыми) — для диаграмм |

Если данных нет (нет выполненных заявок), время выполнения — `OptionalDouble.empty()`, и окно пишет «нет данных»
вместо деления на ноль.

#### `ExportService` (роль D) — **новый**

- `exportRequests(List<ConnectionRequest>, Path)` и `exportSubscribers(List<Subscriber>, Path)`.
- Готовит строки в понятном виде: **ФИО и телефон абонента вместо `subscriberId`**, русские названия типов
  и статусов, даты `дд.ММ.гггг ЧЧ:мм`. Для абонентов дополнительно считает «Заявок всего» и «Активных заявок».
- Формат выбирается **по расширению**: `.csv` → `CsvExporter`, `.xlsx` → `ExcelExporter`, другое →
  `ValidationException("file", "Поддерживаются только форматы .csv и .xlsx")`.

Отличие от КР1: там экспортировались **все** заявки с техническими полями (ID, ID абонента, `NEW`…),
и только в `.xlsx`. Теперь экспортируется **текущая выборка** таблицы (с учётом поиска, фильтров
и сортировки), в двух форматах и в читаемом виде.

### 7.5 `util/` — вспомогательные классы

| Класс | Было | Стало |
|---|---|---|
| `DatabaseManager` | см. раздел 6 | см. раздел 6 |
| `ExcelExporter` | `exportRequests(List<ConnectionRequest>, String filePath)`: фиксированные 8 столбцов, жирная шапка, автоширина; ошибка → `RuntimeException` | `export(TableData, Path)`: любая таблица; серая жирная шапка, автоширина с ограничением 60 символов (длинные описания не растягивают лист), **закреплённая первая строка**; ошибка → `ExportException` с подсказкой «файл открыт в другой программе?» |
| `CsvExporter` | — | **новый**: UTF-8 **с BOM** и разделитель `;` — русский Excel сразу открывает файл по столбцам без «кракозябр»; значения с `;`, кавычками и переводами строк берутся в кавычки, кавычки удваиваются (RFC 4180); строки разделяются `\r\n` |
| `TableData` | — | **новый** record: `title`, `headers`, `rows` (строки уже текстом). Один и тот же `TableData` пишется и в CSV, и в XLSX — подготовка данных не зависит от формата |
| `InputReader` | безопасный ввод из консоли | **удалён** (консоли нет; ввод — поля формы, проверка — в сервисах) |

### 7.6 `ui/` → `fx/`: новый интерфейс

Консольный пакет `ui/` (7 классов) удалён целиком, вместо него — пакет `fx/` (18 классов).

#### Точка входа и инфраструктура

| Класс | Назначение |
|---|---|
| `fx/IspFxApplication` | наследник `Application`. В `start(Stage)`: ставит обработчик непойманных исключений (показывает окно ошибки через `Platform.runLater`), вызывает `AppContext.create()` (при ошибке БД — понятное окно и выход), создаёт `DataStore` и `ViewLoader`, загружает `main-view.fxml`, создаёт `Scene` с CSS, задаёт размеры окна (1280×800, минимум 1000×640), показывает окно и загружает данные |
| `fx/Launcher` | `main` для запуска из jar (см. раздел 5) |
| `fx/ViewLoader` | загрузка FXML (`load("…fxml")` → `View<C>(root, controller)`), фабрика контроллеров (передаёт сервисы в конструкторы), создание модальных окон (`modalStage`) и сцен с CSS (`styledScene`), путь к CSS (`stylesheet()`) |
| `fx/FormDialogs` | открытие формы заявки и формы абонента как модальных окон; возвращает `Optional` с сохранённым объектом или пусто при «Отмена». Одну и ту же форму заявки открывают обе вкладки — без дублирования |
| `fx/DataStore` | `ObservableList<Subscriber>`, `ObservableList<RequestRow>`, `reload()` (через сервисы, без SQL), подсчёт заявок/активных заявок абонента, сообщение для строки состояния (`messageProperty`) |
| `fx/model/RequestRow` | строка таблицы: заявка + её абонент. В таблице показываются ФИО и телефон, а не технический `subscriberId` (требование «не выводить технические данные») |

#### Контроллеры

| Контроллер | FXML | Что делает |
|---|---|---|
| `MainController` | `main-view.fxml` | шапка (кнопки «Обновить», «Статистика»), вкладки (подключены через `fx:include`), строка состояния; окно статистики — немодальное, при повторном нажатии поднимается уже открытое |
| `RequestsController` | `requests-tab.fxml` | таблица заявок (`FilteredList` → `SortedList` → `TableView`), поиск, фильтры статус/тип/период (с проверкой периода), 5 вариантов сортировки, карточка выбранной заявки с данными абонента и допустимыми переходами, кнопки «Новая заявка», «Изменить» (и двойной щелчок), «Сменить статус» (`ChoiceDialog` только с допустимыми статусами), «Удалить» (с подтверждением), «Экспорт в файл…»; кнопки без выбранной строки неактивны; счётчик «Показано X из Y» |
| `SubscribersController` | `subscribers-tab.fxml` | таблица абонентов (столбцы «Заявок» и «Активных»), поиск по 4 полям, фильтр «только с активными заявками», CRUD с подтверждением удаления (с предупреждением, сколько закрытых заявок удалится вместе с абонентом), «Новая заявка для абонента» (форма открывается с уже выбранным абонентом), нижняя таблица — заявки выбранного абонента, экспорт абонентов |
| `FormController<T>` | — | **общая база форм**: регистрация полей по ключам, сброс ошибок, показ ошибки (для `ValidationException` — подсветка поля и фокус; для `Business`/`EntityNotFound` — текст под формой; для ошибок БД — отдельное окно), закрытие окна с результатом, кнопка «Отмена» |
| `RequestFormController` | `request-form.fxml` | абонент выбирается из `ComboBox` (отсортирован по ФИО, показ «ФИО · телефон»), тип — `ComboBox`, статус: при создании только «Новая» (заблокирован), при редактировании — текущий + допустимые переходы; поле тарифа активно только для подключения/смены тарифа; счётчик символов описания «N / 1000 (не менее 5 символов)» |
| `SubscriberFormController` | `subscriber-form.fxml` | ФИО, телефон, email, адрес, дата регистрации (`DatePicker`, по умолчанию сегодня; при редактировании заблокирована, т. к. сервис её не меняет); ручной ввод даты проверяется на формат `дд.мм.гггг` |
| `StatisticsController` | `statistics-view.fxml` | выбор периода (с проверкой), 11 плиток, круговая диаграмма по статусам (нулевые не показываются), горизонтальная столбчатая по типам (целые числа на оси) |

#### Вспомогательные классы `fx/util`

| Класс | Назначение |
|---|---|
| `Dialogs` | окна ошибки/информации/подтверждения/выбора. `showError(Window, Throwable)` — **единая обработка исключений**: каждому типу свой заголовок («Некорректные данные», «Операция невозможна», «Запись не найдена», «Ошибка базы данных», «Ошибка экспорта», «Непредвиденная ошибка»); stack trace — только в журнал slf4j. В подтверждении удаления по умолчанию выбрана «Отмена» |
| `Cells` | фабрики ячеек: форматирование (даты, enum'ы — ячейка хранит исходное значение, поэтому сортировка правильная) и цветная «плашка» статуса |
| `Formats` | форматы дат `дд.ММ.гггг` и `дд.ММ.гггг ЧЧ:мм`, числа с запятой, часы → «2 д 5 ч», `StringConverter` для `ComboBox`, конвертер `DatePicker`, который не роняет приложение при неверном ручном вводе |
| `Option<T>` | пункт списка «подпись + значение» — нужен для пунктов «Все статусы» / «Все типы» (значение `null`) |
| `ExportFiles` | `FileChooser` для экспорта: фильтры «Книга Excel (*.xlsx)» и «Текст CSV (*.csv)», имя по умолчанию `zayavki_ГГГГ-ММ-ДД.xlsx`, стартовая папка — домашняя папка пользователя (**никаких «зашитых» путей**); если пользователь стёр расширение — добавляется расширение выбранного типа |

### 7.7 Ресурсы: FXML, CSS, настройки

Все в `src/main/resources/`.

| Файл | Статус | Содержимое |
|---|---|---|
| `ru/mirea/ispteam/fx/view/main-view.fxml` | новый | `BorderPane`: шапка, `TabPane` с двумя `fx:include`, строка состояния |
| `…/view/requests-tab.fxml` | новый | панель действий, блок «Поиск, фильтры и сортировка» (`FlowPane` — переносится при сужении окна), `SplitPane`: таблица + карточка заявки фиксированной ширины |
| `…/view/subscribers-tab.fxml` | новый | панель действий, поиск/фильтр, вертикальный `SplitPane`: абоненты + их заявки |
| `…/view/request-form.fxml` | новый | форма заявки (`GridPane`), подсказки под полями, блок ошибки, «Отмена»/«Сохранить» (Enter = сохранить, Esc = отмена) |
| `…/view/subscriber-form.fxml` | новый | форма абонента |
| `…/view/statistics-view.fxml` | новый | период, 11 плиток (`FlowPane`), `PieChart` и `BarChart` |
| `…/fx/css/app.css` | новый | всё оформление: палитра в переменных на `.root`; шапка, вкладки, карточки; кнопки (`primary`, `danger`, `flat`, неактивные); таблицы (чередование строк, выделение, наведение); плашки 6 статусов; поля ввода; **состояния ошибки** (`field-error` — красная рамка и фон, `form-error`, `error-text`); плитки статистики; диалоги |
| `database.properties.example` | новый (вместо `db.properties.example`) | см. раздел 6 |
| `simplelogger.properties` | новый | формат журнала slf4j (время, уровень `info`) |
| `schema.sql`, `seed-data.sql` | **без изменений** | 2 таблицы с PK/FK/UNIQUE/CHECK; 5 абонентов, 12 заявок |

### 7.8 Тесты

| Класс | Статус | Что проверяет |
|---|---|---|
| `ServiceChecks` | из КР1 (B), **изменены 2 проверки** | 186 сценариев бизнес-правил. Проверки «D search integration» и «D statistics integration» вызывали удалённые методы `ConnectionRequestQueryService.searchBySubscriber` и `StatisticsService.totalSubscribers/activeRequests` — теперь они делают то же через `RequestFilter` и `buildReport`. Число проверок не изменилось |
| `BusinessRulesTest` | новый | запускает все `ServiceChecks` внутри `mvn test` (раньше их надо было запускать вручную через `java -cp …`); таблица `allowedTransitions`; ключ поля в `ValidationException`; запрет даты регистрации в будущем |
| `RequestFilterTest` | новый | поиск по ФИО/телефону/описанию/тарифу без учёта регистра; комбинация условий (И); границы периода включительно; ошибка при «с» > «по»; `SubscriberFilter` |
| `StatisticsServiceTest` | новый | все показатели `buildReport` на подготовленных данных (в т. ч. среднее/мин/макс время, популярный тариф, нулевые значения в распределениях); пустая БД без деления на ноль; ошибка периода |
| `ExportServiceTest` | новый | CSV: BOM, шапка, экранирование `;` и кавычек; XLSX: лист «Абоненты» читается обратно через POI; неподдерживаемое расширение |
| `TestData` | новый | репозитории в памяти для тестов без PostgreSQL |

Итого `mvnw test`: **16 тестов** (включая 186 проверок B), БД не нужна.

---

## 8. Функции КР1 → где они в КР2

Пункты главного меню КР1 (`PKS.md` разд. 1) и их замена:

| Меню КР1 | КР2 |
|---|---|
| 1. Абоненты (список, добавить, изменить, удалить) | вкладка «Абоненты»: таблица, «+ Новый абонент», «Изменить» / двойной щелчок, «Удалить» с подтверждением |
| 2. Заявки (список, создать, изменить, сменить статус, удалить) | вкладка «Заявки»: таблица, «+ Новая заявка», «Изменить», «Сменить статус», «Удалить» с подтверждением |
| 3. Поиск (по абоненту / по описанию) | одно поле «Поиск» на вкладке «Заявки» (+ поиск на вкладке «Абоненты») |
| 4. Фильтрация и сортировка | блок «Поиск, фильтры и сортировка» + сортировка щелчком по заголовкам |
| 5. Статистика | кнопка «Статистика» в шапке → отдельное окно |
| 6. Экспорт данных | кнопка «Экспорт в файл…» на каждой вкладке |
| 7. Вывести таблицы базы данных | не нужен: таблицы всегда на экране; связанные данные — карточка заявки и заявки абонента |
| 0. Выход | закрытие окна |
| Ввод ID абонента числом при создании заявки | выбор абонента из `ComboBox` (методичка, часть 4) |
| Повторный ввод при ошибке (`InputReader`) | подсветка поля + текст ошибки, данные в форме сохраняются |
| Обработчик ошибок `ConsoleHelper` | `Dialogs.showError` + `FormController.showError` |

---

## 9. Удалённые файлы и почему

| Файл | Почему удалён |
|---|---|
| `src/main/java/ru/mirea/ispteam/Main.java` | точка входа консольного приложения; сборка зависимостей перенесена в `AppContext`, точка входа — `fx/IspFxApplication` |
| `ui/ConsoleApp.java`, `ui/SubscriberMenu.java`, `ui/RequestMenu.java`, `ui/StatisticsMenu.java` | консольные меню — заменены окнами JavaFX. Методичка: «нельзя использовать консоль как основной интерфейс после перехода на JavaFX» |
| `ui/ConsoleHelper.java`, `ui/TablePrinter.java` | вывод ошибок и таблиц в консоль — заменены `Dialogs` и `TableView` |
| `ui/Labels.java` | русские подписи enum'ов перенесены в сами enum'ы (`getTitle()`) |
| `util/InputReader.java` | чтение ввода из консоли — заменено полями форм |
| `service/ConnectionRequestQueryService.java` | консольный поиск/фильтр/сортировка с отдельным запросом к БД на каждое действие — заменены `RequestFilter` + `FilteredList`/`SortedList` (см. 7.4) |
| методы `StatisticsService.totalSubscribers()` … `countByType()` | использовались только консольным меню — заменены `buildReport()` |
| метод `ExcelExporter.exportRequests(List, String)` | использовался только консолью — заменён `ExportService` + `ExcelExporter.export(TableData, Path)` |
| `src/main/resources/db.properties.example` | заменён `database.properties.example` |
| `D.md` | отчёт роли D о КР1; устарел (в нём написано, что B и `Main` не реализованы) |
| `exec-maven-plugin` в `pom.xml` | запускал консольное приложение |

Всё удалённое остаётся в истории git (`origin/main`, ветки `feature/*`) — при необходимости его можно посмотреть
или восстановить, например: `git show origin/main:src/main/java/ru/mirea/ispteam/ui/RequestMenu.java`.

**Оставлены** документы КР1, которые нужны и для КР2:
- `PKS.md` — спецификация предметной области, полей, enum'ов, переходов статусов и 6 бизнес-правил
  (на неё ссылаются комментарии в коде; методичка требует «тех же сущностей и бизнес-правил»);
- `diagram.md` — ER-диаграмма БД (на защиту нужна «база данных или скрипты для её создания»).

---

## 10. Что НЕ изменилось

- **Схема БД** (`schema.sql`): таблицы `subscribers` и `connection_requests`, PK, FK, `NOT NULL`, `UNIQUE`,
  `CHECK` для типа и статуса, индекс по `subscriber_id`.
- **Тестовые данные** (`seed-data.sql`): 5 абонентов, 12 заявок, все 6 статусов и 5 типов.
- **Репозитории** (весь пакет `repository/`) — SQL и маппинг строк.
- **`Subscriber`, `ConnectionRequest`** — поля, конструкторы, `equals/hashCode/toString`.
- **`BusinessException`, `EntityNotFoundException`, `DatabaseException`.**
- **6 бизнес-правил** из `PKS.md` разд. 2.4 и таблица переходов статусов из разд. 2.3.
- **Пакет** `ru.mirea.ispteam`, разделение ролей A/B/C/D.

---

## 11. Изменения контрактов из `PKS.md` — что важно знать команде

`PKS.md` просит не менять контракты в одиночку, поэтому вот полный список того, что поменялось:

1. **`ConnectionRequestQueryService` удалён** (контракт D из разд. 4). Замена — `RequestFilter` (см. 7.4).
2. **`StatisticsService`**: вместо 6 методов — `buildReport(from, to)`.
3. **`ExcelExporter`**: вместо `exportRequests(list, path)` — `export(TableData, Path)`.
4. **`ValidationException`**: добавлен конструктор с ключом поля и `getField()` (старый конструктор остался).
5. **`ConnectionRequestService`**: добавлен `public static allowedTransitions(...)` (сигнатуры CRUD не менялись).
6. **`RequestStatus` / `RequestType`**: добавлены `getTitle()`, `isActive()`, `usesTariff()` (`name()` не изменился).
7. **`SubscriberService.create`**: новое правило — дата регистрации не в будущем.
8. **Настройки БД**: `db.properties` → `database.properties`.
9. **Java 17 → 21** — у каждого участника должен быть установлен JDK 21.
10. **Сборка объектов** — в `AppContext`, а не в `Main`.

Контракты `CrudRepository`, `SubscriberRepository`, `ConnectionRequestRepository` и CRUD-методы обоих
сервисов B (`create`, `getById`, `getAll`, `update`, `changeStatus`, `delete`) **не изменились**.

---

## 12. Как всё проверено

1. **Сборка:** `mvnw.cmd clean package` — `BUILD SUCCESS`, собирается `target/isp-team.jar` (~30 МБ, с JavaFX и драйвером).
2. **Unit-тесты:** 16 тестов, 0 ошибок; внутри — все 186 проверок бизнес-правил B.
3. **Интерфейс на настоящей PostgreSQL.** Во временной папке (вне репозитория) запускалась встроенная
   PostgreSQL 16; приложение само создало схему и загрузило тестовые данные. Окна рендерились без показа
   на экране и сохранялись как скриншоты. Автоматически проверены 26 сценариев:
   - загрузка 5 абонентов и 12 заявок, сортировка по умолчанию «сначала новые»;
   - поиск по ФИО, телефону, описанию; фильтр по статусу, по статусу+типу, по типу; период июль 2024;
     ошибка при «с» > «по» и сброс;
   - сортировка по статусу и по абоненту;
   - заявки выбранного абонента; фильтр «только с активными заявками»;
   - форма заявки: пустая → подсвечено «Абонент»; описание `abc` → подсвечено «Описание»; корректные данные → сохранено;
   - форма абонента: телефон `8900` → подсвечен; существующий телефон → сообщение; дата в будущем → подсвечена; корректные данные → сохранено;
   - окно статистики; экспорт в `.xlsx` и `.csv` (14 строк CSV = шапка + 13 заявок).
4. **Не проверено** (нет доступа): запуск с вашей реальной БД, `docker compose up` (на ПК не установлен Docker),
   демо-система из архива КР2.

---

## 13. Полный список файлов КР2

```
.
├── .env.example                      параметры БД для Docker (шаблон)
├── .gitignore                        + database.properties, .env, *.csv
├── .mvn/wrapper/maven-wrapper.properties
├── docker-compose.yml                PostgreSQL для БД КР1 (порт 55433)
├── mvnw, mvnw.cmd                    Maven Wrapper
├── run.ps1, run.sh                   запуск JavaFX-клиента
├── pom.xml                           Java 21, JavaFX 21, JDBC, POI, slf4j, JUnit
├── README.md                         запуск и соответствие требованиям
├── KR2.md                            подготовка к защите
├── difference.md                     этот файл
├── PKS.md                            спецификация предметной области (КР1)
├── diagram.md                        ER-диаграмма
└── src
    ├── main/java/ru/mirea/ispteam
    │   ├── AppContext.java
    │   ├── exception/  BusinessException, DatabaseException, EntityNotFoundException, ExportException, ValidationException
    │   ├── model/      ConnectionRequest, RequestStatus, RequestType, Subscriber
    │   ├── repository/ CrudRepository, SubscriberRepository(+Impl), ConnectionRequestRepository(+Impl)
    │   ├── service/    SubscriberService, ConnectionRequestService, RequestFilter, SubscriberFilter,
    │   │               StatisticsService, StatisticsReport, ExportService
    │   ├── util/       DatabaseManager, CsvExporter, ExcelExporter, TableData
    │   └── fx/
    │       ├── IspFxApplication, Launcher, ViewLoader, FormDialogs, DataStore
    │       ├── model/      RequestRow
    │       ├── controller/ MainController, RequestsController, SubscribersController,
    │       │               FormController, RequestFormController, SubscriberFormController, StatisticsController
    │       └── util/       Cells, Dialogs, ExportFiles, Formats, Option
    ├── main/resources
    │   ├── database.properties.example, simplelogger.properties, schema.sql, seed-data.sql
    │   └── ru/mirea/ispteam/fx/
    │       ├── css/app.css
    │       └── view/ main-view, requests-tab, subscribers-tab, request-form, subscriber-form, statistics-view (.fxml)
    └── test/java/ru/mirea/ispteam/service
        ServiceChecks, BusinessRulesTest, RequestFilterTest, StatisticsServiceTest, ExportServiceTest, TestData
```
