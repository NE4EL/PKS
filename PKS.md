# КР1 — Информационная система интернет-провайдера
## Распределение ролей, архитектура и эталонная структура классов

> Этот файл — единый источник правды для команды из 4 человек и для любого Claude/ассистента, который будет помогать конкретному участнику. Каждый раздел закреплён за одной ролью. Не меняйте контракты (сигнатуры методов, имена классов/полей) в одиночку — любое изменение контракта согласуется в общем чате/PR, иначе части проекта перестанут собираться вместе.

---

## 1. Предметная область

**Тема:** Интернет-провайдер
**Основная сущность:** `ConnectionRequest` — заявка абонента (подключение, смена тарифа, техподдержка, отключение, замена оборудования)
**Сущность-участник:** `Subscriber` — абонент

Пакет проекта: `ru.mirea.ispteam`

```
СИСТЕМА УЧЁТА ЗАЯВОК ИНТЕРНЕТ-ПРОВАЙДЕРА
========================================
1. Абоненты
2. Заявки
3. Поиск
4. Фильтрация и сортировка
5. Статистика
6. Экспорт данных
7. Вывести таблицы базы данных
0. Выход
```

---

## 2. Доменная модель (эталон — не менять без согласования)

### 2.1 Subscriber (Абонент)

| Поле | Тип | Ограничения |
|---|---|---|
| id | Long | PK, автоинкремент |
| fullName | String | NOT NULL |
| phone | String | NOT NULL, UNIQUE, формат `+7XXXXXXXXXX` |
| email | String | UNIQUE, формат email |
| address | String | NOT NULL |
| registrationDate | LocalDate | NOT NULL, по умолчанию — дата создания |

### 2.2 ConnectionRequest (Заявка)

| Поле | Тип | Ограничения |
|---|---|---|
| id | Long | PK, автоинкремент |
| subscriberId | Long | FK -> subscribers.id, NOT NULL |
| type | RequestType (enum) | NOT NULL |
| status | RequestStatus (enum) | NOT NULL, default `NEW` |
| tariffPlan | String | nullable (актуально для NEW_CONNECTION / TARIFF_CHANGE) |
| description | String | NOT NULL |
| createdAt | LocalDateTime | NOT NULL |
| updatedAt | LocalDateTime | NOT NULL |

### 2.3 Enum'ы (обязательно минимум 1, у нас — 2)

```java
public enum RequestStatus {
    NEW, IN_PROGRESS, APPROVED, COMPLETED, REJECTED, CANCELLED
}

public enum RequestType {
    NEW_CONNECTION, TARIFF_CHANGE, TECHNICAL_SUPPORT, DISCONNECTION, EQUIPMENT_REPLACEMENT
}
```

**Разрешённые переходы статусов (эталон для бизнес-правил):**

```
NEW -> IN_PROGRESS -> APPROVED -> COMPLETED
NEW, IN_PROGRESS -> REJECTED
NEW, IN_PROGRESS, APPROVED -> CANCELLED
COMPLETED, REJECTED, CANCELLED -> (терминальные, переходов нет)
```

### 2.4 Бизнес-правила (минимум 5 — реализуются в `service/`, не в UI)

1. `fullName` и `phone` абонента обязательны и должны пройти валидацию формата — иначе `ValidationException`.
2. Нельзя создать заявку для несуществующего `subscriberId` — `EntityNotFoundException`.
3. Нельзя выполнить переход статуса, не входящий в таблицу выше — `BusinessException`.
4. Нельзя удалить абонента, если у него есть заявки в статусах `NEW/IN_PROGRESS/APPROVED` — `BusinessException`.
5. Нельзя создать вторую активную (`NEW/IN_PROGRESS/APPROVED`) заявку типа `NEW_CONNECTION` для одного абонента — `BusinessException`.
6. `description` заявки не может быть пустым или короче 5 символов — `ValidationException`.

### 2.5 Поиск / фильтрация / сортировка / статистика

- **Поиск (≥2):** по ФИО/телефону абонента (подстрока); по ключевому слову в `description`.
- **Фильтрация (≥2):** по `status`; по `type`; (опционально — по диапазону `createdAt`).
- **Сортировка (≥2):** по `createdAt`; по `status`.
- **Статистика (≥5 показателей):** всего абонентов; всего заявок; активных заявок (`NEW+IN_PROGRESS+APPROVED`); завершённых (`COMPLETED`); отменённых/отклонённых (`CANCELLED+REJECTED`); разбивка по `RequestType`.

