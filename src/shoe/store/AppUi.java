package shoe.store;

import java.awt.EventQueue;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

final class AppUi {
    private static final Logger LOGGER = Logger.getLogger(AppUi.class.getName());

    private AppUi() {
    }

    static void launch(Supplier<? extends JFrame> factory) {
        EventQueue.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo theme : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(theme.getName())) {
                        UIManager.setLookAndFeel(theme.getClassName());
                        break;
                    }
                }
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
                LOGGER.log(Level.FINE, "Tema Nimbus tidak tersedia.", ex);
            }
            JFrame window = factory.get();
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    static void open(JFrame current, JFrame next) {
        next.setLocationRelativeTo(current);
        next.setVisible(true);
        current.dispose();
    }
}
