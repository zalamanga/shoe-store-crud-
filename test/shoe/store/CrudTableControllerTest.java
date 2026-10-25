package shoe.store;

import java.lang.reflect.Proxy;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

public final class CrudTableControllerTest {
    private static int checks;
    private static final String[][] COLUMNS = {
        {"customer_id", "customer_name", "customer_address", "phone_number"},
        {"employee_id", "employee_name", "employee_address", "phone_number"},
        {"supplier_id", "supplier_name", "supplier_address", "supplier_brand", "phone_number"},
        {"shoe_id", "shoe_brand", "shoe_name", "color", "size", "price", "stock"}
    };

    public static void main(String[] args) throws Exception {
        Logger.getLogger(CrudTableController.class.getName()).setLevel(Level.OFF);
        SwingUtilities.invokeAndWait(() -> {
            for (int i = 0; i < COLUMNS.length; i++) {
                verifyCrud(new String[]{"customer", "employee", "supplier", "product"}[i], COLUMNS[i]);
            }
            verifySearch();
            verifyValidation();
            verifyFailures();
        });
        verifyPastedSearch();
        verifyNonblockingAndLatestSearch();
        verifyPendingWriteAndClosingRead();
        System.out.println("CrudTableControllerTest: " + checks + " checks passed.");
    }

    private static void verifyCrud(String tableName, String[] columns) {
        Fixture fixture = new Fixture(tableName, columns);
        fixture.database.rows.add(row(columns, "B02"));
        fixture.database.rows.add(row(columns, "A01"));
        fixture.crud.loadAll();
        await(fixture);
        check(fixture.table.getRowCount() == 2, tableName + " initial load");
        check(!fixture.table.isCellEditable(0, 0), tableName + " read-only cells");
        check(fixture.database.calls.get(0).sql.contains("FROM [dbo].[" + tableName + "]"),
                tableName + " explicit SQL table");
        assertClosed(fixture.database.calls.get(0), fixture.database);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(
                (DefaultTableModel) fixture.table.getModel());
        fixture.table.setRowSorter(sorter);
        sorter.toggleSortOrder(0);
        fixture.table.setRowSelectionInterval(0, 0);
        check(fixture.fields[0].getText().equals("A01"), tableName + " sorted selection uses model row");
        for (int i = 0; i < columns.length; i++) {
            fixture.fields[i].setText(i == 0 ? "renamed'id" : "new-" + i);
        }
        fixture.crud.update();
        await(fixture);
        Call update = fixture.database.mutations().get(0);
        check(update.sql.endsWith("WHERE [" + columns[0] + "] = ?"), tableName + " update key is ID");
        check(update.parameters.size() == columns.length + 1, tableName + " binds every updated column");
        check(update.parameters.get(1).equals("renamed'id"), tableName + " ID can be edited as data");
        check(update.parameters.get(columns.length + 1).equals("A01"), tableName + " original ID selects update target");
        for (int i = 1; i < columns.length; i++) {
            check(update.parameters.get(i + 1).equals("new-" + i), tableName + " updates column " + columns[i]);
        }
        check(!update.sql.contains("renamed'id"), tableName + " input never becomes SQL text");
        check(fixture.table.getSelectedRow() == -1 && fixture.fields[0].getText().isEmpty(),
                tableName + " mutation clears selection and inputs");
        assertClosed(update, fixture.database);

        fixture.table.setRowSelectionInterval(0, 0);
        String originalId = fixture.fields[0].getText();
        fixture.fields[0].setText("different typed ID");
        fixture.crud.delete();
        await(fixture);
        Call delete = fixture.database.mutations().get(1);
        check(delete.sql.equals("DELETE FROM [dbo].[" + tableName + "] WHERE [" + columns[0] + "] = ?"),
                tableName + " delete uses primary ID");
        check(delete.parameters.get(1).equals(originalId), tableName + " typed ID cannot redirect delete");
        check(fixture.dialogs.confirmedId.equals(originalId), tableName + " confirmation identifies selected ID");
        assertClosed(delete, fixture.database);

        fixture.fill("  O'Neil%_  ");
        fixture.crud.insert();
        await(fixture);
        Call insert = fixture.database.mutations().get(2);
        check(insert.sql.startsWith("INSERT INTO [dbo].[" + tableName + "] ("), tableName + " explicit insert columns");
        check(insert.parameters.size() == columns.length, tableName + " binds all insert values");
        check(insert.parameters.get(1).equals("O'Neil%_"), tableName + " trims without altering SQL-special input");
        check(!insert.sql.contains("O'Neil"), tableName + " insert is parameterized");
        assertClosed(insert, fixture.database);
        fixture.crud.stopSearch();
    }

