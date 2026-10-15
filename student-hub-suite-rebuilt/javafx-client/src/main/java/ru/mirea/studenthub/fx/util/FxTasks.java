package ru.mirea.studenthub.fx.util;

import javafx.concurrent.Task;
import ru.mirea.studenthub.fx.net.ApiException;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Запускает сетевые операции вне JavaFX Application Thread.
 */
public final class FxTasks {
    private FxTasks() { }

    public static <T> void run(Callable<T> work, Consumer<T> success, Consumer<Throwable> failure) {
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return work.call(); }
        };
        task.setOnSucceeded(event -> success.accept(task.getValue()));
        task.setOnFailed(event -> failure.accept(task.getException()));
        Thread thread = new Thread(task, "student-hub-api");
        thread.setDaemon(true);
        thread.start();
    }

    public static String friendly(Throwable error) {
        Throwable value = error;
        while (value.getCause() != null && !(value instanceof ApiException)) value = value.getCause();
        if (value instanceof ApiException api && api.getCorrelationId() != null) {
            return api.getMessage() + " [" + api.getCorrelationId() + "]";
        }
        return value.getMessage() == null ? value.getClass().getSimpleName() : value.getMessage();
    }
}
