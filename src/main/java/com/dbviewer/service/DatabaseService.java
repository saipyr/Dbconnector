package com.dbviewer.service;

import com.dbviewer.model.DatabaseConnection;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseService {
    
    private Connection connection;
    
    public Connection connect(DatabaseConnection dbConnection) throws SQLException {
        // Close existing connection if any
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
        
        String url;
        
        switch (dbConnection.getDbType()) {
            case "postgresql":
                url = "jdbc:postgresql://" + dbConnection.getHost() + ":" + dbConnection.getPort() + "/" + dbConnection.getDatabase();
                break;
            case "mysql":
                url = "jdbc:mysql://" + dbConnection.getHost() + ":" + dbConnection.getPort() + "/" + dbConnection.getDatabase();
                break;
            case "sqlserver":
                url = "jdbc:sqlserver://" + dbConnection.getHost() + ":" + dbConnection.getPort() + ";databaseName=" + dbConnection.getDatabase();
                break;
            case "oracle":
                url = "jdbc:oracle:thin:@" + dbConnection.getHost() + ":" + dbConnection.getPort() + ":" + dbConnection.getDatabase();
                break;
            default:
                throw new SQLException("Unsupported database type: " + dbConnection.getDbType());
        }
        
        connection = DriverManager.getConnection(url, dbConnection.getUsername(), dbConnection.getPassword());
        return connection;
    }
    
    public List<String> getTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        
        if (connection == null || connection.isClosed()) {
            return tables;
        }
        
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet rs = metaData.getTables(null, "public", "%", new String[]{"TABLE"});
        
        while (rs.next()) {
            tables.add(rs.getString("TABLE_NAME"));
        }
        
        rs.close();
        return tables;
    }
    
    public Map<String, Object> executeQuery(String query, int page, int pageSize) throws SQLException {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        
        if (connection == null || connection.isClosed()) {
            throw new SQLException("No active database connection");
        }
        
        Statement stmt = connection.createStatement();
        
        // For pagination
        int totalRows = 0;
        if (query.toLowerCase().startsWith("select")) {
            // Get total count for pagination
            String countQuery = "SELECT COUNT(*) FROM (" + query + ") as count_query";
            ResultSet countRs = stmt.executeQuery(countQuery);
            if (countRs.next()) {
                totalRows = countRs.getInt(1);
            }
            countRs.close();
            
            // Add pagination to query
            int offset = (page - 1) * pageSize;
            query += " LIMIT " + pageSize + " OFFSET " + offset;
        }
        
        ResultSet rs = stmt.executeQuery(query);
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Get column names
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnName(i));
        }
        
        // Get data
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            rows.add(row);
        }
        
        rs.close();
        stmt.close();
        
        result.put("columns", columns);
        result.put("data", rows);
        result.put("totalRows", totalRows);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", (int) Math.ceil((double) totalRows / pageSize));
        
        return result;
    }
    
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}