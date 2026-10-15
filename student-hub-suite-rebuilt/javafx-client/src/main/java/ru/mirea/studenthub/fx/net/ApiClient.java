package ru.mirea.studenthub.fx.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.mirea.studenthub.fx.model.Models;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Типобезопасный REST-клиент JavaFX.
 *
 * <p>Клиент работает только через REST API и не содержит JDBC-доступа к PostgreSQL.
 * Проверка ролей, валидация и аудит выполняются на сервере.</p>
 */
public final class ApiClient {
    private final URI baseUri;
    private final HttpClient http;
    private final ObjectMapper json;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile String accessToken;
    private volatile String refreshToken;
    private volatile Models.CurrentUser currentUser;

    /** Создаёт клиент и запрещает незашифрованный удалённый HTTP. */
    public ApiClient(String baseUrl) {
        this.baseUri = normalizeBaseUri(baseUrl);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        this.json = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public Models.CurrentUser currentUser() { return currentUser; }

    /** Входит как native-клиент; пароль сохраняется только в локальной переменной вызова. */
    public Models.AuthResponse login(String email, String password) {
        Map<String, Object> body = Map.of("email", email, "password", password, "clientType", "NATIVE");
        Models.AuthResponse response = sendJson("/auth/login", "POST", body, Models.AuthResponse.class, false);
        applySession(response);
        return response;
    }

    /** JavaFX по умолчанию не восстанавливает сессию между запусками. */
    public boolean restoreSession() {
        return false;
    }

    /** Отзывает refresh и гарантированно очищает локальную память даже при сетевой ошибке. */
    public void logout() {
        String token = refreshToken;
        try {
            if (token != null) sendJson("/auth/logout", "POST", Map.of("refreshToken", token), Void.class, false);
        } finally {
            clearSession();
        }
    }

    /**
     * Первичная замена временного пароля.
     *
     * <p>Текущий временный пароль повторно не передаётся: он уже проверен при login.
     * После успешной операции refresh ротируется и клиент получает новый access JWT
     * с {@code passwordChangeRequired=false}.</p>
     */
    public void changeInitialPassword(String newPassword) {
        sendJson("/auth/change-initial-password", "POST",
                Map.of("newPassword", newPassword), Void.class, true);

        if (!refreshSession()) {
            throw new ApiException(401,
                    "Пароль изменён, но сессию не удалось обновить. Выполните вход новым паролем.",
                    null);
        }
    }

    /**
     * Обычная смена постоянного пароля требует текущий пароль.
     * После смены пароля сервер отзывает refresh-сессии, поэтому локальная сессия очищается сразу.
     */
    public void changePassword(String currentPassword, String newPassword) {
        sendJson("/auth/change-password", "POST",
                Map.of("currentPassword", currentPassword, "newPassword", newPassword), Void.class, true);
        clearSession();
    }

    public Models.Analytics analytics() {
        return sendJson("/analytics/summary", "GET", null, Models.Analytics.class, true);
    }

    public Models.PageResponse<Models.Student> students(String query, int page, int size, String sort) {
        String path = "/students?q=" + encode(query) + "&page=" + page + "&size=" + size + "&sort=" + encode(sort);
        JavaType type = json.getTypeFactory().constructParametricType(Models.PageResponse.class, Models.Student.class);
        return send(path, "GET", null, type, true, true);
    }

    public Models.Student student(UUID id) {
        return sendJson("/students/" + id, "GET", null, Models.Student.class, true);
    }

    public Models.Student createStudent(Models.StudentRequest request) {
        return sendJson("/students", "POST", request, Models.Student.class, true);
    }

    public Models.Student updateStudent(UUID id, Models.StudentRequest request) {
        return sendJson("/students/" + id, "PUT", request, Models.Student.class, true);
    }

    public Models.Student archiveStudent(Models.Student student) {
        return sendJson("/students/" + student.id(), "DELETE", null, Models.Student.class, true);
    }

    public Models.Student restoreStudent(UUID id) {
        return sendJson("/students/" + id + "/restore", "POST", Map.of(), Models.Student.class, true);
    }

    public Models.BulkResult bulk(Set<UUID> ids, Models.BulkAction action,
                                  Models.StudentStatus status, String tag) {
        return sendJson("/students/bulk", "POST", new Models.BulkRequest(ids, action, status, tag),
                Models.BulkResult.class, true);
    }

    public List<Models.Note> notes(UUID studentId) {
        JavaType type = json.getTypeFactory().constructCollectionType(List.class, Models.Note.class);
        return send("/students/" + studentId + "/notes", "GET", null, type, true, true);
    }

    public Models.Note createNote(UUID studentId, String body, boolean pinned) {
        return sendJson("/students/" + studentId + "/notes", "POST",
                new Models.NoteRequest(body, pinned, null), Models.Note.class, true);
    }

    public Models.Note updateNote(UUID studentId, Models.Note note, String body, boolean pinned) {
        return sendJson("/students/" + studentId + "/notes/" + note.id(), "PUT",
                new Models.NoteRequest(body, pinned, note.version()), Models.Note.class, true);
    }

    public void deleteNote(UUID studentId, Models.Note note) {
        sendJson("/students/" + studentId + "/notes/" + note.id(), "DELETE", null, Void.class, true);
    }

    public List<Models.Group> groups(boolean activeOnly) {
        JavaType type = json.getTypeFactory().constructCollectionType(List.class, Models.Group.class);
        return send("/groups?activeOnly=" + activeOnly, "GET", null, type, true, true);
    }

    public Models.Group createGroup(Models.GroupRequest request) {
        return sendJson("/groups", "POST", request, Models.Group.class, true);
    }

    public Models.Group updateGroup(UUID id, Models.GroupRequest request) {
        return sendJson("/groups/" + id, "PUT", request, Models.Group.class, true);
    }

    public Models.Group setGroupActive(UUID id, boolean active, long version) {
        return sendJson("/groups/" + id + "/active?active=" + active + "&version=" + version,
                "PATCH", null, Models.Group.class, true);
    }

    public Models.PageResponse<Models.User> users(int page) {
        JavaType type = json.getTypeFactory().constructParametricType(Models.PageResponse.class, Models.User.class);
        return send("/admin/users?page=" + page + "&size=100&sort=email,asc", "GET", null, type, true, true);
    }

    public Models.User createUser(String email, String displayName, String temporaryPassword,
                                  Set<Models.Role> roles) {
        return sendJson("/admin/users", "POST",
                new Models.CreateUserRequest(email, displayName, temporaryPassword, roles), Models.User.class, true);
    }

    public Models.User updateUser(Models.User user, String email, String displayName,
                                  boolean enabled, Set<Models.Role> roles) {
        return sendJson("/admin/users/" + user.id(), "PUT",
                new Models.UpdateUserRequest(email, displayName, enabled, roles, user.version()), Models.User.class, true);
    }

    public void resetUserPassword(UUID id, String temporaryPassword) {
        sendJson("/admin/users/" + id + "/reset-password", "POST",
                new Models.ResetPasswordRequest(temporaryPassword), Void.class, true);
    }

    public Models.PageResponse<Models.Audit> audit(String query, int page) {
        JavaType type = json.getTypeFactory().constructParametricType(Models.PageResponse.class, Models.Audit.class);
        return send("/admin/audit?q=" + encode(query) + "&page=" + page + "&size=100&sort=occurred,desc",
                "GET", null, type, true, true);
    }

    public Models.ImportPreview previewImport(Path file, Models.ImportMode mode) {
        return multipart("/students/import/preview?mode=" + mode, file, Models.ImportPreview.class);
    }

    public Models.ImportResult applyImport(Path file, Models.ImportMode mode) {
        return multipart("/students/import/apply?mode=" + mode, file, Models.ImportResult.class);
    }

    public void exportStudents(String query, Path destination) {
        HttpResponse<byte[]> response = sendRaw("/students/export?q=" + encode(query), "GET", null, true, true);
        try {
            Files.write(destination, response.body());
        } catch (IOException ex) {
            throw new ApiException(0, "Не удалось сохранить файл: " + ex.getMessage(), null);
        }
    }

    private <T> T multipart(String path, Path file, Class<T> responseType) {
        String boundary = "----StudentHub" + UUID.randomUUID();
        byte[] body;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" +
                    safeFilename(file.getFileName().toString()) + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: text/csv\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(Files.readAllBytes(file));
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            body = out.toByteArray();
        } catch (IOException ex) {
            throw new ApiException(0, "Не удалось прочитать CSV: " + ex.getMessage(), null);
        }
        JavaType type = json.getTypeFactory().constructType(responseType);
        return send(path, "POST", HttpRequest.BodyPublishers.ofByteArray(body), type, true, true,
                Map.of("Content-Type", "multipart/form-data; boundary=" + boundary));
    }

