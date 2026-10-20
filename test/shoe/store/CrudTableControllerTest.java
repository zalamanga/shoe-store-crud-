package shoe.store;

import java.lang.reflect.Proxy;
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
        System.out.println("CrudTableControllerTest: " + checks + " checks passed.");
    }

    private static void verifyCrud(String tableName, String[] columns) {
        Fixture fixture = new Fixture(tableName, columns);
        fixture.database.rows.add(row(columns, "B02"));
        fixture.database.rows.add(row(columns, "A01"));
        fixture.crud.loadAll();
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
        Call delete = fixture.database.mutations().get(1);
        check(delete.sql.equals("DELETE FROM [dbo].[" + tableName + "] WHERE [" + columns[0] + "] = ?"),
                tableName + " delete uses primary ID");
        check(delete.parameters.get(1).equals(originalId), tableName + " typed ID cannot redirect delete");
        check(fixture.dialogs.confirmedId.equals(originalId), tableName + " confirmation identifies selected ID");
        assertClosed(delete, fixture.database);

        fixture.fill("  O'Neil%_  ");
        fixture.crud.insert();
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
        Call select = fixture.database.calls.get(0);
        check(select.sql.contains("LIKE ? ESCAPE '~'"), "search uses bound LIKE pattern");
        check(!select.sql.contains("OR 1=1--"), "search input never becomes SQL");
        check(select.parameters.size() == COLUMNS[0].length, "search binds each column");
        for (String value : select.parameters.values()) {
            check(value.equals("%~%~_~~~[' OR 1=1--%"), "search escapes literal wildcard characters");
        }
        assertClosed(select, fixture.database);
        fixture.crud.loadAll();
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
        fixture.table.setRowSelectionInterval(0, 0);
        fixture.dialogs.confirmDelete = false;
        fixture.crud.delete();
        check(fixture.database.mutations().isEmpty(), "cancelled delete does not execute SQL");
        fixture.dialogs.confirmDelete = true;
        fixture.dialogs.onConfirm = () -> {
            fixture.crud.loadAll();
            fixture.table.clearSelection();
        };
        fixture.crud.delete();
        check(fixture.database.mutations().get(0).parameters.get(1).equals("A01"),
                "delete targets confirmed ID even when modal callback refreshes selection");
        fixture.crud.stopSearch();
        product.crud.stopSearch();
    }

    private static void verifyFailures() {
        Fixture fixture = new Fixture("customer", COLUMNS[0]);
        fixture.fill("value");
        fixture.database.affected = 0;
        fixture.crud.insert();
        check(!fixture.dialogs.hasInformation(), "zero affected rows cannot report success");
        check(fixture.fields[0].getText().isEmpty(), "zero affected rows clears stale input/selection");
        fixture.database.affected = -1;
        fixture.fill("value");
        fixture.crud.insert();
        check(!fixture.dialogs.hasInformation(), "unknown affected count cannot report success");

        fixture.database.failMutation = true;
        fixture.fill("value");
        fixture.crud.insert();
        check(!fixture.dialogs.hasInformation(), "SQL failure cannot report success");
        check(fixture.fields[0].getText().equals("value"), "failed save preserves input");
        assertClosed(fixture.database.calls.get(2), fixture.database);
        fixture.database.failMutation = false;
        fixture.database.failQuery = true;
        ((DefaultTableModel) fixture.table.getModel()).addRow(new Object[]{"A01", "existing", "address", "phone"});
        fixture.table.setRowSelectionInterval(0, 0);
        fixture.fields[1].setText("unsaved change");
        fixture.crud.loadAll();
        assertClosed(fixture.database.calls.get(3), fixture.database);
        check(fixture.table.getRowCount() == 1 && fixture.fields[1].getText().equals("unsaved change"),
                "failed Read preserves previous table and unsaved input");
        check(fixture.dialogs.types.contains(JOptionPane.ERROR_MESSAGE), "database failure is visible");

        Fixture reading = new Fixture("customer", COLUMNS[0]);
        reading.database.rows.add(row(COLUMNS[0], "A01"));
        reading.database.failRead = true;
        reading.crud.loadAll();
        assertClosed(reading.database.calls.get(0), reading.database);

        Fixture saved = new Fixture("customer", COLUMNS[0]);
        saved.database.failQuery = true;
        saved.fill("value");
        saved.crud.insert();
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
            check(holder[0].database.calls.get(0).parameters.get(1).equals("%pasted text%"),
                    "debounced search reads current document contents");
            holder[0].crud.stopSearch();
        });
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
                                if (queried != null) {
                                    queried.countDown();
                                }
                                if (failQuery) {
                                    throw new SQLException("Simulated read failure");
                                }
                                call.resultCreated = true;
                                return results(call);
                            case "executeUpdate":
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

        private ResultSet results(Call call) {
            int[] cursor = {-1};
            return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{ResultSet.class}, (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "next":
                                return ++cursor[0] < rows.size();
                            case "getString":
                                if (failRead) {
                                    throw new SQLException("Simulated result read failure");
                                }
                                return rows.get(cursor[0]).get((String) arguments[0]);
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