    private static void verifySearch() {
        Fixture fixture = new Fixture("customer", COLUMNS[0]);
        fixture.search.setText("%_~[' OR 1=1--");
        fixture.crud.loadFiltered();
        await(fixture);
        Call select = fixture.database.calls.get(0);
        check(select.sql.contains("LIKE ? ESCAPE '~'"), "search uses bound LIKE pattern");
        check(!select.sql.contains("OR 1=1--"), "search input never becomes SQL");
        check(select.parameters.size() == COLUMNS[0].length, "search binds each column");
        for (String value : select.parameters.values()) {
            check(value.equals("%~%~_~~~[' OR 1=1--%"), "search escapes literal wildcard characters");
        }
        assertClosed(select, fixture.database);
        fixture.crud.loadAll();
        await(fixture);
        Call all = fixture.database.calls.get(1);
        check(fixture.search.getText().isEmpty(), "Read clears search");
        check(!all.sql.contains(" WHERE ") && all.parameters.isEmpty(), "Read reloads unfiltered records");
        fixture.crud.stopSearch();
    }

    private static void verifyValidation() {
        Fixture fixture = new Fixture("customer", COLUMNS[0]);
        fixture.crud.update();
        fixture.crud.delete();
        check(fixture.database.opened == 0, "update/delete require selection before connection");
        check(fixture.dialogs.confirmedId == null, "missing selection does not open delete confirmation");
        for (int i = 0; i < fixture.fields.length; i++) {
            fixture.fill("valid");
            fixture.fields[i].setText("  ");
            fixture.crud.insert();
        }
        check(fixture.database.opened == 0, "every empty field prevents insert");

        Fixture product = new Fixture("product", COLUMNS[3]);
        product.crud.setValidator(Product::validateProduct);
        String[] valid = {"P01", "Brand", "Shoe", "Black", "42.5", "0", "0"};
        check(Product.validateProduct(valid) == null, "fractional size and zero price/stock are valid");
        String[] limits = {"P01", "Brand", "Shoe", "Black", "999.99", "9999999999999999.99", "2147483647"};
        check(Product.validateProduct(limits) == null, "schema upper bounds are valid");
        String[] trailingZeros = {"P01", "Brand", "Shoe", "Black", "42.5000", "1.2000", "1"};
        check(Product.validateProduct(trailingZeros) == null, "redundant decimal zeros are valid");
        check(trailingZeros[4].equals("42.5") && trailingZeros[5].equals("1.2"), "decimals normalize without rounding");
        String[] exponents = {"P01", "Brand", "Shoe", "Black", "4.2e1", "1e2", "1"};
        check(Product.validateProduct(exponents) == null && exponents[4].equals("42") && exponents[5].equals("100"),
                "exponent values become SQL-compatible plain decimals");
        for (String[] invalid : new String[][]{
            {"4", "0"}, {"4", "-1"}, {"4", "NaN"},
            {"4", "1000"}, {"4", "42.001"},
            {"5", "-0.01"}, {"5", "free"}, {"5", "10000000000000000"}, {"5", "1.001"},
            {"6", "-1"}, {"6", "1.5"}, {"6", "2147483648"}
        }) {
            String[] values = valid.clone();
            values[Integer.parseInt(invalid[0])] = invalid[1];
            for (int i = 0; i < values.length; i++) {
                product.fields[i].setText(values[i]);
            }
            product.crud.insert();
        }
        check(product.database.opened == 0, "invalid size/price/stock never reaches database");

        fixture.database.rows.add(row(COLUMNS[0], "A01"));
        fixture.crud.loadAll();
        await(fixture);
        fixture.table.setRowSelectionInterval(0, 0);
        fixture.dialogs.confirmDelete = false;
        fixture.crud.delete();
        check(fixture.database.mutations().isEmpty(), "cancelled delete does not execute SQL");
        fixture.dialogs.confirmDelete = true;
        fixture.dialogs.onConfirm = () -> {
            fixture.table.clearSelection();
        };
        fixture.crud.delete();
        await(fixture);
        check(fixture.database.mutations().get(0).parameters.get(1).equals("A01"),
                "delete targets confirmed ID even when modal callback clears selection");
        fixture.crud.stopSearch();
        product.crud.stopSearch();
    }

