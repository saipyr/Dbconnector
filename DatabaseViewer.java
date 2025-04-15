import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class DatabaseViewer extends JFrame {
    private ConnectionPanel connectionPanel;
    private QueryPanel queryPanel;
    private ResultPanel resultPanel;
    private DatabaseConnection dbConnection;
    
    public DatabaseViewer() {
        setTitle("PostgreSQL Database Viewer");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        dbConnection = new DatabaseConnection();
        
        // Create panels
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        connectionPanel = new ConnectionPanel(this);
        queryPanel = new QueryPanel(this);
        resultPanel = new ResultPanel();
        
        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        mainPanel.add(queryPanel, BorderLayout.CENTER);
        mainPanel.add(resultPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        setVisible(true);
    }
    
    public Connection getConnection() {
        return dbConnection.getConnection();
    }
    
    public void setConnection(Connection connection) {
        dbConnection.setConnection(connection);
    }
    
    public ConnectionPanel getConnectionPanel() {
        return connectionPanel;
    }
    
    public QueryPanel getQueryPanel() {
        return queryPanel;
    }
    
    public ResultPanel getResultPanel() {
        return resultPanel;
    }
    
    public void updateStatus(String message) {
        connectionPanel.updateStatus(message);
    }
}