# ИС интернет-провайдера (КР1)

Учебный проект команды из 4 человек — консольная система учёта заявок абонентов интернет-провайдера.
Многослойная архитектура на Java + JDBC + PostgreSQL. Полное ТЗ и распределение ролей — в [`PKS.md`](PKS.md).

## Технологии
- Java 17
- Maven
- PostgreSQL (JDBC)
- Apache POI (экспорт в `.xlsx`)

## Архитектура (слои)
```
ui/ (C)  →  service/ (B, D)  →  repository/ (A)  →  PostgreSQL
                 ↓
             model/ (D)  ← используется всеми
```
Правило: `ui/` не содержит SQL, `service/` не зависит от консоли. Точка сборки зависимостей — `Main.java`.

## Кто за что отвечает
| Роль | Зона | Пакеты/файлы |
|---|---|---|
| **A** (тимлид) | БД, JDBC, репозитории, инфраструктура | `repository/`, `util/DatabaseManager`, `exception/DatabaseException`, `resources/*.sql`, `pom.xml` |
| **B** | Бизнес-логика, исключения | `service/SubscriberService`, `service/ConnectionRequestService`, `exception/{EntityNotFound,Business,Validation}Exception` |
| **C** | Консольный UI | `Main`, `ui/*`, `util/InputReader` |
| **D** | Модель, поиск/фильтр/сорт, статистика, экспорт | `model/*`, `service/ConnectionRequestQueryService`, `service/StatisticsService`, `util/ExcelExporter` |

Готовые (A) части реализованы. Остальные — компилируемые заглушки, кидающие `UnsupportedOperationException` с пометкой `TODO(<роль>)`.

## Как запустить

### 1. Создать базу данных
```bash
createdb isp
# или в psql:  CREATE DATABASE isp;
```

### 2. Настроить подключение
Скопируйте шаблон и впишите **свои** локальные значения (реальный `db.properties`
в git не коммитится — у каждого участника он свой):
```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```
```properties
db.url=jdbc:postgresql://localhost:5432/isp
db.user=postgres      # у Homebrew-установки обычно = имя пользователя ОС
db.password=postgres  # у Homebrew-установки обычно пустой
db.initOnStartup=true
```
При `db.initOnStartup=true` приложение само применит `schema.sql` при старте и наполнит БД
данными из `seed-data.sql` (только если таблица `subscribers` пустая).
Если предпочитаете применять SQL вручную:
```bash
psql -d isp -f src/main/resources/schema.sql
psql -d isp -f src/main/resources/seed-data.sql
```

### 3. Собрать и запустить
```bash
mvn clean package          # соберёт target/isp-team.jar со всеми зависимостями
java -jar target/isp-team.jar
# либо во время разработки:
mvn compile exec:java
```

## Git-workflow
- Ветки: `feature/database` (A), `feature/service` (B), `feature/console-ui` (C), `feature/d-model-export` (D)
- `main` — только через Pull Request с ревью от A
- Коммиты с префиксом роли: `[A] add DatabaseManager`, `[B] implement status transition rule`
- **Один файл — один владелец.** Сигнатуры методов из `PKS.md` разд. 4 менять только по согласованию команды (иначе части перестанут собираться вместе).

## Статус скелета
- [x] Структура пакетов `ru.mirea.ispteam`, `pom.xml`, `.gitignore`
- [x] `schema.sql`, `seed-data.sql` (5 абонентов, 12 заявок)
- [x] Репозитории + `DatabaseManager` + исключения (A — реализовано, B-исключения тоже готовы)
- [~] `model/` — только поля/геттеры-сеттеры (нужно для компиляции); `equals/hashCode/toString` — **D**
- [x] `Main.java` (сборка зависимостей) — **C**
- [x] Бизнес-логика сервисов — **B**
- [x] Консольный UI — **C**
- [x] Поиск/фильтр/статистика/экспорт — **D**
