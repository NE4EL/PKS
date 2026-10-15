package ru.mirea.studenthub.android.net;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import ru.mirea.studenthub.android.security.SessionManager;

/** Добавляет Bearer access JWT и correlation ID, никогда не журналируя секреты. */
public final class AuthInterceptor implements Interceptor {
    private static final Set<String> PUBLIC_AUTH_PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout"
    )));

    private final SessionManager session;

    public AuthInterceptor(SessionManager session) {
        this.session = session;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder()
                .header("X-Correlation-ID", UUID.randomUUID().toString());

        String token = session.accessToken();
        String path = original.url().encodedPath();

        /*
         * /auth/me, /auth/change-password и /auth/change-initial-password
         * являются защищёнными endpoints и получают Bearer JWT.
         * Без токена остаются только публичные login/refresh/logout.
         */
        if (token != null && !PUBLIC_AUTH_PATHS.contains(path)) {
            builder.header("Authorization", "Bearer " + token);
        }
        return chain.proceed(builder.build());
    }
}
