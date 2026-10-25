package shoe.store;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

public class login_page extends javax.swing.JFrame {


    private static final Logger LOGGER = Logger.getLogger(login_page.class.getName());

    public login_page() {
        initComponents();
        setTitle("Shoe Store - Login");
        login.setForeground(java.awt.Color.BLACK);
        jLabel2.setLabelFor(username);
        jLabel3.setLabelFor(pass);
        getRootPane().setDefaultButton(login);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        login = new javax.swing.JButton();
        pass = new javax.swing.JPasswordField();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        jLabel1.setText("Form Login");

        jLabel2.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel2.setText("Username");

        username.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usernameActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Arial", 1, 12)); // NOI18N
        jLabel3.setText("Password");

        login.setBackground(new java.awt.Color(51, 204, 0));
        login.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N
        login.setForeground(java.awt.Color.white);
        login.setText("Login");
        login.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loginActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Arial", 1, 24)); // NOI18N
        jLabel4.setText("Shoes Store");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(103, 103, 103)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(jLabel2))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addComponent(jLabel3))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addComponent(pass, javax.swing.GroupLayout.PREFERRED_SIZE, 191, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(117, 117, 117)
                        .addComponent(login, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(94, 94, 94)
                        .addComponent(jLabel4)))
                .addContainerGap(49, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addComponent(jLabel1)
                .addGap(33, 33, 33)
                .addComponent(jLabel2)
                .addGap(6, 6, 6)
                .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jLabel3)
                .addGap(6, 6, 6)
                .addComponent(pass, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(login, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36)
                .addComponent(jLabel4)
                .addContainerGap(92, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void loginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loginActionPerformed
        attemptLogin();
    }//GEN-LAST:event_loginActionPerformed

    private void usernameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_usernameActionPerformed
        pass.requestFocusInWindow();
    }//GEN-LAST:event_usernameActionPerformed


    private void attemptLogin() {
        if (!login.isEnabled()) {
            return;
        }
        String enteredUsername = username.getText().trim();
        char[] enteredPassword = pass.getPassword();
        if (enteredUsername.isEmpty() || enteredPassword.length == 0) {
            Arrays.fill(enteredPassword, '\0');
            JOptionPane.showMessageDialog(this, "Username dan password wajib diisi.",
                    "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setLoginEnabled(false);
        new SwingWorker<LoginService.Result, Void>() {
            @Override
            protected LoginService.Result doInBackground() throws SQLException {
                try {
                    return LoginService.check(Database::getConnection, enteredUsername,
                            enteredPassword, Database.allowLegacyPasswords());
                } finally {
                    Arrays.fill(enteredPassword, '\0');
                }
            }

            @Override
            protected void done() {
                if (!isDisplayable()) {
                    return;
                }
                try {
                    LoginService.Result result = get();
                    if (result == LoginService.Result.SUCCESS || result == LoginService.Result.LEGACY_SUCCESS) {
                        if (result == LoginService.Result.LEGACY_SUCCESS) {
                            JOptionPane.showMessageDialog(login_page.this,
                                    "Akun ini masih memakai password lama. Jalankan AccountTool migrate "
                                    + "untuk akun ini, lalu nonaktifkan SHOE_ALLOW_LEGACY_PASSWORDS.",
                                    "Migrasi password diperlukan", JOptionPane.WARNING_MESSAGE);
                        }
                        AppUi.open(login_page.this, new MenuUtama());
                    } else if (result == LoginService.Result.LEGACY_DISABLED) {
                        JOptionPane.showMessageDialog(login_page.this,
                                "Akun lama belum dimigrasikan. Jalankan AccountTool migrate USERNAME "
                                + "dari terminal. Lihat bagian migrasi akun pada README.",
                                "Migrasi password diperlukan", JOptionPane.WARNING_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(login_page.this,
                                "Username atau password tidak sesuai.", "Login gagal",
                                JOptionPane.WARNING_MESSAGE);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof SQLException) {
                        SQLException sql = (SQLException) cause;
                        LOGGER.log(Level.WARNING, "Login gagal: SQLState={0}, kode={1}",
                                new Object[] {sql.getSQLState(), sql.getErrorCode()});
                    } else {
                        LOGGER.log(Level.SEVERE, "Login gagal.", cause);
                    }
                    JOptionPane.showMessageDialog(login_page.this,
                            "Login belum dapat diproses. Periksa database.properties dan koneksi SQL Server.",
                            "Koneksi database", JOptionPane.ERROR_MESSAGE);
                } finally {
                    pass.setText("");
                    setLoginEnabled(true);
                    pass.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setLoginEnabled(boolean enabled) {
        login.setEnabled(enabled);
        username.setEnabled(enabled);
        pass.setEnabled(enabled);
    }

    public static void main(String[] args) {
        AppUi.launch(login_page::new);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JButton login;
    private javax.swing.JPasswordField pass;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables
}
