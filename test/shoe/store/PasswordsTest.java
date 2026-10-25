package shoe.store;

import java.util.Base64;

public final class PasswordsTest {
    public static void main(String[] args) {
        char[] secret = "  sepatu-toko-2026  ".toCharArray();
        String first = Passwords.hash(secret);
        String second = Passwords.hash(secret);
        check(!first.equals(second), "Each account gets a fresh random salt");
        check(first.length() <= 100 && !first.contains("sepatu"), "Encoding fits old 255-character column");
        check(Passwords.verify(secret, first), "Correct password and significant spaces");
        check(!Passwords.verify("sepatu-toko-2026".toCharArray(), first), "Whitespace is significant");
        check(!Passwords.verify("incorrect-secret".toCharArray(), first), "Wrong password rejected");
        check(!Passwords.verify(secret, "plain-password"), "Plaintext is not an encoded hash");
        for (String malformed : new String[]{
            "pbkdf2-sha256$599999$AAAA$AAAA", "pbkdf2-sha256$9999999$AAAA$AAAA",
            "pbkdf2-sha256$600000$!$!", "pbkdf2-sha256$600000$$", "pbkdf2-other$600000$a$b",
            "PBKDF2-SHA256$broken",
            "pbkdf2-sha256$600000$" + Base64.getEncoder().encodeToString(new byte[16]) + "$AA=="
        }) {
            check(!Passwords.verify(secret, malformed), "Malformed/bounded parameters rejected");
            check(!Passwords.verifyLegacy(malformed.toCharArray(), malformed), "No malformed hash fallback");
        }
        expectInvalid(new char[11]);
        expectInvalid(new char[129]);
        expectInvalid(null);
        check(Passwords.verifyLegacy("old secret".toCharArray(), "old secret"), "Explicit legacy comparison");
        check(!Passwords.verifyLegacy("wrong".toCharArray(), "old secret"), "Legacy wrong secret");
        System.out.println("PasswordsTest: salted hash, bounds, significant spaces, malformed formats and legacy checks passed");
    }

    private static void expectInvalid(char[] secret) {
        try {
            Passwords.hash(secret);
            throw new AssertionError("Expected password length rejection");
        } catch (IllegalArgumentException expected) {
            // A new account must never store a short or accidentally unbounded password.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
