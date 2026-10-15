# Как запустить проект КР2 (ИС интернет-провайдера, JavaFX)

Инструкция по шагам: что установить, как поднять базы в Docker, как запустить приложение и как проверить,
что всё работает. Подходит для Windows и macOS.

Всего запускаются две независимые системы:

| Что | Зачем | Где | Порт PostgreSQL |
|---|---|---|---|
| **Наш проект** — JavaFX-клиент провайдера + БД КР1 | это и есть сдаваемая КР2 | репозиторий `PKS` | `55433` |
| **Демо-система Student Hub** из архива преподавателя | методичка требует развернуть её через Docker и показать | папка `student-hub-suite-rebuilt` (в git не хранится) | `5433` |

Порты разные, поэтому обе системы можно держать запущенными одновременно.

---

## 1. Что установить

| Программа | Windows | macOS |
|---|---|---|
| **Docker Desktop** | <https://www.docker.com/products/docker-desktop/> | то же (выбрать Apple Silicon или Intel) |
| **JDK 21** | Temurin 21: <https://adoptium.net/> — при установке отметить «Set JAVA_HOME» | `brew install --cask temurin@21` |
| **Maven** | не нужен для нашего проекта (есть `mvnw.cmd`); нужен только для примера из архива | `brew install maven` (только для примера из архива) |
| Git | <https://git-scm.com/> | уже есть (`xcode-select --install`) |

Проверка:

```bash
java -version          # должна быть версия 21
docker --version
docker compose version
```

На macOS в каждом новом терминале (или один раз в `~/.zshrc`):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

**Перед любыми командами `docker ...` запустите Docker Desktop** и дождитесь статуса *Running*.

---

## 2. Получить проект

```bash
git clone -b pr2 https://github.com/NE4EL/PKS.git
cd PKS
```

Архив преподавателя (`student-hub-suite-rebuilt`) в репозитории **нет** — скачайте его со страницы КР2
и распакуйте **рядом** с `PKS`, а не внутрь (чтобы случайно не закоммитить чужой код и его пароли):

```
папка/
├── PKS/                         ← наш репозиторий
└── student-hub-suite-rebuilt/   ← архив преподавателя
```

---

## 3. Демо-система из архива (Docker)

По методичке её нужно хотя бы раз развернуть и уметь показать. Для нашего приложения она не нужна.

**Windows (PowerShell):**

```powershell
cd student-hub-suite-rebuilt
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
```

**macOS / Linux** (скрипт `.ps1` там не работает — вызываем Docker Compose напрямую):

```bash
cd student-hub-suite-rebuilt/backend
docker compose up -d --wait
```

Файл с паролями — `backend/.env` (уже лежит в архиве). Первый запуск может занять несколько
минут: Docker скачивает образы и собирает backend.

**Проверка:**

1. `docker compose ps` — контейнеры в состоянии `running` / `healthy`.
2. Открывается <http://localhost:8080/> — веб-интерфейс системы.
3. <http://localhost:8080/actuator/health> показывает `{"status":"UP"}`.
4. Вход: логин и пароль администратора — в `backend/.env` (`BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`).
   При первом входе система может попросить сменить пароль.

**Пример JavaFX-клиента из архива** (необязательно, только посмотреть):

```bash
cd student-hub-suite-rebuilt/javafx-client
./run.sh            # macOS;  Windows: .\run.ps1   (нужен установленный Maven)
```

> В PDF-методичке указан порт PostgreSQL `55432`, но в этой версии архива он `5433`
> (см. `POSTGRES_PORT` в `backend/.env`).

---

## 4. Наш проект: база данных КР1 в Docker

Все команды — из корня `PKS`.

**4.1. Пароль БД.** Скопируйте шаблон и впишите **свой** пароль в `ISP_DB_PASSWORD`:

```bash
cp .env.example .env               # Windows: copy .env.example .env
```

**4.2. Запуск PostgreSQL:**

```bash
docker compose up -d --wait
docker compose ps                  # isp-db должен быть healthy
```

