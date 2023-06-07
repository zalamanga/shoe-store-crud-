/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package shoe.store;

/**
 *
 * @author USER
 */

import java.awt.Color;
import java.sql.Connection;
//Ambil Library java.sql.DriverManager
import java.sql.DriverManager;
//Ambil Library java.sql.PreparedStatement
import java.sql.PreparedStatement;
//Ambil Library java.sql.Resultset
import java.sql.ResultSet;
//Ambil Library java.sql.SQLException
import java.sql.SQLException;
//Ambil Library java.sql.Statement
import java.sql.Statement;
//Ambil Library java.sql.JOptionPane
import javax.swing.JOptionPane; 
//Ambil Library java.sql.JTable
import javax.swing.JTable;
//Ambil Library java.sql.DefaultTableModel
import javax.swing.table.DefaultTableModel;
//Ambil Library java.sql.TableModel
import javax.swing.table.TableModel;



public class Employee extends javax.swing.JFrame {


    public Employee() {
        initComponents();
    }

    //Driver dari database yang digunakan - Database Microsoft Sql SErver sqljdbc4
    String driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    //alamat lokasi database yang digunakan lihat dari service yang dibuat
    String url = "jdbc:sqlserver://localhost\\LAPTOP-EADDRC3D:1433;databaseName=SHOEStoremanaj";
    // login user ke database
    String user = "sa";
    // login password ke database
    String password = "1234"; 
    //https://docs.oracle.com/javase/7/docs/api/java/sql/Statement.html
    Statement st=null;
    //https://docs.oracle.com/javase/7/docs/api/java/sql/ResultSet.html
    ResultSet rs=null;
    //https://docs.oracle.com/javase/7/docs/api/javax/swing/table/DefaultTableModel.html
    DefaultTableModel tabModel = new DefaultTableModel(new Object []{"employee_id","employee_name","employee_address","phone_number"},0);
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        empnum = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        empid = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        empname = new javax.swing.JTextField();
        search = new javax.swing.JTextField();
        btninsert = new javax.swing.JButton();
        btndelete = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        btnupdate = new javax.swing.JButton();
        btnread = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        empadr = new javax.swing.JTextField();
        btnback = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jLabel2.setText("Eemployee ID");

        jLabel3.setText("EmployeeName");

