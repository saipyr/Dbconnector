package com.dbconnector.controller;

import com.dbconnector.model.ConnectionInfo;
import com.dbconnector.service.ConnectionStorageService;
import com.dbconnector.service.LoggingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/connections")
public class ConnectionController {

    @Autowired
    private ConnectionStorageService connectionStorageService;
    
    @Autowired
    private LoggingService loggingService;

    @GetMapping
    public ResponseEntity<List<ConnectionInfo>> getAllConnections() {
        loggingService.logAccess("Retrieving all saved connections");
        return ResponseEntity.ok(connectionStorageService.getAllConnections());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> saveConnection(@RequestBody ConnectionInfo connectionInfo) {
        loggingService.logAudit("Saving connection: " + connectionInfo.getName());
        
        try {
            ConnectionInfo savedConnection = connectionStorageService.saveConnection(connectionInfo);
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Connection saved successfully",
                "connection", savedConnection
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error saving connection", e);
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to save connection: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getConnection(@PathVariable String id) {
        loggingService.logAccess("Retrieving connection with ID: " + id);
        
        try {
            ConnectionInfo connection = connectionStorageService.getConnection(id);
            if (connection != null) {
                return ResponseEntity.ok(connection);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Connection not found"));
            }
        } catch (Exception e) {
            loggingService.logError("Error retrieving connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error retrieving connection: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConnection(@PathVariable String id) {
        loggingService.logAudit("Deleting connection with ID: " + id);
        
        try {
            boolean deleted = connectionStorageService.deleteConnection(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Connection deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Connection not found"));
            }
        } catch (Exception e) {
            loggingService.logError("Error deleting connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error deleting connection: " + e.getMessage()));
        }
    }
}