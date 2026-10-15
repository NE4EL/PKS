package ru.mirea.studenthub.android.net;

import android.content.Context;

import java.net.URI;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.mirea.studenthub.android.BuildConfig;
import ru.mirea.studenthub.android.security.EncryptedTokenStore;
import ru.mirea.studenthub.android.security.SessionManager;

/**
 * Создаёт public и authenticated Retrofit-клиенты приложения.
 *
 * <p>Логирование HTTP body не подключено, чтобы пароли, JWT и данные студентов не
 * попадали в Logcat. Release использует HTTPS; debug HTTP ограничен локальными адресами.</p>
 */
public final class ApiProvider {
    private static final Set<String> DEBUG_HTTP_HOSTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "10.0.2.2",
            "localhost",
            "127.0.0.1"
    )));

    private final SessionManager session;
    private final StudentHubApi publicApi;
    private final StudentHubApi secureApi;

    public ApiProvider(Context context, String baseUrl) {
        String normalized = validateBaseUrl(baseUrl);

        session = new SessionManager(
                new EncryptedTokenStore(context.getApplicationContext())
        );

        OkHttpClient publicClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        publicApi = new Retrofit.Builder()
                .baseUrl(normalized)
                .client(publicClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StudentHubApi.class);

        OkHttpClient secureClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(session))
                .authenticator(new RefreshAuthenticator(session, publicApi))
                .build();

        secureApi = new Retrofit.Builder()
                .baseUrl(normalized)
                .client(secureClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(StudentHubApi.class);
    }

    public StudentHubApi publicApi() {
        return publicApi;
    }

    public StudentHubApi secureApi() {
        return secureApi;
    }

    public SessionManager session() {
        return session;
    }

    private static String validateBaseUrl(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("API_BASE_URL is empty");
        }

        String normalized = value.endsWith("/") ? value : value + "/";
        URI uri = URI.create(normalized);
        String scheme = uri.getScheme();
        String host = uri.getHost();

        if (!BuildConfig.DEBUG && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Release API must use HTTPS");
        }

        if (BuildConfig.DEBUG
                && "http".equalsIgnoreCase(scheme)
                && (host == null || !DEBUG_HTTP_HOSTS.contains(host.toLowerCase()))) {
            throw new IllegalArgumentException(
                    "Debug cleartext is limited to emulator/localhost"
            );
        }

        return normalized;
    }
}
