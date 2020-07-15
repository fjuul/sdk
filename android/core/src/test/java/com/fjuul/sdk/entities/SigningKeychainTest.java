package com.fjuul.sdk.entities;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

@RunWith(Enclosed.class)
public class SigningKeychainTest {

    public static class GetFirstValidTest {
        @Test
        public void getFirstValid_WithExpiredSigningKey_returnsEmptyOptional() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, -1);
            Date expiresAt = calendar.getTime();
            SigningKeychain keychain =
                    new SigningKeychain(new SigningKey("key-id", "REAL_SECRET", expiresAt));
            assertEquals("returns empty optional", false, keychain.getFirstValid().isPresent());
        }

        @Test
        public void getFirstValid_WithValidSigningKey_returnsOptionalWithKey() {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 1);
            Date expiresAt = calendar.getTime();
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", expiresAt);
            SigningKeychain keychain = new SigningKeychain(key);
            assertEquals("returns optional with key", key, keychain.getFirstValid().get());
        }
    }

    public static class InvalidateKeyByIdTest {
        @Test
        public void invalidateKeyById_KeyNotFound_returnsFalse() {
            SigningKeychain keychain =
                    new SigningKeychain(new SigningKey("key-id", "REAL_SECRET", new Date()));
            boolean result = keychain.invalidateKeyById("wrong-key-id");
            assertFalse("key not found", result);
        }

        @Test
        public void invalidateKeyById_KeyWasFound_returnsTrue() {
            SigningKey key = new SigningKey("key-id", "REAL_SECRET", new Date());
            SigningKeychain keychain = new SigningKeychain(key);
            assertTrue(key.isValid());
            boolean result = keychain.invalidateKeyById("key-id");
            assertTrue("key was found", result);
            assertFalse("invalidate the found key", key.isValid());
        }
    }
}