    private static void verifyFailures() {
        Fixture fixture = new Fixture("customer", COLUMNS[0]);
        fixture.fill("value");
        fixture.database.affected = 0;
        fixture.crud.insert();
        await(fixture);
        check(!fixture.dialogs.hasInformation(), "zero affected rows cannot report success");
        check(fixture.fields[0].getText().isEmpty(), "zero affected rows clears stale input/selection");
        fixture.database.affected = -1;
        fixture.fill("value");
        fixture.crud.insert();
        await(fixture);
        check(!fixture.dialogs.hasInformation(), "unknown affected count cannot report success");

        fixture.database.failMutation = true;
        fixture.fill("value");
        fixture.crud.insert();
        await(fixture);
        check(!fixture.dialogs.hasInformation(), "SQL failure cannot report success");
        check(fixture.fields[0].getText().equals("value"), "failed save preserves input");
        assertClosed(fixture.database.calls.get(2), fixture.database);
        fixture.database.failMutation = false;
        fixture.database.failQuery = true;
        ((DefaultTableModel) fixture.table.getModel()).addRow(new Object[]{"A01", "existing", "address", "phone"});
        fixture.table.setRowSelectionInterval(0, 0);
        fixture.fields[1].setText("unsaved change");
        fixture.crud.loadAll();
        await(fixture);
        assertClosed(fixture.database.calls.get(3), fixture.database);
        check(fixture.table.getRowCount() == 1 && fixture.fields[1].getText().equals("unsaved change"),
                "failed Read preserves previous table and unsaved input");
        check(fixture.dialogs.types.contains(JOptionPane.ERROR_MESSAGE), "database failure is visible");

        Fixture reading = new Fixture("customer", COLUMNS[0]);
        reading.database.rows.add(row(COLUMNS[0], "A01"));
        reading.database.failRead = true;
        reading.crud.loadAll();
        await(reading);
        assertClosed(reading.database.calls.get(0), reading.database);

        Fixture saved = new Fixture("customer", COLUMNS[0]);
        saved.database.failQuery = true;
        saved.fill("value");
        saved.crud.insert();
        await(saved);
        check(saved.database.mutations().size() == 1, "refresh failure does not repeat mutation");
        check(saved.dialogs.messages.stream().anyMatch(message -> message.startsWith("The change was saved, but")),
                "refresh failure distinguishes saved change from failed refresh");
        check(!saved.dialogs.hasInformation(), "failed refresh does not show generic success");
        check(saved.database.opened == saved.database.closed, "all failure-path connections close");
        fixture.crud.stopSearch();
        saved.crud.stopSearch();
        reading.crud.stopSearch();
    }

