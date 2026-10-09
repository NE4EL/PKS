package ru.mirea.ispteam.fx.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;
import ru.mirea.ispteam.fx.DataStore;
import ru.mirea.ispteam.fx.FormDialogs;
import ru.mirea.ispteam.fx.model.RequestRow;
import ru.mirea.ispteam.fx.util.Cells;
import ru.mirea.ispteam.fx.util.Dialogs;
import ru.mirea.ispteam.fx.util.ExportFiles;
import ru.mirea.ispteam.fx.util.Formats;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.ExportService;
import ru.mirea.ispteam.service.SubscriberFilter;
import ru.mirea.ispteam.service.SubscriberService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Вкладка «Абоненты» (subscribers-tab.fxml): список абонентов, поиск и фильтр,
 * CRUD абонента и связанные данные — заявки выбранного абонента.
 */
public class SubscribersController {

    private final DataStore store;
    private final SubscriberService subscriberService;
    private final ExportService exportService;
    private final FormDialogs forms;
    private FilteredList<Subscriber> filtered;
    private final ObservableList<RequestRow> subscriberRequests = FXCollections.observableArrayList();

    @FXML private TextField searchField;
    @FXML private CheckBox onlyActiveCheck;

    @FXML private TableView<Subscriber> table;
    @FXML private TableColumn<Subscriber, String> nameColumn;
    @FXML private TableColumn<Subscriber, String> phoneColumn;
    @FXML private TableColumn<Subscriber, String> emailColumn;
    @FXML private TableColumn<Subscriber, String> addressColumn;
    @FXML private TableColumn<Subscriber, LocalDate> registeredColumn;
    @FXML private TableColumn<Subscriber, Long> requestsColumn;
    @FXML private TableColumn<Subscriber, Long> activeColumn;

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button newRequestButton;
    @FXML private Label countLabel;

    @FXML private Label relatedTitle;
    @FXML private TableView<RequestRow> requestsTable;
    @FXML private TableColumn<RequestRow, Long> reqIdColumn;
    @FXML private TableColumn<RequestRow, RequestType> reqTypeColumn;
    @FXML private TableColumn<RequestRow, RequestStatus> reqStatusColumn;
    @FXML private TableColumn<RequestRow, String> reqTariffColumn;
    @FXML private TableColumn<RequestRow, LocalDateTime> reqCreatedColumn;
    @FXML private TableColumn<RequestRow, String> reqDescriptionColumn;

    public SubscribersController(DataStore store, SubscriberService subscriberService,
                                 ExportService exportService, FormDialogs forms) {
        this.store = store;
        this.subscriberService = subscriberService;
        this.exportService = exportService;
        this.forms = forms;
    }

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(c -> value(c.getValue().getFullName()));
        phoneColumn.setCellValueFactory(c -> value(c.getValue().getPhone()));
        emailColumn.setCellValueFactory(c -> value(c.getValue().getEmail()));
        addressColumn.setCellValueFactory(c -> value(c.getValue().getAddress()));
        registeredColumn.setCellValueFactory(c -> value(c.getValue().getRegistrationDate()));
        registeredColumn.setCellFactory(Cells.formatted(Formats::date));
        requestsColumn.setCellValueFactory(c -> value(store.requestCount(c.getValue())));
        activeColumn.setCellValueFactory(c -> value(store.activeRequestCount(c.getValue())));

        filtered = new FilteredList<>(store.subscribers());
        SortedList<Subscriber> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.setPlaceholder(new Label("Нет абонентов, удовлетворяющих условиям поиска"));
        table.getSortOrder().setAll(List.of(nameColumn));

        searchField.textProperty().addListener((obs, old, text) -> applyFilter());
        onlyActiveCheck.selectedProperty().addListener((obs, old, value) -> applyFilter());
        // Активные заявки меняются при работе на вкладке «Заявки» — пересчитываем фильтр.
        store.requests().addListener((ListChangeListener<RequestRow>) change -> {
            applyFilter();
            showRelated(table.getSelectionModel().getSelectedItem());
        });

        countLabel.textProperty().bind(Bindings.createStringBinding(
                () -> "Показано абонентов: " + filtered.size() + " из " + store.subscribers().size(),
                filtered, store.subscribers()));

        var selected = table.getSelectionModel().selectedItemProperty();
        editButton.disableProperty().bind(selected.isNull());
        deleteButton.disableProperty().bind(selected.isNull());
        newRequestButton.disableProperty().bind(selected.isNull());
        selected.addListener((obs, old, subscriber) -> showRelated(subscriber));

        table.setRowFactory(tv -> {
            TableRow<Subscriber> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });

