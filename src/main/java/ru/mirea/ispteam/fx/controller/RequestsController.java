package ru.mirea.ispteam.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.fx.DataStore;
import ru.mirea.ispteam.fx.FormDialogs;
import ru.mirea.ispteam.fx.model.RequestRow;
import ru.mirea.ispteam.fx.util.Cells;
import ru.mirea.ispteam.fx.util.Dialogs;
import ru.mirea.ispteam.fx.util.ExportFiles;
import ru.mirea.ispteam.fx.util.Formats;
import ru.mirea.ispteam.fx.util.Option;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.ExportService;
import ru.mirea.ispteam.service.RequestFilter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Вкладка «Заявки» (requests-tab.fxml): таблица заявок, поиск, фильтры, сортировка,
 * карточка выбранной заявки с данными абонента, CRUD и экспорт.
 *
 * Цепочка данных для таблицы:
 *   store.requests() (ObservableList, все заявки из БД)
 *     -> FilteredList (поиск и фильтры: предикат из RequestFilter)
 *     -> SortedList   (сортировка: компаратор берётся у TableView)
 *     -> TableView
 * Любое изменение исходного списка или условий автоматически отражается в таблице.
 */
public class RequestsController {

    /** Готовые варианты сортировки: какой столбец и в каком направлении. */
    private enum SortMode {
        NEWEST("Сначала новые"),
        OLDEST("Сначала старые"),
        STATUS("По статусу"),
        SUBSCRIBER("По абоненту (А–Я)"),
        TYPE("По типу заявки");

        private final String title;

        SortMode(String title) {
            this.title = title;
        }
    }

    private final DataStore store;
    private final ConnectionRequestService requestService;
    private final ExportService exportService;
    private final FormDialogs forms;
    private FilteredList<RequestRow> filtered;

    @FXML private TextField searchField;
    @FXML private ComboBox<Option<RequestStatus>> statusFilter;
    @FXML private ComboBox<Option<RequestType>> typeFilter;
    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private ComboBox<SortMode> sortBox;
    @FXML private Label filterError;

    @FXML private TableView<RequestRow> table;
    @FXML private TableColumn<RequestRow, Long> idColumn;
    @FXML private TableColumn<RequestRow, String> subscriberColumn;
    @FXML private TableColumn<RequestRow, String> phoneColumn;
    @FXML private TableColumn<RequestRow, RequestType> typeColumn;
    @FXML private TableColumn<RequestRow, RequestStatus> statusColumn;
    @FXML private TableColumn<RequestRow, String> tariffColumn;
    @FXML private TableColumn<RequestRow, String> descriptionColumn;
    @FXML private TableColumn<RequestRow, LocalDateTime> createdColumn;

    @FXML private Button editButton;
    @FXML private Button statusButton;
    @FXML private Button deleteButton;
    @FXML private Label countLabel;

    @FXML private Label detailsPlaceholder;
    @FXML private VBox detailsContent;
    @FXML private Label detailNumber;
    @FXML private Label detailStatus;
    @FXML private Label detailType;
    @FXML private Label detailTariff;
    @FXML private Label detailCreated;
    @FXML private Label detailUpdated;
    @FXML private Label detailDescription;
    @FXML private Label detailSubscriber;
    @FXML private Label detailPhone;
    @FXML private Label detailEmail;
    @FXML private Label detailAddress;
    @FXML private Label detailSubscriberRequests;
    @FXML private Label detailTransitions;

    public RequestsController(DataStore store, ConnectionRequestService requestService,
                              ExportService exportService, FormDialogs forms) {
        this.store = store;
        this.requestService = requestService;
        this.exportService = exportService;
        this.forms = forms;
    }