    private static void verifyPastedSearch() throws Exception {
        Fixture[] holder = new Fixture[1];
        CountDownLatch queried = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new Fixture("customer", COLUMNS[0]);
            holder[0].database.queried = queried;
            holder[0].search.setText("pasted text");
        });
        check(queried.await(3, TimeUnit.SECONDS), "document listener searches pasted text without key event");
        SwingUtilities.invokeAndWait(() -> {
            await(holder[0]);
            check(holder[0].database.calls.get(0).parameters.get(1).equals("%pasted text%"),
                    "debounced search reads current document contents");
            holder[0].crud.stopSearch();
        });
    }

    private static void verifyNonblockingAndLatestSearch() throws Exception {
        Fixture[] holder = new Fixture[1];
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch heartbeat = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Fixture fixture = new Fixture("customer", COLUMNS[0]);
            holder[0] = fixture;
            fixture.database.rows.add(row(COLUMNS[0], "OLD"));
            fixture.database.entered = entered;
            fixture.database.release = release;
            fixture.crud.loadAll();
            fixture.fill("duplicate action");
            fixture.crud.insert();
            SwingUtilities.invokeLater(heartbeat::countDown);
        });
        check(entered.await(2, TimeUnit.SECONDS), "JDBC read starts on worker");
        check(heartbeat.await(1, TimeUnit.SECONDS), "EDT remains responsive while JDBC is blocked");
        SwingUtilities.invokeAndWait(() -> {
            Fixture fixture = holder[0];
            fixture.database.rows.clear();
            fixture.database.rows.add(row(COLUMNS[0], "NEW"));
            fixture.search.setText("new");
            fixture.crud.loadFiltered();
            release.countDown();
            await(fixture);
            check(fixture.database.mutations().isEmpty(), "Action during pending read cannot create a write");
            check(fixture.database.calls.size() == 2, "Latest search runs once after current read");
            check("%new%".equals(fixture.database.calls.get(1).parameters.get(1)), "Queued search captures latest filter");
            check("NEW".equals(fixture.table.getValueAt(0, 0)), "Stale read cannot replace latest results");
            fixture.crud.requestClose();
        });
    }

    private static void verifyPendingWriteAndClosingRead() throws Exception {
        Fixture[] holder = new Fixture[1];
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Fixture fixture = new Fixture("customer", COLUMNS[0]);
            holder[0] = fixture;
            fixture.database.entered = entered;
            fixture.database.release = release;
            fixture.database.blockWrite = true;
            fixture.fill("saved once");
            fixture.crud.insert();
            fixture.crud.insert();
            check(!fixture.crud.requestClose(), "Close and Back are blocked while a write is pending");
            check(fixture.dialogs.messages.stream().anyMatch(message -> message.contains("being saved")),
                    "Pending write explains why leaving must wait");
        });
        check(entered.await(2, TimeUnit.SECONDS), "Pending write reaches JDBC worker");
        release.countDown();
        SwingUtilities.invokeAndWait(() -> {
            await(holder[0]);
            check(holder[0].database.mutations().size() == 1, "Repeated save cannot replay a pending mutation");
            check(holder[0].crud.requestClose(), "Leaving is allowed after write and refresh complete");
        });

        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch finish = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            Fixture fixture = new Fixture("customer", COLUMNS[0]);
            holder[0] = fixture;
            fixture.database.rows.add(row(COLUMNS[0], "LATE"));
            fixture.database.entered = reading;
            fixture.database.release = finish;
            fixture.crud.loadAll();
        });
        check(reading.await(2, TimeUnit.SECONDS), "Closing-read scenario starts query");
        SwingUtilities.invokeAndWait(() -> {
            check(holder[0].crud.requestClose(), "A pending read can be closed");
            finish.countDown();
            await(holder[0]);
            check(holder[0].table.getRowCount() == 0, "Late read does not update a closed form");
            check(holder[0].dialogs.messages.isEmpty(), "Closed read cannot display an error dialog");
        });
    }

    private static void await(Fixture fixture) {
        if (!fixture.crud.isWorking()) {
            return;
        }
        SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        Timer timer = new Timer(5, event -> {
            if (!fixture.crud.isWorking() || System.nanoTime() >= deadline) {
                ((Timer) event.getSource()).stop();
                loop.exit();
            }
        });
        timer.start();
        loop.enter();
        timer.stop();
        check(!fixture.crud.isWorking(), "Worker completes within deadline");
    }

    private static Map<String, String> row(String[] columns, String id) {
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.length; i++) {
            row.put(columns[i], i == 0 ? id : "value-" + i);
        }
        return row;
    }

    private static void assertClosed(Call call, FakeDatabase database) {
        check(call.timeout == 10, "query has 10-second timeout");
        check(call.closed, "prepared statement closes");
        check(!call.resultCreated || call.resultClosed, "result set closes");
        check(database.opened == database.closed, "connections close");
    }

    private static void check(boolean condition, String description) {
        if (!condition) {
            throw new AssertionError(description);
        }
        checks++;
    }

    private static final class Fixture {
        private final JTable table;
        private final JTextField search = new JTextField();
        private final JTextField[] fields;
        private final FakeDatabase database = new FakeDatabase();
        private final DialogRecorder dialogs = new DialogRecorder();
        private final CrudTableController crud;

        Fixture(String tableName, String[] columns) {
            table = new JTable(new DefaultTableModel(columns, 0));
            fields = new JTextField[columns.length];
            Arrays.setAll(fields, index -> new JTextField());
            crud = new CrudTableController(table, search, tableName, columns, database, dialogs, fields);
        }

        void fill(String value) {
            for (JTextField field : fields) {
                field.setText(value);
            }
        }
    }

    private static final class DialogRecorder implements CrudTableController.Dialogs {
        private final List<String> messages = new ArrayList<>();
        private final List<Integer> types = new ArrayList<>();
        private boolean confirmDelete = true;
        private String confirmedId;
        private Runnable onConfirm;

        @Override
        public void message(String message, String title, int type) {
            check(SwingUtilities.isEventDispatchThread(), "Dialogs only run on EDT");
            messages.add(message);
            types.add(type);
        }

        @Override
        public boolean confirmDelete(String id) {
            confirmedId = id;
            if (onConfirm != null) {
                onConfirm.run();
            }
            return confirmDelete;
        }

        boolean hasInformation() {
            return types.contains(JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static final class Call {
        private final String sql;
        private final Map<Integer, String> parameters = new LinkedHashMap<>();
        private int timeout;
        private boolean closed;
        private boolean resultCreated;
        private boolean resultClosed;

        Call(String sql) {
            this.sql = sql;
        }
    }

    private static final class FakeDatabase implements CrudTableController.ConnectionProvider {
        private final List<Call> calls = new ArrayList<>();
        private final List<Map<String, String>> rows = new ArrayList<>();
        private int opened;
        private int closed;
        private int affected = 1;
        private boolean failMutation;
        private boolean failQuery;
        private boolean failRead;
        private CountDownLatch queried;
        private CountDownLatch entered;
        private CountDownLatch release;
        private boolean blockWrite;

        @Override
        public Connection getConnection() {
            opened++;
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "prepareStatement":
                                Call call = new Call((String) arguments[0]);
                                calls.add(call);
                                return statement(call);
                            case "close":
                                closed++;
                                return null;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }

        private PreparedStatement statement(Call call) {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "setString":
                                call.parameters.put((Integer) arguments[0], (String) arguments[1]);
                                return null;
                            case "setQueryTimeout":
                                call.timeout = (Integer) arguments[0];
                                return null;
                            case "executeQuery":
                                List<Map<String, String>> snapshot = new ArrayList<>(rows);
                                if (!blockWrite) {
                                    blockOnce();
                                }
                                if (queried != null) {
                                    queried.countDown();
                                }
                                if (failQuery) {
                                    throw new SQLException("Simulated read failure");
                                }
                                call.resultCreated = true;
                                return results(call, snapshot);
                            case "executeUpdate":
                                if (blockWrite) {
                                    blockOnce();
                                }
                                if (failMutation) {
                                    throw new SQLException("Simulated write failure");
                                }
                                return affected;
                            case "close":
                                call.closed = true;
                                return null;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }

        private void blockOnce() throws SQLException {
            if (entered != null) {
                CountDownLatch pending = release;
                entered.countDown();
                entered = null;
                try {
                    if (!pending.await(3, TimeUnit.SECONDS)) {
                        throw new SQLException("Synthetic blocking query timed out");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Synthetic query interrupted", exception);
                }
            }
        }

        private ResultSet results(Call call, List<Map<String, String>> snapshot) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class}, (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "next":
                                return ++cursor[0] < snapshot.size();
                            case "getString":
                                if (failRead) {
                                    throw new SQLException("Simulated result read failure");
                                }
                                return snapshot.get(cursor[0]).get((String) arguments[0]);
                            case "close":
                                call.resultClosed = true;
                                return null;
                            default:
                                throw new UnsupportedOperationException(method.getName());
                        }
                    });
        }

        List<Call> mutations() {
            List<Call> mutations = new ArrayList<>();
            for (Call call : calls) {
                if (!call.sql.startsWith("SELECT ")) {
                    mutations.add(call);
                }
            }
            return mutations;
        }
    }
}
