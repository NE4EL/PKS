package ru.mirea.studenthub.android.net;

import com.google.gson.Gson;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLException;

import retrofit2.Response;
import ru.mirea.studenthub.android.model.ApiModels;

/**
 * Преобразует безопасный JSON ApiError backend в понятный русский текст.
 *
 * <p>Технические имена полей, например {@code phone}, заменяются на русские подписи.
 * Формат сообщений совпадает с web-клиентом.</p>
 */
public final class ApiErrorUtil {
    private static final Gson GSON = new Gson();
    private static final Map<String, String> FIELD_LABELS = new LinkedHashMap<>();

    static {
        FIELD_LABELS.put("externalId", "внешний ID");
        FIELD_LABELS.put("firstName", "имя");
        FIELD_LABELS.put("lastName", "фамилия");
        FIELD_LABELS.put("middleName", "отчество");
        FIELD_LABELS.put("email", "email");
        FIELD_LABELS.put("phone", "телефон");
        FIELD_LABELS.put("birthDate", "дата рождения");
        FIELD_LABELS.put("groupId", "группа");
        FIELD_LABELS.put("status", "статус");
        FIELD_LABELS.put("averageGrade", "средний балл");
        FIELD_LABELS.put("hasDebt", "задолженность");
        FIELD_LABELS.put("tags", "теги");
        FIELD_LABELS.put("version", "версия");
        FIELD_LABELS.put("temporaryPassword", "временный пароль");
        FIELD_LABELS.put("newPassword", "новый пароль");
        FIELD_LABELS.put("currentPassword", "текущий пароль");
    }

    private ApiErrorUtil() {
    }

    public static String message(Response<?> response) {
        try {
            if (response.errorBody() == null) {
                return "HTTP " + response.code();
            }

            ApiModels.ApiError error = GSON.fromJson(
                    response.errorBody().charStream(),
                    ApiModels.ApiError.class
            );

            StringBuilder text = new StringBuilder(
                    error != null && error.message != null
                            ? error.message
                            : "HTTP " + response.code()
            );

            if (error != null && error.validationErrors != null) {
                for (Map.Entry<String, String> entry : error.validationErrors.entrySet()) {
                    text.append("\n")
                            .append(fieldLabel(entry.getKey()))
                            .append(": ")
                            .append(lowerFirst(entry.getValue()));
                }
            }

            if (error != null && error.correlationId != null && !error.correlationId.trim().isEmpty()) {
                text.append("\nID: ").append(error.correlationId);
            }

            return normalizeAfterColon(text.toString());
        } catch (RuntimeException ex) {
            return "HTTP " + response.code();
        }
    }

    public static String throwable(Throwable error) {
        if (error == null) {
            return "Неизвестная ошибка";
        }

        /*
         * Сырые URL, hostname, stack trace и текст TLS/IO ошибок пользователю не показываются
         * исключения. Эти детали полезны разработчику, но могут раскрывать внутреннюю
         * сетевую конфигурацию приложения.
         */
        if (error instanceof SocketTimeoutException) {
            return "Сервер не ответил вовремя";
        }
        if (error instanceof UnknownHostException || error instanceof ConnectException) {
            return "Не удалось подключиться к серверу Student Hub";
        }
        if (error instanceof SSLException) {
            return "Не удалось установить защищённое соединение";
        }
        if (error instanceof java.io.IOException) {
            return "Ошибка сетевого соединения";
        }

        return "Не удалось выполнить операцию";
    }

    private static String fieldLabel(String field) {
        return FIELD_LABELS.getOrDefault(field, field);
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.substring(0, 1).toLowerCase(Locale.forLanguageTag("ru-RU")) + value.substring(1);
    }

    private static String normalizeAfterColon(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean afterColon = false;

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (afterColon && Character.isLetter(ch)) {
                result.append(Character.toLowerCase(ch));
                afterColon = false;
                continue;
            }

            result.append(ch);
            if (ch == ':') {
                afterColon = true;
            } else if (!Character.isWhitespace(ch) && afterColon) {
                afterColon = false;
            }
        }

        return result.toString();
    }
}
