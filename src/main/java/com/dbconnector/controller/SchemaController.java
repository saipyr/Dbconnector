package com.dbconnector.controller;

import com.dbconnector.service.SchemaService;
import com.dbconnector.service.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;
    
    @Autowired
    private LoggingService loggingService;

    @GetMapping("/databases")
    public ResponseEntity<?> getDatabases(@RequestParam String connectionId) {
        loggingService.logAccess("Retrieving databases for connection: " + connectionId);
        
        try {
            Map<String, Object> result = schemaService.getDatabases(connectionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving databases", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving databases: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/schemas")
    public ResponseEntity<?> getSchemas(@RequestParam String connectionId, 
                                       @RequestParam(required = false) String database) {
        loggingService.logAccess("Retrieving schemas for connection: " + connectionId);
        
        try {
            Map<String, Object> result = schemaService.getSchemas(connectionId, database);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving schemas", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving schemas: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<?> getTables(@RequestParam String connectionId,
                                      @RequestParam(required = false) String schema) {
        loggingService.logAccess("Retrieving tables for connection: " + connectionId);
        
        try {
            Map<String, Object> result = schemaService.getTables(connectionId, schema);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving tables", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving tables: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/table-structure")
    public ResponseEntity<?> getTableStructure(@RequestParam String connectionId,
                                              @RequestParam String tableName,
                                              @RequestParam(required = false) String schema) {
        loggingService.logAccess("Retrieving table structure for: " + tableName);
        
        try {
            Map<String, Object> result = schemaService.getTableStructure(connectionId, tableName, schema);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving table structure", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving table structure: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/indexes")
    public ResponseEntity<?> getTableIndexes(@RequestParam String connectionId,
                                            @RequestParam String tableName,
                                            @RequestParam(required = false) String schema) {
        loggingService.logAccess("Retrieving indexes for table: " + tableName);
        
        try {
            Map<String, Object> result = schemaService.getTableIndexes(connectionId, tableName, schema);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving table indexes", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving table indexes: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/foreign-keys")
    public ResponseEntity<?> getForeignKeys(@RequestParam String connectionId,
                                           @RequestParam String tableName,
                                           @RequestParam(required = false) String schema) {
        loggingService.logAccess("Retrieving foreign keys for table: " + tableName);
        
        try {
            Map<String, Object> result = schemaService.getForeignKeys(connectionId, tableName, schema);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving foreign keys", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving foreign keys: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/views")
    public ResponseEntity<?> getViews(@RequestParam String connectionId,
                                     @RequestParam(required = false) String schema) {
        loggingService.logAccess("Retrieving views for connection: " + connectionId);
        
        try {
            Map<String, Object> result = schemaService.getViews(connectionId, schema);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving views", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving views: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/procedures")
    public ResponseEntity<?> getProcedures(@RequestParam String connectionId,
                                          @RequestParam(required = false) String schema) {
        loggingService.logAccess("Retrieving procedures for connection: " + connectionId);
        
        try {
            Map<String, Object> result = schemaService.getProcedures(connectionId, schema);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving procedures", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving procedures: " + e.getMessage()
            ));
        }
    }
}