    @FXML
    private void initialize() {
        setUpColumns();
        setUpFilters();

        filtered = new FilteredList<>(store.requests());
        SortedList<RequestRow> sorted = new SortedList<>(filtered);
        // Клик по заголовку столбца меняет comparator таблицы, SortedList пересортировывает данные.
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPlaceholder(new Label("Нет заявок, удовлетворяющих условиям поиска"));

        countLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Показано заявок: " + filtered.size() + " из " + store.requests().size(),
                filtered, store.requests()));

        var selected = table.getSelectionModel().selectedItemProperty();
        editButton.disableProperty().bind(selected.isNull());
        deleteButton.disableProperty().bind(selected.isNull());
        statusButton.disableProperty().bind(selected.isNull());
        selected.addListener((obs, old, row) -> showDetails(row));
        showDetails(null);

        // Двойной щелчок по строке — редактирование.
        table.setRowFactory(tv -> {
            TableRow<RequestRow> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        sortBox.getSelectionModel().select(SortMode.NEWEST);
    }

    private void setUpColumns() {
        idColumn.setCellValueFactory(c -> value(c.getValue().id()));
        subscriberColumn.setCellValueFactory(c -> value(c.getValue().subscriberName()));
        phoneColumn.setCellValueFactory(c -> value(c.getValue().subscriberPhone()));
        typeColumn.setCellValueFactory(c -> value(c.getValue().type()));
        typeColumn.setCellFactory(Cells.formatted(RequestType::getTitle));
        statusColumn.setCellValueFactory(c -> value(c.getValue().status()));
        statusColumn.setCellFactory(Cells.statusBadge());
        tariffColumn.setCellValueFactory(c -> value(c.getValue().tariffPlan()));
        descriptionColumn.setCellValueFactory(c -> value(c.getValue().description()));
        createdColumn.setCellValueFactory(c -> value(c.getValue().createdAt()));
        createdColumn.setCellFactory(Cells.formatted(Formats::dateTime));
    }

    private void setUpFilters() {
        statusFilter.getItems().setAll(options("Все статусы", RequestStatus.values(), RequestStatus::getTitle));
        statusFilter.getSelectionModel().selectFirst();
        typeFilter.getItems().setAll(options("Все типы", RequestType.values(), RequestType::getTitle));
        typeFilter.getSelectionModel().selectFirst();
        fromDate.setConverter(Formats.dateConverter());
        toDate.setConverter(Formats.dateConverter());
        sortBox.getItems().setAll(SortMode.values());
        sortBox.setConverter(Formats.converter(mode -> mode.title));

        searchField.textProperty().addListener((obs, old, text) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, old, value) -> applyFilter());
        typeFilter.valueProperty().addListener((obs, old, value) -> applyFilter());
        fromDate.valueProperty().addListener((obs, old, value) -> applyFilter());
        toDate.valueProperty().addListener((obs, old, value) -> applyFilter());
        sortBox.valueProperty().addListener((obs, old, mode) -> applySort(mode));
    }

    /** Собирает RequestFilter из элементов управления и передаёт его предикат в FilteredList. */
    private void applyFilter() {
        if (filtered == null) return;
        String query = searchField.getText();
        RequestStatus status = selected(statusFilter);
        RequestType type = selected(typeFilter);
        RequestFilter filter = new RequestFilter(query, status, type, fromDate.getValue(), toDate.getValue());
        try {
            filter.validate();
            setFilterError(null);
        } catch (ValidationException e) {
            // Некорректный период не применяем, остальные условия продолжают работать.
            setFilterError(e.getMessage());
            filter = new RequestFilter(query, status, type, null, null);
        }
        RequestFilter active = filter;
        filtered.setPredicate(row -> active.matches(row.request(), row.subscriber()));
    }

    private void applySort(SortMode mode) {
        if (mode == null) return;
        TableColumn<RequestRow, ?> column = switch (mode) {
            case NEWEST, OLDEST -> createdColumn;
            case STATUS -> statusColumn;
            case SUBSCRIBER -> subscriberColumn;
            case TYPE -> typeColumn;
        };
        column.setSortType(mode == SortMode.NEWEST ? TableColumn.SortType.DESCENDING : TableColumn.SortType.ASCENDING);
        table.getSortOrder().setAll(List.of(column));
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        typeFilter.getSelectionModel().selectFirst();
        fromDate.setValue(null);
        toDate.setValue(null);
        fromDate.getEditor().clear();
        toDate.getEditor().clear();
        sortBox.getSelectionModel().select(SortMode.NEWEST);
    }

    @FXML
    private void onAdd() {
        try {
            forms.requestForm(window(), null, null).ifPresent(saved -> {
                reloadAndSelect(saved.getId());
                store.setMessage("Заявка №" + saved.getId() + " создана");
            });
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void onEdit() {
        RequestRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        try {
            forms.requestForm(window(), row.request(), null).ifPresent(saved -> {
                reloadAndSelect(saved.getId());
                store.setMessage("Заявка №" + saved.getId() + " изменена");
            });
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void onChangeStatus() {
        RequestRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        Set<RequestStatus> allowed = ConnectionRequestService.allowedTransitions(row.status());
        if (allowed.isEmpty()) {
            Dialogs.showInfo(window(), "Статус изменить нельзя",
                    "Статус «" + row.status().getTitle() + "» — конечный: заявка уже закрыта.");
            return;
        }
        List<Option<RequestStatus>> choices = allowed.stream()
                .map(s -> new Option<>(s.getTitle(), s)).toList();
        Dialogs.choose(window(), "Смена статуса", "Заявка №" + row.id() + ": сейчас «"
                + row.status().getTitle() + "». Новый статус:", choices).ifPresent(newStatus -> {
            try {
                requestService.changeStatus(row.id(), newStatus);
                reloadAndSelect(row.id());
                store.setMessage("Заявка №" + row.id() + ": статус изменён на «" + newStatus.getTitle() + "»");
            } catch (RuntimeException e) {
                Dialogs.showError(window(), e);
            }
        });
    }

    @FXML
    private void onDelete() {
        RequestRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;
        boolean confirmed = Dialogs.confirm(window(), "Удаление заявки",
                "Удалить заявку №" + row.id() + " («" + row.type().getTitle() + "», абонент "
                        + row.subscriberName() + ")?\nЭто действие нельзя отменить.", "Удалить");
        if (!confirmed) return;
        try {
            requestService.delete(row.id());
            store.reload();
            store.setMessage("Заявка №" + row.id() + " удалена");
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    /** Экспорт ровно того, что сейчас видно в таблице: с учётом поиска, фильтров и сортировки. */
    @FXML
    private void onExport() {
        if (table.getItems().isEmpty()) {
            Dialogs.showInfo(window(), "Нечего экспортировать", "В таблице нет заявок. Измените условия поиска.");
            return;
        }
        ExportFiles.chooseTarget(window(), "Экспорт заявок", "zayavki").ifPresent(path -> {
            try {
                List<ConnectionRequest> rows = table.getItems().stream().map(RequestRow::request).toList();
                exportService.exportRequests(rows, path);
                store.setMessage("Экспортировано заявок: " + rows.size() + " → " + path);
                Dialogs.showInfo(window(), "Экспорт завершён",
                        "Сохранено заявок: " + rows.size() + "\nФайл: " + path);
            } catch (RuntimeException e) {
                Dialogs.showError(window(), e);
            }
        });
    }

    private void showDetails(RequestRow row) {
        boolean hasRow = row != null;
        detailsPlaceholder.setVisible(!hasRow);
        detailsPlaceholder.setManaged(!hasRow);
        detailsContent.setVisible(hasRow);
        detailsContent.setManaged(hasRow);
        if (!hasRow) return;

        detailNumber.setText("Заявка №" + row.id());
        detailStatus.setText(row.status().getTitle());
        detailStatus.getStyleClass().setAll("label", "badge", Cells.statusStyle(row.status()));
        detailType.setText(row.type().getTitle());
        detailTariff.setText(Formats.text(row.tariffPlan()));
        detailCreated.setText(Formats.dateTime(row.createdAt()));
        detailUpdated.setText(Formats.dateTime(row.updatedAt()));
        detailDescription.setText(row.description());
        Set<RequestStatus> next = ConnectionRequestService.allowedTransitions(row.status());
        detailTransitions.setText(next.isEmpty() ? "нет (статус конечный)"
                : String.join(", ", next.stream().map(RequestStatus::getTitle).toList()));

        Subscriber s = row.subscriber();
        detailSubscriber.setText(row.subscriberName());
        detailPhone.setText(s != null ? s.getPhone() : "—");
        detailEmail.setText(s != null ? Formats.text(s.getEmail()) : "—");
        detailAddress.setText(s != null ? s.getAddress() : "—");
        detailSubscriberRequests.setText(s != null
                ? store.requestCount(s) + " (активных: " + store.activeRequestCount(s) + ")" : "—");
    }

    private void reloadAndSelect(Long id) {
        store.reload();
        table.getItems().stream()
                .filter(row -> Objects.equals(row.id(), id))
                .findFirst()
                .ifPresentOrElse(row -> {
                    table.getSelectionModel().select(row);
                    table.scrollTo(row);
                }, () -> store.setMessage("Заявка №" + id + " сохранена, но скрыта текущими фильтрами"));
    }

    private void setFilterError(String message) {
        filterError.setText(message == null ? "" : message);
        filterError.setVisible(message != null);
        filterError.setManaged(message != null);
        for (DatePicker picker : List.of(fromDate, toDate)) {
            picker.getStyleClass().remove("field-error");
            if (message != null) picker.getStyleClass().add("field-error");
        }
    }

    private Window window() {
        return table.getScene().getWindow();
    }

    private static <T> ReadOnlyObjectWrapper<T> value(T value) {
        return new ReadOnlyObjectWrapper<>(value);
    }

    private static <T> T selected(ComboBox<Option<T>> box) {
        Option<T> option = box.getValue();
        return option == null ? null : option.value();
    }

    private static <T> List<Option<T>> options(String allLabel, T[] values, Function<T, String> label) {
        List<Option<T>> options = new ArrayList<>();
        options.add(new Option<>(allLabel, null));
        for (T value : values) {
            options.add(new Option<>(label.apply(value), value));
        }
        return options;
    }
}
