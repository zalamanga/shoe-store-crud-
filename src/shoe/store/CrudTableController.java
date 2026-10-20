package shoe.store;

import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
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

    CrudTableController(Component owner, JTable table, JTextField search,
            String tableName, String[] columns, JTextField... fields) {
        this(table, search, tableName, columns, Database::getConnection,
                new SwingDialogs(owner), fields);
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
        }
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

    void stopSearch() {
        searchTimer.stop();
    }

    void loadAll() {
        resetSearch();
        loadFiltered();
    }

    void loadFiltered() {
        searchTimer.stop();
        loadRows("Could not load records.");
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
        searchTimer.stop();
        if (!requireSelection()) {
            return;
        }
        String id = selectedId;
        if (!dialogs.confirmDelete(id)) {
            return;
        }
        String sql = "DELETE FROM " + tableName + " WHERE " + quoteIdentifier(columns[0]) + " = ?";
        executeMutation(sql, new String[]{id}, "Record deleted.");
    }

    private void scheduleSearch() {
        if (!resettingSearch) {
            searchTimer.restart();
        }
    }

    private boolean loadRows(String failureMessage) {
        String filter = search.getText().trim();
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
        } catch (SQLException exception) {
            showDatabaseError(failureMessage, exception);
            return false;
        }
        clearForm();
        model.setRowCount(0);
        for (Object[] row : rows) {
            model.addRow(row);
        }
        return true;
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
        int affected;
        try (Connection connection = connections.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 1, values[i]);
            }
            affected = statement.executeUpdate();
        } catch (SQLException exception) {
            showDatabaseError("The change could not be saved.", exception);
            return;
        }
        if (affected <= 0) {
            clearForm();
            dialogs.message(
                    "No record was changed. Reload the table and select the record again.",
                    "Record not changed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        resetSearch();
        clearForm();
        if (loadRows("The change was saved, but the table could not be refreshed.")) {
            dialogs.message(successMessage, "Saved", JOptionPane.INFORMATION_MESSAGE);
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
        LOGGER.log(Level.WARNING, message, exception);
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
