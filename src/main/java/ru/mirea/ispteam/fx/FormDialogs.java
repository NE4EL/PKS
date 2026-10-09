package ru.mirea.ispteam.fx;

import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.ispteam.fx.controller.RequestFormController;
import ru.mirea.ispteam.fx.controller.SubscriberFormController;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.Subscriber;

import java.util.Optional;

/**
 * Открытие форм добавления/редактирования. Вынесено отдельно, потому что одну и ту же форму заявки
 * открывают и вкладка «Заявки», и вкладка «Абоненты» («Новая заявка для абонента») — без дублирования кода.
 */
public final class FormDialogs {

    private final ViewLoader views;

    FormDialogs(ViewLoader views) {
        this.views = views;
    }

    /**
     * @param existing заявка для редактирования или null для новой
     * @param preset   абонент, который будет выбран в новой заявке (может быть null)
     * @return сохранённая заявка или пусто, если пользователь нажал «Отмена»
     */
    public Optional<ConnectionRequest> requestForm(Window owner, ConnectionRequest existing, Subscriber preset) {
        ViewLoader.View<RequestFormController> view = views.load("request-form.fxml");
        view.controller().init(existing, preset);
        String title = existing == null ? "Новая заявка" : "Редактирование заявки №" + existing.getId();
        Stage stage = views.modalStage(owner, title, view.root());
        stage.showAndWait();
        return view.controller().result();
    }

    public Optional<Subscriber> subscriberForm(Window owner, Subscriber existing) {
        ViewLoader.View<SubscriberFormController> view = views.load("subscriber-form.fxml");
        view.controller().init(existing);
        String title = existing == null ? "Новый абонент" : "Редактирование абонента";
        Stage stage = views.modalStage(owner, title, view.root());
        stage.showAndWait();
        return view.controller().result();
    }
}
