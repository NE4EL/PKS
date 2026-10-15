package ru.mirea.studenthub.android.security;

import ru.mirea.studenthub.android.model.ApiModels;

/**
 * Единственный владелец сессионного состояния Android-клиента.
 *
 * <p>Access JWT остаётся только в памяти процесса. Долгоживущий refresh token хранится
 * только в зашифрованном виде через Android Keystore.</p>
 */
public final class SessionManager {
    private final EncryptedTokenStore store;
    private volatile String accessToken;
    private volatile ApiModels.CurrentUser user;
    private final Object refreshLock = new Object();

    public SessionManager(EncryptedTokenStore store) {
        this.store = store;
    }

    /** Применяет результат login/refresh и атомарно обновляет локальную сессию. */
    public void apply(ApiModels.AuthResponse response) {
        if (response == null) {
            return;
        }
        accessToken = response.accessToken;
        user = response.user;
        if (response.refreshToken != null && !response.refreshToken.trim().isEmpty()) {
            store.save(response.refreshToken);
        }
    }

    public String accessToken() {
        return accessToken;
    }

    public String refreshToken() {
        return store.load();
    }

    public ApiModels.CurrentUser user() {
        return user;
    }

    /** Обновляет только профиль после /auth/me, не касаясь токенов. */
    public void setUser(ApiModels.CurrentUser user) {
        this.user = user;
    }

    public Object refreshLock() {
        return refreshLock;
    }

    public boolean hasRefreshToken() {
        String token = refreshToken();
        return token != null && !token.trim().isEmpty();
    }

    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.trim().isEmpty();
    }

    public void clear() {
        accessToken = null;
        user = null;
        store.clear();
    }
}