---

## 3. Архитектура пакетов (эталон)

```
src/main/java/ru/mirea/ispteam/
├── Main.java                                  [C]
├── model/
│   ├── Subscriber.java                        [D]
│   ├── ConnectionRequest.java                 [D]
│   ├── RequestStatus.java                     [D]
│   └── RequestType.java                       [D]
├── repository/
│   ├── CrudRepository.java        (interface) [A]
│   ├── SubscriberRepository.java  (interface) [A]
│   ├── SubscriberRepositoryImpl.java           [A]
│   ├── ConnectionRequestRepository.java (int.) [A]
│   └── ConnectionRequestRepositoryImpl.java    [A]
├── service/
│   ├── SubscriberService.java                  [B]
│   ├── ConnectionRequestService.java           [B]
│   ├── ConnectionRequestQueryService.java  (поиск/фильтр/сорт/стат.) [D]
│   └── StatisticsService.java                  [D]
├── exception/
│   ├── EntityNotFoundException.java            [B]
│   ├── BusinessException.java                  [B]
│   ├── ValidationException.java                [B]
│   └── DatabaseException.java                  [A]
├── ui/
│   ├── ConsoleApp.java        (главное меню)   [C]
│   ├── SubscriberMenu.java                     [C]
│   ├── RequestMenu.java                        [C]
│   └── StatisticsMenu.java                     [C]
└── util/
    ├── DatabaseManager.java                    [A]
    ├── ExcelExporter.java                      [D]
    └── InputReader.java   (валидированный ввод)[C]

src/main/resources/
├── schema.sql                                  [A]
└── seed-data.sql                               [A]
```

Буквы `[A][B][C][D]` = зона ответственности участника (раздел 4). **Один файл — один явный владелец**, чтобы не было конфликтов в git.

---

## 4. Роли участников команды

### Участник A — Team/Tech Lead: База данных, JDBC, репозитории, инфраструктура
*(рекомендуется тебе — учитывая опыт с инфраструктурой/бэкендом)*

**Отвечает за:**
- Проектирование ER-диаграммы и схемы БД (`schema.sql`, `seed-data.sql`)
- `util/DatabaseManager.java` — подключение через JDBC (пул или простое соединение), try-with-resources
- `repository/` — интерфейс `CrudRepository<T, ID>` + реализации `SubscriberRepositoryImpl`, `ConnectionRequestRepositoryImpl` на `PreparedStatement`
- `exception/DatabaseException.java`
- Настройку git-репозитория: ветки, `.gitignore`, `pom.xml`, ревью и мерж Pull Request'ов остальных участников
- Финальную сборку и проверку, что проект компилируется и запускается у всех

**Контракт интерфейса (эталон, не менять без согласования всей команды):**

```java
public interface CrudRepository<T, ID> {
    T save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    T update(T entity);
    void deleteById(ID id);
}

public interface SubscriberRepository extends CrudRepository<Subscriber, Long> { }

public interface ConnectionRequestRepository extends CrudRepository<ConnectionRequest, Long> {
    List<ConnectionRequest> findBySubscriberId(Long subscriberId);
}
```

**Сдаёт на защиту:** ER-диаграмму, `schema.sql`, умеет объяснять Statement vs PreparedStatement, связи FK.

---

### Участник B — Бизнес-логика и обработка ошибок

**Отвечает за:**
- `service/SubscriberService.java`, `service/ConnectionRequestService.java` — CRUD-операции с проверкой бизнес-правил (используют репозитории из [A])
- Все 6 бизнес-правил из п. 2.4
- `exception/EntityNotFoundException.java`, `BusinessException.java`, `ValidationException.java`
- Обработку некорректного ввода на уровне сервиса (проброс осмысленных исключений наверх в UI)

**Контракт сервиса (эталон):**

```java
public class ConnectionRequestService {
    ConnectionRequest create(ConnectionRequest request);
    ConnectionRequest getById(Long id);
    List<ConnectionRequest> getAll();
    ConnectionRequest update(Long id, ConnectionRequest updated);
    void changeStatus(Long id, RequestStatus newStatus); // проверяет таблицу переходов
    void delete(Long id);
}
```

**Сдаёт на защиту:** объяснение каждого бизнес-правила и какое исключение оно кидает.

---

### Участник C — Консольный интерфейс (UI)

