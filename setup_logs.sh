#!/bin/bash

echo "=== Setting up DB Connector Logs ==="

# Use relative paths instead of absolute paths
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${SCRIPT_DIR}/logs"
ARCHIVED_DIR="${LOG_DIR}/archived"

mkdir -p "${LOG_DIR}"
mkdir -p "${ARCHIVED_DIR}"

# Create empty log files if they don't exist
touch "${LOG_DIR}/dbconnector.log"
touch "${LOG_DIR}/access.log"
touch "${LOG_DIR}/audit.log"

# Set permissions
chmod -R 755 "${LOG_DIR}"

echo "Log directory setup complete at: ${LOG_DIR}"
echo "You can find logs in this directory for analysis."

# Create application.properties file with H2 in-memory database for development
echo "=== Setting up application configuration ==="
CONFIG_DIR="${SCRIPT_DIR}/src/main/resources"
mkdir -p "${CONFIG_DIR}"

cat > "${CONFIG_DIR}/application.properties" << 'EOF'
# Server configuration
server.port=8081

# Disable DataSource auto-configuration
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

# Logging configuration
logging.level.root=INFO
logging.level.com.dbconnector=DEBUG
logging.file.name=${user.dir}/logs/dbconnector.log
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Custom logging paths
logging.custom.access=${user.dir}/logs/access.log
logging.custom.audit=${user.dir}/logs/audit.log

# Connection storage path
app.connections.storage-path=${user.dir}/connections.dat

# Custom database drivers storage path
app.drivers.storage-path=${user.dir}/drivers

# File upload configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
spring.servlet.multipart.enabled=true
EOF

# Create drivers directory for custom JDBC drivers
DRIVERS_DIR="${SCRIPT_DIR}/drivers"
mkdir -p "${DRIVERS_DIR}"
chmod -R 755 "${DRIVERS_DIR}"

echo "Application configuration complete."
echo "Created application.properties with database configuration."
echo "Created drivers directory for custom JDBC drivers at: ${DRIVERS_DIR}"

# Create a fix for the main application class
echo "=== Fixing application main class ==="
MAIN_DIR="${SCRIPT_DIR}/src/main/java/com/dbconnector"
mkdir -p "${MAIN_DIR}"

cat > "${MAIN_DIR}/DbConnectorApplication.java" << 'EOF'
package com.dbconnector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class DbConnectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbConnectorApplication.class, args);
    }
}
EOF

# Create driver controller for handling JAR uploads
echo "=== Creating Driver Controller ==="
CONTROLLER_DIR="${SCRIPT_DIR}/src/main/java/com/dbconnector/controller"
mkdir -p "${CONTROLLER_DIR}"

cat > "${CONTROLLER_DIR}/DriverController.java" << 'EOF'
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
EOF

# Create driver model
echo "=== Creating Driver Model ==="
MODEL_DIR="${SCRIPT_DIR}/src/main/java/com/dbconnector/model"
mkdir -p "${MODEL_DIR}"

cat > "${MODEL_DIR}/DriverInfo.java" << 'EOF'
package com.dbconnector.model;

import java.io.Serializable;
import java.util.UUID;

