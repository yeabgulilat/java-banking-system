package com.habeshabank.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing utility.
 *
 * Current implementation: SHA-256 + random salt (Base64 encoded).
 * The stored format is:  "SALT$HASH"
 *
 * This is intentionally swappable — replace the hash/verify methods with
 * BCrypt (e.g. jBCrypt library) when that dependency is added, without
 * changing any call-sites in AuthService.
 *
 * No UI or database dependencies.
 */
public final class PasswordUtil {

    private static final String ALGORITHM   = "SHA-256";
    private static final int    SALT_BYTES  = 16;
    private static final String SEPARATOR   = "$";

    private PasswordUtil() { /* utility class */ }

    /**
     * Hashes a plain-text password with a new random salt.
     *
     * @param plainPassword the raw password from the user
     * @return  "SALT$HASH" — store this string; never the original password
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank.");
        }
        byte[] salt     = generateSalt();
        String saltB64  = Base64.getEncoder().encodeToString(salt);
        String hashB64  = sha256(saltB64 + plainPassword);
        return saltB64 + SEPARATOR + hashB64;
    }

    /**
     * Verifies a plain-text password against a stored hash.
     *
     * @param plainPassword  the password to check
     * @param storedHash     the value previously returned by {@link #hash}
     * @return true if the password matches
     */
    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        String[] parts = storedHash.split("\\" + SEPARATOR, 2);
        if (parts.length != 2) return false;
        String saltB64       = parts[0];
        String expectedHash  = parts[1];
        String actualHash    = sha256(saltB64 + plainPassword);
        return constantTimeEquals(expectedHash, actualHash);
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static String sha256(String input) {
        try {
            MessageDigest md     = MessageDigest.getInstance(ALGORITHM);
            byte[]        digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in standard Java
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Constant-time string comparison to prevent timing attacks. */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}