package ru.mirea.ispteam.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.ispteam.fx.DataStore;
import ru.mirea.ispteam.fx.ViewLoader;
import ru.mirea.ispteam.fx.util.Dialogs;

/**
 * Контроллер главного окна (main-view.fxml): шапка с общими действиями, вкладки, строка состояния.
 * Вкладки «Заявки» и «Абоненты» подключены через fx:include и имеют собственные контроллеры.
 */
public class MainController {

    private final ViewLoader views;
    private final DataStore store;
    private Stage statisticsStage;
    private StatisticsController statisticsController;

    @FXML
    private TabPane tabs;
    @FXML
    private Label statusLabel;

    public MainController(ViewLoader views, DataStore store) {
        this.views = views;
        this.store = store;
    }

    // Вызывается FXMLLoader после того, как все поля с @FXML заполнены.
    @FXML
    private void initialize() {
        statusLabel.textProperty().bind(store.messageProperty());
    }

    /** Перечитать данные из БД (кнопка «Обновить» и первый запуск). */
    @FXML
    public void refresh() {
        try {
            store.reload();
            store.setMessage("Данные загружены: абонентов — " + store.subscribers().size()
                    + ", заявок — " + store.requests().size());
            if (statisticsController != null && statisticsStage.isShowing()) {
                statisticsController.refresh();
            }
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    /** Отдельное (немодальное) окно статистики. Повторное нажатие поднимает уже открытое окно. */
    @FXML
    private void onStatistics() {
        try {
            if (statisticsStage == null) {
                ViewLoader.View<StatisticsController> view = views.load("statistics-view.fxml");
                statisticsController = view.controller();
                statisticsStage = new Stage();
                statisticsStage.initOwner(window());
                statisticsStage.setTitle("Статистика по заявкам");
                statisticsStage.setScene(views.styledScene(view.root()));
                statisticsStage.setWidth(1040);
                statisticsStage.setHeight(760);
            }
            statisticsController.refresh();
            statisticsStage.show();
            statisticsStage.toFront();
        } catch (RuntimeException e) {
            Dialogs.showError(window(), e);
        }
    }

    private Window window() {
        return tabs.getScene().getWindow();
    }
}
