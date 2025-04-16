package com.dbconnector.controller;

import com.dbconnector.service.LogService;
// Add spring-boot-starter-web dependency to your pom.xml:
// <dependency>
//     <groupId>org.springframework.boot</groupId>
//     <artifactId>spring-boot-starter-web</artifactId>
// </dependency>
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @PostMapping
    public ResponseEntity<?> saveLog(@RequestBody Map<String, Object> logData) {
        String type = (String) logData.get("type");
        Map<String, Object> entry = (Map<String, Object>) logData.get("entry");
        
        logService.saveLog(type, entry);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/cleanup")
    public ResponseEntity<?> cleanupLogs(@RequestBody Map<String, Object> request) {
        int maxFiles = (int) request.get("maxFiles");
        logService.cleanupOldLogs(maxFiles);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearLogs(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type");
        logService.clearLogs(type);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> getLogs(@RequestParam(required = false) String type) {
        return ResponseEntity.ok(logService.getLogs(type));
    }
}