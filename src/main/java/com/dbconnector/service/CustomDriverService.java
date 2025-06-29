package com.dbconnector.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CustomDriverService {

    private final Map<String, CustomDriverInfo> customDrivers = new ConcurrentHashMap<>();

    public static class CustomDriverInfo {
        private String id;
        private String name;
        private String jarFilePath;
        private String driverClassName;
        private String urlTemplate;

        public CustomDriverInfo(String id, String name, String jarFilePath, String driverClassName, String urlTemplate) {
            this.id = id;
            this.name = name;
            this.jarFilePath = jarFilePath;
            this.driverClassName = driverClassName;
            this.urlTemplate = urlTemplate;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getJarFilePath() { return jarFilePath; }
        public String getDriverClassName() { return driverClassName; }
        public String getUrlTemplate() { return urlTemplate; }
    }

    public void registerCustomDriver(CustomDriverInfo driverInfo) {
        customDrivers.put(driverInfo.getId(), driverInfo);
    }

    public CustomDriverInfo getCustomDriver(String customDriverId) {
        return customDrivers.get(customDriverId);
    }

    public Map<String, CustomDriverInfo> getAllCustomDrivers() {
        return customDrivers;
    }
}