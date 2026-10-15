package ru.mirea.studenthub.android.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Шифрует refresh-токен ключом Android Keystore.
 *
 * <p>Refresh-токен не хранится в SharedPreferences открытым текстом. Для шифрования
 * используется AES/GCM с ключом из Android Keystore.</p>
 */
public final class EncryptedTokenStore {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "student_hub_refresh_key_v1";
    private static final String PREFS = "student_hub_secure_session";
    private static final String TOKEN = "refresh_ciphertext";
    private static final int IV_LENGTH = 12;
    private final SharedPreferences preferences;

    public EncryptedTokenStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Сохраняет токен; null очищает хранилище. */
    public synchronized void save(String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) { clear(); return; }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] encrypted = cipher.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            byte[] packed = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(encrypted, 0, packed, iv.length, encrypted.length);
            preferences.edit().putString(TOKEN, Base64.encodeToString(packed, Base64.NO_WRAP)).apply();
        } catch (Exception ex) {
            clear();
            throw new IllegalStateException("Не удалось защитить refresh-токен", ex);
        }
    }

    /** Возвращает токен либо null; повреждённое значение уничтожается. */
    public synchronized String load() {
        String encoded = preferences.getString(TOKEN, null);
        if (encoded == null) return null;
        try {
            byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
            if (packed.length <= IV_LENGTH) throw new IllegalStateException("Invalid ciphertext");
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[packed.length - IV_LENGTH];
            System.arraycopy(packed, 0, iv, 0, IV_LENGTH);
            System.arraycopy(packed, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            clear();
            return null;
        }
    }

    public synchronized void clear() {
        preferences.edit().remove(TOKEN).apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);
        java.security.Key key = keyStore.getKey(ALIAS, null);
        if (key instanceof SecretKey secretKey) return secretKey;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build());
        return generator.generateKey();
    }
}
