package ru.mirea.ispteam.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.ispteam.AppContext;
import ru.mirea.ispteam.exception.DatabaseException;
import ru.mirea.ispteam.fx.controller.MainController;
import ru.mirea.ispteam.fx.util.Dialogs;

/**
 * КР2. Точка входа JavaFX-клиента (mvn javafx:run).
 *
 * Stage — окно приложения (его создаёт JavaFX и передаёт в start()).
 * Scene — содержимое окна: дерево узлов (Node), загруженное из main-view.fxml.
 * Node — любой элемент интерфейса: кнопка, таблица, контейнер.
 */
public class IspFxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(IspFxApplication.class);

    @Override
    public void start(Stage stage) {
        // Любое непойманное исключение — в журнал и понятное окно, а не stack trace пользователю.
        // runLater: окно нельзя открывать через showAndWait во время отрисовки/layout, где могла возникнуть ошибка.
        Thread.currentThread().setUncaughtExceptionHandler(
                (thread, error) -> Platform.runLater(() -> Dialogs.showError(stage, error)));

        AppContext context;
        try {
            context = AppContext.create();
        } catch (DatabaseException e) {
            log.error("Нет подключения к БД", e);
            Dialogs.showError(null, "Нет подключения к базе данных", e.getMessage()
                    + "\n\nПроверьте, что PostgreSQL запущен (docker compose up -d), и параметры в файле "
                    + "src/main/resources/database.properties.");
            Platform.exit();
            return;
        }

        DataStore store = new DataStore(context.subscriberService(), context.requestService());
        ViewLoader views = new ViewLoader(context, store);
        ViewLoader.View<MainController> main = views.load("main-view.fxml");

        Scene scene = views.styledScene(main.root());
        stage.setTitle("ИС интернет-провайдера — учёт заявок");
        stage.setScene(scene);
        stage.setWidth(1280);
        stage.setHeight(800);
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.show();

        main.controller().refresh();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
