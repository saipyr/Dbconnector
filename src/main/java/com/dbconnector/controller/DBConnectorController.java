package com.dbconnector.controller;

import com.dbconnector.service.ConnectionService;
import com.dbconnector.service.LocalDatabaseService;
import com.dbconnector.service.CloudConnectionService;
import com.dbconnector.service.QueryService;
import com.dbconnector.service.CustomDriverService;
import com.dbconnector.service.SchemaService;
import com.dbconnector.service.CredentialService;
import com.dbconnector.service.QueryStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.sql.Connection;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/dbconnector")
public class DBConnectorController {
    @Autowired
    private ConnectionService connectionService;
    @Autowired
    private LocalDatabaseService localDatabaseService;
    @Autowired
    private CloudConnectionService cloudConnectionService;
    @Autowired
    private QueryService queryService;
    @Autowired
    private CustomDriverService customDriverService;
    @Autowired
    private SchemaService schemaService;
    @Autowired
    private CredentialService credentialService;
    @Autowired
    private QueryStorageService queryStorageService;

    @PostMapping("/action")
    public ResponseEntity<?> handleAction(@RequestBody Map<String, Object> request) {
        String action = (String) request.get("action");
        if (action == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'action' parameter"));
        }
        try {
            switch (action) {
                case "connect":
                    return handleConnect(request);
                case "disconnect":
                    return handleDisconnect(request);
                case "execute_query":
                    return handleExecuteQuery(request);
                case "save_query":
                    return handleSaveQuery(request);
                case "list_saved_queries":
                    return handleListSavedQueries(request);
                case "delete_saved_query":
                    return handleDeleteSavedQuery(request);
                case "save_credentials":
                    return handleSaveCredentials(request);
                case "load_credentials":
                    return handleLoadCredentials(request);
                case "list_saved_credentials":
                    return handleListSavedCredentials(request);
                case "delete_credentials":
                    return handleDeleteCredentials(request);
                case "import_jar":
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Use /import-jar endpoint for file upload"));
                case "list_imported_jars":
                    return handleListImportedJars(request);
                case "delete_jar":
                    return handleDeleteJar(request);
                case "list_tables":
                    return handleListTables(request);
                default:
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Unknown action: " + action));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // --- Handler Implementations ---

    private ResponseEntity<?> handleConnect(Map<String, Object> request) {
        // Sample payload: {"action":"connect", "connection_type":"local_db", "credentials":{...}, "credentials_profile_name":"..."}
        String connectionType = (String) request.get("connection_type");
        Map<String, Object> credentials = (Map<String, Object>) request.get("credentials");
        String profileName = (String) request.get("credentials_profile_name");
        if (credentials == null && profileName != null) {
            credentials = credentialService.loadCredentials(profileName);
            if (credentials == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Credentials profile not found: " + profileName));
            }
        }
        if (connectionType == null || credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'connection_type' or 'credentials' parameter"));
        }
        try {
            Connection connection = null;
            String connectionId = null;
            switch (connectionType) {
                case "local_db": {
                    // Required: database_file_path
                    String dbFile = (String) credentials.get("database_file_path");
                    String username = (String) credentials.get("username");
                    String password = (String) credentials.get("password");
                    if (dbFile == null) {
                        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'database_file_path' for local_db"));
                    }
                    com.dbconnector.model.LocalConnectionInfo info = new com.dbconnector.model.LocalConnectionInfo();
                    info.setDatabase(dbFile);
                    info.setUsername(username);
                    info.setPassword(password);
                    info.setDbType("sqlite"); // Default to sqlite for file-based local DB
                    info.setId(UUID.randomUUID().toString());
                    connection = localDatabaseService.connectToLocalDatabase(info);
                    connectionId = info.getId();
                    connectionService.addConnection(connectionId, connection);
                    break;
                }
                case "cloud_db": {
                    // Required: host, database_name, username, password
                    String host = (String) credentials.get("host");
                    Integer port = credentials.get("port") != null ? ((Number) credentials.get("port")).intValue() : null;
                    String dbName = (String) credentials.get("database_name");
                    String username = (String) credentials.get("username");
                    String password = (String) credentials.get("password");
                    Boolean ssl = credentials.get("ssl_enabled") != null ? (Boolean) credentials.get("ssl_enabled") : false;
                    if (host == null || dbName == null || username == null || password == null) {
                        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing required fields for cloud_db"));
                    }
                    com.dbconnector.model.CloudConnectionInfo info = new com.dbconnector.model.CloudConnectionInfo();
                    info.setEndpoint(host);
                    info.setPort(port != null ? port : 5432); // Default to 5432
                    info.setDatabase(dbName);
                    info.setUsername(username);
                    info.setPassword(password);
                    info.setUseSSL(ssl);
                    info.setId(UUID.randomUUID().toString());
                    // For demo, default to AWS RDS
                    connection = cloudConnectionService.connectToAWSRDS(info);
                    connectionId = info.getId();
                    connectionService.addConnection(connectionId, connection);
                    break;
                }
                case "jdbc_odbc": {
                    // Required: driver_class, connection_string
                    String driverClass = (String) credentials.get("driver_class");
                    String connStr = (String) credentials.get("connection_string");
                    String username = (String) credentials.get("username");
                    String password = (String) credentials.get("password");
                    String jarFileName = (String) credentials.get("jar_file_name");
                    if (driverClass == null || connStr == null) {
                        return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'driver_class' or 'connection_string' for jdbc_odbc"));
                    }
                    // Load the driver if needed
                    if (jarFileName != null) {
                        for (com.dbconnector.service.CustomDriverService.CustomDriverInfo info : customDriverService.getAllCustomDrivers().values()) {
                            if (info.getJarFilePath().endsWith(jarFileName)) {
                                localDatabaseService.getClass().getClassLoader().loadClass(info.getDriverClassName());
                                break;
                            }
                        }
                    } else {
                        Class.forName(driverClass);
                    }
                    java.util.Properties props = new java.util.Properties();
                    if (username != null) props.setProperty("user", username);
                    if (password != null) props.setProperty("password", password);
                    connection = java.sql.DriverManager.getConnection(connStr, props);
                    connectionId = UUID.randomUUID().toString();
                    connectionService.addConnection(connectionId, connection);
                    break;
                }
                default:
                    return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Unknown connection_type: " + connectionType));
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Connected successfully", "connectionId", connectionId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleDisconnect(Map<String, Object> request) {
        String connectionId = (String) request.get("connectionId");
        if (connectionId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'connectionId' parameter"));
        }
        connectionService.removeConnection(connectionId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Disconnected successfully", "connectionId", connectionId));
    }

    private ResponseEntity<?> handleExecuteQuery(Map<String, Object> request) {
        // Sample payload: {"action":"execute_query", "connectionId":"...", "query":"SELECT * FROM ..."} or {"action":"execute_query", "connectionId":"...", "query_name":"..."}
        String connectionId = (String) request.get("connectionId");
        String query = (String) request.get("query");
        String queryName = (String) request.get("query_name");
        if (connectionId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'connectionId' parameter"));
        }
        if (query == null && queryName != null) {
            query = queryStorageService.loadQuery(queryName);
            if (query == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Saved query not found: " + queryName));
            }
        }
        if (query == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'query' or 'query_name' parameter"));
        }
        try {
            Map<String, Object> result = queryService.executeQuery(connectionId, query, 1, 100);
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleSaveQuery(Map<String, Object> request) {
        // Sample payload: {"action":"save_query", "query_name":"...", "query":"..."}
        String queryName = (String) request.get("query_name");
        String query = (String) request.get("query");
        if (queryName == null || query == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'query_name' or 'query' parameter"));
        }
        queryStorageService.saveQuery(queryName, query);
        return ResponseEntity.ok(Map.of("success", true, "message", "Query saved", "query_name", queryName));
    }

    private ResponseEntity<?> handleListSavedQueries(Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("success", true, "queries", queryStorageService.listQueryNames()));
    }

    private ResponseEntity<?> handleDeleteSavedQuery(Map<String, Object> request) {
        String queryName = (String) request.get("query_name");
        if (queryName == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'query_name' parameter"));
        }
        boolean deleted = queryStorageService.deleteQuery(queryName);
        return ResponseEntity.ok(Map.of("success", deleted, "message", deleted ? "Query deleted" : "Query not found", "query_name", queryName));
    }

    private ResponseEntity<?> handleSaveCredentials(Map<String, Object> request) {
        // Sample payload: {"action":"save_credentials", "credentials_profile_name":"...", "credentials":{...}}
        String profileName = (String) request.get("credentials_profile_name");
        Map<String, Object> credentials = (Map<String, Object>) request.get("credentials");
        if (profileName == null || credentials == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'credentials_profile_name' or 'credentials' parameter"));
        }
        credentialService.saveCredentials(profileName, credentials);
        return ResponseEntity.ok(Map.of("success", true, "message", "Credentials saved", "credentials_profile_name", profileName));
    }

    private ResponseEntity<?> handleLoadCredentials(Map<String, Object> request) {
        String profileName = (String) request.get("credentials_profile_name");
        if (profileName == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'credentials_profile_name' parameter"));
        }
        Map<String, Object> credentials = credentialService.loadCredentials(profileName);
        if (credentials == null) {
            return ResponseEntity.ok(Map.of("success", false, "message", "Credentials not found", "credentials_profile_name", profileName));
        }
        return ResponseEntity.ok(Map.of("success", true, "credentials", credentials, "credentials_profile_name", profileName));
    }

    private ResponseEntity<?> handleListSavedCredentials(Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("success", true, "profiles", credentialService.listCredentialProfiles()));
    }

    private ResponseEntity<?> handleDeleteCredentials(Map<String, Object> request) {
        String profileName = (String) request.get("credentials_profile_name");
        if (profileName == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'credentials_profile_name' parameter"));
        }
        boolean deleted = credentialService.deleteCredentials(profileName);
        return ResponseEntity.ok(Map.of("success", deleted, "message", deleted ? "Credentials deleted" : "Credentials not found", "credentials_profile_name", profileName));
    }

    // --- File upload for JARs ---
    @PostMapping("/import-jar")
    public ResponseEntity<?> importJar(@RequestParam("name") String name,
                                       @RequestParam("className") String className,
                                       @RequestParam("urlTemplate") String urlTemplate,
                                       @RequestParam("defaultPort") int defaultPort,
                                       @RequestParam("driverFile") MultipartFile driverFile) {
        try {
            // Save the driver file to the drivers/ directory
            String driversDir = "drivers";
            File dir = new File(driversDir);
            if (!dir.exists()) dir.mkdirs();
            String uniqueId = UUID.randomUUID().toString();
            String fileName = uniqueId + "_" + driverFile.getOriginalFilename();
            Path filePath = Paths.get(driversDir, fileName);
            Files.copy(driverFile.getInputStream(), filePath);

            // Register with CustomDriverService
            CustomDriverService.CustomDriverInfo driverInfo = new CustomDriverService.CustomDriverInfo(
                uniqueId, name, filePath.toString(), className, urlTemplate
            );
            customDriverService.registerCustomDriver(driverInfo);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Driver JAR uploaded and registered successfully",
                "driver_id", uniqueId,
                "file_name", fileName
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "File upload failed: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> handleListImportedJars(Map<String, Object> request) {
        return ResponseEntity.ok(Map.of("success", true, "jars", customDriverService.getAllCustomDrivers().values()));
    }

    private ResponseEntity<?> handleDeleteJar(Map<String, Object> request) {
        String jarName = (String) request.get("jar_file_name");
        if (jarName == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'jar_file_name' parameter"));
        }
        // Find and remove the driver by file name
        boolean deleted = false;
        for (CustomDriverService.CustomDriverInfo info : customDriverService.getAllCustomDrivers().values()) {
            if (info.getJarFilePath().endsWith(jarName)) {
                // Remove from service
                customDriverService.getAllCustomDrivers().remove(info.getId());
                // Delete file
                try {
                    Files.deleteIfExists(Paths.get(info.getJarFilePath()));
                    deleted = true;
                } catch (IOException e) {
                    return ResponseEntity.status(500).body(Map.of("success", false, "message", "File deletion failed: " + e.getMessage()));
                }
                break;
            }
        }
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "JAR deleted successfully", "jar_file_name", jarName));
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "JAR not found", "jar_file_name", jarName));
        }
    }

    private ResponseEntity<?> handleListTables(Map<String, Object> request) {
        // Sample payload: {"action":"list_tables", "connectionId":"...", "table_name":"..."} or {"action":"list_tables", "connectionId":"...", "table_name":["table1","table2"]}
        String connectionId = (String) request.get("connectionId");
        Object tableNameObj = request.get("table_name");
        if (connectionId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Missing 'connectionId' parameter"));
        }
        try {
            Map<String, Object> result;
            if (tableNameObj == null) {
                result = schemaService.getTables(connectionId, null);
            } else if (tableNameObj instanceof String) {
                result = schemaService.getTables(connectionId, null); // Filter below
                String tableName = (String) tableNameObj;
                result.put("tables", ((java.util.List<?>) result.get("tables")).stream().filter(t -> t.toString().contains(tableName)).toList());
            } else if (tableNameObj instanceof java.util.List) {
                result = schemaService.getTables(connectionId, null); // Filter below
                java.util.List<?> names = (java.util.List<?>) tableNameObj;
                result.put("tables", ((java.util.List<?>) result.get("tables")).stream().filter(t -> names.stream().anyMatch(n -> t.toString().contains(n.toString()))).toList());
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid 'table_name' parameter type"));
            }
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
} 