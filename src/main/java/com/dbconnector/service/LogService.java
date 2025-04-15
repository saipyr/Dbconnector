package com.dbconnector.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogService {

    private static final String LOG_DIR = "logs";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Set<String> LOG_TYPES = new HashSet<>(Arrays.asList(
            "access", "monitoring", "connection", "stdout", "stdin", "common"
    ));

    public LogService() {
        // Create log directory if it doesn't exist
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        
        // Create subdirectories for each log type
        for (String type : LOG_TYPES) {
            File typeDir = new File(LOG_DIR + "/" + type);
            if (!typeDir.exists()) {
                typeDir.mkdirs();
            }
        }
    }

    public void saveLog(String type, Map<String, Object> entry) {
        if (!LOG_TYPES.contains(type)) {
            throw new IllegalArgumentException("Invalid log type: " + type);
        }

        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            String logFile = LOG_DIR + "/" + type + "/" + today + ".log";
            
            // Append to log file
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(objectMapper.writeValueAsString(entry) + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanupOldLogs(int maxFiles) {
        for (String type : LOG_TYPES) {
            try {
                Path typePath = Paths.get(LOG_DIR, type);
                if (!Files.exists(typePath)) {
                    continue;
                }
                
                // Get all log files for this type
                List<Path> logFiles = Files.list(typePath)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".log"))
                        .sorted((p1, p2) -> {
                            try {
                                return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1));
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .collect(Collectors.toList());
                
                // Delete old files beyond the retention limit
                if (logFiles.size() > maxFiles) {
                    for (int i = maxFiles; i < logFiles.size(); i++) {
                        Files.delete(logFiles.get(i));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void clearLogs(String type) {
        if ("all".equals(type)) {
            for (String logType : LOG_TYPES) {
                clearLogsOfType(logType);
            }
        } else if (LOG_TYPES.contains(type)) {
            clearLogsOfType(type);
        } else {
            throw new IllegalArgumentException("Invalid log type: " + type);
        }
    }

    private void clearLogsOfType(String type) {
        try {
            Path typePath = Paths.get(LOG_DIR, type);
            if (Files.exists(typePath)) {
                Files.list(typePath)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".log"))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, List<Map<String, Object>>> getLogs(String type) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        
        if (type != null && LOG_TYPES.contains(type)) {
            result.put(type, getLogsOfType(type));
        } else {
            for (String logType : LOG_TYPES) {
                result.put(logType, getLogsOfType(logType));
            }
        }
        
        return result;
    }

    private List<Map<String, Object>> getLogsOfType(String type) {
        List<Map<String, Object>> logs = new ArrayList<>();
        
        try {
            Path typePath = Paths.get(LOG_DIR, type);
            if (!Files.exists(typePath)) {
                return logs;
            }
            
            // Get today's log file
            String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);
            Path logFile = typePath.resolve(today + ".log");
            
            if (Files.exists(logFile)) {
                List<String> lines = Files.readAllLines(logFile);
                for (String line : lines) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> entry = objectMapper.readValue(line, Map.class);
                        logs.add(entry);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return logs;
    }
}