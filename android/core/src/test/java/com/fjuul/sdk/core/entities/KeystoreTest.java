package com.fjuul.sdk.core.entities;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import androidx.test.core.app.ApplicationProvider;

@RunWith(Enclosed.class)
public class KeystoreTest {
    public static final String DUMMY_USER_TOKEN = "USER_TOKEN";
    public static final JsonAdapter<SigningKey> keyJsonAdapter = createKeyJsonAdapter();

    public static JsonAdapter<SigningKey> createKeyJsonAdapter() {
        Moshi moshi = new Moshi.Builder().add(Date.class, new Rfc3339DateJsonAdapter()).build();
        return moshi.adapter(SigningKey.class);
    }

    public static class WithInMemoryStorage {
        InMemoryStorage storage;
        Keystore keystore;

        @Before
        public void beforeSetup() {
            storage = new InMemoryStorage();
            keystore = new Keystore(storage);
        }

        @Test
        public void getValidKey_WithExpiredSigningKey_returnsEmptyOptional() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            Date expiresAt = calendar.getTime();
            storage.set("signing-key", keyJsonAdapter.toJson(new SigningKey("key-id", "REAL_SECRET", expiresAt)));
            assertFalse("returns empty optional", keystore.getValidKey().isPresent());
        }

        @Test
        public void getValidKey_WithValidSigningKey_returnsOptionalWithKey() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            storage.set("signing-key", keyJsonAdapter.toJson(key));
            Optional<SigningKey> result = keystore.getValidKey();
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
            keystore.setKey(key);
            assertThat("saves signing key in the storage", storage.get("signing-key"), not(is(emptyOrNullString())));
            SigningKey savedKey = keyJsonAdapter.fromJson(storage.get("signing-key"));
            assertEquals(key.getId(), savedKey.getId());
            assertEquals(key.getSecret(), savedKey.getSecret());
            assertEquals(0, key.getExpiresAt().compareTo(savedKey.getExpiresAt()));
        }
    }

    @RunWith(RobolectricTestRunner.class)
    @Config(sdk = {Build.VERSION_CODES.P})
    public static class WithPersistentStorage {
        PersistentStorage storage;
        Keystore keystore;
        SharedPreferences preferences;

        @Before
        public void beforeSetup() {
            Context context = ApplicationProvider.getApplicationContext();
            preferences =
                context.getSharedPreferences("com.fjuul.sdk.persistence." + DUMMY_USER_TOKEN, Context.MODE_PRIVATE);
            storage = new PersistentStorage(context, DUMMY_USER_TOKEN);
            keystore = new Keystore(storage);
        }

        @Test
        public void getValidKey_WithExpiredSigningKey_returnsEmptyOptional() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            Date expiresAt = calendar.getTime();
            preferences.edit()
                .putString("signing-key." + DUMMY_USER_TOKEN,
                    keyJsonAdapter.toJson(new SigningKey("key-id", "REAL_SECRET", expiresAt)))
                .commit();
            assertFalse("returns empty optional", keystore.getValidKey().isPresent());
        }

        @Test
        public void getValidKey_WithValidSigningKey_returnsOptionalWithKey() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            preferences.edit().putString("signing-key", keyJsonAdapter.toJson(key)).commit();
            Optional<SigningKey> result = keystore.getValidKey();
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
            keystore.setKey(key);

            assertThat("saves signing key in the shared prefs",
                preferences.getString("signing-key", null),
                not(is(emptyOrNullString())));
            SigningKey savedKey = keyJsonAdapter.fromJson(storage.get("signing-key"));
            assertEquals(key.getId(), savedKey.getId());
            assertEquals(key.getSecret(), savedKey.getSecret());
            assertEquals(0, key.getExpiresAt().compareTo(savedKey.getExpiresAt()));
        }

    }
}
