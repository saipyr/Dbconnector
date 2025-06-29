package com.dbconnector.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EnhancedConnectionPoolService {
    
    @Autowired
    private LoggingService loggingService;
    
    @Autowired
    private CloudMonitoringService monitoringService;
    
    private final Map<String, HikariDataSource> connectionPools = new ConcurrentHashMap<>();
    private final Map<String, ConnectionPoolConfig> poolConfigs = new ConcurrentHashMap<>();
    
    /**
     * Create optimized connection pool for cloud database
     */
    public DataSource createCloudConnectionPool(String connectionId, String jdbcUrl, 
                                               String username, String password, 
                                               String cloudProvider, Map<String, Object> options) {
        
        loggingService.logInfo("Creating optimized connection pool for " + cloudProvider + ": " + connectionId);
        
        // Close existing pool if exists
        closeConnectionPool(connectionId);
        
        HikariConfig config = new HikariConfig();
        
        // Basic connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("CloudPool-" + connectionId);
        
        // Cloud-optimized settings
        applyCloudOptimizations(config, cloudProvider, options);
        
        // Performance optimizations
        applyPerformanceOptimizations(config, options);
        
        // Monitoring and health checks
        applyMonitoringConfiguration(config, connectionId);
        
        try {
            HikariDataSource dataSource = new HikariDataSource(config);
            connectionPools.put(connectionId, dataSource);
            
            // Store configuration for monitoring
            ConnectionPoolConfig poolConfig = new ConnectionPoolConfig(connectionId, cloudProvider, config);
            poolConfigs.put(connectionId, poolConfig);
            
            loggingService.logInfo("Connection pool created successfully for " + connectionId);
            
            return dataSource;
            
        } catch (Exception e) {
            loggingService.logError("Failed to create connection pool for " + connectionId, e);
            throw new RuntimeException("Failed to create connection pool", e);
        }
    }
    
    /**
     * Apply cloud-specific optimizations
     */
    private void applyCloudOptimizations(HikariConfig config, String cloudProvider, Map<String, Object> options) {
        switch (cloudProvider.toLowerCase()) {
            case "aws":
                applyAWSOptimizations(config, options);
                break;
            case "azure":
                applyAzureOptimizations(config, options);
                break;
            case "gcp":
                applyGCPOptimizations(config, options);
                break;
            default:
                applyDefaultOptimizations(config);
        }
    }
    
    /**
     * Apply AWS-specific optimizations
     */
    private void applyAWSOptimizations(HikariConfig config, Map<String, Object> options) {
        // AWS RDS optimizations
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // AWS-specific connection properties
        config.addDataSourceProperty("useSSL", "true");
        config.addDataSourceProperty("requireSSL", "true");
        config.addDataSourceProperty("verifyServerCertificate", "true");
        config.addDataSourceProperty("useAWSIAM", options.getOrDefault("useIAM", false));
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        loggingService.logInfo("Applied AWS-specific connection pool optimizations");
    }
    
    /**
     * Apply Azure-specific optimizations
     */
    private void applyAzureOptimizations(HikariConfig config, Map<String, Object> options) {
        // Azure SQL Database optimizations
        config.setMaximumPoolSize(15);
        config.setMinimumIdle(3);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(300000); // 5 minutes (Azure has shorter idle timeouts)
        config.setMaxLifetime(1200000); // 20 minutes
        config.setLeakDetectionThreshold(60000);
        
        // Azure-specific connection properties
        config.addDataSourceProperty("encrypt", "true");
        config.addDataSourceProperty("trustServerCertificate", "false");
        config.addDataSourceProperty("hostNameInCertificate", "*.database.windows.net");
        config.addDataSourceProperty("loginTimeout", "30");
        
        if ((Boolean) options.getOrDefault("useAzureAD", false)) {
            config.addDataSourceProperty("authentication", "ActiveDirectoryPassword");
        }
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        loggingService.logInfo("Applied Azure-specific connection pool optimizations");
    }
    
    /**
     * Apply GCP-specific optimizations
     */
    private void applyGCPOptimizations(HikariConfig config, Map<String, Object> options) {
        // Google Cloud SQL optimizations
        config.setMaximumPoolSize(25);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000);
        
        // GCP-specific connection properties
        if ((Boolean) options.getOrDefault("useCloudSQLProxy", false)) {
            config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
            config.addDataSourceProperty("cloudSqlInstance", options.get("cloudSQLInstance"));
        }
        
        config.addDataSourceProperty("sslmode", "require");
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        loggingService.logInfo("Applied GCP-specific connection pool optimizations");
    }
    
    /**
     * Apply default optimizations
     */
    private void applyDefaultOptimizations(HikariConfig config) {
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        loggingService.logInfo("Applied default connection pool optimizations");
    }
    
    /**
     * Apply performance optimizations
     */
    private void applyPerformanceOptimizations(HikariConfig config, Map<String, Object> options) {
        // Performance tuning
        config.setAutoCommit(true);
        config.setReadOnly(false);
        config.setIsolateInternalQueries(false);
        config.setAllowPoolSuspension(false);
        config.setRegisterMbeans(true);
        
        // Custom options
        if (options.containsKey("maxPoolSize")) {
            config.setMaximumPoolSize((Integer) options.get("maxPoolSize"));
        }
        
        if (options.containsKey("minIdle")) {
            config.setMinimumIdle((Integer) options.get("minIdle"));
        }
        
        if (options.containsKey("connectionTimeout")) {
            config.setConnectionTimeout((Long) options.get("connectionTimeout"));
        }
        
        loggingService.logInfo("Applied performance optimizations to connection pool");
    }
    
    /**
     * Apply monitoring configuration
     */
    private void applyMonitoringConfiguration(HikariConfig config, String connectionId) {
        config.setMetricRegistry(null); // We'll use our custom monitoring
        config.setHealthCheckRegistry(null);
        
        // Enable JMX for monitoring
        config.setRegisterMbeans(true);
        
        loggingService.logInfo("Applied monitoring configuration for connection pool: " + connectionId);
    }
    
    /**
     * Get connection from pool
     */
    public Connection getConnection(String connectionId) throws SQLException {
        HikariDataSource dataSource = connectionPools.get(connectionId);
        if (dataSource == null) {
            throw new SQLException("Connection pool not found for: " + connectionId);
        }
        
        long startTime = System.currentTimeMillis();
        try {
            Connection connection = dataSource.getConnection();
            long duration = System.currentTimeMillis() - startTime;
            
            // Record metrics
            ConnectionPoolConfig config = poolConfigs.get(connectionId);
            if (config != null) {
                monitoringService.recordConnectionAttempt(config.getCloudProvider(), true, duration);
            }
            
            return connection;
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            
            // Record failed attempt
            ConnectionPoolConfig config = poolConfigs.get(connectionId);
            if (config != null) {
                monitoringService.recordConnectionAttempt(config.getCloudProvider(), false, duration);
            }
            
            throw e;
        }
    }
    
    /**
     * Get connection pool statistics
     */
    public Map<String, Object> getPoolStatistics(String connectionId) {
        HikariDataSource dataSource = connectionPools.get(connectionId);
        if (dataSource == null) {
            return Collections.emptyMap();
        }
        
        HikariPoolMXBean poolBean = dataSource.getHikariPoolMXBean();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("activeConnections", poolBean.getActiveConnections());
        stats.put("idleConnections", poolBean.getIdleConnections());
        stats.put("totalConnections", poolBean.getTotalConnections());
        stats.put("threadsAwaitingConnection", poolBean.getThreadsAwaitingConnection());
        
        return stats;
    }
    
    /**
     * Get all pool statistics
     */
    public Map<String, Map<String, Object>> getAllPoolStatistics() {
        Map<String, Map<String, Object>> allStats = new HashMap<>();
        
        for (String connectionId : connectionPools.keySet()) {
            allStats.put(connectionId, getPoolStatistics(connectionId));
        }
        
        return allStats;
    }
    
    /**
     * Close connection pool
     */
    public void closeConnectionPool(String connectionId) {
        HikariDataSource dataSource = connectionPools.remove(connectionId);
        if (dataSource != null) {
            dataSource.close();
            poolConfigs.remove(connectionId);
            loggingService.logInfo("Connection pool closed for: " + connectionId);
        }
    }
    
    /**
     * Close all connection pools
     */
    public void closeAllPools() {
        for (String connectionId : new ArrayList<>(connectionPools.keySet())) {
            closeConnectionPool(connectionId);
        }
        loggingService.logInfo("All connection pools closed");
    }
    
    /**
     * Connection pool configuration
     */
    private static class ConnectionPoolConfig {
        private final String connectionId;
        private final String cloudProvider;
        private final HikariConfig hikariConfig;
        
        public ConnectionPoolConfig(String connectionId, String cloudProvider, HikariConfig hikariConfig) {
            this.connectionId = connectionId;
            this.cloudProvider = cloudProvider;
            this.hikariConfig = hikariConfig;
        }
        
        public String getConnectionId() { return connectionId; }
        public String getCloudProvider() { return cloudProvider; }
        public HikariConfig getHikariConfig() { return hikariConfig; }
    }
}