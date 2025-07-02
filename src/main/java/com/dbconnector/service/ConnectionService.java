package com.dbconnector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class ConnectionService {
    
    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();
    
    public Connection getConnection(String connectionId) {
        return activeConnections.get(connectionId);
    }
    
    public void addConnection(String connectionId, Connection connection) {
        activeConnections.put(connectionId, connection);
    }
    
    @Autowired
    private LoggingService loggingService;
    
    public void removeConnection(String connectionId) {
        Connection connection = activeConnections.remove(connectionId);
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Log error but don't throw
                loggingService.logError("Error closing connection: " + connectionId, e);
            }
        }
    }
    
    public boolean hasConnection(String connectionId) {
        return activeConnections.containsKey(connectionId);
    }
}