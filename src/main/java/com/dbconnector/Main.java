package com.dbconnector;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            // Load PostgreSQL JDBC Driver
            Class.forName("org.postgresql.Driver");
            
            // Set look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            SwingUtilities.invokeLater(() -> {
                new DatabaseViewer();
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}