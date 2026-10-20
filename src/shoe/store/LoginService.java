package shoe.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class LoginService {
    private LoginService() {
    }

    static boolean authenticate(String username, char[] password) throws SQLException {
        return authenticate(Database::getConnection, username, password);
    }

    static boolean authenticate(ConnectionProvider connections, String username, char[] password)
            throws SQLException {
        if (username == null || username.trim().isEmpty() || password == null || password.length == 0) {
            return false;
        }
        String sql = "SELECT 1 FROM dbo.login WHERE username = ? AND password = ?";
        try (Connection connection = connections.open();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(10);
            statement.setString(1, username.trim());
            statement.setString(2, new String(password));
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection open() throws SQLException;
    }
}
