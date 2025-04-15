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
