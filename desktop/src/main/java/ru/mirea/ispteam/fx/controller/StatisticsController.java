package ru.mirea.ispteam.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.fx.util.Dialogs;
import ru.mirea.ispteam.fx.util.Formats;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.service.StatisticsReport;
import ru.mirea.ispteam.service.StatisticsService;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Окно статистики (statistics-view.fxml). Сами показатели считает StatisticsService.buildReport(),
 * контроллер только выводит готовые числа в плитки и диаграммы.
 */
public class StatisticsController {

    private final StatisticsService statisticsService;

    @FXML private DatePicker fromDate;
    @FXML private DatePicker toDate;
    @FXML private Label periodError;

    @FXML private Label totalSubscribersValue;
    @FXML private Label totalRequestsValue;
    @FXML private Label activeValue;
    @FXML private Label completedValue;
    @FXML private Label cancelledValue;
    @FXML private Label completedPercentValue;
    @FXML private Label avgPerSubscriberValue;
    @FXML private Label avgTimeValue;
    @FXML private Label minMaxTimeValue;
    @FXML private Label popularTariffValue;
    @FXML private Label periodValue;
    @FXML private Label periodCaption;

    @FXML private PieChart statusChart;
    @FXML private BarChart<Number, String> typeChart;
    @FXML private NumberAxis countAxis;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @FXML
    private void initialize() {
        fromDate.setConverter(Formats.dateConverter());
        toDate.setConverter(Formats.dateConverter());
        fromDate.valueProperty().addListener((obs, old, value) -> refresh());
        toDate.valueProperty().addListener((obs, old, value) -> refresh());
        // На оси количества — только целые числа.
        countAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return value.doubleValue() == Math.rint(value.doubleValue()) ? String.valueOf(value.intValue()) : "";
            }

            @Override
            public Number fromString(String text) {
                return Integer.valueOf(text);
            }
        });
        periodError.setManaged(false);
    }

    @FXML
    public void refresh() {
        StatisticsReport report;
        try {
            report = statisticsService.buildReport(fromDate.getValue(), toDate.getValue());
            setPeriodError(null);
        } catch (ValidationException e) {
            setPeriodError(e.getMessage());
            return;
        } catch (RuntimeException e) {
            Dialogs.showError(fromDate.getScene() != null ? fromDate.getScene().getWindow() : null, e);
            return;
        }
        show(report);
    }

    @FXML
    private void onResetPeriod() {
        fromDate.setValue(null);
        toDate.setValue(null);
        fromDate.getEditor().clear();
        toDate.getEditor().clear();
    }

    private void show(StatisticsReport r) {
        totalSubscribersValue.setText(String.valueOf(r.totalSubscribers()));
        totalRequestsValue.setText(String.valueOf(r.totalRequests()));
        activeValue.setText(String.valueOf(r.activeRequests()));
        completedValue.setText(String.valueOf(r.completedRequests()));
        cancelledValue.setText(String.valueOf(r.cancelledOrRejected()));
        completedPercentValue.setText(Formats.number(r.completedPercent()) + " %");
        avgPerSubscriberValue.setText(Formats.number(r.avgRequestsPerSubscriber()));
        avgTimeValue.setText(hours(r.avgProcessingHours()));
        minMaxTimeValue.setText(r.minProcessingHours().isPresent()
                ? hours(r.minProcessingHours()) + " / " + hours(r.maxProcessingHours()) : "нет данных");
        popularTariffValue.setText(r.mostPopularTariff() != null ? r.mostPopularTariff() : "нет данных");
        periodValue.setText(String.valueOf(r.requestsInPeriod()));
        periodCaption.setText(periodText(r));

        statusChart.getData().clear();
        for (Map.Entry<RequestStatus, Long> e : r.byStatus().entrySet()) {
            if (e.getValue() > 0) {
                statusChart.getData().add(new PieChart.Data(e.getKey().getTitle() + " (" + e.getValue() + ")", e.getValue()));
            }
        }

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Количество заявок");
        for (Map.Entry<RequestType, Long> e : r.byType().entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getValue(), e.getKey().getTitle()));
        }
        typeChart.setData(FXCollections.observableArrayList(List.of(series)));
    }

    private static String periodText(StatisticsReport r) {
        if (r.periodFrom() == null && r.periodTo() == null) return "Заявок за всё время (период не выбран)";
        return "Заявок за период " + (r.periodFrom() != null ? "с " + Formats.date(r.periodFrom()) + " " : "")
                + (r.periodTo() != null ? "по " + Formats.date(r.periodTo()) : "");
    }

    private static String hours(OptionalDouble value) {
        return value.isPresent() ? Formats.hours(value.getAsDouble()) : "нет данных";
    }

    private void setPeriodError(String message) {
        periodError.setText(message == null ? "" : message);
        periodError.setVisible(message != null);
        periodError.setManaged(message != null);
    }
}
