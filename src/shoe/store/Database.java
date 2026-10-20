package shoe.store;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

public final class Database {
    private Database() {
    }

    public static Connection getConnection() throws SQLException {
        Path configPath = Paths.get(System.getProperty("shoe.db.config", "database.properties"));
        Properties config = loadConfiguration(configPath, System.getenv());
        return DriverManager.getConnection(config.getProperty("url"),
                config.getProperty("user"), config.getProperty("password"));
    }

    static Properties loadConfiguration(Path configPath, Map<String, String> environment)
            throws SQLException {
        Properties file = new Properties();
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
                file.load(reader);
            } catch (IOException | IllegalArgumentException ex) {
                throw new SQLException("Konfigurasi database tidak dapat dibaca. Periksa database.properties.", ex);
            }
        }

        Properties config = new Properties();
        config.setProperty("url", required(file, environment, "db.url", "SHOE_DB_URL", true));
        config.setProperty("user", required(file, environment, "db.user", "SHOE_DB_USER", true));
        config.setProperty("password", required(file, environment, "db.password", "SHOE_DB_PASSWORD", false));
        if (!config.getProperty("url").startsWith("jdbc:sqlserver://")) {
            throw new SQLException("db.url / SHOE_DB_URL harus berupa URL JDBC SQL Server.");
        }
        return config;
    }

    private static String required(Properties file, Map<String, String> environment,
            String key, String variable, boolean trim) throws SQLException {
        String value = environment.containsKey(variable) ? environment.get(variable) : file.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("Konfigurasi " + key + " belum diisi. Salin database.properties.example "
                    + "ke database.properties atau atur " + variable + ".");
        }
        return trim ? value.trim() : value;
    }
}
