# КР2 — подготовка к защите

На защите вопрос могут задать **любому** участнику по **любой** части. Ниже — короткие ответы со ссылками
на код, сценарий демонстрации и зоны ответственности.

## 1. Кто что объясняет (зоны как в КР1)

| Роль | Зона КР2 | Файлы |
|---|---|---|
| **A** | Docker, БД, JDBC, сборка | `docker-compose.yml`, `.env.example`, `util/DatabaseManager`, `database.properties.example`, `AppContext`, `pom.xml`, `mvnw` |
| **B** | Бизнес-правила, валидация, ошибки | `service/SubscriberService`, `service/ConnectionRequestService`, `exception/*`, `fx/controller/FormController`, `fx/util/Dialogs` |
| **C** | JavaFX-интерфейс | `fx/IspFxApplication`, `fx/ViewLoader`, `fx/DataStore`, `fx/controller/{Main,Requests,Subscribers,RequestForm,SubscriberForm}Controller`, `*.fxml`, `app.css` |
| **D** | Поиск/фильтр/сортировка, статистика, экспорт | `service/RequestFilter`, `service/SubscriberFilter`, `service/StatisticsService` + `StatisticsReport`, `fx/controller/StatisticsController`, `service/ExportService`, `util/CsvExporter`, `util/ExcelExporter`, `fx/util/ExportFiles` |

## 2. Сценарий демонстрации (≈7 минут)

1. **Docker.** `docker compose ps` → `isp-db` healthy. (Плюс демо-система из архива: `http://localhost:8080/actuator/health` → `UP`.)
2. **Запуск.** `.\mvnw.cmd javafx:run` → окно, внизу «Данные загружены: абонентов — 5, заявок — 12».
3. **Таблица и связанные данные.** Выбрать заявку → справа карточка с данными абонента.
   Вкладка «Абоненты» → выбрать абонента → внизу его заявки.
4. **Поиск.** Ввести `иванов` (ФИО), `+7916` (телефон), `роутер` (описание).
5. **Фильтры.** Статус «Новая», тип «Техподдержка», период «с 01.07.2024 по 31.07.2024».
   Поставить «с» позже «по» → красная подсказка, период не применяется.
6. **Сортировка.** Список «Сортировка» → «По статусу», «По абоненту»; щелчок по заголовку «Создана».
7. **CRUD.** «+ Новая заявка» → «Сохранить» с пустыми полями → подсвечено «Абонент»;
   описание `abc` → ошибка «от 5 до 1000 символов»; заполнить правильно → заявка появилась и выделена.
   «Изменить» (или двойной щелчок) → форма с текущими значениями. «Сменить статус» → только допустимые.
   «Удалить» → подтверждение.
8. **Валидация абонента.** Телефон `8900` → «формат +7XXXXXXXXXX»; телефон существующего абонента →
   «уже существует»; дата регистрации в будущем → ошибка. Удалить абонента с активными заявками → «нельзя».
9. **Статистика.** Кнопка «Статистика» → 11 плиток и 2 диаграммы, выбрать период.
10. **Экспорт.** «Экспорт в файл…» → выбрать место и тип (.xlsx / .csv) → открыть файл в Excel.
    Экспортируется ровно то, что видно в таблице (с учётом фильтров и сортировки).

## 3. Ответы на вопросы методички

**Как запускается готовая система через Docker и как проверяется PostgreSQL и backend?**
Из корня архива: `powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1`
(или `docker compose --env-file .env -f backend/docker-compose.yml up -d --wait`). Docker поднимает два
контейнера: PostgreSQL (healthcheck `pg_isready`) и Spring Boot backend. Проверка: `docker compose ps`
(статус healthy/running), `http://localhost:8080/` открывается, `http://localhost:8080/actuator/health` → `UP`.
Наша БД КР1 запускается так же — `docker compose up -d --wait` по `docker-compose.yml` в корне,
порт 55433; `schema.sql` и `seed-data.sql` выполняются автоматически при первом запуске пустого volume.

**Как запускается JavaFX-приложение?**
`mvnw javafx:run` → плагин `javafx-maven-plugin` запускает `IspFxApplication`. JavaFX вызывает `start(Stage)`:
`AppContext.create()` читает `database.properties`, проверяет подключение к БД, применяет `schema.sql`;
затем `ViewLoader` загружает `main-view.fxml`, создаётся `Scene` с CSS, окно показывается, данные загружаются.
Если БД недоступна — понятное окно ошибки и выход. Для jar главный класс `Launcher` (не наследует
`Application`, иначе запуск из classpath не работает).

**Stage, Scene, Node?**
`Stage` — окно (главное, модальные формы, окно статистики). `Scene` — содержимое окна, к ней подключается CSS.
`Node` — любой элемент дерева сцены: `Button`, `TableView`, `BorderPane`… Узлы образуют дерево (scene graph).

