package ru.mirea.ispteam.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.mirea.ispteam.fx.DataStore;
import ru.mirea.ispteam.fx.util.Formats;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.ConnectionRequestService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Форма заявки (request-form.fxml): добавление и редактирование.
 *
 * Связанная сущность «абонент» выбирается из выпадающего списка (ComboBox),
 * а не вводится вручную как числовой ID. Статус можно сменить только на допустимый
 * по таблице переходов.
 */
public class RequestFormController extends FormController<ConnectionRequest> {

    private static final int DESCRIPTION_MAX = 1000;

    private final DataStore store;
    private final ConnectionRequestService requestService;
    private ConnectionRequest existing;

    @FXML private ComboBox<Subscriber> subscriberBox;
    @FXML private ComboBox<RequestType> typeBox;
    @FXML private ComboBox<RequestStatus> statusBox;
    @FXML private TextField tariffField;
    @FXML private Label tariffHint;
    @FXML private TextArea descriptionArea;
    @FXML private Label descriptionCounter;
    @FXML private Label statusHint;

    public RequestFormController(DataStore store, ConnectionRequestService requestService) {
        this.store = store;
        this.requestService = requestService;
    }

    @FXML
    private void initialize() {
        registerField("subscriber", subscriberBox);
        registerField("type", typeBox);
        registerField("status", statusBox);
        registerField("tariffPlan", tariffField);
        registerField("description", descriptionArea);

        List<Subscriber> subscribers = new ArrayList<>(store.subscribers());
        subscribers.sort(Comparator.comparing(Subscriber::getFullName));
        subscriberBox.getItems().setAll(subscribers);
        subscriberBox.setConverter(Formats.converter(s -> s.getFullName() + "  ·  " + s.getPhone()));

        typeBox.getItems().setAll(RequestType.values());
        typeBox.setConverter(Formats.converter(RequestType::getTitle));
        typeBox.valueProperty().addListener((obs, old, type) -> updateTariffState(type));
        updateTariffState(null);

        statusBox.setConverter(Formats.converter(RequestStatus::getTitle));

        descriptionArea.textProperty().addListener((obs, old, text) -> updateCounter(text));
        updateCounter("");
        clearErrors();
    }

    /**
     * @param request заявка для редактирования или null для новой
     * @param preset  абонент, выбранный заранее (кнопка «Новая заявка для абонента»), может быть null
     */
    public void init(ConnectionRequest request, Subscriber preset) {
        this.existing = request;
        if (request == null) {
            statusBox.getItems().setAll(RequestStatus.NEW);
            statusBox.setValue(RequestStatus.NEW);
            statusBox.setDisable(true);
            statusHint.setText("Новая заявка всегда создаётся со статусом «Новая»");
            if (preset != null) selectSubscriber(preset.getId());
            return;
        }
        selectSubscriber(request.getSubscriberId());
        typeBox.setValue(request.getType());
        tariffField.setText(request.getTariffPlan());
        descriptionArea.setText(request.getDescription());

        List<RequestStatus> statuses = new ArrayList<>();
        statuses.add(request.getStatus());
        statuses.addAll(ConnectionRequestService.allowedTransitions(request.getStatus()));
        statusBox.getItems().setAll(statuses);
        statusBox.setValue(request.getStatus());
        statusBox.setDisable(statuses.size() == 1);
        statusHint.setText(statuses.size() == 1
                ? "Статус «" + request.getStatus().getTitle() + "» — конечный"
                : "В списке — только допустимые переходы статуса");
    }

    @FXML
    private void onSave() {
        clearErrors();
        Subscriber subscriber = subscriberBox.getValue();
        RequestType type = typeBox.getValue();
        // Тариф сохраняется только для типов, где он имеет смысл.
        String tariff = type != null && type.usesTariff() ? tariffField.getText() : null;
        ConnectionRequest data = new ConnectionRequest(
                subscriber != null ? subscriber.getId() : null,
                type, statusBox.getValue(), tariff, descriptionArea.getText(), null, null);
        try {
            ConnectionRequest saved = existing == null
                    ? requestService.create(data)
                    : requestService.update(existing.getId(), data);
            finish(saved);
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    private void selectSubscriber(Long id) {
        subscriberBox.getItems().stream()
                .filter(s -> Objects.equals(s.getId(), id))
                .findFirst()
                .ifPresent(subscriberBox::setValue);
    }

    private void updateTariffState(RequestType type) {
        boolean usesTariff = type != null && type.usesTariff();
        tariffField.setDisable(!usesTariff);
        tariffHint.setText(usesTariff
                ? "Например: «Домашний 200». Не более 100 символов"
                : "Тариф указывается только для подключения и смены тарифа");
    }

    private void updateCounter(String text) {
        int length = text == null ? 0 : text.strip().length();
        descriptionCounter.setText(length + " / " + DESCRIPTION_MAX + " (не менее 5 символов)");
        descriptionCounter.getStyleClass().remove("counter-error");
        if (length > DESCRIPTION_MAX) descriptionCounter.getStyleClass().add("counter-error");
    }
}
