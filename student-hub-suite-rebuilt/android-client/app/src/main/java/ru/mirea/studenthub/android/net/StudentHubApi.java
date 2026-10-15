package ru.mirea.studenthub.android.net;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.mirea.studenthub.android.model.ApiModels;

/**
 * Retrofit-контракт текущего Spring REST API Student Hub.
 *
 * <p>Все endpoint-методы собраны в одном интерфейсе, чтобы контракт Android-клиента
 * было проще сверять со Swagger/OpenAPI.</p>
 */
public interface StudentHubApi {
    @POST("auth/login")
    Call<ApiModels.AuthResponse> login(@Body ApiModels.LoginRequest request);

    @POST("auth/refresh")
    Call<ApiModels.AuthResponse> refresh(@Body ApiModels.RefreshRequest request);

    @POST("auth/logout")
    Call<Void> logout(@Body ApiModels.RefreshRequest request);

    @POST("auth/change-password")
    Call<Void> changePassword(@Body ApiModels.ChangePasswordRequest request);

    @POST("auth/change-initial-password")
    Call<Void> changeInitialPassword(@Body ApiModels.InitialPasswordChangeRequest request);

    @GET("auth/me")
    Call<ApiModels.CurrentUser> me();

    @GET("analytics/summary")
    Call<ApiModels.Analytics> analytics();

    @GET("students")
    Call<ApiModels.PageResponse<ApiModels.Student>> students(
            @Query("q") String query,
            @Query("page") int page,
            @Query("size") int size,
            @Query("sort") String sort
    );

    @GET("students/{id}")
    Call<ApiModels.Student> student(@Path("id") String id);

    @POST("students")
    Call<ApiModels.Student> createStudent(@Body ApiModels.StudentRequest request);

    @PUT("students/{id}")
    Call<ApiModels.Student> updateStudent(@Path("id") String id,
                                          @Body ApiModels.StudentRequest request);

    @DELETE("students/{id}")
    Call<ApiModels.Student> archiveStudent(@Path("id") String id);

    @POST("students/{id}/restore")
    Call<ApiModels.Student> restoreStudent(@Path("id") String id);

    @Headers("X-Confirm-Delete: DELETE")
    @DELETE("students/{id}/permanent")
    Call<Void> permanentDeleteStudent(@Path("id") String id);

    @POST("students/bulk")
    Call<ApiModels.BulkResult> bulk(@Body ApiModels.BulkRequest request);

    @GET("students/export")
    Call<ResponseBody> exportStudents(@Query("q") String query);

    @Multipart
    @POST("students/import/preview")
    Call<ApiModels.ImportPreview> previewImport(
            @Query("mode") ApiModels.ImportMode mode,
            @Part MultipartBody.Part file
    );

    @Multipart
    @POST("students/import/apply")
    Call<ApiModels.ImportResult> applyImport(
            @Query("mode") ApiModels.ImportMode mode,
            @Part MultipartBody.Part file
    );

    @GET("students/{studentId}/notes")
    Call<List<ApiModels.Note>> notes(@Path("studentId") String studentId);

    @POST("students/{studentId}/notes")
    Call<ApiModels.Note> createNote(@Path("studentId") String studentId,
                                    @Body ApiModels.NoteRequest request);

    @PUT("students/{studentId}/notes/{noteId}")
    Call<ApiModels.Note> updateNote(@Path("studentId") String studentId,
                                    @Path("noteId") String noteId,
                                    @Body ApiModels.NoteRequest request);

    @DELETE("students/{studentId}/notes/{noteId}")
    Call<Void> deleteNote(@Path("studentId") String studentId,
                          @Path("noteId") String noteId);

    @GET("groups")
    Call<List<ApiModels.Group>> groups(@Query("activeOnly") boolean activeOnly);

    @POST("groups")
    Call<ApiModels.Group> createGroup(@Body ApiModels.GroupRequest request);

    @PUT("groups/{id}")
    Call<ApiModels.Group> updateGroup(@Path("id") String id,
                                      @Body ApiModels.GroupRequest request);

    @PATCH("groups/{id}/active")
    Call<ApiModels.Group> setGroupActive(@Path("id") String id,
                                         @Query("active") boolean active,
                                         @Query("version") long version);

    @GET("admin/users")
    Call<ApiModels.PageResponse<ApiModels.User>> users(@Query("page") int page,
                                                        @Query("size") int size,
                                                        @Query("sort") String sort);

    @POST("admin/users")
    Call<ApiModels.User> createUser(@Body ApiModels.CreateUserRequest request);

    @PUT("admin/users/{id}")
    Call<ApiModels.User> updateUser(@Path("id") String id,
                                    @Body ApiModels.UpdateUserRequest request);

    @POST("admin/users/{id}/reset-password")
    Call<Void> resetUserPassword(@Path("id") String id,
                                 @Body ApiModels.ResetPasswordRequest request);

    @GET("admin/audit")
    Call<ApiModels.PageResponse<ApiModels.Audit>> audit(@Query("q") String query,
                                                         @Query("page") int page,
                                                         @Query("size") int size,
                                                         @Query("sort") String sort);
}
