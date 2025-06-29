package com.dbconnector.service;

import com.dbconnector.model.CloudConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class CloudConnectionService {
    
    @Autowired
    private LoggingService loggingService;
    
    private final Map<String, Connection> cloudConnections = new ConcurrentHashMap<>();
    
    /**
     * Create connection to AWS RDS
     */
    public Connection connectToAWSRDS(CloudConnectionInfo connectionInfo) throws SQLException {
        loggingService.logInfo("Connecting to AWS RDS: " + connectionInfo.getEndpoint());
        
        Properties props = new Properties();
        props.setProperty("user", connectionInfo.getUsername());
        props.setProperty("password", connectionInfo.getPassword());
        
        // Enable SSL for RDS
        if (connectionInfo.isUseSSL()) {
            props.setProperty("useSSL", "true");
            props.setProperty("requireSSL", "true");
            props.setProperty("verifyServerCertificate", "true");
        }
        
        // AWS RDS specific properties
        if (connectionInfo.isUseIAM()) {
            props.setProperty("useAWSIAM", "true");
            // Note: This would require AWS SDK integration
        }
        
        String url = buildRDSConnectionUrl(connectionInfo);
        Connection connection = DriverManager.getConnection(url, props);
        
        cloudConnections.put(connectionInfo.getId(), connection);
        loggingService.logInfo("Successfully connected to AWS RDS");
        
        return connection;
    }
    
    /**
     * Create connection to Azure SQL Database
     */
    public Connection connectToAzureSQL(CloudConnectionInfo connectionInfo) throws SQLException {
        loggingService.logInfo("Connecting to Azure SQL Database: " + connectionInfo.getEndpoint());
        
        Properties props = new Properties();
        props.setProperty("user", connectionInfo.getUsername());
        props.setProperty("password", connectionInfo.getPassword());
        
        // Azure SQL specific properties
        props.setProperty("encrypt", "true");
        props.setProperty("trustServerCertificate", "false");
        props.setProperty("hostNameInCertificate", "*.database.windows.net");
        props.setProperty("loginTimeout", "30");
        
        // Azure AD authentication
        if (connectionInfo.isUseAzureAD()) {
            props.setProperty("authentication", "ActiveDirectoryPassword");
            // Note: This would require Azure SDK integration
        }
        
        String url = buildAzureSQLConnectionUrl(connectionInfo);
        Connection connection = DriverManager.getConnection(url, props);
        
        cloudConnections.put(connectionInfo.getId(), connection);
        loggingService.logInfo("Successfully connected to Azure SQL Database");
        
        return connection;
    }
    
    /**
     * Create connection to Google Cloud SQL
     */
    public Connection connectToGoogleCloudSQL(CloudConnectionInfo connectionInfo) throws SQLException {
        loggingService.logInfo("Connecting to Google Cloud SQL: " + connectionInfo.getEndpoint());
        
        Properties props = new Properties();
        props.setProperty("user", connectionInfo.getUsername());
        props.setProperty("password", connectionInfo.getPassword());
        
        // Google Cloud SQL specific properties
        if (connectionInfo.isUseCloudSQLProxy()) {
            props.setProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
            props.setProperty("cloudSqlInstance", connectionInfo.getCloudSQLInstance());
            // Note: This would require Google Cloud SQL connector
        }
        
        // SSL configuration for Cloud SQL
        if (connectionInfo.isUseSSL()) {
            props.setProperty("sslmode", "require");
        }
        
        String url = buildGoogleCloudSQLConnectionUrl(connectionInfo);
        Connection connection = DriverManager.getConnection(url, props);
        
        cloudConnections.put(connectionInfo.getId(), connection);
        loggingService.logInfo("Successfully connected to Google Cloud SQL");
        
        return connection;
    }
    
    /**
     * Test cloud database connection
     */
    public boolean testCloudConnection(CloudConnectionInfo connectionInfo) {
        try {
            Connection testConnection = null;
            
            switch (connectionInfo.getCloudProvider()) {
                case "aws":
                    testConnection = connectToAWSRDS(connectionInfo);
                    break;
                case "azure":
                    testConnection = connectToAzureSQL(connectionInfo);
                    break;
                case "gcp":
                    testConnection = connectToGoogleCloudSQL(connectionInfo);
                    break;
                default:
                    throw new SQLException("Unsupported cloud provider: " + connectionInfo.getCloudProvider());
            }
            
            // Test the connection with a simple query
            if (testConnection != null && !testConnection.isClosed()) {
                testConnection.createStatement().execute("SELECT 1");
                testConnection.close();
                return true;
            }
            
        } catch (SQLException e) {
            loggingService.logError("Cloud connection test failed", e);
        }
        
        return false;
    }
    
    /**
     * Get active cloud connection
     */
    public Connection getCloudConnection(String connectionId) {
        return cloudConnections.get(connectionId);
    }
    
    /**
     * Close cloud connection
     */
    public void closeCloudConnection(String connectionId) {
        Connection connection = cloudConnections.remove(connectionId);
        if (connection != null) {
            try {
                connection.close();
                loggingService.logInfo("Closed cloud connection: " + connectionId);
            } catch (SQLException e) {
                loggingService.logError("Error closing cloud connection", e);
            }
        }
    }
    
    private String buildRDSConnectionUrl(CloudConnectionInfo connectionInfo) {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:").append(connectionInfo.getDbType()).append("://");
        url.append(connectionInfo.getEndpoint());
        if (connectionInfo.getPort() > 0) {
            url.append(":").append(connectionInfo.getPort());
        }
        url.append("/").append(connectionInfo.getDatabase());
        return url.toString();
    }
    
    private String buildAzureSQLConnectionUrl(CloudConnectionInfo connectionInfo) {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:sqlserver://");
        url.append(connectionInfo.getEndpoint());
        if (connectionInfo.getPort() > 0) {
            url.append(":").append(connectionInfo.getPort());
        }
        url.append(";database=").append(connectionInfo.getDatabase());
        return url.toString();
    }
    
    private String buildGoogleCloudSQLConnectionUrl(CloudConnectionInfo connectionInfo) {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:").append(connectionInfo.getDbType()).append("://");
        
        if (connectionInfo.isUseCloudSQLProxy()) {
            url.append("google/").append(connectionInfo.getCloudSQLInstance());
        } else {
            url.append(connectionInfo.getEndpoint());
            if (connectionInfo.getPort() > 0) {
                url.append(":").append(connectionInfo.getPort());
            }
        }
        
        url.append("/").append(connectionInfo.getDatabase());
        return url.toString();
    }
}