        reqIdColumn.setCellValueFactory(c -> value(c.getValue().id()));
        reqTypeColumn.setCellValueFactory(c -> value(c.getValue().type()));
        reqTypeColumn.setCellFactory(Cells.formatted(RequestType::getTitle));
        reqStatusColumn.setCellValueFactory(c -> value(c.getValue().status()));
        reqStatusColumn.setCellFactory(Cells.statusBadge());
        reqTariffColumn.setCellValueFactory(c -> value(c.getValue().tariffPlan()));
        reqCreatedColumn.setCellValueFactory(c -> value(c.getValue().createdAt()));
        reqCreatedColumn.setCellFactory(Cells.formatted(Formats::dateTime));
        reqDescriptionColumn.setCellValueFactory(c -> value(c.getValue().description()));
        SortedList<RequestRow> sortedRequests = new SortedList<>(subscriberRequests);
        sortedRequests.comparatorProperty().bind(requestsTable.comparatorProperty());
        requestsTable.setItems(sortedRequests);
        requestsTable.setPlaceholder(new Label("Выберите абонента, чтобы увидеть его заявки"));
        showRelated(null);
    }

    private void applyFilter() {
        SubscriberFilter filter = new SubscriberFilter(searchField.getText(), onlyActiveCheck.isSelected());
        filtered.setPredicate(s -> filter.matches(s, store.activeRequestCount(s)));
    }

    /** Связанные данные: все заявки выбранного абонента. */
    private void showRelated(Subscriber subscriber) {
        if (subscriber == null) {
            relatedTitle.setText("Заявки абонента");
            subscriberRequests.clear();
            return;
        }
        List<RequestRow> rows = store.requestsOf(subscriber);
        relatedTitle.setText("Заявки абонента: " + subscriber.getFullName() + " — " + rows.size());
        subscriberRequests.setAll(rows);
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        onlyActiveCheck.setSelected(false);
    }

    @FXML
    private void onAdd() {
        try {
            forms.subscriberForm(window(), null).ifPresent(saved -> {
                reloadAndSelect(saved.getId());
                store.setMessage("Абонент «" + saved.getFullName() + "» добавлен");
            });
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void onEdit() {
        Subscriber subscriber = table.getSelectionModel().getSelectedItem();
        if (subscriber == null) return;
        try {
            forms.subscriberForm(window(), subscriber).ifPresent(saved -> {
                reloadAndSelect(saved.getId());
                store.setMessage("Данные абонента «" + saved.getFullName() + "» изменены");
            });
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void onDelete() {
        Subscriber subscriber = table.getSelectionModel().getSelectedItem();
        if (subscriber == null) return;
        long total = store.requestCount(subscriber);
        String details = total == 0 ? "" : "\nВместе с ним будут удалены его закрытые заявки: " + total + ".";
        boolean confirmed = Dialogs.confirm(window(), "Удаление абонента",
                "Удалить абонента «" + subscriber.getFullName() + "» (" + subscriber.getPhone() + ")?"
                        + details + "\nЭто действие нельзя отменить.", "Удалить");
        if (!confirmed) return;
        try {
            subscriberService.delete(subscriber.getId());
            store.reload();
            store.setMessage("Абонент «" + subscriber.getFullName() + "» удалён");
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void onNewRequest() {
        Subscriber subscriber = table.getSelectionModel().getSelectedItem();
        if (subscriber == null) return;
        try {
            forms.requestForm(window(), null, subscriber).ifPresent(saved -> {
                reloadAndSelect(subscriber.getId());
                store.setMessage("Заявка №" + saved.getId() + " для абонента «" + subscriber.getFullName() + "» создана");
            });
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    @FXML
    private void onExport() {
        if (table.getItems().isEmpty()) {
            Dialogs.showInfo(window(), "Нечего экспортировать", "В таблице нет абонентов. Измените условия поиска.");
            return;
        }
        ExportFiles.chooseTarget(window(), "Экспорт абонентов", "abonenty").ifPresent(path -> {
            try {
                List<Subscriber> rows = List.copyOf(table.getItems());
                exportService.exportSubscribers(rows, path);
                store.setMessage("Экспортировано абонентов: " + rows.size() + " → " + path);
                Dialogs.showInfo(window(), "Экспорт завершён",
                        "Сохранено абонентов: " + rows.size() + "\nФайл: " + path);
            } catch (RuntimeException e) {
                Dialogs.showError(window(), e);
            }
        });
    }

    private void reloadAndSelect(Long id) {
        store.reload();
        table.getItems().stream()
                .filter(s -> Objects.equals(s.getId(), id))
                .findFirst()
                .ifPresent(s -> {
                    table.getSelectionModel().select(s);
                    table.scrollTo(s);
                });
    }

    private Window window() {
        return table.getScene().getWindow();
    }

    private static <T> ReadOnlyObjectWrapper<T> value(T value) {
        return new ReadOnlyObjectWrapper<>(value);
    }
}
