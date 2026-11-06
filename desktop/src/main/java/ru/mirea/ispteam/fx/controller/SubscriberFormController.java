package ru.mirea.ispteam.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.fx.util.Formats;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.SubscriberService;

import java.time.LocalDate;

/**
 * Форма абонента (subscriber-form.fxml): добавление и редактирование.
 * Проверку ФИО, телефона, email, адреса, уникальности и даты выполняет SubscriberService.
 */
public class SubscriberFormController extends FormController<Subscriber> {

    private final SubscriberService subscriberService;
    private Subscriber existing;

    @FXML private TextField fullNameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField addressField;
    @FXML private DatePicker registrationPicker;
    @FXML private Label registrationHint;

    public SubscriberFormController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @FXML
    private void initialize() {
        registerField("fullName", fullNameField);
        registerField("phone", phoneField);
        registerField("email", emailField);
        registerField("address", addressField);
        registerField("registrationDate", registrationPicker);
        registrationPicker.setConverter(Formats.dateConverter());
        clearErrors();
    }

    public void init(Subscriber subscriber) {
        this.existing = subscriber;
        if (subscriber == null) {
            registrationPicker.setValue(LocalDate.now());
            registrationHint.setText("Не позже сегодняшнего дня");
            return;
        }
        fullNameField.setText(subscriber.getFullName());
        phoneField.setText(subscriber.getPhone());
        emailField.setText(subscriber.getEmail());
        addressField.setText(subscriber.getAddress());
        registrationPicker.setValue(subscriber.getRegistrationDate());
        registrationPicker.setDisable(true);
        registrationHint.setText("Дата регистрации не меняется при редактировании");
    }

    @FXML
    private void onSave() {
        clearErrors();
        try {
            Subscriber data = new Subscriber(fullNameField.getText(), phoneField.getText(),
                    emailField.getText(), addressField.getText(), readRegistrationDate());
            Subscriber saved = existing == null
                    ? subscriberService.create(data)
                    : subscriberService.update(existing.getId(), data);
            finish(saved);
        } catch (RuntimeException e) {
            showError(e);
        }
    }

    // Дату можно выбрать в календаре или ввести вручную — ручной ввод проверяем на формат.
    private LocalDate readRegistrationDate() {
        if (existing != null) return existing.getRegistrationDate();
        String text = registrationPicker.getEditor().getText();
        if (text == null || text.isBlank()) return null;
        LocalDate date = Formats.dateConverter().fromString(text);
        if (date == null) {
            throw new ValidationException("registrationDate", "Дата регистрации: введите дату в формате дд.мм.гггг");
        }
        return date;
    }
}
