package com.dbconnector.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class SchemaService {

    @Autowired
    private ConnectionService connectionService;
    
    @Autowired
    private LoggingService loggingService;

    public Map<String, Object> getDatabases(String connectionId) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> databases = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getCatalogs()) {
            while (rs.next()) {
                Map<String, Object> database = new HashMap<>();
                database.put("name", rs.getString("TABLE_CAT"));
                databases.add(database);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("databases", databases);
        return result;
    }

    public Map<String, Object> getSchemas(String connectionId, String database) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> schemas = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getSchemas()) {
            while (rs.next()) {
                Map<String, Object> schema = new HashMap<>();
                schema.put("name", rs.getString("TABLE_SCHEM"));
                schema.put("catalog", rs.getString("TABLE_CATALOG"));
                schemas.add(schema);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("schemas", schemas);
        return result;
    }

    public Map<String, Object> getTables(String connectionId, String schema) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        String[] types = {"TABLE", "VIEW", "SYSTEM TABLE"};
        try (ResultSet rs = metaData.getTables(null, schema, "%", types)) {
            while (rs.next()) {
                Map<String, Object> table = new HashMap<>();
                table.put("name", rs.getString("TABLE_NAME"));
                table.put("type", rs.getString("TABLE_TYPE"));
                table.put("schema", rs.getString("TABLE_SCHEM"));
                table.put("catalog", rs.getString("TABLE_CAT"));
                table.put("remarks", rs.getString("REMARKS"));
                tables.add(table);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("tables", tables);
        return result;
    }

    public Map<String, Object> getTableStructure(String connectionId, String tableName, String schema) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        Map<String, Object> structure = new HashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();

        // Get columns
        List<Map<String, Object>> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(null, schema, tableName, "%")) {
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", rs.getString("COLUMN_NAME"));
                column.put("type", rs.getString("TYPE_NAME"));
                column.put("size", rs.getInt("COLUMN_SIZE"));
                column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                column.put("defaultValue", rs.getString("COLUMN_DEF"));
                column.put("position", rs.getInt("ORDINAL_POSITION"));
                column.put("remarks", rs.getString("REMARKS"));
                column.put("autoIncrement", "YES".equals(rs.getString("IS_AUTOINCREMENT")));
                columns.add(column);
            }
        }

        // Get primary keys
        List<Map<String, Object>> primaryKeys = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(null, schema, tableName)) {
            while (rs.next()) {
                Map<String, Object> pk = new HashMap<>();
                pk.put("columnName", rs.getString("COLUMN_NAME"));
                pk.put("keySeq", rs.getInt("KEY_SEQ"));
                pk.put("pkName", rs.getString("PK_NAME"));
                primaryKeys.add(pk);
            }
        }

        structure.put("columns", columns);
        structure.put("primaryKeys", primaryKeys);
        structure.put("success", true);
        
        return structure;
    }

    public Map<String, Object> getTableIndexes(String connectionId, String tableName, String schema) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> indexes = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getIndexInfo(null, schema, tableName, false, false)) {
            while (rs.next()) {
                Map<String, Object> index = new HashMap<>();
                index.put("name", rs.getString("INDEX_NAME"));
                index.put("columnName", rs.getString("COLUMN_NAME"));
                index.put("unique", !rs.getBoolean("NON_UNIQUE"));
                index.put("type", rs.getInt("TYPE"));
                index.put("position", rs.getInt("ORDINAL_POSITION"));
                index.put("ascending", "A".equals(rs.getString("ASC_OR_DESC")));
                indexes.add(index);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("indexes", indexes);
        return result;
    }

    public Map<String, Object> getForeignKeys(String connectionId, String tableName, String schema) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> foreignKeys = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getImportedKeys(null, schema, tableName)) {
            while (rs.next()) {
                Map<String, Object> fk = new HashMap<>();
                fk.put("name", rs.getString("FK_NAME"));
                fk.put("columnName", rs.getString("FKCOLUMN_NAME"));
                fk.put("referencedTable", rs.getString("PKTABLE_NAME"));
                fk.put("referencedColumn", rs.getString("PKCOLUMN_NAME"));
                fk.put("updateRule", rs.getInt("UPDATE_RULE"));
                fk.put("deleteRule", rs.getInt("DELETE_RULE"));
                fk.put("keySeq", rs.getInt("KEY_SEQ"));
                foreignKeys.add(fk);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("foreignKeys", foreignKeys);
        return result;
    }

    public Map<String, Object> getViews(String connectionId, String schema) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> views = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        String[] types = {"VIEW"};
        try (ResultSet rs = metaData.getTables(null, schema, "%", types)) {
            while (rs.next()) {
                Map<String, Object> view = new HashMap<>();
                view.put("name", rs.getString("TABLE_NAME"));
                view.put("schema", rs.getString("TABLE_SCHEM"));
                view.put("catalog", rs.getString("TABLE_CAT"));
                view.put("remarks", rs.getString("REMARKS"));
                views.add(view);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("views", views);
        return result;
    }

    public Map<String, Object> getProcedures(String connectionId, String schema) throws SQLException {
        Connection connection = connectionService.getConnection(connectionId);
        if (connection == null) {
            throw new SQLException("No active connection found");
        }

        List<Map<String, Object>> procedures = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getProcedures(null, schema, "%")) {
            while (rs.next()) {
                Map<String, Object> procedure = new HashMap<>();
                procedure.put("name", rs.getString("PROCEDURE_NAME"));
                procedure.put("schema", rs.getString("PROCEDURE_SCHEM"));
                procedure.put("catalog", rs.getString("PROCEDURE_CAT"));
                procedure.put("remarks", rs.getString("REMARKS"));
                procedure.put("type", rs.getInt("PROCEDURE_TYPE"));
                procedures.add(procedure);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("procedures", procedures);
        return result;
    }
}