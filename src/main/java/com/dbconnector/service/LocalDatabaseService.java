package com.dbconnector.service;

import com.dbconnector.model.LocalConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocalDatabaseService {
    
    @Autowired
    private LoggingService loggingService;
    
    @Autowired
    private CustomDriverService customDriverService;
    
    @Autowired
    private EnhancedConnectionPoolService connectionPoolService;
    
    private final Map<String, Connection> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> customClassLoaders = new ConcurrentHashMap<>();
    
    /**
     * Connect to local database with enhanced driver support
     */
    public Connection connectToLocalDatabase(LocalConnectionInfo connectionInfo) throws SQLException {
        loggingService.logInfo("Connecting to local database: " + connectionInfo.getDbType());
        
        try {
            // Load appropriate driver
            loadDatabaseDriver(connectionInfo);
            
            // Build connection URL
            String connectionUrl = buildConnectionUrl(connectionInfo);
            
            // Create connection properties
            Properties props = createConnectionProperties(connectionInfo);
            
            // Establish connection
            Connection connection = DriverManager.getConnection(connectionUrl, props);
            
            // Configure connection
            configureConnection(connection, connectionInfo);
            
            // Store active connection
            activeConnections.put(connectionInfo.getId(), connection);
            
            loggingService.logInfo("Successfully connected to local database: " + connectionInfo.getDbType());
            
            return connection;
            
        } catch (Exception e) {
            loggingService.logError("Failed to connect to local database", e);
            throw new SQLException("Connection failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create optimized connection pool for local database
     */
    public Connection createLocalConnectionPool(LocalConnectionInfo connectionInfo) throws SQLException {
        loggingService.logInfo("Creating connection pool for local database: " + connectionInfo.getDbType());
        
        try {
            // Load driver first
            loadDatabaseDriver(connectionInfo);
            
            // Build connection URL
            String connectionUrl = buildConnectionUrl(connectionInfo);
            
            // Create pool options
            Map<String, Object> poolOptions = createPoolOptions(connectionInfo);
            
            // Create connection pool
            javax.sql.DataSource dataSource = connectionPoolService.createCloudConnectionPool(
                connectionInfo.getId(),
                connectionUrl,
                connectionInfo.getUsername(),
                connectionInfo.getPassword(),
                "local-" + connectionInfo.getDbType(),
                poolOptions
            );
            
            // Get connection from pool
            Connection connection = dataSource.getConnection();
            
            loggingService.logInfo("Connection pool created successfully for: " + connectionInfo.getDbType());
            
            return connection;
            
        } catch (Exception e) {
            loggingService.logError("Failed to create connection pool", e);
            throw new SQLException("Pool creation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load database driver (built-in or custom)
     */
    private void loadDatabaseDriver(LocalConnectionInfo connectionInfo) throws Exception {
        String dbType = connectionInfo.getDbType();
        
        if (connectionInfo.isUseCustomDriver() && connectionInfo.getCustomDriverId() != null) {
            // Load custom driver
            loadCustomDriver(connectionInfo.getCustomDriverId());
        } else {
            // Load built-in driver
            loadBuiltInDriver(dbType);
        }
    }
    
    /**
     * Load built-in database drivers
     */
    private void loadBuiltInDriver(String dbType) throws ClassNotFoundException {
        String driverClass = getBuiltInDriverClass(dbType);
        
        if (driverClass != null) {
            Class.forName(driverClass);
            loggingService.logInfo("Loaded built-in driver for: " + dbType);
        } else {
            throw new ClassNotFoundException("No built-in driver available for: " + dbType);
        }
    }
    
    /**
     * Load custom JDBC driver
     */
    private void loadCustomDriver(String customDriverId) throws Exception {
        CustomDriverService.CustomDriverInfo driverInfo = customDriverService.getCustomDriver(customDriverId);
        
        if (driverInfo == null) {
            throw new Exception("Custom driver not found: " + customDriverId);
        }
        
        // Check if already loaded
        if (customClassLoaders.containsKey(customDriverId)) {
            loggingService.logInfo("Custom driver already loaded: " + driverInfo.getName());
            return;
        }
        
        // Load JAR file
        File jarFile = new File(driverInfo.getJarFilePath());
        if (!jarFile.exists()) {
            throw new Exception("Driver JAR file not found: " + driverInfo.getJarFilePath());
        }
        
        // Create custom class loader
        URL[] urls = { jarFile.toURI().toURL() };
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        
        // Load driver class
        Class<?> driverClass = classLoader.loadClass(driverInfo.getDriverClassName());
        Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
        
        // Register driver
        DriverManager.registerDriver(new DriverWrapper(driver));
        
        // Store class loader
        customClassLoaders.put(customDriverId, classLoader);
        
        loggingService.logInfo("Loaded custom driver: " + driverInfo.getName());
    }
    
    /**
     * Get built-in driver class name
     */
    private String getBuiltInDriverClass(String dbType) {
        Map<String, String> driverClasses = new HashMap<>();
        driverClasses.put("postgresql", "org.postgresql.Driver");
        driverClasses.put("mysql", "com.mysql.cj.jdbc.Driver");
        driverClasses.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        driverClasses.put("oracle", "oracle.jdbc.driver.OracleDriver");
        driverClasses.put("sqlite", "org.sqlite.JDBC");
        driverClasses.put("h2", "org.h2.Driver");
        driverClasses.put("hsqldb", "org.hsqldb.jdbc.JDBCDriver");
        driverClasses.put("derby", "org.apache.derby.jdbc.EmbeddedDriver");
        driverClasses.put("mariadb", "org.mariadb.jdbc.Driver");
        driverClasses.put("firebird", "org.firebirdsql.jdbc.FBDriver");
        
        return driverClasses.get(dbType.toLowerCase());
    }
    
    /**
     * Build connection URL based on database type
     */
    private String buildConnectionUrl(LocalConnectionInfo connectionInfo) {
        String dbType = connectionInfo.getDbType().toLowerCase();
        
        if (connectionInfo.isUseCustomDriver() && connectionInfo.getCustomDriverId() != null) {
            // Use custom URL template
            CustomDriverService.CustomDriverInfo driverInfo = customDriverService.getCustomDriver(connectionInfo.getCustomDriverId());
            if (driverInfo != null) {
                return buildCustomConnectionUrl(driverInfo, connectionInfo);
            }
        }
        
        // Build standard connection URL
        return buildStandardConnectionUrl(dbType, connectionInfo);
    }
    
    /**
     * Build custom connection URL using template
     */
    private String buildCustomConnectionUrl(CustomDriverService.CustomDriverInfo driverInfo, LocalConnectionInfo connectionInfo) {
        String urlTemplate = driverInfo.getUrlTemplate();
        
        // Replace placeholders
        urlTemplate = urlTemplate.replace("{host}", connectionInfo.getHost());
        urlTemplate = urlTemplate.replace("{port}", String.valueOf(connectionInfo.getPort()));
        urlTemplate = urlTemplate.replace("{database}", connectionInfo.getDatabase());
        
        // Replace additional parameters
        if (connectionInfo.getAdditionalParams() != null) {
            for (Map.Entry<String, String> entry : connectionInfo.getAdditionalParams().entrySet()) {
                urlTemplate = urlTemplate.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return urlTemplate;
    }
    
    /**
     * Build standard connection URL
     */
    private String buildStandardConnectionUrl(String dbType, LocalConnectionInfo connectionInfo) {
        StringBuilder url = new StringBuilder();
        
        switch (dbType) {
            case "postgresql":
                url.append("jdbc:postgresql://")
                   .append(connectionInfo.getHost())
                   .append(":").append(connectionInfo.getPort())
                   .append("/").append(connectionInfo.getDatabase());
                break;
                
            case "mysql":
            case "mariadb":
                url.append("jdbc:").append(dbType).append("://")
                   .append(connectionInfo.getHost())
                   .append(":").append(connectionInfo.getPort())
                   .append("/").append(connectionInfo.getDatabase());
                break;
                
            case "sqlserver":
                url.append("jdbc:sqlserver://")
                   .append(connectionInfo.getHost())
                   .append(":").append(connectionInfo.getPort())
                   .append(";databaseName=").append(connectionInfo.getDatabase());
                break;
                
            case "oracle":
                url.append("jdbc:oracle:thin:@")
                   .append(connectionInfo.getHost())
                   .append(":").append(connectionInfo.getPort())
                   .append(":").append(connectionInfo.getDatabase());
                break;
                
            case "sqlite":
                url.append("jdbc:sqlite:").append(connectionInfo.getDatabase());
                break;
                
            case "h2":
                if (connectionInfo.getDatabase().startsWith("file:") || connectionInfo.getDatabase().startsWith("mem:")) {
                    url.append("jdbc:h2:").append(connectionInfo.getDatabase());
                } else {
                    url.append("jdbc:h2:file:").append(connectionInfo.getDatabase());
                }
                break;
                
            case "hsqldb":
                url.append("jdbc:hsqldb:file:").append(connectionInfo.getDatabase());
                break;
                
            case "derby":
                url.append("jdbc:derby:").append(connectionInfo.getDatabase()).append(";create=true");
                break;
                
            case "firebird":
                url.append("jdbc:firebirdsql://")
                   .append(connectionInfo.getHost())
                   .append(":").append(connectionInfo.getPort())
                   .append("/").append(connectionInfo.getDatabase());
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported database type: " + dbType);
        }
        
        // Add additional parameters
        if (connectionInfo.getConnectionParams() != null && !connectionInfo.getConnectionParams().isEmpty()) {
            String separator = url.toString().contains("?") ? "&" : "?";
            url.append(separator);
            
            List<String> params = new ArrayList<>();
            for (Map.Entry<String, String> entry : connectionInfo.getConnectionParams().entrySet()) {
                params.add(entry.getKey() + "=" + entry.getValue());
            }
            url.append(String.join("&", params));
        }
        
        return url.toString();
    }
    
    /**
     * Create connection properties
     */
    private Properties createConnectionProperties(LocalConnectionInfo connectionInfo) {
        Properties props = new Properties();
        
        // Basic authentication
        if (connectionInfo.getUsername() != null) {
            props.setProperty("user", connectionInfo.getUsername());
        }
        if (connectionInfo.getPassword() != null) {
            props.setProperty("password", connectionInfo.getPassword());
        }
        
        // SSL/TLS configuration
        if (connectionInfo.isUseSSL()) {
            configureSSLProperties(props, connectionInfo);
        }
        
        // Connection timeouts
        if (connectionInfo.getConnectionTimeout() > 0) {
            props.setProperty("connectTimeout", String.valueOf(connectionInfo.getConnectionTimeout() * 1000));
        }
        if (connectionInfo.getSocketTimeout() > 0) {
            props.setProperty("socketTimeout", String.valueOf(connectionInfo.getSocketTimeout() * 1000));
        }
        
        // Database-specific properties
        configureDatabaseSpecificProperties(props, connectionInfo);
        
        // Custom properties
        if (connectionInfo.getCustomProperties() != null) {
            props.putAll(connectionInfo.getCustomProperties());
        }
        
        return props;
    }
    
    /**
     * Configure SSL properties
     */
    private void configureSSLProperties(Properties props, LocalConnectionInfo connectionInfo) {
        String dbType = connectionInfo.getDbType().toLowerCase();
        
        switch (dbType) {
            case "postgresql":
                props.setProperty("ssl", "true");
                props.setProperty("sslmode", connectionInfo.getSslMode());
                if (connectionInfo.getSslCertPath() != null) {
                    props.setProperty("sslcert", connectionInfo.getSslCertPath());
                }
                if (connectionInfo.getSslKeyPath() != null) {
                    props.setProperty("sslkey", connectionInfo.getSslKeyPath());
                }
                if (connectionInfo.getSslRootCertPath() != null) {
                    props.setProperty("sslrootcert", connectionInfo.getSslRootCertPath());
                }
                break;
                
            case "mysql":
            case "mariadb":
                props.setProperty("useSSL", "true");
                props.setProperty("requireSSL", "true");
                props.setProperty("verifyServerCertificate", String.valueOf(connectionInfo.isVerifyServerCert()));
                if (connectionInfo.getSslCertPath() != null) {
                    props.setProperty("clientCertificateKeyStoreUrl", connectionInfo.getSslCertPath());
                }
                break;
                
            case "sqlserver":
                props.setProperty("encrypt", "true");
                props.setProperty("trustServerCertificate", String.valueOf(!connectionInfo.isVerifyServerCert()));
                break;
        }
    }
    
    /**
     * Configure database-specific properties
     */
    private void configureDatabaseSpecificProperties(Properties props, LocalConnectionInfo connectionInfo) {
        String dbType = connectionInfo.getDbType().toLowerCase();
        
        switch (dbType) {
            case "postgresql":
                props.setProperty("ApplicationName", "DB Connector");
                props.setProperty("prepareThreshold", "5");
                break;
                
            case "mysql":
            case "mariadb":
                props.setProperty("useUnicode", "true");
                props.setProperty("characterEncoding", "UTF-8");
                props.setProperty("autoReconnect", String.valueOf(connectionInfo.isAutoReconnect()));
                props.setProperty("cachePrepStmts", "true");
                props.setProperty("prepStmtCacheSize", "250");
                props.setProperty("prepStmtCacheSqlLimit", "2048");
                break;
                
            case "sqlserver":
                props.setProperty("applicationName", "DB Connector");
                props.setProperty("selectMethod", "cursor");
                break;
                
            case "oracle":
                props.setProperty("oracle.jdbc.ReadTimeout", "60000");
                props.setProperty("oracle.net.CONNECT_TIMEOUT", "30000");
                break;
        }
    }
    
    /**
     * Configure connection settings
     */
    private void configureConnection(Connection connection, LocalConnectionInfo connectionInfo) throws SQLException {
        // Set auto-commit
        connection.setAutoCommit(connectionInfo.isAutoCommit());
        
        // Set transaction isolation
        if (connectionInfo.getTransactionIsolation() != null) {
            connection.setTransactionIsolation(getTransactionIsolationLevel(connectionInfo.getTransactionIsolation()));
        }
        
        // Set read-only mode
        if (connectionInfo.isReadOnly()) {
            connection.setReadOnly(true);
        }
        
        // Set schema if specified
        if (connectionInfo.getDefaultSchema() != null) {
            try {
                connection.setSchema(connectionInfo.getDefaultSchema());
            } catch (SQLException e) {
                // Some databases don't support setSchema, try USE statement
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("USE " + connectionInfo.getDefaultSchema());
                } catch (SQLException ex) {
                    loggingService.logError("Failed to set default schema", ex);
                }
            }
        }
    }
    
    /**
     * Get transaction isolation level
     */
    private int getTransactionIsolationLevel(String isolation) {
        switch (isolation.toUpperCase()) {
            case "READ_UNCOMMITTED":
                return Connection.TRANSACTION_READ_UNCOMMITTED;
            case "READ_COMMITTED":
                return Connection.TRANSACTION_READ_COMMITTED;
            case "REPEATABLE_READ":
                return Connection.TRANSACTION_REPEATABLE_READ;
            case "SERIALIZABLE":
                return Connection.TRANSACTION_SERIALIZABLE;
            default:
                return Connection.TRANSACTION_READ_COMMITTED;
        }
    }
    
    /**
     * Create connection pool options
     */
    private Map<String, Object> createPoolOptions(LocalConnectionInfo connectionInfo) {
        Map<String, Object> options = new HashMap<>();
        
        // Pool sizing
        options.put("maxPoolSize", connectionInfo.getMaxPoolSize());
        options.put("minIdle", connectionInfo.getMinPoolSize());
        
        // Timeouts
        options.put("connectionTimeout", (long) connectionInfo.getConnectionTimeout() * 1000);
        options.put("idleTimeout", (long) connectionInfo.getIdleTimeout() * 1000);
        options.put("maxLifetime", (long) connectionInfo.getMaxLifetime() * 1000);
        
        // Validation
        options.put("validationTimeout", 5000L);
        
        return options;
    }
    
    /**
     * Test database connection
     */
    public boolean testConnection(LocalConnectionInfo connectionInfo) {
        try {
            Connection connection = connectToLocalDatabase(connectionInfo);
            
            // Test with a simple query
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SELECT 1");
            }
            
            connection.close();
            return true;
            
        } catch (Exception e) {
            loggingService.logError("Connection test failed", e);
            return false;
        }
    }
    
    /**
     * Get active connection
     */
    public Connection getConnection(String connectionId) {
        return activeConnections.get(connectionId);
    }
    
    /**
     * Close connection
     */
    public void closeConnection(String connectionId) {
        Connection connection = activeConnections.remove(connectionId);
        if (connection != null) {
            try {
                connection.close();
                loggingService.logInfo("Closed connection: " + connectionId);
            } catch (SQLException e) {
                loggingService.logError("Error closing connection", e);
            }
        }
    }
    
    /**
     * Get supported database types
     */
    public List<String> getSupportedDatabaseTypes() {
        return Arrays.asList(
            "postgresql", "mysql", "mariadb", "sqlserver", "oracle",
            "sqlite", "h2", "hsqldb", "derby", "firebird"
        );
    }
    
    /**
     * Get database metadata
     */
    public Map<String, Object> getDatabaseMetadata(String connectionId) throws SQLException {
        Connection connection = getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("Connection not found: " + connectionId);
        }
        
        DatabaseMetaData metaData = connection.getMetaData();
        Map<String, Object> metadata = new HashMap<>();
        
        metadata.put("databaseProductName", metaData.getDatabaseProductName());
        metadata.put("databaseProductVersion", metaData.getDatabaseProductVersion());
        metadata.put("driverName", metaData.getDriverName());
        metadata.put("driverVersion", metaData.getDriverVersion());
        metadata.put("url", metaData.getURL());
        metadata.put("userName", metaData.getUserName());
        metadata.put("supportsTransactions", metaData.supportsTransactions());
        metadata.put("supportsStoredProcedures", metaData.supportsStoredProcedures());
        metadata.put("maxConnections", metaData.getMaxConnections());
        
        return metadata;
    }
    
    /**
     * Driver wrapper for custom drivers
     */
    private static class DriverWrapper implements Driver {
        private final Driver driver;
        
        public DriverWrapper(Driver driver) {
            this.driver = driver;
        }
        
        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return driver.connect(url, info);
        }
        
        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }
        
        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }
        
        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }
        
        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }
        
        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }
        
        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }
    }
}