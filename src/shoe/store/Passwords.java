package shoe.store;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

final class Passwords {
    private static final String PREFIX = "pbkdf2-sha256$";
    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private Passwords() {
    }

    static String hash(char[] password) {
        validateNew(password);
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        try {
            return PREFIX + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt)
                    + "$" + Base64.getEncoder().encodeToString(derived);
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    static boolean isEncoded(String stored) {
        return stored != null && stored.regionMatches(true, 0, "pbkdf2-", 0, 7);
    }

    static boolean verify(char[] password, String stored) {
        if (password == null || password.length == 0 || stored == null
                || stored.length() > 255 || !stored.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = stored.split("\\$", -1);
        if (parts.length != 4 || !parts[1].matches("[0-9]{6,7}")) {
            return false;
        }
        byte[] actual = null;
        byte[] expected = null;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATIONS || iterations > 1_200_000) {
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);
            if (salt.length != SALT_BYTES || expected.length != HASH_BYTES) {
                return false;
            }
            actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException exception) {
            return false;
        } finally {
            if (actual != null) {
                Arrays.fill(actual, (byte) 0);
            }
            if (expected != null) {
                Arrays.fill(expected, (byte) 0);
            }
        }
    }

    static boolean verifyLegacy(char[] password, String stored) {
        if (password == null || stored == null || isEncoded(stored)) {
            return false;
        }
        byte[] entered = new String(password).getBytes(StandardCharsets.UTF_8);
        byte[] expected = stored.getBytes(StandardCharsets.UTF_8);
        try {
            return MessageDigest.isEqual(expected, entered);
        } finally {
            Arrays.fill(entered, (byte) 0);
            Arrays.fill(expected, (byte) 0);
        }
    }

    static void validateNew(char[] password) {
        if (password == null || password.length < 12 || password.length > 128) {
            throw new IllegalArgumentException("Password baru harus berisi 12 sampai 128 karakter.");
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 tidak tersedia pada Java ini.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
