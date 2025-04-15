package com.dbconnector.service;

import com.dbconnector.model.ConnectionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionStorageService {

    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();
    private final String storageFilePath;
    
    @Autowired
    private LoggingService loggingService;
    
    public ConnectionStorageService() {
        // Store connections in the user's home directory
        this.storageFilePath = System.getProperty("user.home") + "/Desktop/Trae/Dbconnector/connections.dat";
        loadConnections();
    }
    
    public List<ConnectionInfo> getAllConnections() {
        return new ArrayList<>(connections.values());
    }
    
    public ConnectionInfo getConnection(String id) {
        return connections.get(id);
    }
    
    public ConnectionInfo saveConnection(ConnectionInfo connectionInfo) {
        if (connectionInfo.getId() == null) {
            connectionInfo.setId(UUID.randomUUID().toString());
        }
        
        connections.put(connectionInfo.getId(), connectionInfo);
        persistConnections();
        
        return connectionInfo;
    }
    
    public boolean deleteConnection(String id) {
        if (connections.containsKey(id)) {
            connections.remove(id);
            persistConnections();
            return true;
        }
        return false;
    }
    
    private void loadConnections() {
        File file = new File(storageFilePath);
        if (!file.exists()) {
            // Create directory if it doesn't exist
            file.getParentFile().mkdirs();
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<String, ConnectionInfo> loadedConnections = (Map<String, ConnectionInfo>) ois.readObject();
            connections.putAll(loadedConnections);
            loggingService.logInfo("Loaded " + connections.size() + " saved connections");
        } catch (Exception e) {
            loggingService.logError("Error loading saved connections", e);
        }
    }
    
    private void persistConnections() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFilePath))) {
            oos.writeObject(new HashMap<>(connections));
            loggingService.logInfo("Saved " + connections.size() + " connections to disk");
        } catch (Exception e) {
            loggingService.logError("Error saving connections to disk", e);
        }
    }
}