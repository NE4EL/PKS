package ru.mirea.studenthub.android;

import android.app.Application;

import ru.mirea.studenthub.android.net.ApiProvider;

/**
 * Application-scope composition root.
 *
 * <p>Сетевые зависимости создаются один раз на уровне Application. Все Activity
 * используют общие SessionManager, Keystore и блокировку refresh.</p>
 */
public final class StudentHubApp extends Application {
    private ApiProvider apiProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        apiProvider = new ApiProvider(this, BuildConfig.API_BASE_URL);
    }

    public ApiProvider api() {
        return apiProvider;
    }
}
