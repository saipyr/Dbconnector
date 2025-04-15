package com.dbviewer.controller;

import com.dbviewer.model.DatabaseConnection;
import com.dbviewer.service.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DatabaseController {
    
    @Autowired
    private DatabaseService databaseService;
    
    @GetMapping("/")
    public String home() {
        return "index";
    }
    
    @PostMapping("/connect")
    @ResponseBody
    public ResponseEntity<?> connect(@RequestBody DatabaseConnection connection) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            databaseService.connect(connection);
            response.put("success", true);
            response.put("message", "Connected successfully");
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Connection failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/tables")
    @ResponseBody
    public ResponseEntity<?> getTables() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<String> tables = databaseService.getTables();
            response.put("success", true);
            response.put("tables", tables);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Failed to get tables: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/execute")
    @ResponseBody
    public ResponseEntity<?> executeQuery(@RequestParam String query, 
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> result = databaseService.executeQuery(query, page, pageSize);
            response.put("success", true);
            response.put("result", result);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("success", false);
            response.put("message", "Query execution failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/disconnect")
    @ResponseBody
    public ResponseEntity<?> disconnect() {
        databaseService.disconnect();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Disconnected successfully");
        return ResponseEntity.ok(response);
    }
}