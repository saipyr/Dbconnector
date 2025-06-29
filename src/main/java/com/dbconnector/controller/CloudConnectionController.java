package com.dbconnector.controller;

import com.dbconnector.model.CloudConnectionInfo;
import com.dbconnector.service.CloudConnectionService;
import com.dbconnector.service.LoggingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.util.Map;

@RestController
@RequestMapping("/api/cloud-connections")
public class CloudConnectionController {

    @Autowired
    private CloudConnectionService cloudConnectionService;
    
    @Autowired
    private LoggingService loggingService;

    @PostMapping("/test")
    public ResponseEntity<?> testCloudConnection(@RequestBody CloudConnectionInfo connectionInfo) {
        loggingService.logAccess("Testing cloud database connection: " + connectionInfo.getCloudProvider());
        
        try {
            boolean isConnected = cloudConnectionService.testCloudConnection(connectionInfo);
            
            Map<String, Object> response = Map.of(
                "success", isConnected,
                "message", isConnected ? "Connection successful" : "Connection failed",
                "provider", connectionInfo.getCloudProvider(),
                "endpoint", connectionInfo.getEndpoint()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error testing cloud connection", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Connection test failed: " + e.getMessage(),
                "provider", connectionInfo.getCloudProvider()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/connect")
    public ResponseEntity<?> connectToCloud(@RequestBody CloudConnectionInfo connectionInfo) {
        loggingService.logAudit("Connecting to cloud database: " + connectionInfo.getCloudProvider());
        
        try {
            Connection connection = null;
            
            switch (connectionInfo.getCloudProvider().toLowerCase()) {
                case "aws":
                    connection = cloudConnectionService.connectToAWSRDS(connectionInfo);
                    break;
                case "azure":
                    connection = cloudConnectionService.connectToAzureSQL(connectionInfo);
                    break;
                case "gcp":
                    connection = cloudConnectionService.connectToGoogleCloudSQL(connectionInfo);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported cloud provider: " + connectionInfo.getCloudProvider());
            }
            
            if (connection != null && !connection.isClosed()) {
                Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Successfully connected to " + connectionInfo.getCloudProvider(),
                    "connectionId", connectionInfo.getId(),
                    "provider", connectionInfo.getCloudProvider(),
                    "endpoint", connectionInfo.getEndpoint()
                );
                
                return ResponseEntity.ok(response);
            } else {
                throw new RuntimeException("Failed to establish connection");
            }
            
        } catch (Exception e) {
            loggingService.logError("Error connecting to cloud database", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to connect: " + e.getMessage(),
                "provider", connectionInfo.getCloudProvider()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/disconnect/{connectionId}")
    public ResponseEntity<?> disconnectFromCloud(@PathVariable String connectionId) {
        loggingService.logAudit("Disconnecting from cloud database: " + connectionId);
        
        try {
            cloudConnectionService.closeCloudConnection(connectionId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Successfully disconnected from cloud database",
                "connectionId", connectionId
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error disconnecting from cloud database", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to disconnect: " + e.getMessage(),
                "connectionId", connectionId
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/providers")
    public ResponseEntity<?> getSupportedCloudProviders() {
        Map<String, Object> providers = Map.of(
            "aws", Map.of(
                "name", "Amazon Web Services",
                "services", new String[]{"RDS", "Aurora", "DocumentDB", "Redshift"},
                "authMethods", new String[]{"username/password", "IAM"}
            ),
            "azure", Map.of(
                "name", "Microsoft Azure",
                "services", new String[]{"SQL Database", "Cosmos DB", "Database for PostgreSQL", "Database for MySQL"},
                "authMethods", new String[]{"username/password", "Azure AD"}
            ),
            "gcp", Map.of(
                "name", "Google Cloud Platform",
                "services", new String[]{"Cloud SQL", "BigQuery", "Firestore", "Cloud Spanner"},
                "authMethods", new String[]{"username/password", "Service Account"}
            )
        );
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "providers", providers
        ));
    }
}