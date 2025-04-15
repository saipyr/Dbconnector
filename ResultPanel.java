import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class ResultPanel extends JPanel {
    private JTable resultTable;
    private JButton prevButton, nextButton;
    private JLabel pageInfoLabel;
    
    private int currentPage = 1;
    private int pageSize = 20;
    private int totalRows = 0;
    
    public ResultPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createTitledBorder("Results"));
        
        initComponents();
    }
    
    private void initComponents() {
        // Result table
        resultTable = new JTable();
        JScrollPane resultScroll = new JScrollPane(resultTable);
        resultScroll.setPreferredSize(new Dimension(850, 300));
        
        // Pagination controls
        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        prevButton = new JButton("Previous Page");
        prevButton.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                // The parent will handle reloading data
            }
        });
        
        nextButton = new JButton("Next Page");
        nextButton.addActionListener(e -> {
            int maxPages = (int) Math.ceil((double) totalRows / pageSize);
            if (currentPage < maxPages) {
                currentPage++;
                // The parent will handle reloading data
            }
        });
        
        pageInfoLabel = new JLabel("Page: 0 of 0");
        
        paginationPanel.add(prevButton);
        paginationPanel.add(pageInfoLabel);
        paginationPanel.add(nextButton);
        
        add(resultScroll, BorderLayout.CENTER);
        add(paginationPanel, BorderLayout.SOUTH);
    }
    
    public void displayResults(ResultSet rs) throws SQLException {
        // Get column names
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Create table model
        DefaultTableModel model = new DefaultTableModel();
        
        // Add column names
        for (int i = 1; i <= columnCount; i++) {
            model.addColumn(metaData.getColumnName(i));
        }
        
        // Add rows
        while (rs.next()) {
            Object[] row = new Object[columnCount];
            for (int i = 1; i <= columnCount; i++) {
                row[i - 1] = rs.getObject(i);
            }
            model.addRow(row);
        }
        
        resultTable.setModel(model);
        updatePageInfo();
    }
    
    public void displayMessage(String message) {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Message");
        model.addRow(new Object[]{message});
        resultTable.setModel(model);
    }
    
    public void updatePageInfo() {
        int maxPages = (int) Math.ceil((double) totalRows / pageSize);
        pageInfoLabel.setText("Page: " + currentPage + " of " + maxPages + " (Total rows: " + totalRows + ")");
    }
    
    public void updatePageInfo(String customMessage) {
        pageInfoLabel.setText(customMessage);
    }
    
    public int getCurrentPage() {
        return currentPage;
    }
    
    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }
    
    public int getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
    
    public int getTotalRows() {
        return totalRows;
    }
    
    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
        updatePageInfo();
    }
}