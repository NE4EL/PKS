package ru.mirea.studenthub.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.mirea.studenthub.android.StudentHubApp;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.net.ApiErrorUtil;
import ru.mirea.studenthub.android.net.ApiProvider;

/**
 * Базовая Activity экранов Student Hub.
 *
 * <p>FLAG_SECURE блокирует screenshots и preview в Recent Apps для экранов с
 * персональными данными. Здесь же обрабатываются ошибки Retrofit
 * ошибок, чтобы Activity не показывали пользователю сырые HTTP/Java детали.</p>
 */
public abstract class SecureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    protected final ApiProvider api() {
        return ((StudentHubApp) getApplication()).api();
    }

    protected final ApiModels.CurrentUser currentUser() {
        return api().session().user();
    }

    /** Переводит пользователя на login, если access-сессия отсутствует. */
    protected final boolean requireSession() {
        if (api().session().hasAccessToken() && currentUser() != null) return true;

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return false;
    }

    protected final void message(String text) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
    }

    protected final void errorDialog(String title, String text) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton("Понятно", null)
                .show();
    }

    /**
     * Унифицированный async вызов. Retrofit на Android возвращает callback на main thread.
     * При окончательном 401 локальная сессия очищается и открывается экран входа.
     */
    protected final <T> void enqueue(Call<T> call,
                                     Consumer<T> success,
                                     Consumer<String> failure) {
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<T> source, Response<T> response) {
                if (response.isSuccessful()) {
                    success.accept(response.body());
                    return;
                }

                String text = ApiErrorUtil.message(response);
                if (response.code() == 401) {
                    api().session().clear();

                    /*
                     * Для защищённого экрана окончательный 401 означает, что refresh
                     * уже не помог. Стек Activity очищается, после чего открывается
                     * экран входа, чтобы не оставлять устаревшие данные на экране.
                     */
                    if (!(SecureActivity.this instanceof LoginActivity)) {
                        Intent intent = new Intent(SecureActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                }
                failure.accept(text);
            }

            @Override
            public void onFailure(Call<T> source, Throwable throwable) {
                failure.accept(ApiErrorUtil.throwable(throwable));
            }
        });
    }
}