При первом запуске контейнер сам выполнит `schema.sql` (таблицы) и `seed-data.sql`
(5 абонентов, 12 заявок).

**4.3. Настройки JDBC-подключения приложения:**

```bash
cp src/main/resources/database.properties.example src/main/resources/database.properties
# Windows: copy src\main\resources\database.properties.example src\main\resources\database.properties
```

Откройте `src/main/resources/database.properties` и проверьте:

```properties
db.url=jdbc:postgresql://localhost:55433/isp
db.user=isp_app
db.password=ТОТ_ЖЕ_ПАРОЛЬ_ЧТО_В_.env
db.initOnStartup=true
```

Файлы `.env` и `database.properties` в git **не коммитятся** — у каждого участника свои.

---

## 5. Наш проект: запуск JavaFX-приложения

**Windows:**

```powershell
.\mvnw.cmd javafx:run
# или: powershell -ExecutionPolicy Bypass -File .\run.ps1
```

**macOS / Linux:**

```bash
chmod +x mvnw run.sh               # один раз после clone
./mvnw javafx:run                  # или ./run.sh
```

Первый запуск скачивает зависимости (1–3 минуты). Успешный запуск — открылось окно, внизу строка
**«Данные загружены: абонентов — 5, заявок — 12»**.

**IntelliJ IDEA:** File → Open → папка `PKS` (как Maven-проект) → Project SDK = 21 →
запустить класс `ru.mirea.ispteam.fx.Launcher` или Maven → Plugins → javafx → `javafx:run`.

---

## 6. Полезные команды

| Команда (Windows: `.\mvnw.cmd`, macOS: `./mvnw`) | Что делает |
|---|---|
| `mvnw test` | 16 unit-тестов, база не нужна — быстрая проверка, что проект собирается |
| `mvnw clean compile` | только компиляция |
| `mvnw javafx:run` | запуск приложения |
| `mvnw clean package`, затем `java -jar target/isp-team.jar` | сборка jar и запуск из него |

---

## 7. Остановка

```bash
# в корне PKS — наша БД
docker compose down

# в student-hub-suite-rebuilt/backend — демо-система
docker compose down
```

**Не добавляйте `-v`** — этот флаг удаляет volume, и все данные БД пропадут.

---

## 8. Если что-то не работает

| Симптом | Причина и решение |
|---|---|
| `docker: command not found` / `Cannot connect to the Docker daemon` | Docker Desktop не установлен или не запущен — запустите и дождитесь *Running*. |
| `Задайте ISP_DB_PASSWORD в файле .env` | Не создан `.env` в корне `PKS` — шаг 4.1. |
| `Нет файла ... database.properties` | Не выполнен шаг 4.3. |
| Окно открылось, но ошибка подключения к БД | Контейнер `isp-db` не запущен (`docker compose ps`) или пароль в `database.properties` не совпадает с `.env`. |
| `password authentication failed`, хотя пароли совпадают | Volume создан раньше с другим паролем. Верните старый пароль **или** (данные пропадут!) `docker compose down -v` и `docker compose up -d --wait`. |
| `port is already allocated` | Порт занят. Поменяйте `ISP_DB_PORT` в `.env` и порт в `db.url`. |
| `JAVA_HOME is not set` / `release version 21 not supported` | Не установлен JDK 21 или `JAVA_HOME` указывает на другую версию (см. раздел 1). |
| macOS: `permission denied: ./mvnw` | `chmod +x mvnw run.sh` |
| Демо-система: страница 8080 не открывается | Подождите — backend ещё собирается; смотрите `docker compose logs -f backend` в `student-hub-suite-rebuilt/backend`. |

---

## 9. Сценарий показа на защите

Пошаговый сценарий демонстрации (≈7 минут) и ответы на вопросы методички — в [`KR2.md`](KR2.md).
Команды там написаны для Windows; на macOS вместо `.\mvnw.cmd` используйте `./mvnw`.
