package shoe.store;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AccountToolTest {
    private static final char[] NEW_PASSWORD = "synthetic-new-password".toCharArray();

    public static void main(String[] args) throws Exception {
        createsHashOnly();
        migratesOneLockedAccount();
        rejectsWrongOldSecret();
        rejectsEncodedOrMissingAccount();
        rejectsInsufficientSchema();
        rollsBackUpdateFailure();
        rejectsShortPasswordBeforeOpeningConnection();
        System.out.println("AccountToolTest: 7 account creation/migration and rollback groups passed");
    }

    private static void createsHashOnly() throws SQLException {
        JdbcStub jdbc = new JdbcStub();
        AccountTool.create(jdbc::connection, "  user'one  ", NEW_PASSWORD);
        check(jdbc.inserts == 1 && jdbc.updates == 0, "Create inserts exactly one account");
        check("user'one".equals(jdbc.lastParameters.get(1)), "Trimmed username is bound data");
        check(Passwords.verify(NEW_PASSWORD, jdbc.stored), "Only the derived password is stored");
        check(!jdbc.lastSql.contains("user'one"), "Username never enters SQL text");
        check(jdbc.closed && jdbc.resourcesOpened == jdbc.resourcesClosed, "Create closes JDBC resources");
    }

    private static void migratesOneLockedAccount() throws SQLException {
        JdbcStub jdbc = new JdbcStub();
        jdbc.stored = "old password";
        AccountTool.migrate(jdbc::connection, "legacy", "old password".toCharArray(), NEW_PASSWORD);
        check(jdbc.lockedRead && jdbc.updates == 1 && jdbc.committed, "Migration locks and commits one row");
        check(!jdbc.rolledBack && Passwords.verify(NEW_PASSWORD, jdbc.stored), "Replacement is a valid hash");
        check("legacy".equals(jdbc.lastParameters.get(2)), "Only named account is updated");
        check(jdbc.closed && jdbc.resourcesOpened == jdbc.resourcesClosed, "Migration closes all JDBC resources");
    }

    private static void rejectsWrongOldSecret() throws SQLException {
        JdbcStub jdbc = new JdbcStub();
        expectRejected(() -> AccountTool.migrate(jdbc::connection, "legacy", "incorrect".toCharArray(), NEW_PASSWORD));
        check(jdbc.updates == 0 && jdbc.rolledBack && !jdbc.committed, "Wrong old secret makes no change");
        check("legacy-password".equals(jdbc.stored), "Wrong old secret retains original record");
    }

    private static void rejectsEncodedOrMissingAccount() throws SQLException {
        JdbcStub encoded = new JdbcStub();
        encoded.stored = "pbkdf2-sha256$broken";
        expectRejected(() -> AccountTool.migrate(encoded::connection, "legacy", encoded.stored.toCharArray(), NEW_PASSWORD));
        check(encoded.updates == 0 && encoded.rolledBack, "Malformed encoded account cannot be treated as legacy");
        JdbcStub missing = new JdbcStub();
        missing.present = false;
        expectRejected(() -> AccountTool.migrate(missing::connection, "missing", "old".toCharArray(), NEW_PASSWORD));
        check(missing.updates == 0 && missing.rolledBack, "Missing account never inserts during migration");
    }

    private static void rejectsInsufficientSchema() throws SQLException {
        JdbcStub shortColumn = new JdbcStub();
        shortColumn.capacity = 20;
        expectRejected(() -> AccountTool.create(shortColumn::connection, "new", NEW_PASSWORD));
        check(shortColumn.inserts == 0 && shortColumn.closed, "Short password column fails before write");
        JdbcStub paddedColumn = new JdbcStub();
        paddedColumn.passwordType = Types.NCHAR;
        expectRejected(() -> AccountTool.create(paddedColumn::connection, "new", NEW_PASSWORD));
        check(paddedColumn.inserts == 0, "Fixed-width column cannot pad and invalidate stored hashes");
        JdbcStub nonUnique = new JdbcStub();
        nonUnique.unique = false;
        expectRejected(() -> AccountTool.migrate(nonUnique::connection, "legacy", "legacy-password".toCharArray(), NEW_PASSWORD));
        check(nonUnique.updates == 0 && !nonUnique.lockedRead, "Non-unique custom table fails before row migration");
        JdbcStub composite = new JdbcStub();
        composite.composite = true;
        expectRejected(() -> AccountTool.create(composite::connection, "new", NEW_PASSWORD));
        check(composite.inserts == 0, "Composite key is not a unique username guarantee");
        JdbcStub filtered = new JdbcStub();
        filtered.filtered = true;
        expectRejected(() -> AccountTool.create(filtered::connection, "new", NEW_PASSWORD));
        check(filtered.inserts == 0, "Filtered unique index is insufficient");
    }

    private static void rollsBackUpdateFailure() throws SQLException {
        JdbcStub jdbc = new JdbcStub();
        jdbc.failUpdate = true;
        try {
            AccountTool.migrate(jdbc::connection, "legacy", "legacy-password".toCharArray(), NEW_PASSWORD);
            throw new AssertionError("Expected simulated update failure");
        } catch (SQLException expected) {
            check(jdbc.rolledBack && !jdbc.committed, "Failed write rolls back");
            check("legacy-password".equals(jdbc.stored), "Failed write preserves stored secret");
        }
        check(jdbc.resourcesOpened == jdbc.resourcesClosed, "Failed migration closes resources");
    }

    private static void rejectsShortPasswordBeforeOpeningConnection() throws SQLException {
        expectRejected(() -> AccountTool.create(() -> {
            throw new AssertionError("Invalid new password must not open a database");
        }, "new", "short".toCharArray()));
    }

    private static void expectRejected(SqlAction action) throws SQLException {
        try {
            action.run();
            throw new AssertionError("Expected controlled account rejection");
        } catch (IllegalArgumentException expected) {
            check(!expected.getMessage().contains("legacy-password"), "Error does not include a secret");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }

    private static final class JdbcStub {
        String stored = "legacy-password";
        String pending;
        String lastSql;
        Map<Integer, String> lastParameters = new HashMap<>();
        boolean transaction;
        boolean closed;
        boolean committed;
        boolean rolledBack;
        boolean lockedRead;
        boolean present = true;
        boolean unique = true;
        boolean composite;
        boolean filtered;
        boolean failUpdate;
        int capacity = 255;
        int passwordType = Types.NVARCHAR;
        int inserts;
        int updates;
        int resourcesOpened;
        int resourcesClosed;

        Connection connection() {
            return proxy(Connection.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "getCatalog": return "synthetic";
                    case "getMetaData": return metadata();
                    case "setAutoCommit": transaction = !(Boolean) args[0]; return null;
                    case "prepareStatement": return statement((String) args[0]);
                    case "commit": stored = pending; committed = true; return null;
                    case "rollback": pending = null; rolledBack = true; return null;
                    case "close": closed = true; return null;
                    default: throw new AssertionError("Unexpected Connection method: " + method.getName());
                }
            });
        }

        DatabaseMetaData metadata() {
            return proxy(DatabaseMetaData.class, (object, method, args) -> {
                if ("getColumns".equals(method.getName())) {
                    return rows(List.of(row("DATA_TYPE", passwordType, "COLUMN_SIZE", capacity)));
                }
                if ("getIndexInfo".equals(method.getName())) {
                    List<Map<String, Object>> indexes = new ArrayList<>();
                    indexes.add(row("INDEX_NAME", "PK_login", "COLUMN_NAME", "username", "NON_UNIQUE", !unique,
                            "FILTER_CONDITION", filtered ? "username IS NOT NULL" : null));
                    if (composite) {
                        indexes.add(row("INDEX_NAME", "PK_login", "COLUMN_NAME", "another_id", "NON_UNIQUE", false));
                    }
                    return rows(indexes);
                }
                throw new AssertionError("Unexpected metadata method: " + method.getName());
            });
        }

        PreparedStatement statement(String sql) {
            resourcesOpened++;
            lastSql = sql;
            Map<Integer, String> parameters = new LinkedHashMap<>();
            return proxy(PreparedStatement.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "setQueryTimeout": check((Integer) args[0] == 10, "Bound SQL timeout"); return null;
                    case "setString": parameters.put((Integer) args[0], (String) args[1]); return null;
                    case "executeQuery":
                        lockedRead = sql.contains("UPDLOCK, HOLDLOCK") && transaction;
                        return rows(present ? List.of(row("password", stored)) : List.of());
                    case "executeUpdate":
                        lastParameters = parameters;
                        if (failUpdate) { throw new SQLException("Synthetic write failure"); }
                        if (sql.startsWith("INSERT")) { inserts++; stored = parameters.get(2); }
                        else { updates++; pending = parameters.get(1); }
                        return 1;
                    case "close": resourcesClosed++; return null;
                    default: throw new AssertionError("Unexpected statement method: " + method.getName());
                }
            });
        }

        ResultSet rows(List<Map<String, Object>> values) {
            resourcesOpened++;
            int[] cursor = {-1};
            return proxy(ResultSet.class, (object, method, args) -> {
                switch (method.getName()) {
                    case "next": return ++cursor[0] < values.size();
                    case "getString": return (String) values.get(cursor[0]).get(args[0]);
                    case "getInt": return (Integer) values.get(cursor[0]).get(args[0]);
                    case "getBoolean": return (Boolean) values.get(cursor[0]).get(args[0]);
                    case "close": resourcesClosed++; return null;
                    default: throw new AssertionError("Unexpected ResultSet method: " + method.getName());
                }
            });
        }
    }

    private static Map<String, Object> row(Object... pairs) {
        Map<String, Object> row = new HashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            row.put((String) pairs[index], pairs[index + 1]);
        }
        return row;
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
}
