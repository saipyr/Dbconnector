package com.dbconnector.model;

import java.util.Map;
import java.util.UUID;

public class LocalConnectionInfo {
    private String id;
    private String name;
    private String dbType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    
    // Custom driver support
    private boolean useCustomDriver = false;
    private String customDriverId;
    
    // SSL/TLS settings
    private boolean useSSL = false;
    private String sslMode = "prefer";
    private String sslCertPath;
    private String sslKeyPath;
    private String sslRootCertPath;
    private boolean verifyServerCert = true;
    
    // Connection settings
    private int connectionTimeout = 30;
    private int socketTimeout = 0;
    private boolean autoReconnect = true;
    private boolean autoCommit = true;
    private boolean readOnly = false;
    private String transactionIsolation = "READ_COMMITTED";
    private String defaultSchema;
    
    // Connection pool settings
    private int maxPoolSize = 10;
    private int minPoolSize = 2;
    private int idleTimeout = 600;
    private int maxLifetime = 1800;
    
    // Additional parameters
    private Map<String, String> connectionParams;
    private Map<String, String> additionalParams;
    private Map<String, Object> customProperties;
    
    public LocalConnectionInfo() {
        this.id = UUID.randomUUID().toString();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDbType() {
        return dbType;
    }
    
    public void setDbType(String dbType) {
        this.dbType = dbType;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getDatabase() {
        return database;
    }
    
    public void setDatabase(String database) {
        this.database = database;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isUseCustomDriver() {
        return useCustomDriver;
    }
    
    public void setUseCustomDriver(boolean useCustomDriver) {
        this.useCustomDriver = useCustomDriver;
    }
    
    public String getCustomDriverId() {
        return customDriverId;
    }
    
    public void setCustomDriverId(String customDriverId) {
        this.customDriverId = customDriverId;
    }
    
    public boolean isUseSSL() {
        return useSSL;
    }
    
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }
    
    public String getSslMode() {
        return sslMode;
    }
    
    public void setSslMode(String sslMode) {
        this.sslMode = sslMode;
    }
    
    public String getSslCertPath() {
        return sslCertPath;
    }
    
    public void setSslCertPath(String sslCertPath) {
        this.sslCertPath = sslCertPath;
    }
    
    public String getSslKeyPath() {
        return sslKeyPath;
    }
    
    public void setSslKeyPath(String sslKeyPath) {
        this.sslKeyPath = sslKeyPath;
    }
    
    public String getSslRootCertPath() {
        return sslRootCertPath;
    }
    
    public void setSslRootCertPath(String sslRootCertPath) {
        this.sslRootCertPath = sslRootCertPath;
    }
    
    public boolean isVerifyServerCert() {
        return verifyServerCert;
    }
    
    public void setVerifyServerCert(boolean verifyServerCert) {
        this.verifyServerCert = verifyServerCert;
    }
    
    public int getConnectionTimeout() {
        return connectionTimeout;
    }
    
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }
    
    public int getSocketTimeout() {
        return socketTimeout;
    }
    
    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
    
    public boolean isAutoReconnect() {
        return autoReconnect;
    }
    
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }
    
    public boolean isAutoCommit() {
        return autoCommit;
    }
    
    public void setAutoCommit(boolean autoCommit) {
        this.autoCommit = autoCommit;
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
    
    public String getTransactionIsolation() {
        return transactionIsolation;
    }
    
    public void setTransactionIsolation(String transactionIsolation) {
        this.transactionIsolation = transactionIsolation;
    }
    
    public String getDefaultSchema() {
        return defaultSchema;
    }
    
    public void setDefaultSchema(String defaultSchema) {
        this.defaultSchema = defaultSchema;
    }
    
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public int getMinPoolSize() {
        return minPoolSize;
    }
    
    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }
    
    public int getIdleTimeout() {
        return idleTimeout;
    }
    
    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }
    
    public int getMaxLifetime() {
        return maxLifetime;
    }
    
    public void setMaxLifetime(int maxLifetime) {
        this.maxLifetime = maxLifetime;
    }
    
    public Map<String, String> getConnectionParams() {
        return connectionParams;
    }
    
    public void setConnectionParams(Map<String, String> connectionParams) {
        this.connectionParams = connectionParams;
    }
    
    public Map<String, String> getAdditionalParams() {
        return additionalParams;
    }
    
    public void setAdditionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
    }
    
    public Map<String, Object> getCustomProperties() {
        return customProperties;
    }
    
    public void setCustomProperties(Map<String, Object> customProperties) {
        this.customProperties = customProperties;
    }
    
    @Override
    public String toString() {
        return "LocalConnectionInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", dbType='" + dbType + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", username='" + username + '\'' +
                ", useCustomDriver=" + useCustomDriver +
                ", customDriverId='" + customDriverId + '\'' +
                ", useSSL=" + useSSL +
                '}';
    }
}