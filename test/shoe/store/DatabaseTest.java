package shoe.store;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public final class DatabaseTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("connection")) {
            verifyConnection();
            return;
        }
        Path directory = Files.createTempDirectory("shoe-database-test-");
        Path config = directory.resolve("database.properties");
        try {
            Files.writeString(config, "db.url=jdbc:sqlserver://test.invalid:1433;databaseName=test\n"
                    + "db.user=pengguna\u00e9\ndb.password=file-only-value\n", StandardCharsets.UTF_8);
            Properties file = Database.loadConfiguration(config, Collections.emptyMap());
            check(file.getProperty("user").equals("pengguna\u00e9"), "UTF-8 properties");

            Map<String, String> environment = new HashMap<>();
            environment.put("SHOE_DB_URL", " jdbc:sqlserver://env.invalid:1433 ");
            environment.put("SHOE_DB_USER", " env-user ");
            environment.put("SHOE_DB_PASSWORD", " env-password ");
            Properties overridden = Database.loadConfiguration(config, environment);
            check(overridden.getProperty("url").equals("jdbc:sqlserver://env.invalid:1433"), "URL override");
            check(overridden.getProperty("user").equals("env-user"), "username override");
            check(overridden.getProperty("password").equals(" env-password "), "password spaces preserved");
            check(Database.loadConfiguration(directory.resolve("missing.properties"), environment)
                    .getProperty("user").equals("env-user"), "environment-only configuration");

            Map<String, String> partial = Collections.singletonMap("SHOE_DB_USER", "another-user");
            check(Database.loadConfiguration(config, partial).getProperty("password").equals("file-only-value"),
                    "per-field fallback");
            expectConfigurationFailure(directory.resolve("missing.properties"), Collections.emptyMap(), "db.url");
            expectConfigurationFailure(config, Collections.singletonMap("SHOE_DB_PASSWORD", ""), "db.password");
            expectConfigurationFailure(config, Collections.singletonMap("SHOE_DB_URL", "jdbc:other://host"), "SQL Server");
            expectConfigurationFailure(directory, Collections.emptyMap(), "tidak dapat dibaca");

            // A child JVM removes developer environment overrides without changing the current process.
            String executable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            ProcessBuilder child = new ProcessBuilder(executable, "-ea", "-cp", System.getProperty("java.class.path"),
                    "-Dshoe.db.config=" + config, DatabaseTest.class.getName(), "connection");
            child.environment().remove("SHOE_DB_URL");
            child.environment().remove("SHOE_DB_USER");
            child.environment().remove("SHOE_DB_PASSWORD");
            check(child.inheritIO().start().waitFor() == 0, "getConnection uses selected config and JDBC driver");
            System.out.println("DatabaseTest: " + checks + " checks passed.");
        } finally {
            Files.deleteIfExists(config);
            Files.deleteIfExists(directory);
        }
    }

    private static void expectConfigurationFailure(Path path, Map<String, String> environment, String message)
            throws Exception {
        try {
            Database.loadConfiguration(path, environment);
            throw new AssertionError("Expected invalid configuration");
        } catch (SQLException ex) {
            check(ex.getMessage().contains(message), "actionable configuration error");
            check(!ex.getMessage().contains("file-only-value"), "error hides credentials");
        }
    }

    private static void verifyConnection() throws Exception {
        FakeDriver driver = new FakeDriver();
        DriverManager.registerDriver(driver);
        try (Connection connection = Database.getConnection()) {
            check(connection != null, "fake connection");
            check(driver.url.equals("jdbc:sqlserver://test.invalid:1433;databaseName=test"), "config path override");
            check(driver.properties.getProperty("user").equals("pengguna\u00e9"), "driver username");
            check(driver.properties.getProperty("password").equals("file-only-value"), "driver password");
        } finally {
            DriverManager.deregisterDriver(driver);
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static final class FakeDriver implements Driver {
        private String url;
        private Properties properties;

        @Override
        public Connection connect(String url, Properties properties) {
            if (!acceptsURL(url)) {
                return null;
            }
            this.url = url;
            this.properties = properties;
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("close")) {
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }

        @Override
        public boolean acceptsURL(String url) { return url != null && url.startsWith("jdbc:sqlserver://"); }
        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
        @Override
        public int getMajorVersion() { return 1; }
        @Override
        public int getMinorVersion() { return 0; }
        @Override
        public boolean jdbcCompliant() { return false; }
        @Override
        public Logger getParentLogger() { return Logger.getGlobal(); }
    }
}
