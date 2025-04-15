package com.dbconnector.controller;

import com.dbconnector.model.DriverInfo;
import com.dbconnector.service.DriverService;
import com.dbconnector.service.LoggingService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;
    
    @Autowired
    private LoggingService loggingService;

    @GetMapping
    public ResponseEntity<List<DriverInfo>> getAllDrivers() {
        loggingService.logAccess("Retrieving all custom database drivers");
        return ResponseEntity.ok(driverService.getAllDrivers());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadDriver(
            @RequestParam("name") String name,
            @RequestParam("className") String className,
            @RequestParam("urlTemplate") String urlTemplate,
            @RequestParam("defaultPort") int defaultPort,
            @RequestParam("driverFile") MultipartFile driverFile) {
        
        loggingService.logAudit("Uploading custom database driver: " + name);
        
        try {
            DriverInfo driverInfo = new DriverInfo();
            driverInfo.setName(name);
            driverInfo.setClassName(className);
            driverInfo.setUrlTemplate(urlTemplate);
            driverInfo.setDefaultPort(defaultPort);
            
            DriverInfo savedDriver = driverService.saveDriver(driverInfo, driverFile);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Driver uploaded successfully",
                "driver", savedDriver
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            loggingService.logError("Error uploading driver", e);
            
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "Failed to upload driver: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable String id) {
        loggingService.logAudit("Deleting driver with ID: " + id);
        
        try {
            boolean deleted = driverService.deleteDriver(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Driver deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Driver not found"));
            }
        } catch (Exception e) {
            loggingService.logError("Error deleting driver", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error deleting driver: " + e.getMessage()));
        }
    }
}
