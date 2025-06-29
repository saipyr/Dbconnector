package com.dbconnector.controller;

import com.dbconnector.service.DataService;
import com.dbconnector.service.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DataController {

    @Autowired
    private DataService dataService;
    
    @Autowired
    private LoggingService loggingService;

    @GetMapping("/table")
    public ResponseEntity<?> getTableData(@RequestParam String connectionId,
                                         @RequestParam String tableName,
                                         @RequestParam(required = false) String schema,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "50") int pageSize) {
        loggingService.logAccess("Retrieving table data: " + tableName);
        
        try {
            Map<String, Object> result = dataService.getTableData(connectionId, tableName, schema, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error retrieving table data", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving table data: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/insert")
    public ResponseEntity<?> insertRow(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String tableName = (String) request.get("tableName");
        String schema = (String) request.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> rowData = (Map<String, Object>) request.get("data");
        
        loggingService.logAudit("Inserting row into table: " + tableName);
        
        try {
            Map<String, Object> result = dataService.insertRow(connectionId, tableName, schema, rowData);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error inserting row", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error inserting row: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateRow(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String tableName = (String) request.get("tableName");
        String schema = (String) request.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> rowData = (Map<String, Object>) request.get("data");
        @SuppressWarnings("unchecked")
        Map<String, Object> whereClause = (Map<String, Object>) request.get("where");
        
        loggingService.logAudit("Updating row in table: " + tableName);
        
        try {
            Map<String, Object> result = dataService.updateRow(connectionId, tableName, schema, rowData, whereClause);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error updating row", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error updating row: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteRow(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String tableName = (String) request.get("tableName");
        String schema = (String) request.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> whereClause = (Map<String, Object>) request.get("where");
        
        loggingService.logAudit("Deleting row from table: " + tableName);
        
        try {
            Map<String, Object> result = dataService.deleteRow(connectionId, tableName, schema, whereClause);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error deleting row", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error deleting row: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/import")
    public ResponseEntity<?> importData(@RequestParam String connectionId,
                                       @RequestParam String tableName,
                                       @RequestParam(required = false) String schema,
                                       @RequestParam String format,
                                       @RequestParam("file") MultipartFile file) {
        loggingService.logAudit("Importing data into table: " + tableName);
        
        try {
            Map<String, Object> result = dataService.importData(connectionId, tableName, schema, format, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error importing data", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error importing data: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportData(@RequestParam String connectionId,
                                       @RequestParam String tableName,
                                       @RequestParam(required = false) String schema,
                                       @RequestParam String format) {
        loggingService.logAccess("Exporting data from table: " + tableName);
        
        try {
            Map<String, Object> result = dataService.exportData(connectionId, tableName, schema, format);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error exporting data", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error exporting data: " + e.getMessage()
            ));
        }
    }
}