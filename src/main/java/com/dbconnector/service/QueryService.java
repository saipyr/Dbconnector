package com.dbconnector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.Date;
import java.util.*;

@Service
public class QueryService {

    @Autowired
    private ConnectionService connectionService;
    
    @Autowired
    private LoggingService loggingService;

    // In-memory storage for query history and bookmarks
    private final Map<String, List<Map<String, Object>>> queryHistory = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> queryBookmarks = new HashMap<>();

    public Map<String, Object> executeQuery(String connectionId, String query, int page, int pageSize) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        // Add to query history
        addToQueryHistory(connectionId, query);

        Map<String, Object> result = new HashMap<>();
        
        try (Statement stmt = connection.createStatement()) {
            boolean isResultSet = stmt.execute(query);
            
            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    result = processResultSet(rs, page, pageSize);
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                result.put("success", true);
                result.put("updateCount", updateCount);
                result.put("message", "Query executed successfully. Rows affected: " + updateCount);
            }
        }

        return result;
    }

    public Map<String, Object> explainQuery(String connectionId, String query) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        Map<String, Object> result = new HashMap<>();
        String explainQuery = "EXPLAIN " + query;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(explainQuery)) {
            
            List<Map<String, Object>> plan = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                plan.add(row);
            }
            
            result.put("success", true);
            result.put("plan", plan);
        }

        return result;
    }

    public Map<String, Object> validateQuery(String connectionId, String query) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        Map<String, Object> result = new HashMap<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            // Just prepare the statement to validate syntax
            result.put("success", true);
            result.put("valid", true);
            result.put("message", "Query syntax is valid");
        } catch (SQLException e) {
            result.put("success", true);
            result.put("valid", false);
            result.put("message", "Query syntax error: " + e.getMessage());
        }

        return result;
    }

    public Map<String, Object> getQueryHistory(String connectionId) {
        List<Map<String, Object>> history = queryHistory.getOrDefault(connectionId, new ArrayList<>());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("history", history);
        return result;
    }

    public Map<String, Object> bookmarkQuery(String connectionId, String query, String name, String description) {
        Map<String, Object> bookmark = new HashMap<>();
        bookmark.put("id", UUID.randomUUID().toString());
        bookmark.put("name", name);
        bookmark.put("description", description);
        bookmark.put("query", query);
        bookmark.put("createdAt", new Date());
        
        queryBookmarks.computeIfAbsent(connectionId, k -> new ArrayList<>()).add(bookmark);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("bookmark", bookmark);
        return result;
    }

    public Map<String, Object> getBookmarks(String connectionId) {
        List<Map<String, Object>> bookmarks = queryBookmarks.getOrDefault(connectionId, new ArrayList<>());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("bookmarks", bookmarks);
        return result;
    }

    private void addToQueryHistory(String connectionId, String query) {
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("query", query);
        historyEntry.put("executedAt", new Date());
        historyEntry.put("id", UUID.randomUUID().toString());
        
        List<Map<String, Object>> history = queryHistory.computeIfAbsent(connectionId, k -> new ArrayList<>());
        history.add(0, historyEntry); // Add to beginning
        
        // Keep only last 100 queries
        if (history.size() > 100) {
            history.subList(100, history.size()).clear();
        }
    }

    private Map<String, Object> processResultSet(ResultSet rs, int page, int pageSize) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Get column information
        List<Map<String, Object>> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            Map<String, Object> column = new HashMap<>();
            column.put("name", metaData.getColumnName(i));
            column.put("type", metaData.getColumnTypeName(i));
            column.put("size", metaData.getColumnDisplaySize(i));
            columns.add(column);
        }
        
        // Get data with pagination
        List<Map<String, Object>> data = new ArrayList<>();
        int currentRow = 0;
        int startRow = (page - 1) * pageSize;
        int endRow = startRow + pageSize;
        
        while (rs.next() && currentRow < endRow) {
            if (currentRow >= startRow) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                data.add(row);
            }
            currentRow++;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("columns", columns);
        result.put("data", data);
        result.put("currentPage", page);
        result.put("pageSize", pageSize);
        result.put("hasMore", rs.next()); // Check if there are more rows
        
        return result;
    }
}