        jLabel4.setText("Phone Number");

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Employe ID", "Employee Name", "Employee Address", "Phone Number"
            }
        ));
        jTable2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable2MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable2);

        search.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchActionPerformed(evt);
            }
        });
        search.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                searchKeyTyped(evt);
            }
        });

        btninsert.setText("Insert");
        btninsert.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                btninsertMouseClicked(evt);
            }
        });
        btninsert.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btninsertActionPerformed(evt);
            }
        });

        btndelete.setText("Delete");
        btndelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btndeleteActionPerformed(evt);
            }
        });

        jLabel1.setText("Search");

        btnupdate.setText("Update");
        btnupdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnupdateActionPerformed(evt);
            }
        });

        btnread.setText("Read");
        btnread.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnreadActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Eras Bold ITC", 1, 36)); // NOI18N
        jLabel5.setText("EMPLOYEE SHOES STORE");

        jLabel6.setText("Employe Address");

        btnback.setText("Back");
        btnback.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnbackActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addGap(30, 30, 30)
                .addComponent(btnback, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(15, 15, 15))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(127, 127, 127)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(49, 49, 49)
                                .addComponent(empid, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(27, 27, 27)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(28, 28, 28)
                                .addComponent(empname, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(28, 28, 28)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(empadr, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btnupdate))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(empnum, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(btninsert)))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(btndelete)
                                    .addComponent(btnread)))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(67, 67, 67)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 669, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(69, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(btnback, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(empid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel1)
                        .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(empname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(8, 8, 8)
                        .addComponent(jLabel6)
                        .addGap(14, 14, 14)
                        .addComponent(jLabel4))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(38, 38, 38)
                                .addComponent(empadr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(8, 8, 8))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(btnupdate)
                                    .addComponent(btnread))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(empnum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btninsert)
                            .addComponent(btndelete))))
                .addGap(29, 29, 29)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTable2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable2MouseClicked
        // TODO add your handling code here:
        int i = jTable2.getSelectedRow();
        TableModel model = jTable2.getModel();
        empid.setText(model.getValueAt(i,0).toString());
        empname.setText(model.getValueAt(i,1).toString());
        empadr.setText(model.getValueAt(i,2).toString());
        empnum.setText(model.getValueAt(i,3).toString());
    }//GEN-LAST:event_jTable2MouseClicked

    private void searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchActionPerformed

    private void searchKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchKeyTyped
        // TODO add your handling code here:
        DefaultTableModel tabModel = new DefaultTableModel(new Object []{"employee_id","employee_name","employee_address","phone_number"},0);
        try{

            Class.forName(driver);
            Connection con = DriverManager.getConnection(url,user,password);
            String query = "SELECT * FROM employee WHERE employee_id LIKE '%" + search.getText() + "%' OR employee_name LIKE '%" + search.getText() + "%' OR employee_address LIKE '%" + search.getText() + "%' OR phone_number LIKE '%" + search.getText() + "%'";
            rs = st.executeQuery(query);
            while (rs.next()){
                tabModel.addRow(new Object[]{rs.getString("employee_id"),rs.getString("employee_name"), rs.getString("employee_address"), rs.getString("phone_number")});
            }
            jTable2.setModel(tabModel);
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }    // TODO add your ha
    }//GEN-LAST:event_searchKeyTyped

    private void btninsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btninsertActionPerformed
        // TODO add your handling code here:
        try
        {
            //Memanggil driver yang uda dideklarasikan diawal
            Class.forName(driver);
            //Koneksi ke Database Sql server Variabel Con sebagai Connection
            //DriverManager mengatur semua perintah didalamnya dengan fungsi getConnection
            //Deklarasi url database, user, dan passwordS
            Connection con = DriverManager.getConnection(url, user, password);
            //menjalankan perintah sql insert dalam java, gettext untuk mendapatkan data data
            //textfield
            String sql ="insert into employee values ('"+empid.getText()+"','"+empname.getText()+"','"+empadr.getText()+"','"+empnum.getText()+"')";
            //perintah untuk menjalankan perintah diatas
            st.executeUpdate(sql);
            //untuk menampilkan dialog message insert successfully
            JOptionPane.showMessageDialog(this, "Insert Successfully");
            //object tabModel dari class defaultTableModel dimulai dari setRowCount indeks 0
            tabModel.setRowCount(0);
            //definisi variabel st dengan object con untuk mengirim sql ke database
            st = con.createStatement();
            //perintah sql dijalankan
            rs = st.executeQuery("SELECT * FROM employee");
            // membaca per indeks data yang masuk
            while (rs.next()){
                tabModel.addRow(new Object[]{rs.getString("employee_id"),rs.getString("employee_name"), rs.getString("employee_address"),rs.getString("phone_number")});
            }
            //metode setModel dalam class jTable dalam menentukan model data
            jTable2.setModel(tabModel);
        }

        catch(Exception e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_btninsertActionPerformed

    private void btndeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btndeleteActionPerformed
        // TODO add your handling code here:
        try
        {

            Class.forName(driver);
            Connection con = DriverManager.getConnection(url, user, password);
            int i = jTable2.getSelectedRow();
            String value= (jTable2.getModel().getValueAt(i, 0).toString());
            String sql ="delete from dbo.employee where employee_id = '"+empid.getText()+"' ";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.execute();
            //st = con.createStatement();
            //st.execute(sql);

            JOptionPane.showMessageDialog(this, "Delete Successfully");
            tabModel.setRowCount(0);
            st = con.createStatement();
            rs = st.executeQuery("SELECT * FROM employee");
            while (rs.next()){
                { tabModel.addRow(new Object[]{rs.getString("employee_id"),rs.getString("employee_name"), rs.getString("employee_address"),rs.getString("phone_number")});
                }
             }
            jTable2.setModel(tabModel);

        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_btndeleteActionPerformed

    private void btnupdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnupdateActionPerformed
        // TODO add your handling code here:
       try {
    Class.forName(driver);
    Connection con = DriverManager.getConnection(url, user, password);
    int i = jTable2.getSelectedRow();
    String value = (jTable2.getModel().getValueAt(i, 0).toString());
    String sql = "UPDATE dbo.employee SET employee_id = ?, employee_name = ?, employee_address = ? WHERE phone_number = ?";
    PreparedStatement pst = con.prepareStatement(sql);
    pst.setString(1, empid.getText());
    pst.setString(2, empname.getText());
    pst.setString(3, empadr.getText());
    pst.setString(4, empnum.getText());
    int rowsUpdated = pst.executeUpdate();
    if (rowsUpdated > 0) {
        JOptionPane.showMessageDialog(this, "Update Successfully");
        tabModel.setRowCount(0);
        st = con.createStatement();
        rs = st.executeQuery("SELECT * FROM employee");
        while (rs.next()) {
            tabModel.addRow(new Object[]{rs.getString("employee_id"), rs.getString("employee_name"), rs.getString("employee_address"), rs.getString("phone_number")});
        }
        jTable2.setModel(tabModel);
    } else {
        JOptionPane.showMessageDialog(this, "Update Failed");
    }
} catch (Exception e) {
    JOptionPane.showMessageDialog(this, e.getMessage());
}

    }//GEN-LAST:event_btnupdateActionPerformed

    private void btnreadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnreadActionPerformed
        // TODO add your handling code here:

        try
        {
            Class.forName(driver);
            Connection con = DriverManager.getConnection(url, user, password);
            tabModel.setRowCount(0);
            st = con.createStatement();
            rs = st.executeQuery("SELECT * FROM employee");
            while (rs.next())
            { tabModel.addRow(new Object[]{rs.getString("employee_id"),rs.getString("employee_name"), rs.getString("employee_address"),rs.getString("phone_number")});
                }
            jTable2.setModel(tabModel);
        }

        catch(ClassNotFoundException | SQLException e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_btnreadActionPerformed

    private void btnbackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnbackActionPerformed
        // TODO add your handling code here:
                    Employee Employee1 = new Employee ();
                    MenuUtama menu = new MenuUtama();
                    menu.setVisible(true);
                    Employee1.setVisible(false);
                    dispose();
    }//GEN-LAST:event_btnbackActionPerformed

    private void btninsertMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_btninsertMouseClicked
        // TODO add your handling code here:
        try
        {
            //Memanggil driver yang uda dideklarasikan diawal
            Class.forName(driver);
            //Koneksi ke Database Sql server Variabel Con sebagai Connection
            //DriverManager mengatur semua perintah didalamnya dengan fungsi getConnection
            //Deklarasi url database, user, dan passwordS
            Connection con = DriverManager.getConnection(url, user, password);
            //menjalankan perintah sql insert dalam java, gettext untuk mendapatkan data data
            //textfield
            String sql = "INSERT INTO employee VALUES ('" + empid.getText() + "', '" + empname.getText() + "', '" + empadr.getText() + "', '" + empnum.getText() + "')";
            st.executeUpdate(sql);
            //untuk menampilkan dialog message insert successfully 
            JOptionPane.showMessageDialog(this, "Insert Successfully");
            //object tabModel dari class defaultTableModel dimulai dari setRowCount indeks 0
            tabModel.setRowCount(0);
            //definisi variabel st dengan object con untuk mengirim sql ke database
            st = con.createStatement();
            //perintah sql dijalankan
            rs = st.executeQuery("SELECT * FROM employee");
            // membaca per indeks data yang masuk
            while (rs.next())
            //metode setModel dalam class jTable dalam menentukan model data
            jTable2.setModel(tabModel);
            { tabModel.addRow(new Object[]{rs.getString("employee_id"),rs.getString("employee_name"), rs.getString("employee_address"),rs.getString("phone_number")});
                }
        }

        catch(Exception e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }                       
    }//GEN-LAST:event_btninsertMouseClicked

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:
         try{
            Class.forName(driver);
            Connection con = DriverManager.getConnection(url,user,password);
            st = con.createStatement();
            rs = st.executeQuery("SELECT * from employee");
            while (rs.next()){
                tabModel.addRow(new Object[]{rs.getString("employee_id"),rs.getString("employee_name"), rs.getString("employee_address"),rs.getString("phone_number")});
            }
            jTable2.setModel(tabModel);
            } catch(Exception err){
            System.out.println(err.getMessage());
            
            
        }   
    }//GEN-LAST:event_formWindowOpened

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Employee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Employee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Employee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Employee.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Employee().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnback;
    private javax.swing.JButton btndelete;
    private javax.swing.JButton btninsert;
    private javax.swing.JButton btnread;
    private javax.swing.JButton btnupdate;
    private javax.swing.JTextField empadr;
    private javax.swing.JTextField empid;
    private javax.swing.JTextField empname;
    private javax.swing.JTextField empnum;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField search;
    // End of variables declaration//GEN-END:variables
}