package com.dbconnector.model;

import java.util.UUID;

public class CloudConnectionInfo {
    private String id;
    private String name;
    private String cloudProvider; // aws, azure, gcp
    private String endpoint;
    private int port;
    private String database;
    private String username;
    private String password;
    private String dbType;
    
    // SSL/TLS settings
    private boolean useSSL = true;
    private String sslMode = "require";
    private String sslCertPath;
    
    // Cloud-specific authentication
    private boolean useIAM = false;
    private boolean useAzureAD = false;
    private boolean useServiceAccount = false;
    private String serviceAccountPath;
    
    // Cloud SQL specific
    private boolean useCloudSQLProxy = false;
    private String cloudSQLInstance;
    
    // Connection settings
    private int connectionTimeout = 30;
    private int socketTimeout = 0;
    private boolean autoReconnect = true;
    
    // Monitoring settings
    private boolean enableMonitoring = false;
    private String monitoringEndpoint;
    
    public CloudConnectionInfo() {
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
    
    public String getCloudProvider() {
        return cloudProvider;
    }
    
    public void setCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
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
    
    public String getDbType() {
        return dbType;
    }
    
    public void setDbType(String dbType) {
        this.dbType = dbType;
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
    
    public boolean isUseIAM() {
        return useIAM;
    }
    
    public void setUseIAM(boolean useIAM) {
        this.useIAM = useIAM;
    }
    
    public boolean isUseAzureAD() {
        return useAzureAD;
    }
    
    public void setUseAzureAD(boolean useAzureAD) {
        this.useAzureAD = useAzureAD;
    }
    
    public boolean isUseServiceAccount() {
        return useServiceAccount;
    }
    
    public void setUseServiceAccount(boolean useServiceAccount) {
        this.useServiceAccount = useServiceAccount;
    }
    
    public String getServiceAccountPath() {
        return serviceAccountPath;
    }
    
    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }
    
    public boolean isUseCloudSQLProxy() {
        return useCloudSQLProxy;
    }
    
    public void setUseCloudSQLProxy(boolean useCloudSQLProxy) {
        this.useCloudSQLProxy = useCloudSQLProxy;
    }
    
    public String getCloudSQLInstance() {
        return cloudSQLInstance;
    }
    
    public void setCloudSQLInstance(String cloudSQLInstance) {
        this.cloudSQLInstance = cloudSQLInstance;
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
    
    public boolean isEnableMonitoring() {
        return enableMonitoring;
    }
    
    public void setEnableMonitoring(boolean enableMonitoring) {
        this.enableMonitoring = enableMonitoring;
    }
    
    public String getMonitoringEndpoint() {
        return monitoringEndpoint;
    }
    
    public void setMonitoringEndpoint(String monitoringEndpoint) {
        this.monitoringEndpoint = monitoringEndpoint;
    }
    
    @Override
    public String toString() {
        return "CloudConnectionInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cloudProvider='" + cloudProvider + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", port=" + port +
                ", database='" + database + '\'' +
                ", username='" + username + '\'' +
                ", dbType='" + dbType + '\'' +
                ", useSSL=" + useSSL +
                ", useIAM=" + useIAM +
                ", useAzureAD=" + useAzureAD +
                '}';
    }
}