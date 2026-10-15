package ru.mirea.studenthub.android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/** Безопасная работа с CSV через Storage Access Framework без broad storage permissions. */
public final class FileTransferUtil {
    private static final long MAX_IMPORT_BYTES = 5L * 1024L * 1024L;

    private FileTransferUtil() {
    }

    /** Копирует выбранный пользователем документ в приватный cache и ограничивает размер. */
    public static File copyCsvToCache(Context context, Uri uri) throws IOException {
        File target = new File(context.getCacheDir(), "students-import-" + System.nanoTime() + ".csv");
        ContentResolver resolver = context.getContentResolver();

        try (InputStream in = resolver.openInputStream(uri);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) throw new IOException("Не удалось открыть выбранный файл");

            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_IMPORT_BYTES) {
                    throw new IOException("CSV-файл превышает 5 МБ");
                }
                out.write(buffer, 0, read);
            }
        } catch (IOException ex) {
            // При ошибке частично записанный CSV удаляется из cache.
            //noinspection ResultOfMethodCallIgnored
            target.delete();
            throw ex;
        }

        return target;
    }

    /** Потоково сохраняет export-response в Uri, предоставленный SAF. */
    public static void writeResponse(Context context, Uri uri, ResponseBody body) throws IOException {
        try (InputStream in = body.byteStream();
             OutputStream out = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (out == null) throw new IOException("Не удалось открыть файл назначения");
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        }
    }
}