    private <T> T sendJson(String path, String method, Object body, Class<T> type, boolean authenticated) {
        JavaType javaType = json.getTypeFactory().constructType(type);
        HttpRequest.BodyPublisher publisher = body == null ? null : HttpRequest.BodyPublishers.ofString(writeJson(body));
        return send(path, method, publisher, javaType, authenticated, true);
    }

    private <T> T send(String path, String method, HttpRequest.BodyPublisher body, JavaType type,
                       boolean authenticated, boolean allowRefresh) {
        return send(path, method, body, type, authenticated, allowRefresh, Map.of("Content-Type", "application/json"));
    }

    private <T> T send(String path, String method, HttpRequest.BodyPublisher body, JavaType type,
                       boolean authenticated, boolean allowRefresh, Map<String, String> extraHeaders) {
        HttpResponse<byte[]> response = sendRaw(path, method, body, authenticated, allowRefresh, extraHeaders);
        if (type.getRawClass() == Void.class || response.body().length == 0) return null;
        try {
            return json.readValue(response.body(), type);
        } catch (IOException ex) {
            throw new ApiException(response.statusCode(), "Сервер вернул некорректный JSON", correlation(response));
        }
    }

    private HttpResponse<byte[]> sendRaw(String path, String method, HttpRequest.BodyPublisher body,
                                         boolean authenticated, boolean allowRefresh) {
        return sendRaw(path, method, body, authenticated, allowRefresh, Map.of());
    }

