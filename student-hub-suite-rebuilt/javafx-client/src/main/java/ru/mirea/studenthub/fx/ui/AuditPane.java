package ru.mirea.studenthub.fx.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;
import ru.mirea.studenthub.fx.util.Ui;

/** Просмотр неизменяемого журнала аудита. */
public final class AuditPane extends BorderPane {
    private final ApiClient api;private final Window owner;private final TextField query=new TextField();private final TableView<Models.Audit> table=new TableView<>();
    public AuditPane(ApiClient api,Window owner){this.api=api;this.owner=owner;setPadding(new Insets(16));query.setPromptText("Пользователь, событие или correlation ID");query.setPrefWidth(460);query.setOnKeyPressed(e->{if(e.getCode()== KeyCode.ENTER)load();});Button find=new Button("Найти");find.getStyleClass().add("primary-button");find.setOnAction(e->load());setTop(new ToolBar(query,find));configure();setCenter(table);load();}
    private void configure(){table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);table.getColumns().add(column("Дата",a->String.valueOf(a.occurredAt())));table.getColumns().add(column("Пользователь",a->value(a.actorEmail())));table.getColumns().add(column("Событие",Models.Audit::eventType));table.getColumns().add(column("Сущность",a->value(a.entityType())));table.getColumns().add(column("Описание",Models.Audit::summary));table.getColumns().add(column("Correlation ID",a->value(a.correlationId())));table.setPlaceholder(Ui.emptyLabel("События не найдены"));}
    private void load(){table.setDisable(true);FxTasks.run(()->api.audit(query.getText().trim(),0),page->{table.setItems(FXCollections.observableArrayList(page.content()));table.setDisable(false);},error->{table.setDisable(false);Ui.error(owner,error);});}
    private static TableColumn<Models.Audit,String> column(String title,java.util.function.Function<Models.Audit,String> mapper){TableColumn<Models.Audit,String> c=new TableColumn<>(title);c.setCellValueFactory(d->new ReadOnlyStringWrapper(mapper.apply(d.getValue())));return c;}
    private static String value(String v){return v==null||v.isBlank()?"—":v;}
}