**Отвечает за:**
- `Main.java` — точка входа, запуск `ConsoleApp`
- `ui/ConsoleApp.java` — главное меню (п. 1)
- `ui/SubscriberMenu.java`, `ui/RequestMenu.java`, `ui/StatisticsMenu.java` — подменю
- `util/InputReader.java` — безопасное чтение ввода (цикл повторного запроса при ошибке, парсинг чисел с `try/catch NumberFormatException`)
- Форматирование вывода таблиц в консоли (읽аемо, выровнено)

**Правило:** UI не содержит SQL и не знает о `Connection`/`ResultSet` — только вызывает методы `service/`.

**Сдаёт на защиту:** демонстрацию рабочего приложения, обработку некорректного ввода вживую.

---

### Участник D — Модель, поиск/фильтрация/сортировка, статистика, экспорт

**Отвечает за:**
- `model/` — `Subscriber.java`, `ConnectionRequest.java`, `RequestStatus.java`, `RequestType.java` (геттеры/сеттеры, конструкторы, `equals/hashCode/toString`)
- `service/ConnectionRequestQueryService.java` — поиск/фильтрация/сортировка через Stream API поверх `service/ConnectionRequestService.getAll()` (не трогает файлы [B])
- `service/StatisticsService.java` — подсчёт 5+ показателей
- `util/ExcelExporter.java` — экспорт в `.xlsx` (Apache POI)
- Подготовку тестовых данных (согласованно с A, в `seed-data.sql`): ≥5 абонентов, ≥10 заявок, ≥3 статуса

**Контракт (эталон):**

```java
public class ConnectionRequestQueryService {
    List<ConnectionRequest> searchBySubscriber(String query);
    List<ConnectionRequest> searchByDescription(String keyword);
    List<ConnectionRequest> filterByStatus(RequestStatus status);
    List<ConnectionRequest> filterByType(RequestType type);
    List<ConnectionRequest> sortByCreatedAt(boolean ascending);
    List<ConnectionRequest> sortByStatus();
}
```

**Сдаёт на защиту:** демонстрацию поиска/фильтрации/сортировки/статистики, экспортированный `.xlsx`.

---

## 5. Как это связывается (чтобы никто не запутался)

```
ui/ (C)  --->  service/ (B, D)  --->  repository/ (A)  --->  MySQL/PostgreSQL
                    |
                model/ (D)  <--- используется всеми слоями
```

- **C** зависит от **B** и **D** (вызывает их публичные методы), но никогда не обращается к `repository/` напрямую.
- **B** зависит от **A** (репозитории) и **D** (модели/исключения от B).
- **D** (QueryService) зависит от **B** (`ConnectionRequestService.getAll()`), не дублирует бизнес-логику.
- **A** ни от кого не зависит внутри проекта, кроме `model/` (D).

Если нужно изменить сигнатуру метода из раздела 4 — сначала пишете в общий чат/issue, потом меняете.

---

## 6. Git-workflow

- Ветки: `feature/a-database`, `feature/b-service`, `feature/c-ui`, `feature/d-model-export`
- `main` защищена — мерж только через Pull Request с ревью от A (тимлида)
- Коммиты: `[A] add DatabaseManager`, `[B] implement status transition rule`, и т.д.
- Каждый PR — маленький и по своей папке, чтобы избежать конфликтов
- Интеграционная сборка и финальный тест — за 2–3 дня до защиты, делает A совместно со всеми

---

## 7. Чеклист к сдаче

- [ ] Исходный код (весь пакет `ru.mirea.ispteam`)
- [ ] `pom.xml`
- [ ] `schema.sql` (создание таблиц с PK/FK/NOT NULL/UNIQUE)
- [ ] ER-диаграмма (`subscribers` 1—N `connection_requests`)
- [ ] `seed-data.sql` (≥5 абонентов, ≥10 заявок, ≥3 статуса)
- [ ] Экспортированный `.xlsx`
- [ ] Инструкция по запуску (README.md с шагами: создать БД → применить schema.sql → настроить подключение → `mvn clean package` → запуск)
- [ ] Рабочее консольное приложение, не падающее при некорректном вводе
- [ ] Каждый участник готов объяснить свою часть (см. "Сдаёт на защиту" в разделе 4)

---

## 8. Задел на будущее (КР2–КР4)

Предметная область и структура `model`/`service`/`repository` не меняются в следующих контрольных — они лишь получают новый слой сверху:
`КР1 Console -> КР2 JavaFX -> КР3 Spring + REST API -> КР4 Android`.
Поэтому уже сейчас важно, чтобы `service/` не зависел от консоли — это и есть многослойная архитектура, которая позволит просто подменить UI-слой в будущем.
