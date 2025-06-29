package com.dbconnector.controller;

import com.dbconnector.service.QueryService;
import com.dbconnector.service.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;
    
    @Autowired
    private LoggingService loggingService;

    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String query = (String) request.get("query");
        Integer page = (Integer) request.getOrDefault("page", 1);
        Integer pageSize = (Integer) request.getOrDefault("pageSize", 50);
        
        loggingService.logAccess("Executing query on connection: " + connectionId);
        
        try {
            Map<String, Object> result = queryService.executeQuery(connectionId, query, page, pageSize);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error executing query", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error executing query: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/explain")
    public ResponseEntity<?> explainQuery(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String query = (String) request.get("query");
        
        loggingService.logAccess("Explaining query on connection: " + connectionId);
        
        try {
            Map<String, Object> result = queryService.explainQuery(connectionId, query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            loggingService.logError("Error explaining query", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error explaining query: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateQuery(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String query = (String) request.get("query");
        
        try {
            Map<String, Object> result = queryService.validateQuery(connectionId, query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error validating query: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getQueryHistory(@RequestParam String connectionId) {
        try {
            Map<String, Object> result = queryService.getQueryHistory(connectionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving query history: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/bookmark")
    public ResponseEntity<?> bookmarkQuery(@RequestBody Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        String query = (String) request.get("query");
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        
        try {
            Map<String, Object> result = queryService.bookmarkQuery(connectionId, query, name, description);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error bookmarking query: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<?> getBookmarks(@RequestParam String connectionId) {
        try {
            Map<String, Object> result = queryService.getBookmarks(connectionId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Error retrieving bookmarks: " + e.getMessage()
            ));
        }
    }
}