public class DriverInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String className;
    private String urlTemplate;
    private int defaultPort;
    private String fileName;
    
    public DriverInfo() {
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
    
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public String getUrlTemplate() {
        return urlTemplate;
    }
    
    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }
    
    public int getDefaultPort() {
        return defaultPort;
    }
    
    public void setDefaultPort(int defaultPort) {
        this.defaultPort = defaultPort;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    @Override
    public String toString() {
        return "DriverInfo{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", className='" + className + '\'' +
                ", urlTemplate='" + urlTemplate + '\'' +
                ", defaultPort=" + defaultPort +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
EOF

# Create driver service
echo "=== Creating Driver Service ==="
SERVICE_DIR="${SCRIPT_DIR}/src/main/java/com/dbconnector/service"
mkdir -p "${SERVICE_DIR}"

cat > "${SERVICE_DIR}/DriverService.java" << 'EOF'
package com.dbconnector.service;

import com.dbconnector.model.DriverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DriverService {

    private final Map<String, DriverInfo> drivers = new ConcurrentHashMap<>();
    private final String driversStoragePath;
    private final String driversDirectory;
    
    @Autowired
    private LoggingService loggingService;
    
    public DriverService(@Value("${app.drivers.storage-path}") String driversDirectory) {
        this.driversDirectory = driversDirectory;
        this.driversStoragePath = driversDirectory + "/drivers.dat";
        
        // Create directory if it doesn't exist
        File dir = new File(driversDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        loadDrivers();
    }
    
    public List<DriverInfo> getAllDrivers() {
        return new ArrayList<>(drivers.values());
    }
    
    public DriverInfo getDriver(String id) {
        return drivers.get(id);
    }
    
    public DriverInfo saveDriver(DriverInfo driverInfo, MultipartFile driverFile) throws IOException {
        // Generate a unique filename
        String originalFilename = driverFile.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = driverInfo.getId() + extension;
        
        // Save the file
        Path filePath = Paths.get(driversDirectory, fileName);
        Files.write(filePath, driverFile.getBytes());
        
        // Update driver info
        driverInfo.setFileName(fileName);
        
        // Save to memory
        drivers.put(driverInfo.getId(), driverInfo);
        
        // Persist to disk
        persistDrivers();
        
        loggingService.logInfo("Saved driver: " + driverInfo.getName() + " with file: " + fileName);
        
        return driverInfo;
    }
    
    public boolean deleteDriver(String id) {
        DriverInfo driver = drivers.get(id);
        if (driver == null) {
            return false;
        }
        
        // Remove from memory
        drivers.remove(id);
        
        // Delete the file
        try {
            Path filePath = Paths.get(driversDirectory, driver.getFileName());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            loggingService.logError("Error deleting driver file", e);
        }
        
        // Persist changes
        persistDrivers();
        
        return true;
    }
    
    private void loadDrivers() {
        File file = new File(driversStoragePath);
        if (!file.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, DriverInfo> loadedDrivers = (Map<String, DriverInfo>) ois.readObject();
            drivers.putAll(loadedDrivers);
            loggingService.logInfo("Loaded " + drivers.size() + " custom database drivers");
        } catch (Exception e) {
            loggingService.logError("Error loading custom database drivers", e);
        }
    }
    
    private void persistDrivers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(driversStoragePath))) {
            oos.writeObject(new HashMap<>(drivers));
            loggingService.logInfo("Saved " + drivers.size() + " custom database drivers to disk");
        } catch (Exception e) {
            loggingService.logError("Error saving custom database drivers to disk", e);
        }
    }
}
EOF

# Create JavaScript for handling custom database drivers
echo "=== Creating JavaScript for custom database drivers ==="
JS_DIR="${SCRIPT_DIR}/src/main/resources/static/js"
mkdir -p "${JS_DIR}/modules"

cat > "${JS_DIR}/modules/dbDrivers.js" << 'EOF'
/**
 * Database Drivers Module
 * Handles custom database driver management
 */
