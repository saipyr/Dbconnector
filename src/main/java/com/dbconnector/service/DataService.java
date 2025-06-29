package com.dbconnector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.*;
import java.util.*;
import java.io.*;

@Service
public class DataService {

    @Autowired
    private ConnectionService connectionService;
    
    @Autowired
    private LoggingService loggingService;

    public Map<String, Object> getTableData(String connectionId, String tableName, String schema, int page, int pageSize) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        Map<String, Object> result = new HashMap<>();
        
        // Build table name with schema if provided
        String fullTableName = schema != null && !schema.isEmpty() ? schema + "." + tableName : tableName;
        
        // Get total count
        String countQuery = "SELECT COUNT(*) FROM " + fullTableName;
        int totalRows = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(countQuery)) {
            if (rs.next()) {
                totalRows = rs.getInt(1);
            }
        }
        
        // Get paginated data
        int offset = (page - 1) * pageSize;
        String dataQuery = "SELECT * FROM " + fullTableName + " LIMIT " + pageSize + " OFFSET " + offset;
        
        List<Map<String, Object>> columns = new ArrayList<>();
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(dataQuery)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // Get column information
            for (int i = 1; i <= columnCount; i++) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", metaData.getColumnName(i));
                column.put("type", metaData.getColumnTypeName(i));
                column.put("size", metaData.getColumnDisplaySize(i));
                column.put("nullable", metaData.isNullable(i) == ResultSetMetaData.columnNullable);
                columns.add(column);
            }
            
            // Get data rows
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                data.add(row);
            }
        }
        
        result.put("success", true);
        result.put("columns", columns);
        result.put("data", data);
        result.put("totalRows", totalRows);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalRows / pageSize));
        
        return result;
    }

    public Map<String, Object> insertRow(String connectionId, String tableName, String schema, Map<String, Object> rowData) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        String fullTableName = schema != null && !schema.isEmpty() ? schema + "." + tableName : tableName;
        
        // Build INSERT query
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            if (columns.length() > 0) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(entry.getKey());
            values.append("?");
            parameters.add(entry.getValue());
        }
        
        String insertQuery = "INSERT INTO " + fullTableName + " (" + columns + ") VALUES (" + values + ")";
        
        try (PreparedStatement stmt = connection.prepareStatement(insertQuery)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rowsAffected", rowsAffected);
            result.put("message", "Row inserted successfully");
            
            return result;
        }
    }

    public Map<String, Object> updateRow(String connectionId, String tableName, String schema, 
                                        Map<String, Object> rowData, Map<String, Object> whereClause) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        String fullTableName = schema != null && !schema.isEmpty() ? schema + "." + tableName : tableName;
        
        // Build UPDATE query
        StringBuilder setClause = new StringBuilder();
        StringBuilder whereClauseStr = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        
        // SET clause
        for (Map.Entry<String, Object> entry : rowData.entrySet()) {
            if (setClause.length() > 0) {
                setClause.append(", ");
            }
            setClause.append(entry.getKey()).append(" = ?");
            parameters.add(entry.getValue());
        }
        
        // WHERE clause
        for (Map.Entry<String, Object> entry : whereClause.entrySet()) {
            if (whereClauseStr.length() > 0) {
                whereClauseStr.append(" AND ");
            }
            whereClauseStr.append(entry.getKey()).append(" = ?");
            parameters.add(entry.getValue());
        }
        
        String updateQuery = "UPDATE " + fullTableName + " SET " + setClause + " WHERE " + whereClauseStr;
        
        try (PreparedStatement stmt = connection.prepareStatement(updateQuery)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rowsAffected", rowsAffected);
            result.put("message", "Row updated successfully");
            
            return result;
        }
    }

    public Map<String, Object> deleteRow(String connectionId, String tableName, String schema, 
                                        Map<String, Object> whereClause) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        String fullTableName = schema != null && !schema.isEmpty() ? schema + "." + tableName : tableName;
        
        // Build DELETE query
        StringBuilder whereClauseStr = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        
        for (Map.Entry<String, Object> entry : whereClause.entrySet()) {
            if (whereClauseStr.length() > 0) {
                whereClauseStr.append(" AND ");
            }
            whereClauseStr.append(entry.getKey()).append(" = ?");
            parameters.add(entry.getValue());
        }
        
        String deleteQuery = "DELETE FROM " + fullTableName + " WHERE " + whereClauseStr;
        
        try (PreparedStatement stmt = connection.prepareStatement(deleteQuery)) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
            
            int rowsAffected = stmt.executeUpdate();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rowsAffected", rowsAffected);
            result.put("message", "Row deleted successfully");
            
            return result;
        }
    }

    public Map<String, Object> importData(String connectionId, String tableName, String schema, 
                                         String format, MultipartFile file) throws Exception {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        Map<String, Object> result = new HashMap<>();
        
        if ("csv".equalsIgnoreCase(format)) {
            result = importFromCSV(connection, tableName, schema, file);
        } else if ("json".equalsIgnoreCase(format)) {
            result = importFromJSON(connection, tableName, schema, file);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        
        return result;
    }

    public Map<String, Object> exportData(String connectionId, String tableName, String schema, String format) throws Exception {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        Map<String, Object> result = new HashMap<>();
        
        if ("csv".equalsIgnoreCase(format)) {
            result = exportToCSV(connection, tableName, schema);
        } else if ("json".equalsIgnoreCase(format)) {
            result = exportToJSON(connection, tableName, schema);
        } else {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        
        return result;
    }

    private Map<String, Object> importFromCSV(Connection connection, String tableName, String schema, MultipartFile file) throws Exception {
        // Implementation for CSV import
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "CSV import functionality to be implemented");
        return result;
    }

    private Map<String, Object> importFromJSON(Connection connection, String tableName, String schema, MultipartFile file) throws Exception {
        // Implementation for JSON import
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "JSON import functionality to be implemented");
        return result;
    }

    private Map<String, Object> exportToCSV(Connection connection, String tableName, String schema) throws Exception {
        // Implementation for CSV export
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "CSV export functionality to be implemented");
        return result;
    }

    private Map<String, Object> exportToJSON(Connection connection, String tableName, String schema) throws Exception {
        // Implementation for JSON export
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "JSON export functionality to be implemented");
        return result;
    }
}