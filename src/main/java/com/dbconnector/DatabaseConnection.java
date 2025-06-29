package com.dbconnector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private Connection connection;
    
    public Connection getConnection() {
        return connection;
    }
    
    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    
    public Connection connect(String host, String port, String dbName, String user, String password) throws SQLException {
        // Close existing connection if any
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        
        // Connect to database
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        connection = DriverManager.getConnection(url, user, password);
        return connection;
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}