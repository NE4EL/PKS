package ru.mirea.ispteam.fx;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import ru.mirea.ispteam.fx.model.RequestRow;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.SubscriberService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Данные, которые сейчас показаны в интерфейсе. Общие для вкладок «Заявки» и «Абоненты».
 *
 * Хранятся в ObservableList: TableView подписан на изменения списка, поэтому после
 * reload() (setAll) все таблицы, фильтры и счётчики обновляются автоматически —
 * вручную перерисовывать таблицу не нужно.
 *
 * Данные читаются только через сервисы (никакого SQL в UI).
 */
public class DataStore {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;

    private final ObservableList<Subscriber> subscribers = FXCollections.observableArrayList();
    private final ObservableList<RequestRow> requests = FXCollections.observableArrayList();
    private final ReadOnlyStringWrapper message = new ReadOnlyStringWrapper("");

    public DataStore(SubscriberService subscriberService, ConnectionRequestService requestService) {
        this.subscriberService = subscriberService;
        this.requestService = requestService;
    }

    /** Перечитывает абонентов и заявки из БД. */
    public void reload() {
        List<Subscriber> loadedSubscribers = subscriberService.getAll();
        List<ConnectionRequest> loadedRequests = requestService.getAll();
        Map<Long, Subscriber> byId = loadedSubscribers.stream()
                .collect(Collectors.toMap(Subscriber::getId, Function.identity()));

        // Сначала заявки: колонки «Заявок/Активных» у абонентов считаются по ним.
        requests.setAll(loadedRequests.stream()
                .map(r -> new RequestRow(r, byId.get(r.getSubscriberId())))
                .toList());
        subscribers.setAll(loadedSubscribers);
    }

    public ObservableList<Subscriber> subscribers() {
        return subscribers;
    }

    public ObservableList<RequestRow> requests() {
        return requests;
    }

    public List<RequestRow> requestsOf(Subscriber subscriber) {
        return requests.stream()
                .filter(row -> Objects.equals(row.request().getSubscriberId(), subscriber.getId()))
                .toList();
    }

    public long requestCount(Subscriber subscriber) {
        return requestsOf(subscriber).size();
    }

    public long activeRequestCount(Subscriber subscriber) {
        return requestsOf(subscriber).stream().filter(row -> row.status().isActive()).count();
    }

    /** Сообщение в строке состояния главного окна. */
    public ReadOnlyStringProperty messageProperty() {
        return message.getReadOnlyProperty();
    }

    public void setMessage(String text) {
        message.set(LocalTime.now().format(TIME) + "  " + text);
    }
}