const DbDriversModule = (function() {
    // Store for custom drivers
    let customDrivers = [];
    let logger;
    let toastManager;
    
    /**
     * Initialize the database drivers module
     * @param {Object} dependencies - Module dependencies
     * @returns {Object} Database drivers interface
     */
    function init(dependencies) {
        logger = dependencies.logger || console;
        toastManager = dependencies.toastManager || {
            showToast: (message, type) => { console.log(message); }
        };
        
        logger.info('Database Drivers module initialized');
        
        // Load custom drivers
        loadCustomDrivers();
        
        // Set up event listeners
        document.getElementById('saveCustomDbDriver').addEventListener('click', uploadCustomDriver);
        
        // Update port when database type changes
        document.getElementById('dbType').addEventListener('change', function() {
            updateDefaultPort(this.value);
        });
        
        return {
            addCustomDriver,
            getCustomDrivers,
            loadCustomDrivers,
            getDriverById,
            getDefaultPortForDriver
        };
    }
    
    /**
     * Upload a custom database driver
     */
    function uploadCustomDriver() {
        const name = document.getElementById('customDbName').value;
        const driverFile = document.getElementById('customDbDriver').files[0];
        const className = document.getElementById('customDbClassName').value;
        const urlTemplate = document.getElementById('customDbUrlTemplate').value;
        const defaultPort = parseInt(document.getElementById('customDbDefaultPort').value);
        
        if (!name || !driverFile || !className || !urlTemplate || isNaN(defaultPort)) {
            toastManager.showToast('Please fill in all fields', 'warning');
            return;
        }
        
        logger.info('Uploading custom database driver: ' + name);
        
        const formData = new FormData();
        formData.append('name', name);
        formData.append('className', className);
        formData.append('urlTemplate', urlTemplate);
        formData.append('defaultPort', defaultPort);
        formData.append('driverFile', driverFile);
        
        fetch('/api/drivers', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                toastManager.showToast(`Database driver "${name}" added successfully`, 'success');
                
                // Add to local store
                customDrivers.push(data.driver);
                
                // Update UI
                updateDriversDropdown();
                
                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('customDbDriverModal'));
                modal.hide();
                
                // Clear form
                document.getElementById('customDbDriverForm').reset();
            } else {
                throw new Error(data.message || 'Failed to add database driver');
            }
        })
        .catch(error => {
            logger.error('Error adding custom database driver: ' + error.message);
            toastManager.showToast(`Error adding driver: ${error.message}`, 'danger');
        });
    }
    
    /**
     * Load custom database drivers from the server
     */
    function loadCustomDrivers() {
        fetch('/api/drivers')
            .then(response => response.json())
            .then(data => {
                customDrivers = data;
                updateDriversDropdown();
                logger.info(`Loaded ${customDrivers.length} custom database drivers`);
            })
            .catch(error => {
                logger.error('Error loading custom database drivers: ' + error.message);
            });
    }
    
    /**
     * Update the database type dropdown with custom drivers
     */
    function updateDriversDropdown() {
        const dbTypeSelect = document.getElementById('dbType');
        
        // Remove existing custom options
        Array.from(dbTypeSelect.options).forEach(option => {
            if (option.dataset.custom === 'true') {
                dbTypeSelect.removeChild(option);
            }
        });
        
        // Add custom drivers
        if (customDrivers.length > 0) {
            // Add separator
            const separator = document.createElement('option');
            separator.disabled = true;
            separator.dataset.custom = 'true';
            separator.textContent = '──────────────';
            dbTypeSelect.appendChild(separator);
            
            // Add custom drivers
            customDrivers.forEach(driver => {
                const option = document.createElement('option');
                option.value = 'custom:' + driver.id;
                option.textContent = driver.name;
                option.dataset.custom = 'true';
                dbTypeSelect.appendChild(option);
            });
        }
    }
    
    /**
     * Get all custom drivers
     * @returns {Array} Array of custom drivers
     */
    function getCustomDrivers() {
        return [...customDrivers];
    }
    
    /**
     * Get a driver by ID
     * @param {string} id - Driver ID
     * @returns {Object|null} Driver object or null if not found
     */
    function getDriverById(id) {
        return customDrivers.find(driver => driver.id === id) || null;
    }
    
    /**
     * Update the port field with the default port for the selected database type
     * @param {string} dbType - Database type
     */
    function updateDefaultPort(dbType) {
        const portInput = document.getElementById('port');
        
        if (dbType.startsWith('custom:')) {
            const driverId = dbType.split(':')[1];
            const driver = getDriverById(driverId);
            if (driver) {
                portInput.value = driver.defaultPort;
            }
        } else {
            // Default ports for standard database types
            const defaultPorts = {
                'postgresql': 5432,
                'mysql': 3306,
                'sqlserver': 1433,
                'oracle': 1521
            };
            
            portInput.value = defaultPorts[dbType] || '';
        }
    }
    
    /**
     * Get the default port for a driver
     * @param {string} dbType - Database type
     * @returns {number} Default port
     */
    function getDefaultPortForDriver(dbType) {
        if (dbType.startsWith('custom:')) {
            const driverId = dbType.split(':')[1];
            const driver = getDriverById(driverId);
            if (driver) {
                return driver.defaultPort;
            }
        } else {
            // Default ports for standard database types
            const defaultPorts = {
                'postgresql': 5432,
                'mysql': 3306,
                'sqlserver': 1433,
                'oracle': 1521
            };
            
            return defaultPorts[dbType] || 0;
        }
        
        return 0;
    }
    
    return {
        init
    };
})();
EOF

echo "Custom database driver upload functionality has been added!"
echo "Users can now upload JDBC driver JAR files directly from the UI."