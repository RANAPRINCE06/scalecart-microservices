package com.nahid.userservice.security;


import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;



public class AdvancedPasswordHasher {


    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int SALT_BYTES = 32; // 256 bits
    private static final int HASH_BYTES = 64; // 512 bits
    private static final int ITERATIONS = 310000; // High iteration count for security
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String FORMAT_PATTERN = "%s:%d:%s:%s";

    public String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        try {
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, HASH_BYTES);

            return String.format(FORMAT_PATTERN,
                    ALGORITHM,
                    ITERATIONS,
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(hash));

        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }


    public boolean verifyPassword(String password, String storedHash) {
        try {
            String[] parts = storedHash.split(":");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid stored hash format");
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] hash = Base64.getDecoder().decode(parts[3]);

            byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, hash.length);

            return constantTimeEquals(hash, testHash);

        } catch (Exception e) {
            throw new RuntimeException("Error verifying password", e);
        }
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }

        return result == 0;
    }
}
