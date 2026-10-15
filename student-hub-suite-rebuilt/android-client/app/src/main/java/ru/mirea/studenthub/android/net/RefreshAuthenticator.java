package ru.mirea.studenthub.android.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.security.SessionManager;

/**
 * Ротирует native refresh-токен после 401 и повторяет исходный запрос ровно один раз.
 *
 * <p>Refresh синхронизирован: несколько параллельных 401 не смогут одновременно
 * предъявить серверу один одноразовый refresh token.</p>
 */
public final class RefreshAuthenticator implements Authenticator {
    private static final Set<String> NON_RETRY_PATHS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout"
    )));

    private final SessionManager session;
    private final StudentHubApi publicApi;

    public RefreshAuthenticator(SessionManager session, StudentHubApi publicApi) {
        this.session = session;
        this.publicApi = publicApi;
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        if (responseCount(response) >= 2 || NON_RETRY_PATHS.contains(response.request().url().encodedPath())) {
            return null;
        }

        synchronized (session.refreshLock()) {
            String currentAccess = session.accessToken();
            String requestAuth = response.request().header("Authorization");

            // Пока этот запрос ждал lock, другой поток мог уже обновить access token.
            if (currentAccess != null
                    && requestAuth != null
                    && !requestAuth.equals("Bearer " + currentAccess)) {
                return response.request().newBuilder()
                        .header("Authorization", "Bearer " + currentAccess)
                        .build();
            }

            String refresh = session.refreshToken();
            if (refresh == null) return null;

            retrofit2.Response<ApiModels.AuthResponse> refreshResponse = publicApi
                    .refresh(new ApiModels.RefreshRequest(refresh))
                    .execute();

            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                session.clear();
                return null;
            }

            session.apply(refreshResponse.body());
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + session.accessToken())
                    .build();
        }
    }

    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) count++;
        return count;
    }
}
