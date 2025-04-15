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
