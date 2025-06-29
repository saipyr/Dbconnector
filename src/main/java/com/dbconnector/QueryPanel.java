package com.dbconnector;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class QueryPanel extends JPanel {
    private JComboBox<String> tableList;
    private JTextArea queryArea;
    private JButton executeButton;
    private DatabaseViewer parent;
    private String currentTable = "";
    
    public QueryPanel(DatabaseViewer parent) {
        this.parent = parent;
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Query"));
        
        initComponents();
    }
    
    private void initComponents() {
        // Table list
        JPanel tablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tablePanel.add(new JLabel("Tables:"));
        tableList = new JComboBox<>();
        tableList.setPreferredSize(new Dimension(200, 25));
        tableList.addActionListener(e -> {
            if (tableList.getSelectedItem() != null) {
                currentTable = tableList.getSelectedItem().toString();
                int pageSize = parent.getResultPanel().getPageSize();
                queryArea.setText("SELECT * FROM " + currentTable + " LIMIT " + pageSize + " OFFSET 0");
                loadTableData(currentTable);
            }
        });
        tablePanel.add(tableList);
        
        // Query area
        queryArea = new JTextArea(5, 40);
        queryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane queryScroll = new JScrollPane(queryArea);
        
        // Execute button
        executeButton = new JButton("Execute Query");
        executeButton.addActionListener(e -> executeQuery());
        
        add(tablePanel, BorderLayout.NORTH);
        add(queryScroll, BorderLayout.CENTER);
        add(executeButton, BorderLayout.SOUTH);
    }
    
    public void loadTables() {
        try {
            tableList.removeAllItems();
            
            Connection connection = parent.getConnection();
            if (connection == null) {
                return;
            }
            
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet tables = metaData.getTables(null, "public", "%", new String[]{"TABLE"});
            
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableList.addItem(tableName);
            }
            
            if (tableList.getItemCount() > 0) {
                tableList.setSelectedIndex(0);
                currentTable = tableList.getItemAt(0);
                int pageSize = parent.getResultPanel().getPageSize();
                queryArea.setText("SELECT * FROM " + currentTable + " LIMIT " + pageSize + " OFFSET 0");
                loadTableData(currentTable);
            }
            
        } catch (SQLException ex) {
            parent.updateStatus("Error loading tables: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public void loadTableData(String tableName) {
        Connection connection = parent.getConnection();
        if (connection == null || tableName == null || tableName.isEmpty()) {
            return;
        }
        
        try {
            ResultPanel resultPanel = parent.getResultPanel();
            
            // Get total row count
            Statement countStmt = connection.createStatement();
            ResultSet countRs = countStmt.executeQuery("SELECT COUNT(*) FROM " + tableName);
            if (countRs.next()) {
                resultPanel.setTotalRows(countRs.getInt(1));
            }
            countRs.close();
            countStmt.close();
            
            // Calculate pagination
            int currentPage = resultPanel.getCurrentPage();
            int pageSize = resultPanel.getPageSize();
            int offset = (currentPage - 1) * pageSize;
            
            // Load data for current page
            String query = "SELECT * FROM " + tableName + " LIMIT " + pageSize + " OFFSET " + offset;
            queryArea.setText(query);
            
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            
            // Display results in the result panel
            resultPanel.displayResults(rs);
            
            rs.close();
            stmt.close();
            
        } catch (SQLException ex) {
            parent.updateStatus("Error loading data: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public void executeQuery() {
        String query = queryArea.getText().trim();
        Connection connection = parent.getConnection();
        
        if (query.isEmpty() || connection == null) {
            return;
        }
        
        try {
            Statement stmt = connection.createStatement();
            
            // Check if it's a SELECT query
            if (query.toUpperCase().startsWith("SELECT")) {
                ResultSet rs = stmt.executeQuery(query);
                
                // Display results
                parent.getResultPanel().displayResults(rs);
                rs.close();
                
                // Reset pagination for custom queries
                parent.getResultPanel().setCurrentPage(1);
                parent.getResultPanel().updatePageInfo("Custom query results");
                
            } else {
                // For non-SELECT queries (INSERT, UPDATE, DELETE, etc.)
                int rowsAffected = stmt.executeUpdate(query);
                parent.getResultPanel().displayMessage("Query executed successfully. Rows affected: " + rowsAffected);
                
                // Refresh table data if we're working with the current table
                if (currentTable != null && !currentTable.isEmpty() && 
                    query.toUpperCase().contains(currentTable.toUpperCase())) {
                    loadTableData(currentTable);
                }
            }
            
            stmt.close();
            parent.updateStatus("Query executed successfully");
            
        } catch (SQLException ex) {
            parent.updateStatus("Query error: " + ex.getMessage());
            parent.getResultPanel().displayMessage("Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    public String getCurrentTable() {
        return currentTable;
    }
}