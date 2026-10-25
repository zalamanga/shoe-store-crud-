package shoe.store;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class LoginServiceTest {
    public static void main(String[] args) throws Exception {
        rejectsEmptyInputWithoutOpeningDatabase();
        authenticatesWithBoundParameters();
        rejectsUnknownCredentials();
        closesResourcesAfterQueryFailure();
        closesConnectionAfterPrepareFailure();
        propagatesConnectionFailure();
        rejectsLegacyByDefault();
        permitsExplicitLegacyTransition();
        malformedHashNeverFallsBack();
        rejectsDuplicateUsernames();
        System.out.println("LoginServiceTest: 10 checks passed");
    }

    private static void rejectsEmptyInputWithoutOpeningDatabase() throws SQLException {
        LoginService.ConnectionProvider unopened = () -> {
            throw new AssertionError("Empty credentials must not open a connection");
        };
        check(!LoginService.authenticate(unopened, "  ", "secret".toCharArray()), "Blank username");
        check(!LoginService.authenticate(unopened, null, "secret".toCharArray()), "Null username");
        check(!LoginService.authenticate(unopened, "admin", new char[0]), "Empty password");
        check(!LoginService.authenticate(unopened, "admin", null), "Null password");
    }

    private static void authenticatesWithBoundParameters() throws SQLException {
        JdbcStub jdbc = new JdbcStub(true);
        String user = "name' OR 1=1 --";
        String password = " pass'word-long ";
        jdbc.stored = Passwords.hash(password.toCharArray());
        check(LoginService.authenticate(jdbc::connection, "  " + user + "  ", password.toCharArray()),
                "Matching credentials");
        check(!jdbc.sql.contains(user), "Username must be a parameter");
        check(!jdbc.sql.contains(password), "Password must be a parameter");
        check(user.equals(jdbc.parameters.get(1)), "Username is trimmed before lookup");
        check(jdbc.parameters.size() == 1, "Password never becomes a SQL parameter");
        check(jdbc.sql.equals("SELECT password FROM dbo.login WHERE username = ?"), "Fetch by username only");
        check(jdbc.timeout > 0, "Query has a timeout");
        jdbc.assertClosed(true);
    }

    private static void rejectsUnknownCredentials() throws SQLException {
        JdbcStub jdbc = new JdbcStub(false);
        check(!LoginService.authenticate(jdbc::connection, "unknown", "wrong".toCharArray()),
                "Unknown credentials are rejected");
        jdbc.assertClosed(true);
    }

    private static void closesResourcesAfterQueryFailure() {
        JdbcStub jdbc = new JdbcStub(false);
        jdbc.queryFailure = true;
        expectSqlException(() -> LoginService.authenticate(jdbc::connection, "admin", "pw".toCharArray()));
        jdbc.assertClosed(false);
    }

    private static void closesConnectionAfterPrepareFailure() {
        JdbcStub jdbc = new JdbcStub(false);
        jdbc.prepareFailure = true;
        expectSqlException(() -> LoginService.authenticate(jdbc::connection, "admin", "pw".toCharArray()));
        check(jdbc.connectionClosed, "Connection is closed when prepare fails");
    }

    private static void propagatesConnectionFailure() {
        expectSqlException(() -> LoginService.authenticate(() -> {
            throw new SQLException("Test connection failure");
        }, "admin", "pw".toCharArray()));
    }

    private static void rejectsLegacyByDefault() throws SQLException {
        JdbcStub jdbc = new JdbcStub(true);
        jdbc.stored = "old-secret";
        check(LoginService.check(jdbc::connection, "legacy", "old-secret".toCharArray(), false)
                == LoginService.Result.LEGACY_DISABLED, "Default requires explicit migration");
        jdbc.assertClosed(true);
    }

    private static void permitsExplicitLegacyTransition() throws SQLException {
        JdbcStub jdbc = new JdbcStub(true);
        jdbc.stored = " old'secret ";
        check(LoginService.check(jdbc::connection, "legacy", " old'secret ".toCharArray(), true)
                == LoginService.Result.LEGACY_SUCCESS, "Explicit transition preserves password whitespace");
        JdbcStub wrong = new JdbcStub(true);
        wrong.stored = jdbc.stored;
        check(LoginService.check(wrong::connection, "legacy", "wrong".toCharArray(), true)
                == LoginService.Result.INVALID, "Legacy wrong password rejected");
    }

    private static void malformedHashNeverFallsBack() throws SQLException {
        JdbcStub jdbc = new JdbcStub(true);
        jdbc.stored = "pbkdf2-sha256$broken";
        check(LoginService.check(jdbc::connection, "admin", jdbc.stored.toCharArray(), true)
                == LoginService.Result.INVALID, "Malformed hash cannot authenticate as literal plaintext");
    }

    private static void rejectsDuplicateUsernames() {
        JdbcStub jdbc = new JdbcStub(true);
        jdbc.duplicate = true;
        expectSqlException(() -> LoginService.authenticate(jdbc::connection, "duplicate", "secret".toCharArray()));
        jdbc.assertClosed(true);
    }

    private static void expectSqlException(SqlAction action) {
        try {
            action.run();
            throw new AssertionError("Expected SQLException");
        } catch (SQLException expected) {
            // Connection failures must reach the UI instead of looking like a wrong password.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        boolean run() throws SQLException;
    }

    private static final class JdbcStub {
        final Map<Integer, String> parameters = new HashMap<>();
        final boolean match;
        String sql;
        int timeout;
        boolean connectionClosed;
        boolean statementClosed;
        boolean resultClosed;
        boolean queryFailure;
        boolean prepareFailure;
        boolean duplicate;
        String stored;

        JdbcStub(boolean match) {
            this.match = match;
        }

        Connection connection() {
            return proxy(Connection.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "prepareStatement":
                        if (prepareFailure) {
                            throw new SQLException("Test prepare failure");
                        }
                        sql = (String) args[0];
                        return statement();
                    case "close":
                        connectionClosed = true;
                        return null;
                    default:
                        throw new AssertionError("Unexpected Connection method: " + method.getName());
                }
            });
        }

        PreparedStatement statement() {
            return proxy(PreparedStatement.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "setString":
                        parameters.put((Integer) args[0], (String) args[1]);
                        return null;
                    case "setQueryTimeout":
                        timeout = (Integer) args[0];
                        return null;
                    case "executeQuery":
                        if (queryFailure) {
                            throw new SQLException("Test query failure");
                        }
                        return rows();
                    case "close":
                        statementClosed = true;
                        return null;
                    default:
                        throw new AssertionError("Unexpected statement method: " + method.getName());
                }
            });
        }

        ResultSet rows() {
            int[] cursor = {0};
            return proxy(ResultSet.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "next":
                        return match && ++cursor[0] <= (duplicate ? 2 : 1);
                    case "getString":
                        return stored;
                    case "close":
                        resultClosed = true;
                        return null;
                    default:
                        throw new AssertionError("Unexpected ResultSet method: " + method.getName());
                }
            });
        }

        void assertClosed(boolean expectResults) {
            check(connectionClosed, "Connection closed");
            check(statementClosed, "Statement closed");
            check(resultClosed == expectResults, "ResultSet lifetime");
        }

        private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
            return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
        }
    }
}
