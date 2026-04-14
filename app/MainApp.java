package app;

import com.hrms.ui.MainDashboard;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

/**
 * Main Entry Point for the HRMS Attrition Analysis System.
 */
public class MainApp {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global UI customizations for a consistent dark theme look in dialogs
        UIManager.put("OptionPane.background", new Color(22, 27, 44));
        UIManager.put("Panel.background", new Color(22, 27, 44));
        UIManager.put("OptionPane.messageForeground", new Color(237, 242, 247));

        SwingUtilities.invokeLater(MainDashboard::new);
    }
}