**Зачем FXML?**
Разметка окна отдельно от логики: структуру интерфейса видно и можно менять без Java-кода,
контроллер содержит только поведение. 6 FXML-файлов: главное окно, 2 вкладки, 2 формы, статистика.

**Как FXML связан с Controller?**
`fx:controller="...RequestsController"` — какой класс управляет разметкой. `fx:id="table"` → поле
`@FXML private TableView<RequestRow> table` (FXMLLoader внедряет его по имени). `onAction="#onAdd"` →
метод `@FXML private void onAdd()`. После внедрения FXMLLoader вызывает `initialize()`.
Контроллеры создаёт наша фабрика `ViewLoader.createController` — так в конструктор передаются сервисы.
Вкладки подключены через `<fx:include fx:id="requests">`.

**Как обновляются данные в TableView?**
`DataStore.reload()` читает данные через сервисы и делает `ObservableList.setAll(...)`. Цепочка
`ObservableList → FilteredList → SortedList → TableView` пересчитывается автоматически: таблица
слушает изменения списка. После добавления/изменения/удаления вызываем `reload()` и выделяем нужную строку.

**Зачем ObservableList?**
Это список, который оповещает слушателей об изменениях. `TableView`, `FilteredList`, `SortedList`,
счётчик «Показано X из Y» (через `Bindings`) подписаны на него — интерфейс обновляется без ручной перерисовки.

**Где бизнес-логика?**
В `service/`: `SubscriberService` (формат ФИО/телефона/email, уникальность, дата регистрации не в будущем,
запрет удаления с активными заявками), `ConnectionRequestService` (абонент должен существовать, описание
5–1000 символов, таблица переходов статусов `allowedTransitions`, не более одной активной заявки на подключение).
Контроллеры правила не дублируют — они вызывают сервис и показывают результат.

**Где работа с данными?**
`repository/*RepositoryImpl` — SQL через `PreparedStatement` (защита от SQL-инъекций), соединение из
`DatabaseManager.getConnection()` в try-with-resources. Параметры подключения — в `database.properties`
(в git не попадает). SQL в контроллерах нет.

**Как обрабатываются исключения?**
JDBC `SQLException` оборачивается в `DatabaseException`; сервисы бросают `ValidationException`,
`BusinessException`, `EntityNotFoundException`. Контроллер ловит `RuntimeException` и вызывает
`Dialogs.showError`, где каждому типу соответствует понятный заголовок и текст. Технические подробности
(stack trace) — только в журнал slf4j. Пустых `catch` нет. Для непойманных ошибок есть
`UncaughtExceptionHandler` в `IspFxApplication`.

**Как проверяются пользовательские данные?**
До сохранения — в сервисе. `ValidationException` содержит ключ поля (`"phone"`, `"description"`…).
`FormController.showError` находит по ключу элемент формы, добавляет CSS-класс `field-error` (красная рамка),
ставит фокус и показывает текст под формой. Кроме того: связанный абонент выбирается из `ComboBox`
(не вводится ID), статус — только из допустимых переходов, счётчик символов описания, проверка формата даты.

**Как реализованы поиск и фильтрация?**
`RequestFilter` (record) хранит условия: строка поиска, статус, тип, период. Метод `matches(request, subscriber)`
проверяет все условия (И). Контроллер на каждое изменение поля создаёт новый `RequestFilter` и передаёт
`filtered.setPredicate(row -> filter.matches(...))`. Тот же `RequestFilter` проверяется в unit-тестах (`RequestFilterTest`) без JavaFX.
Сортировка: `SortedList.comparatorProperty().bind(table.comparatorProperty())`; список «Сортировка» задаёт
`table.getSortOrder()` и направление столбца; ячейки хранят исходные значения (дата, enum), поэтому сравнение
корректное (даты по времени, статусы по порядку жизненного цикла).

**Как рассчитывается статистика?**
`StatisticsService.buildReport(from, to)` один раз читает абонентов и заявки и считает в памяти (Stream API):
всего абонентов/заявок, активные, выполненные, отклонённые+отменённые, долю выполненных (%), среднее число
заявок на абонента, среднее/мин/макс время выполнения (`Duration` между созданием и последним изменением
у выполненных), самый частый тариф (`groupingBy` + `counting` + `max`), распределения по статусам и типам
(`EnumMap`), число заявок за период. Контроллер только выводит готовый `StatisticsReport`.

**Как выполняется экспорт?**
`ExportFiles.chooseTarget` открывает `FileChooser` (путь выбирает пользователь, по умолчанию домашняя папка).
`ExportService` превращает текущие строки таблицы в `TableData` (ФИО вместо ID, русские названия статусов,
даты `дд.ММ.гггг ЧЧ:мм`) и по расширению выбирает `CsvExporter` (UTF-8 с BOM, разделитель `;`,
экранирование кавычек) или `ExcelExporter` (Apache POI, жирная шапка, закреплённая первая строка).
Ошибка записи (файл открыт в Excel) → `ExportException` → понятное сообщение.
