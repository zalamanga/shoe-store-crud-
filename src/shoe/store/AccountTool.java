package shoe.store;

import java.io.Console;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AccountTool {
    private AccountTool() {
    }

    public static void main(String[] args) {
        if (args.length != 2 || !("create".equals(args[0]) || "migrate".equals(args[0]))) {
            System.err.println("Pemakaian: java -cp \"dist/shoe-store.jar;lib/*\" "
                    + "shoe.store.AccountTool create|migrate USERNAME");
            System.exit(2);
        }
        Console console = System.console();
        if (console == null) {
            System.err.println("Jalankan langsung dari terminal interaktif. Password tidak diterima "
                    + "melalui argumen, pipe, atau environment.");
            System.exit(2);
        }
        char[] oldPassword = null;
        char[] newPassword = null;
        char[] confirmation = null;
        int exitCode = 0;
        try {
            if ("migrate".equals(args[0])) {
                oldPassword = console.readPassword("Password lama akun %s: ", args[1]);
            }
            newPassword = console.readPassword("Password baru (12..128 karakter): ");
            confirmation = console.readPassword("Ulangi password baru: ");
            if (newPassword == null || confirmation == null || !Arrays.equals(newPassword, confirmation)) {
                throw new IllegalArgumentException("Konfirmasi password tidak sama atau input dibatalkan.");
            }
            if ("create".equals(args[0])) {
                create(Database::getConnection, args[1], newPassword);
            } else {
                migrate(Database::getConnection, args[1], oldPassword, newPassword);
            }
            console.printf("Akun %s: hash password tersimpan. Tidak ada akun lain yang diubah.%n", args[1]);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            System.err.println(exception.getMessage());
            exitCode = 1;
        } catch (SQLException exception) {
            System.err.println("Hasil operasi belum terkonfirmasi. Periksa akun sebelum mengulangi. "
                    + "Periksa konfigurasi SQL Server, tabel dbo.login, "
                    + "kapasitas password minimal 100 karakter dan indeks unik username. "
                    + "SQLState=" + exception.getSQLState() + ", kode=" + exception.getErrorCode());
            exitCode = 1;
        } finally {
            wipe(oldPassword);
            wipe(newPassword);
            wipe(confirmation);
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static void create(LoginService.ConnectionProvider connections, String username, char[] password)
            throws SQLException {
        String name = username(username);
        String encoded = Passwords.hash(password);
        try (Connection connection = connections.open()) {
            verifySchema(connection, encoded.length());
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO dbo.login (username, password) VALUES (?, ?)")) {
                statement.setQueryTimeout(10);
                statement.setString(1, name);
                statement.setString(2, encoded);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("Tidak tepat satu akun yang dibuat.");
                }
            }
        }
    }

    static void migrate(LoginService.ConnectionProvider connections, String username,
            char[] oldPassword, char[] newPassword) throws SQLException {
        String name = username(username);
        String encoded = Passwords.hash(newPassword);
        try (Connection connection = connections.open()) {
            verifySchema(connection, encoded.length());
            connection.setAutoCommit(false);
            try {
                String stored;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT password FROM dbo.login WITH (UPDLOCK, HOLDLOCK) WHERE username = ?")) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, name);
                    try (ResultSet rows = statement.executeQuery()) {
                        if (!rows.next()) {
                            throw new IllegalArgumentException("Akun tidak ditemukan.");
                        }
                        stored = rows.getString("password");
                        if (rows.next()) {
                            throw new IllegalArgumentException("Username tidak unik. Rapikan data dan indeks lebih dulu.");
                        }
                    }
                }
                if (Passwords.isEncoded(stored)) {
                    throw new IllegalArgumentException("Akun sudah memakai format hash. Migrasi hanya untuk akun lama.");
                }
                if (!Passwords.verifyLegacy(oldPassword, stored)) {
                    throw new IllegalArgumentException("Password lama tidak sesuai. Akun tidak diubah.");
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE dbo.login SET password = ? WHERE username = ?")) {
                    statement.setQueryTimeout(10);
                    statement.setString(1, encoded);
                    statement.setString(2, name);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Migrasi harus mengubah tepat satu akun.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollback) {
                    exception.addSuppressed(rollback);
                }
                throw exception;
            }
        }
    }

    private static void verifySchema(Connection connection, int requiredLength) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        boolean capacity = false;
        try (ResultSet columns = metadata.getColumns(connection.getCatalog(), "dbo", "login", "password")) {
            while (columns.next()) {
                int type = columns.getInt("DATA_TYPE");
                if ((type == Types.NVARCHAR || type == Types.VARCHAR)
                        && columns.getInt("COLUMN_SIZE") >= requiredLength) {
                    capacity = true;
                }
            }
        }
        if (!capacity) {
            throw new IllegalArgumentException("Kolom dbo.login.password memerlukan VARCHAR/NVARCHAR "
                    + "dengan kapasitas hash yang cukup. "
                    + "Cadangkan database lalu ubah kolom menjadi NVARCHAR(255); tool tidak mengubah skema.");
        }
        Map<String, List<String>> indexes = new HashMap<>();
        try (ResultSet rows = metadata.getIndexInfo(connection.getCatalog(), "dbo", "login", true, false)) {
            while (rows.next()) {
                String index = rows.getString("INDEX_NAME");
                String column = rows.getString("COLUMN_NAME");
                String filter = rows.getString("FILTER_CONDITION");
                if (index != null && column != null && !rows.getBoolean("NON_UNIQUE")) {
                    indexes.computeIfAbsent(index, key -> new ArrayList<>()).add(
                            filter == null || filter.trim().isEmpty() ? column : "<filtered>");
                }
            }
        }
        boolean unique = indexes.values().stream().anyMatch(
                columns -> columns.size() == 1 && "username".equalsIgnoreCase(columns.get(0)));
        if (!unique) {
            throw new IllegalArgumentException("dbo.login memerlukan PRIMARY KEY atau UNIQUE tanpa filter "
                    + "pada username saja. Periksa duplikat dan buat indeks setelah mencadangkan database.");
        }
    }

    private static String username(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 100) {
            throw new IllegalArgumentException("Username wajib diisi, maksimal 100 karakter.");
        }
        return value.trim();
    }

    private static void wipe(char[] value) {
        if (value != null) {
            Arrays.fill(value, '\0');
        }
    }
}
