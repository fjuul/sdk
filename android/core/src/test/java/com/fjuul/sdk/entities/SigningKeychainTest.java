package com.fjuul.sdk.entities;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

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
            assertFalse(
                "saves signing key in the storage",
                storage.get("signing-key." + DUMMY_USER_TOKEN).isEmpty());
            SigningKey savedKey = keyJsonAdapter.fromJson(storage.get("signing-key." + DUMMY_USER_TOKEN));
            assertEquals(key.getId(), savedKey.getId());
            assertEquals(key.getSecret(), savedKey.getSecret());
            assertEquals(0, key.getExpiresAt().compareTo(savedKey.getExpiresAt()));
        }
    }

    // TODO: add tests with persistent storage
//    public static class WithPersistentStorage {
//        PersistentStorage storage;
//        SigningKeychain keychain;
//
//        @Before
//        public void beforeSetup() {
//            storage = new PersistentStorage();
//            keychain = new SigningKeychain(storage, DUMMY_USER_TOKEN);
//        }
//    }

//    public static class InvalidateKeyByIdTest {
//        @Test
//        public void invalidateKeyById_KeyNotFound_returnsFalse() {
//            SigningKeychain keychain = new SigningKeychain(new SigningKey("key-id", "REAL_SECRET", new Date()));
//            boolean result = keychain.invalidateKeyById("wrong-key-id");
//            assertFalse("key not found", result);
//        }
//
//        @Test
//        public void invalidateKeyById_KeyWasFound_returnsTrue() {
//            SigningKey key = new SigningKey("key-id", "REAL_SECRET", new Date());
//            SigningKeychain keychain = new SigningKeychain(key);
//            assertTrue(key.isValid());
//            boolean result = keychain.invalidateKeyById("key-id");
//            assertTrue("key was found", result);
//            assertFalse("invalidate the found key", key.isValid());
//        }
//    }
}
