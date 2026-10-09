package ru.mirea.ispteam.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.ispteam.AppContext;
import ru.mirea.ispteam.fx.controller.MainController;
import ru.mirea.ispteam.fx.controller.RequestFormController;
import ru.mirea.ispteam.fx.controller.RequestsController;
import ru.mirea.ispteam.fx.controller.StatisticsController;
import ru.mirea.ispteam.fx.controller.SubscriberFormController;
import ru.mirea.ispteam.fx.controller.SubscribersController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Objects;

/**
 * Загружает FXML-разметку окон и создаёт для неё контроллеры.
 *
 * FXMLLoader читает FXML, создаёт узлы (Node) и по атрибуту fx:controller понимает, какой класс
 * контроллера нужен. Создание контроллера мы перехватываем (setControllerFactory), чтобы передать
 * в его конструктор сервисы — так контроллеры не создают зависимости сами и не знают о БД.
 */
public final class ViewLoader {

    private static final String VIEW_DIR = "/ru/mirea/ispteam/fx/view/";
    private static final String STYLESHEET = "/ru/mirea/ispteam/fx/css/app.css";

    private final AppContext context;
    private final DataStore store;
    private final FormDialogs forms;

    public ViewLoader(AppContext context, DataStore store) {
        this.context = context;
        this.store = store;
        this.forms = new FormDialogs(this);
    }

    /** Результат загрузки FXML: корневой узел и его контроллер. */
    public record View<C>(Parent root, C controller) {
    }

    public <C> View<C> load(String fxmlName) {
        URL fxml = Objects.requireNonNull(ViewLoader.class.getResource(VIEW_DIR + fxmlName),
                "FXML не найден: " + fxmlName);
        FXMLLoader loader = new FXMLLoader(fxml);
        loader.setControllerFactory(this::createController);
        try {
            Parent root = loader.load();
            return new View<>(root, loader.getController());
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось загрузить интерфейс " + fxmlName, e);
        }
    }

    /** Модальное окно (блокирует главное окно, пока открыто) — для форм добавления/редактирования. */
    public Stage modalStage(Window owner, String title, Parent root) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(styledScene(root));
        stage.setResizable(true);
        return stage;
    }

    public Scene styledScene(Parent root) {
        Scene scene = new Scene(root);
        scene.getStylesheets().add(stylesheet());
        return scene;
    }

    public static String stylesheet() {
        return Objects.requireNonNull(ViewLoader.class.getResource(STYLESHEET), "CSS не найден").toExternalForm();
    }

    // Фабрика контроллеров: FXMLLoader вызывает её для fx:controller каждого FXML (в т.ч. fx:include).
    private Object createController(Class<?> type) {
        if (type == MainController.class) {
            return new MainController(this, store);
        }
        if (type == RequestsController.class) {
            return new RequestsController(store, context.requestService(), context.exportService(), forms);
        }
        if (type == SubscribersController.class) {
            return new SubscribersController(store, context.subscriberService(), context.exportService(), forms);
        }
        if (type == RequestFormController.class) {
            return new RequestFormController(store, context.requestService());
        }
        if (type == SubscriberFormController.class) {
            return new SubscriberFormController(context.subscriberService());
        }
        if (type == StatisticsController.class) {
            return new StatisticsController(context.statisticsService());
        }
        throw new IllegalArgumentException("Неизвестный контроллер: " + type.getName());
    }
}
