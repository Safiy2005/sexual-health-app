package com.sddp.sexualhealthapp.security;

import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Service class for secure local storage of sensitive data.
 * Uses Java Preferences API for cross-platform persistence and BCrypt for hashing.
 *
 * Note: This is an educational implementation. Production systems would require
 * additional security measures such as hardware security modules and key rotation.
 *
 * @author SDDP Group 30
 * @version 1.0
 */
public class SecureStorage {

    private static final String PREFS_NODE = "/com/sddp/sexualhealthapp";
    private final Preferences preferences;

    /**
     * Constructs a new SecureStorage instance using the application's preference node.
     */
    public SecureStorage() {
        this.preferences = Preferences.userRoot().node(PREFS_NODE);
    }

    /**
     * Constructs a SecureStorage instance with a custom preferences node (for testing).
     *
     * @param preferences the preferences node to use
     */
    public SecureStorage(Preferences preferences) {
        this.preferences = preferences;
    }

    /**
     * Saves a value associated with a key.
     * For sensitive values, they should be hashed before storage.
     *
     * @param key the key to store the value under
     * @param value the value to store
     * @return true if the save was successful, false otherwise
     */
    public boolean save(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        if (value == null) {
            return false;
        }

        try {
            preferences.put(key, value);
            preferences.flush();
            return true;
        } catch (BackingStoreException e) {
            System.err.println("Failed to save data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Saves a hashed version of a sensitive value.
     * Uses BCrypt for secure hashing.
     *
     * @param key the key to store the hash under
     * @param plainValue the plain value to hash and store
     * @return true if the save was successful, false otherwise
     */
    public boolean saveHashed(String key, String plainValue) {
        if (plainValue == null) {
            return false;
        }

        try {
            String hash = BCrypt.hashpw(plainValue, BCrypt.gensalt(12));
            return save(key, hash);
        } catch (Exception e) {
            System.err.println("Failed to hash and save data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads a value associated with a key.
     *
     * @param key the key to retrieve
     * @return an Optional containing the value if found, empty otherwise
     */
    public Optional<String> load(String key) {
        if (key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }

        String value = preferences.get(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * Verifies a plain value against a stored hash.
     * Used for authentication purposes.
     *
     * @param key the key where the hash is stored
     * @param plainValue the plain value to verify
     * @return true if the plain value matches the stored hash, false otherwise
     */
    public boolean verifyHash(String key, String plainValue) {
        if (plainValue == null) {
            return false;
        }

        Optional<String> storedHash = load(key);
        if (storedHash.isEmpty()) {
            return false;
        }

        try {
            return BCrypt.checkpw(plainValue, storedHash.get());
        } catch (Exception e) {
            System.err.println("Failed to verify hash: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a key exists in storage.
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    public boolean exists(String key) {
        return load(key).isPresent();
    }

    /**
     * Deletes a value associated with a key.
     *
     * @param key the key to delete
     * @return true if the deletion was successful, false otherwise
     */
    public boolean delete(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }

        try {
            preferences.remove(key);
            preferences.flush();
            return true;
        } catch (BackingStoreException e) {
            System.err.println("Failed to delete data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears all stored data in this preference node.
     * Use with caution!
     *
     * @return true if the clear was successful, false otherwise
     */
    public boolean clearAll() {
        try {
            preferences.clear();
            preferences.flush();
            return true;
        } catch (BackingStoreException e) {
            System.err.println("Failed to clear all data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets all keys currently stored in this preference node.
     *
     * @return array of key strings, or empty array if error occurs
     */
    public String[] getAllKeys() {
        try {
            return preferences.keys();
        } catch (BackingStoreException e) {
            System.err.println("Failed to retrieve keys: " + e.getMessage());
            return new String[0];
        }
    }
}
