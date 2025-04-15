import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class ConnectionPanel extends JPanel {
    private JTextField hostField, portField, dbNameField, userField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JLabel statusLabel;
    private DatabaseViewer parent;
    
    public ConnectionPanel(DatabaseViewer parent) {
        this.parent = parent;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder("Database Connection"));
        
        initComponents();
    }
    
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Host
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("Host:"), gbc);
        
        gbc.gridx = 1;
        hostField = new JTextField("localhost", 15);
        add(hostField, gbc);
        
        // Port
        gbc.gridx = 2;
        add(new JLabel("Port:"), gbc);
        
        gbc.gridx = 3;
        portField = new JTextField("5432", 5);
        add(portField, gbc);
        
        // Database name
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Database:"), gbc);
        
        gbc.gridx = 1;
        dbNameField = new JTextField(15);
        add(dbNameField, gbc);
        
        // Username
        gbc.gridx = 2;
        add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 3;
        userField = new JTextField("postgres", 10);
        add(userField, gbc);
        
        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);
        
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        add(passwordField, gbc);
        
        // Connect button
        gbc.gridx = 3;
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connectToDatabase());
        add(connectButton, gbc);
        
        // Status label
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 4;
        statusLabel = new JLabel("Not connected");
        add(statusLabel, gbc);
    }
    
    private void connectToDatabase() {
        String host = hostField.getText();
        String port = portField.getText();
        String dbName = dbNameField.getText();
        String user = userField.getText();
        String password = new String(passwordField.getPassword());
        
        try {
            DatabaseConnection dbConnection = new DatabaseConnection();
            parent.setConnection(dbConnection.connect(host, port, dbName, user, password));
            
            updateStatus("Connected to " + dbName);
            
            // Load tables
            parent.getQueryPanel().loadTables();
            
        } catch (SQLException ex) {
            updateStatus("Connection failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public void updateStatus(String message) {
        statusLabel.setText(message);
    }
}