    private HttpResponse<byte[]> sendRaw(String path, String method, HttpRequest.BodyPublisher body,
                                         boolean authenticated, boolean allowRefresh,
                                         Map<String, String> extraHeaders) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(relative(path)))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("X-Correlation-ID", UUID.randomUUID().toString());
        extraHeaders.forEach(builder::header);
        if (authenticated && accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : body);
        HttpResponse<byte[]> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException ex) {
            throw new ApiException(0, "API недоступен: " + ex.getMessage(), null);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Операция прервана", null);
        }
        if (response.statusCode() == 401 && authenticated && allowRefresh && refreshSession()) {
            // После успешной ротации исходный запрос повторяется один раз.
            return sendRaw(path, method, body, true, false, extraHeaders);
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) throw parseApiError(response);
        return response;
    }

    private boolean refreshSession() {
        refreshLock.lock();
        try {
            String token = refreshToken;
            if (token == null) return false;
            try {
                Models.AuthResponse response = sendJson("/auth/refresh", "POST",
                        Map.of("refreshToken", token), Models.AuthResponse.class, false);
                applySession(response);
                return true;
            } catch (ApiException ex) {
                clearSession();
                return false;
            }
        } finally {
            refreshLock.unlock();
        }
    }

    private void applySession(Models.AuthResponse response) {
        this.accessToken = response.accessToken();
        this.refreshToken = response.refreshToken();
        this.currentUser = response.user();
    }

    private void clearSession() {
        this.accessToken = null;
        this.refreshToken = null;
        this.currentUser = null;
    }

    private ApiException parseApiError(HttpResponse<byte[]> response) {
        try {
            Models.ApiError error = json.readValue(response.body(), Models.ApiError.class);
            String fields = error.validationErrors() == null || error.validationErrors().isEmpty() ? "" :
                    " — " + error.validationErrors().entrySet().stream()
                            .map(entry -> fieldLabel(entry.getKey()) + ": " + lowerFirst(entry.getValue()))
                            .reduce((a,b) -> a + "; " + b).orElse("");
            String message = normalizeAfterColon(Objects.toString(error.message(), "Ошибка API") + fields);
            return new ApiException(response.statusCode(), message, error.correlationId());
        } catch (IOException ignored) {
            return new ApiException(response.statusCode(), "HTTP " + response.statusCode(), correlation(response));
        }
    }

    /** Человекочитаемые русские имена полей backend validation. */
    private static String fieldLabel(String field) {
        return switch (field == null ? "" : field) {
            case "externalId" -> "внешний ID";
            case "firstName" -> "имя";
            case "lastName" -> "фамилия";
            case "middleName" -> "отчество";
            case "email" -> "email";
            case "phone" -> "телефон";
            case "birthDate" -> "дата рождения";
            case "groupId" -> "группа";
            case "status" -> "статус";
            case "averageGrade" -> "средний балл";
            case "hasDebt" -> "задолженность";
            case "tags" -> "теги";
            default -> field;
        };
    }

    /** После двоеточия сообщения в интерфейсе начинаются со строчной буквы. */
    private static String normalizeAfterColon(String source) {
        if (source == null) return null;
        StringBuilder result = new StringBuilder(source.length());
        boolean lowerNext = false;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (lowerNext && Character.isLetter(ch)) {
                result.append(Character.toLowerCase(ch));
                lowerNext = false;
            } else {
                result.append(ch);
                if (ch == ':') lowerNext = true;
                else if (!Character.isWhitespace(ch) && lowerNext) lowerNext = false;
            }
        }
        return result.toString();
    }

    private static String lowerFirst(String source) {
        if (source == null || source.isEmpty()) return source;
        return Character.toLowerCase(source.charAt(0)) + source.substring(1);
    }

    private String writeJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Не удалось сериализовать запрос", ex); }
    }

    private static URI normalizeBaseUri(String source) {
        String value = source == null || source.isBlank() ? "http://localhost:8080/api" : source.trim();
        if (!value.endsWith("/")) value += "/";
        URI uri = URI.create(value);
        boolean local = "localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()) || "::1".equals(uri.getHost());
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !(local && "http".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("Удалённый API должен использовать HTTPS");
        }
        return uri;
    }

    private static String relative(String path) { return path.startsWith("/") ? path.substring(1) : path; }
    private static String encode(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
    private static String correlation(HttpResponse<?> response) { return response.headers().firstValue("X-Correlation-ID").orElse(null); }
    private static String safeFilename(String value) { return value.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_"); }
}
