package shoe.store;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

final class CrudTableController {
    private static final Logger LOGGER = Logger.getLogger(CrudTableController.class.getName());
    private static final int QUERY_TIMEOUT_SECONDS = 10;

    private final JTable table;
    private final JTextField search;
    private final String tableName;
    private final String[] columns;
    private final JTextField[] fields;
    private final DefaultTableModel model;
    private final Timer searchTimer;
    private final ConnectionProvider connections;
    private final Dialogs dialogs;
    private Function<String[], String> validator = values -> null;
    private String selectedId;
    private boolean resettingSearch;
    private JFrame window;
    private String idleTitle;
    private AbstractButton[] actions = new AbstractButton[0];
    private SwingWorker<?, ?> active;
    private boolean writing;
    private boolean closed;
    private long readVersion;
    private String pendingFilter;

    CrudTableController(Component owner, JTable table, JTextField search,
            String tableName, String[] columns, JTextField... fields) {
        this(table, search, tableName, columns, Database::getConnection,
                new SwingDialogs(owner), fields);
        if (owner instanceof JFrame) {
            window = (JFrame) owner;
            idleTitle = window.getTitle();
            int closeOperation = window.getDefaultCloseOperation();
            window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            window.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    if (requestClose()) {
                        window.dispose();
                        if (closeOperation == WindowConstants.EXIT_ON_CLOSE) {
                            System.exit(0);
                        }
                    }
                }

                @Override
                public void windowClosed(WindowEvent event) {
                    closeReads();
                }
            });
        }
    }

    CrudTableController(JTable table, JTextField search, String tableName,
            String[] columns, ConnectionProvider connections, Dialogs dialogs,
            JTextField... fields) {
        if (columns.length != fields.length || columns.length == 0) {
            throw new IllegalArgumentException("Each database column needs an input field.");
        }
        this.table = table;
        this.search = search;
        this.tableName = "[dbo]." + quoteIdentifier(tableName);
        this.columns = columns.clone();
        this.fields = fields.clone();
        this.connections = connections;
        this.dialogs = dialogs;
        for (String column : columns) {
            quoteIdentifier(column);
        }
        String[] headings = new String[columns.length];
        for (int i = 0; i < headings.length; i++) {
            headings[i] = table.getColumnName(i);
            fields[i].getAccessibleContext().setAccessibleName(headings[i]);
        }
        search.getAccessibleContext().setAccessibleName("Search records");
        table.getAccessibleContext().setAccessibleName("Records");
        model = new DefaultTableModel(headings, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                selectRow();
            }
        });
        searchTimer = new Timer(250, event -> loadFiltered());
        searchTimer.setRepeats(false);
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                scheduleSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                scheduleSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                scheduleSearch();
            }
        });
    }

    void setValidator(Function<String[], String> validator) {
        this.validator = validator;
    }

    void bindActions(AbstractButton... buttons) {
        actions = buttons.clone();
    }

    boolean isWorking() {
        return active != null;
    }

    boolean requestClose() {
        if (writing) {
            dialogs.message("A change is being saved. Wait for the result before leaving this form.",
                    "Save in progress", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        closeReads();
        return true;
    }

    private void closeReads() {
        closed = true;
        pendingFilter = null;
        readVersion++;
        searchTimer.stop();
        if (active != null && !writing) {
            active.cancel(true);
        }
    }

    void stopSearch() {
        searchTimer.stop();
    }

    void loadAll() {
        if (closed || writing) {
            return;
        }
        resetSearch();
        loadFiltered();
    }

    void loadFiltered() {
        searchTimer.stop();
        if (closed || writing) {
            return;
        }
        String filter = search.getText().trim();
        long version = ++readVersion;
        if (active != null) {
            pendingFilter = filter;
        } else {
            startRead(filter, version);
        }
    }

    void selectRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            selectedId = null;
            return;
        }
        int row = table.convertRowIndexToModel(viewRow);
        if (row < 0 || row >= model.getRowCount()) {
            selectedId = null;
            return;
        }
        for (int i = 0; i < fields.length; i++) {
            Object value = model.getValueAt(row, i);
            fields[i].setText(value == null ? "" : value.toString());
        }
        selectedId = fields[0].getText();
    }

    void insert() {
        if (closed || active != null) {
            return;
        }
        searchTimer.stop();
        String[] values = validatedValues();
        if (values == null) {
            return;
        }
        String placeholders = String.join(", ", Collections.nCopies(columns.length, "?"));
        String sql = "INSERT INTO " + tableName + " (" + columnList() + ") VALUES ("
                + placeholders + ")";
        executeMutation(sql, values, "Record inserted.");
    }

    void update() {
        if (closed || active != null) {
            return;
        }
        searchTimer.stop();
        if (!requireSelection()) {
            return;
        }
        String[] values = validatedValues();
        if (values == null) {
            return;
        }
        StringJoiner assignments = new StringJoiner(", ");
        for (String column : columns) {
            assignments.add(quoteIdentifier(column) + " = ?");
        }
        String sql = "UPDATE " + tableName + " SET " + assignments + " WHERE "
                + quoteIdentifier(columns[0]) + " = ?";
        String[] parameters = new String[values.length + 1];
        System.arraycopy(values, 0, parameters, 0, values.length);
        // The original selected ID remains the key when the user edits the ID field.
        parameters[values.length] = selectedId;
        executeMutation(sql, parameters, "Record updated.");
    }

    void delete() {
        if (closed || active != null) {
            return;
        }
        searchTimer.stop();
        if (!requireSelection()) {
            return;
        }
        String id = selectedId;
        if (!dialogs.confirmDelete(id)) {
            return;
        }
        if (closed || active != null) {
            return;
        }
        String sql = "DELETE FROM " + tableName + " WHERE " + quoteIdentifier(columns[0]) + " = ?";
        executeMutation(sql, new String[]{id}, "Record deleted.");
    }

    private void scheduleSearch() {
        if (!resettingSearch && !writing && !closed) {
            readVersion++;
            if (pendingFilter != null) {
                pendingFilter = search.getText().trim();
            }
            searchTimer.restart();
        }
    }

    private List<Object[]> readRows(String filter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ").append(columnList())
                .append(" FROM ").append(tableName);
        if (!filter.isEmpty()) {
            StringJoiner conditions = new StringJoiner(" OR ");
            for (String column : columns) {
                conditions.add("CONVERT(NVARCHAR(MAX), " + quoteIdentifier(column) + ") LIKE ? ESCAPE '~'");
            }
            sql.append(" WHERE ").append(conditions);
        }
        sql.append(" ORDER BY ").append(quoteIdentifier(columns[0]));
        List<Object[]> rows = new ArrayList<>();
        try (Connection connection = connections.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            if (!filter.isEmpty()) {
                String pattern = "%" + escapeLike(filter) + "%";
                for (int i = 0; i < columns.length; i++) {
                    statement.setString(i + 1, pattern);
                }
            }
            try (ResultSet results = statement.executeQuery()) {
                while (results.next()) {
                    Object[] row = new Object[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        row[i] = results.getString(columns[i]);
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private void startRead(String filter, long version) {
        setBusy(true, false);
        active = new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws SQLException {
                return readRows(filter);
            }

            @Override
            protected void done() {
                if (closed) {
                    active = null;
                    return;
                }
                try {
                    List<Object[]> rows = get();
                    if (version == readVersion) {
                        applyRows(rows);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (CancellationException exception) {
                    // Closing the form invalidates a pending read.
                } catch (ExecutionException exception) {
                    if (version == readVersion) {
                        showTaskError("Could not load records.", exception.getCause());
                    }
                } finally {
                    active = null;
                    if (pendingFilter != null) {
                        String next = pendingFilter;
                        pendingFilter = null;
                        searchTimer.stop();
                        startRead(next, readVersion);
                    } else {
                        setBusy(false, false);
                    }
                }
            }
        };
        active.execute();
    }

    private void applyRows(List<Object[]> rows) {
        clearForm();
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
    }

    private String[] validatedValues() {
        String[] values = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            values[i] = fields[i].getText().trim();
            if (values[i].isEmpty()) {
                dialogs.message(model.getColumnName(i) + " is required.",
                        "Check input", JOptionPane.WARNING_MESSAGE);
                fields[i].requestFocusInWindow();
                return null;
            }
        }
        String error = validator.apply(values);
        if (error != null) {
            dialogs.message(error, "Check input", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return values;
    }

    private boolean requireSelection() {
        int row = table.getSelectedRow();
        if (selectedId == null || selectedId.trim().isEmpty() || row < 0
                || row >= table.getRowCount()) {
            dialogs.message("Select a record in the table first.",
                    "Select record", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void executeMutation(String sql, String[] values, String successMessage) {
        searchTimer.stop();
        pendingFilter = null;
        readVersion++;
        writing = true;
        setBusy(true, true);
        String[] parameters = values.clone();
        active = new SwingWorker<MutationResult, Void>() {
            @Override
            protected MutationResult doInBackground() throws SQLException {
                int affected;
                try (Connection connection = connections.getConnection();
                        PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                    for (int i = 0; i < parameters.length; i++) {
                        statement.setString(i + 1, parameters[i]);
                    }
                    affected = statement.executeUpdate();
                }
                if (affected <= 0) {
                    return new MutationResult(affected, null, null);
                }
                try {
                    return new MutationResult(affected, readRows(""), null);
                } catch (SQLException refreshError) {
                    return new MutationResult(affected, null, refreshError);
                }
            }

            @Override
            protected void done() {
                if (closed) {
                    active = null;
                    writing = false;
                    return;
                }
                try {
                    MutationResult result = get();
                    if (result.affected <= 0) {
                        clearForm();
                        dialogs.message("No record was changed. Reload the table and select the record again.",
                                "Record not changed", JOptionPane.WARNING_MESSAGE);
                    } else {
                        resetSearch();
                        clearForm();
                        if (result.refreshError == null) {
                            applyRows(result.rows);
                            dialogs.message(successMessage, "Saved", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showDatabaseError("The change was saved, but the table could not be refreshed. "
                                    + "Use Read before editing again. Do not repeat the save.", result.refreshError);
                        }
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException exception) {
                    showTaskError("The database did not confirm the change. Use Read to check the record "
                            + "before saving again.", exception.getCause());
                } finally {
                    active = null;
                    writing = false;
                    setBusy(false, false);
                }
            }
        };
        active.execute();
    }

    private void setBusy(boolean busy, boolean saving) {
        for (AbstractButton action : actions) {
            action.setEnabled(!busy);
        }
        for (JTextField field : fields) {
            field.setEnabled(!busy);
        }
        table.setEnabled(!busy);
        search.setEnabled(!saving);
        if (window != null) {
            window.setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
            window.setTitle(busy ? (saving ? "Saving, please wait... " : "Loading records... ")
                    + idleTitle : idleTitle);
        }
    }

    private void showTaskError(String message, Throwable error) {
        if (error instanceof SQLException) {
            showDatabaseError(message, (SQLException) error);
        } else {
            LOGGER.log(Level.WARNING, "Database task failed: {0}", error.getClass().getSimpleName());
            dialogs.message(message, "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static final class MutationResult {
        private final int affected;
        private final List<Object[]> rows;
        private final SQLException refreshError;

        MutationResult(int affected, List<Object[]> rows, SQLException refreshError) {
            this.affected = affected;
            this.rows = rows;
            this.refreshError = refreshError;
        }
    }

    private void resetSearch() {
        searchTimer.stop();
        resettingSearch = true;
        try {
            search.setText("");
        } finally {
            resettingSearch = false;
        }
    }

    private void clearForm() {
        selectedId = null;
        table.clearSelection();
        for (JTextField field : fields) {
            field.setText("");
        }
    }

    private void showDatabaseError(String message, SQLException exception) {
        LOGGER.log(Level.WARNING, "{0} SQLState={1}, code={2}",
                new Object[]{message, exception.getSQLState(), exception.getErrorCode()});
        dialogs.message(
                message + " Check the database settings, connection and table structure.",
                "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private String columnList() {
        StringJoiner names = new StringJoiner(", ");
        for (String column : columns) {
            names.add(quoteIdentifier(column));
        }
        return names.toString();
    }

    private static String escapeLike(String value) {
        return value.replace("~", "~~").replace("%", "~%")
                .replace("_", "~_").replace("[", "~[");
    }

    private static String quoteIdentifier(String value) {
        if (!value.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid database identifier.");
        }
        return "[" + value + "]";
    }

    @FunctionalInterface
    interface ConnectionProvider {
        Connection getConnection() throws SQLException;
    }

    interface Dialogs {
        void message(String message, String title, int type);

        boolean confirmDelete(String id);
    }

    private static final class SwingDialogs implements Dialogs {
        private final Component owner;

        SwingDialogs(Component owner) {
            this.owner = owner;
        }

        @Override
        public void message(String message, String title, int type) {
            JOptionPane.showMessageDialog(owner, message, title, type);
        }

        @Override
        public boolean confirmDelete(String id) {
            return JOptionPane.showConfirmDialog(owner,
                    "Delete the selected record with ID " + id + "?", "Confirm delete",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
    }
}
