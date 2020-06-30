package com.fjuul.sdk.entities;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SigningKeychain {
    ConcurrentLinkedDeque<SigningKey> signingKeys;

    public SigningKeychain() {
        this.signingKeys = new ConcurrentLinkedDeque<>();
    }

    public SigningKeychain(SigningKey key) {
        this();
        signingKeys.add(key);
    }

    public void appendKey(SigningKey key) {
        // TODO: check if the key is valid
        signingKeys.addFirst(key);
    }

    public Boolean invalidateKeyById(String id) {
        Optional<SigningKey> keyToInvalidate =
                signingKeys.stream().filter(key -> key.getId().equals(id)).findFirst();
        if (keyToInvalidate.isPresent()) {
            keyToInvalidate.get().invalidate();
            return true;
        } else {
            return false;
        }
    }

    public Optional<SigningKey> getFirstValid() {
        return signingKeys.stream().filter(key -> key.isValid()).findFirst();
    }
}
