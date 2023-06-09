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



public class Supplier extends javax.swing.JFrame {

    /**
     * Creates new form Customer
     */
    public Supplier() {
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
    DefaultTableModel tabModel = new DefaultTableModel(new Object []{"supplier_id","supplier_name","supplier_address","supplier_brand","phone_number"},0);
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
        supnum = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        supid = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        supname = new javax.swing.JTextField();
        search = new javax.swing.JTextField();
        btninsert = new javax.swing.JButton();
        btndelete = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        btnupdate = new javax.swing.JButton();
        btnread = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        supadr = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        supbrand = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jLabel2.setText("Supplier ID");

        jLabel3.setText("Supplier Name");

        jLabel4.setText("Phone Number");

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Supplier ID", "Supplier Name", "Supplier Address", "Supplier Brand", "Phone Number"
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
        jLabel5.setText("SUPPLIER SHOES STORE");

        jLabel6.setText("Supplier Address");

        jButton1.setText("Back");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jLabel7.setText("Supplier  Brand");

        supbrand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                supbrandActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(153, 153, 153)
                .addComponent(jLabel5)
                .addGap(63, 63, 63)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(127, 127, 127)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49)
                .addComponent(supid, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(27, 27, 27)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(127, 127, 127)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(supname, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(127, 127, 127)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(supadr, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnupdate)
                .addGap(18, 18, 18)
                .addComponent(btnread))
            .addGroup(layout.createSequentialGroup()
                .addGap(130, 130, 130)
                .addComponent(jLabel7)
                .addGap(49, 49, 49)
                .addComponent(supbrand, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19)
                .addComponent(btninsert)
                .addGap(28, 28, 28)
                .addComponent(btndelete))
            .addGroup(layout.createSequentialGroup()
                .addGap(130, 130, 130)
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(28, 28, 28)
                .addComponent(supnum, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(layout.createSequentialGroup()
                .addGap(80, 80, 80)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 669, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(35, 35, 35)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(supid, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel1))
                    .addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(supname, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(4, 4, 4)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnupdate)
                    .addComponent(btnread)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(supadr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btninsert)
                    .addComponent(btndelete)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(supbrand, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(supnum, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTable2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable2MouseClicked
        // TODO add your handling code here:
      int i = jTable2.getSelectedRow();
        TableModel model = jTable2.getModel();
        supid.setText(model.getValueAt(i,0).toString());
        supname.setText(model.getValueAt(i,1).toString());
        supadr.setText(model.getValueAt(i,2).toString());
        supbrand.setText(model.getValueAt(i,3).toString());
        supnum.setText (model.getValueAt(i, 4) .toString());
        

    }//GEN-LAST:event_jTable2MouseClicked

    private void searchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchActionPerformed

    private void searchKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_searchKeyTyped
        // TODO add your handling code here:
        DefaultTableModel tabModel = new DefaultTableModel(new Object []{"supplier_id","supplier_name","supplier_address","supplier_brand","phone_number"},0);
        try{
            Class.forName(driver);
            Connection con = DriverManager.getConnection(url,user,password);
            String query = "SELECT * FROM supplier WHERE supplier_id like '%" + search.getText() + "%' OR supplier_name like '%" + search.getText() + "%' OR supplier_address like '%" + search.getText() + "%' OR supplier_brand like '%" + search.getText()+"%'" + " OR phone_number like '%" + search.getText()+"%'" ;
            rs = st.executeQuery(query);
            while (rs.next()){
                tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
            }
            jTable2.setModel(tabModel);
        }catch(Exception e){
            JOptionPane.showMessageDialog(null, e);
        }

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
            String sql ="insert into supplier values ('"+supid.getText()+"','"+supname.getText()+"','"+supadr.getText()+"','"+supbrand.getText()+"','"+supnum.getText()+"')";
            //perintah untuk menjalankan perintah diatas
            st.executeUpdate(sql);
            //untuk menampilkan dialog message insert successfully
            JOptionPane.showMessageDialog(this, "Insert Successfully");
            //object tabModel dari class defaultTableModel dimulai dari setRowCount indeks 0
            tabModel.setRowCount(0);
            //definisi variabel st dengan object con untuk mengirim sql ke database
            st = con.createStatement();
            //perintah sql dijalankan
            rs = st.executeQuery("SELECT * FROM supplier");
            // membaca per indeks data yang masuk
            while (rs.next()){
                tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
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
            String sql ="delete from dbo.supplier where supplier_id = '"+supid.getText()+"' ";
            PreparedStatement pst = con.prepareStatement(sql);
            pst.execute();
            //st = con.createStatement();
            //st.execute(sql);

            JOptionPane.showMessageDialog(this, "Delete Successfully");
            tabModel.setRowCount(0);
            st = con.createStatement();
            rs = st.executeQuery("SELECT * FROM supplier");
            while (rs.next()){
               tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
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
    String sql = "UPDATE dbo.supplier SET supplier_id = ?, supplier_name = ?, supplier_address = ?, supplier_brand = ? WHERE phone_number = ?";
    PreparedStatement pst = con.prepareStatement(sql);
    pst.setString(1, supid.getText());
    pst.setString(2, supname.getText());
    pst.setString(3, supadr.getText());
    pst.setString(4, supbrand.getText());
    pst.setString(5, supnum.getText());
    int rowsUpdated = pst.executeUpdate();
    if (rowsUpdated > 0) {
        JOptionPane.showMessageDialog(this, "Update Successfully");
        tabModel.setRowCount(0);
        st = con.createStatement();
        rs = st.executeQuery("SELECT * FROM supplier");
        while (rs.next()) {
            tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
        } 
        jTable2.setModel(tabModel);
    } else {
        JOptionPane.showMessageDialog(this, "Update Failed");
    }
} catch (Exception e) {
    e.printStackTrace();
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
            rs = st.executeQuery("SELECT * FROM supplier");
            while (rs.next())
            {tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
            } 
            jTable2.setModel(tabModel);
        }

        catch(ClassNotFoundException | SQLException e)
        {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_btnreadActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
                    Supplier supplier1 = new Supplier ();
                    MenuUtama menu = new MenuUtama();
                    menu.setVisible(true);
                    supplier1.setVisible(false);
                    dispose();
                    
    }//GEN-LAST:event_jButton1ActionPerformed

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
            String sql = "INSERT INTO supplier VALUES ('" + supid.getText() + "', '" + supname.getText() + "', '" + supadr.getText() + "','" + supbrand.getText() + "' '" + supnum.getText() + "')";
            st.executeUpdate(sql);
            //untuk menampilkan dialog message insert successfully 
            JOptionPane.showMessageDialog(this, "Insert Successfully");
            //object tabModel dari class defaultTableModel dimulai dari setRowCount indeks 0
            tabModel.setRowCount(0);
            //definisi variabel st dengan object con untuk mengirim sql ke database
            st = con.createStatement();
            //perintah sql dijalankan
            rs = st.executeQuery("SELECT * FROM supplier");
            // membaca per indeks data yang masuk
            while (rs.next())
            //metode setModel dalam class jTable dalam menentukan model data
            jTable2.setModel(tabModel);
            {tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
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
            rs = st.executeQuery("SELECT * from supplier");
            while (rs.next()){
                tabModel.addRow(new Object[]{rs.getString("supplier_id"),rs.getString("supplier_name"), rs.getString("supplier_address"),rs.getString("supplier_brand"), rs.getString("phone_number")});
            } 
            jTable2.setModel(tabModel);
            } catch(Exception err){
            System.out.println(err.getMessage());
            
            
        }   
    }//GEN-LAST:event_formWindowOpened

    private void supbrandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_supbrandActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_supbrandActionPerformed

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
            java.util.logging.Logger.getLogger(Supplier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Supplier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Supplier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Supplier.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Supplier().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btndelete;
    private javax.swing.JButton btninsert;
    private javax.swing.JButton btnread;
    private javax.swing.JButton btnupdate;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField search;
    private javax.swing.JTextField supadr;
    private javax.swing.JTextField supbrand;
    private javax.swing.JTextField supid;
    private javax.swing.JTextField supname;
    private javax.swing.JTextField supnum;
    // End of variables declaration//GEN-END:variables
}
