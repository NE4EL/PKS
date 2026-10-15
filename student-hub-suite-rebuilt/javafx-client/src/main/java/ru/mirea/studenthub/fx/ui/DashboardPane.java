package ru.mirea.studenthub.fx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;
import ru.mirea.studenthub.fx.util.Ui;

/** Аналитическая панель desktop-клиента. */
public final class DashboardPane extends BorderPane {
    private final ApiClient api;
    private final Window owner;
    private final FlowPane metrics = new FlowPane(12, 12);
    private final ListView<String> groups = new ListView<>();
    private final ListView<String> statuses = new ListView<>();
    private final ListView<String> risks = new ListView<>();
    private final ProgressIndicator progress = new ProgressIndicator();

    public DashboardPane(ApiClient api, Window owner) {
        this.api = api;
        this.owner = owner;
        setPadding(new Insets(18));
        Button refresh = new Button("Обновить");
        refresh.setOnAction(event -> load());
        HBox top = new HBox(10, new Label("Сводные показатели"), refresh, progress);
        top.setAlignment(Pos.CENTER_LEFT);
        setTop(new VBox(12, top, metrics));
        GridPane lists = new GridPane();
        lists.setHgap(12); lists.setVgap(8); lists.setPadding(new Insets(18, 0, 0, 0));
        lists.add(card("По группам", groups), 0, 0);
        lists.add(card("По статусам", statuses), 1, 0);
        lists.add(card("По риску", risks), 2, 0);
        GridPane.setHgrow(groups, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(statuses, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(risks, javafx.scene.layout.Priority.ALWAYS);
        setCenter(lists);
        progress.setMaxSize(22, 22);
        progress.setVisible(false);
        load();
    }

    private VBox card(String title, ListView<String> list) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        list.setPrefHeight(420);
        VBox box = new VBox(8, label, list);
        box.setPadding(new Insets(14));
        box.getStyleClass().add("card");
        GridPane.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    private void load() {
        progress.setVisible(true);
        FxTasks.run(api::analytics, data -> {
            progress.setVisible(false);
            metrics.getChildren().setAll(
                    metric("Всего", data.total()), metric("Активные", data.active()),
                    metric("Архив", data.archived()), metric("С долгом", data.withDebt()),
                    metric("Средний балл", data.averageGrade() == null ? "—" : data.averageGrade()));
            groups.getItems().setAll(lines(data.byGroup(), value -> value));
            statuses.getItems().setAll(lines(data.byStatus(), Models.StudentStatus::labelOf));
            risks.getItems().setAll(lines(data.byRisk(), Models.RiskLevel::labelOf));
        }, error -> {
            progress.setVisible(false);
            Ui.error(owner, error);
        });
    }

    private VBox metric(String label, Object value) {
        Label caption = new Label(label); caption.getStyleClass().add("muted");
        Label number = new Label(String.valueOf(value)); number.getStyleClass().add("metric-value");
        VBox box = new VBox(4, caption, number); box.setPadding(new Insets(14)); box.getStyleClass().add("card");
        box.setPrefWidth(190);
        return box;
    }

    private static java.util.List<String> lines(java.util.List<Models.NamedCount> values,
                                                   java.util.function.Function<String, String> labeler) {
        return values == null
                ? java.util.List.of()
                : values.stream()
                    .map(item -> labeler.apply(item.name()) + " — " + item.count())
                    .toList();
    }
}
