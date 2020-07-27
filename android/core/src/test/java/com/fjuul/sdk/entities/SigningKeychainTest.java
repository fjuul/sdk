package com.fjuul.sdk.entities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.text.IsEmptyString.isEmptyString;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.hamcrest.core.IsNot;
import org.hamcrest.text.IsEmptyString;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.test.core.app.ApplicationProvider;

@RunWith(Enclosed.class)
public class SigningKeychainTest {
    public static final String DUMMY_USER_TOKEN = "USER_TOKEN";
    public static final JsonAdapter<SigningKey> keyJsonAdapter = createKeyJsonAdapter();

    public static JsonAdapter<SigningKey> createKeyJsonAdapter() {
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        return moshi.adapter(SigningKey.class);
    }

    public static class WithInMemoryStorage {
        InMemoryStorage storage;
        SigningKeychain keychain;

        @Before
        public void beforeSetup() {
            storage = new InMemoryStorage();
            keychain = new SigningKeychain(storage, DUMMY_USER_TOKEN);
        }

        @Test
        public void getValidKey_WithExpiredSigningKey_returnsEmptyOptional() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            Date expiresAt = calendar.getTime();
            storage.set(
                "signing-key." + DUMMY_USER_TOKEN,
                keyJsonAdapter.toJson(new SigningKey("key-id", "REAL_SECRET", expiresAt)));
            assertFalse("returns empty optional", keychain.getValidKey().isPresent());
        }

        @Test
        public void getValidKey_WithValidSigningKey_returnsOptionalWithKey() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            storage.set("signing-key." + DUMMY_USER_TOKEN, keyJsonAdapter.toJson(key));
            Optional<SigningKey> result = keychain.getValidKey();
            assertTrue("returns non-empty optional", result.isPresent());
            assertEquals("returns stored key", key.getId(), result.get().getId());
            assertEquals("returns stored key", key.getSecret(), result.get().getSecret());
        }

        @Test
        public void setKey_WithNotNullValue_setsKeyInStorage() throws IOException {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            keychain.setKey(key);
            assertThat(
                "saves signing key in the storage",
                storage.get("signing-key." + DUMMY_USER_TOKEN),
                not(isEmptyString()));
            SigningKey savedKey = keyJsonAdapter.fromJson(storage.get("signing-key." + DUMMY_USER_TOKEN));
            assertEquals(key.getId(), savedKey.getId());
            assertEquals(key.getSecret(), savedKey.getSecret());
            assertEquals(0, key.getExpiresAt().compareTo(savedKey.getExpiresAt()));
        }
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(manifest= Config.NONE, sdk = {Build.VERSION_CODES.P})
    public static class WithPersistentStorage {
        PersistentStorage storage;
        SigningKeychain keychain;
        SharedPreferences preferences;

        @Before
        public void beforeSetup() {
            Context context = ApplicationProvider.getApplicationContext();
            preferences = context.getSharedPreferences("com.fjuul.sdk.persistence", Context.MODE_PRIVATE);
            storage = new PersistentStorage(context);
            keychain = new SigningKeychain(storage, DUMMY_USER_TOKEN);
        }

        @Test
        public void getValidKey_WithExpiredSigningKey_returnsEmptyOptional() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            Date expiresAt = calendar.getTime();
            preferences.edit().putString(
                "signing-key." + DUMMY_USER_TOKEN,
                keyJsonAdapter.toJson(new SigningKey("key-id", "REAL_SECRET", expiresAt)))
                .commit();
            assertFalse("returns empty optional", keychain.getValidKey().isPresent());
        }

        @Test
        public void getValidKey_WithValidSigningKey_returnsOptionalWithKey() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            preferences.edit().putString("signing-key." + DUMMY_USER_TOKEN, keyJsonAdapter.toJson(key)).commit();
            Optional<SigningKey> result = keychain.getValidKey();
            assertTrue("returns non-empty optional", result.isPresent());
            assertEquals("returns stored key", key.getId(), result.get().getId());
            assertEquals("returns stored key", key.getSecret(), result.get().getSecret());
        }

        @Test
        public void setKey_WithNotNullValue_setsKeyInStorage() throws IOException {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            keychain.setKey(key);

            assertThat(
                "saves signing key in the shared prefs",
                preferences.getString("signing-key." + DUMMY_USER_TOKEN, null),
                not(isEmptyString()));
            SigningKey savedKey = keyJsonAdapter.fromJson(storage.get("signing-key." + DUMMY_USER_TOKEN));
            assertEquals(key.getId(), savedKey.getId());
            assertEquals(key.getSecret(), savedKey.getSecret());
            assertEquals(0, key.getExpiresAt().compareTo(savedKey.getExpiresAt()));
        }

    }
}
