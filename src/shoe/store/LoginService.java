package shoe.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class LoginService {
    private LoginService() {
    }

    static boolean authenticate(String username, char[] password) throws SQLException {
        Result result = check(Database::getConnection, username, password, Database.allowLegacyPasswords());
        return result == Result.SUCCESS || result == Result.LEGACY_SUCCESS;
    }

    static boolean authenticate(ConnectionProvider connections, String username, char[] password)
            throws SQLException {
        return check(connections, username, password, false) == Result.SUCCESS;
    }

    static Result check(ConnectionProvider connections, String username, char[] password,
            boolean allowLegacy) throws SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            return Result.INVALID;
        }
        String sql = "SELECT password FROM dbo.login WHERE username = ?";
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            statement.setString(1, username.trim());
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Result.INVALID;
                }
                String stored = rows.getString("password");
                if (rows.next()) {
                    throw new SQLException("dbo.login.username harus unik.");
                }
                if (Passwords.isEncoded(stored)) {
                    return Passwords.verify(password, stored) ? Result.SUCCESS : Result.INVALID;
                }
                if (!allowLegacy) {
                    return Result.LEGACY_DISABLED;
                }
                return Passwords.verifyLegacy(password, stored) ? Result.LEGACY_SUCCESS : Result.INVALID;
            }
        }
    }

    enum Result {
        SUCCESS, INVALID, LEGACY_DISABLED, LEGACY_SUCCESS
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }
}
