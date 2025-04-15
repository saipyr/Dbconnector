const QueryModule = (function() {
    let currentPage = 1;
    let pageSize = 20;
    let totalPages = 0;
    let editorModule;
    let showStatusCallback;
    let displayResultsCallback;
    
    function init(editor, statusCallback, resultsCallback) {
        editorModule = editor;
        showStatusCallback = statusCallback;
        displayResultsCallback = resultsCallback;
        
        return {
            executeQuery: executeQuery,
            navigatePage: navigatePage,
            updatePaginationInfo: updatePaginationInfo,
            setCurrentPage: function(page) { currentPage = page; },
            getCurrentPage: function() { return currentPage; },
            getPageSize: function() { return pageSize; },
            setTotalPages: function(pages) { totalPages = pages; }
        };
    }
    
    function executeQuery() {
        const query = editorModule.getValue().trim();
        
        if (!query) {
            showStatusCallback('Please enter a query', 'warning');
            return;
        }
        
        fetch(`/execute?query=${encodeURIComponent(query)}&page=${currentPage}&pageSize=${pageSize}`, {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayResultsCallback(data.result);
            } else {
                showStatusCallback(data.message, 'danger');
            }
        })
        .catch(error => {
            showStatusCallback('Error executing query: ' + error, 'danger');
        });
    }
    
    function navigatePage(direction) {
        const newPage = currentPage + direction;
        
        if (newPage < 1 || (totalPages > 0 && newPage > totalPages)) {
            return;
        }
        
        currentPage = newPage;
        executeQuery();
    }
    
    function updatePaginationInfo(totalRows) {
        const pageInfo = document.getElementById('pageInfo');
        pageInfo.textContent = `Page ${currentPage} of ${totalPages} (Total rows: ${totalRows})`;
        
        // Enable/disable pagination buttons
        document.getElementById('prevPage').disabled = currentPage <= 1;
        document.getElementById('nextPage').disabled = currentPage >= totalPages;
    }
    
    // Add this function to the QueryModule
    function exportResults(format) {
        if (!currentResults || !currentResults.data || currentResults.data.length === 0) {
            logger.query('Export failed - no data to export');
            showStatusCallback('No data to export', 'warning');
            return;
        }
        
        logger.query('Exporting query results', { format: format });
        
        try {
            if (format === 'excel') {
                exportToExcel();
            } else if (format === 'pdf') {
                exportToPdf();
            }
        } catch (error) {
            logger.query('Export error', { error: error.toString() });
            showStatusCallback('Error exporting data: ' + error, 'danger');
        }
    }
    
    function exportToExcel() {
        // Create a workbook with a worksheet
        const wb = XLSX.utils.book_new();
        
        // Convert data to worksheet format
        const wsData = [currentResults.columns];
        currentResults.data.forEach(row => {
            const rowData = [];
            currentResults.columns.forEach(col => {
                rowData.push(row[col] || '');
            });
            wsData.push(rowData);
        });
        
        const ws = XLSX.utils.aoa_to_sheet(wsData);
        
        // Add worksheet to workbook
        XLSX.utils.book_append_sheet(wb, ws, 'Query Results');
        
        // Generate filename
        const fileName = 'query_results_' + new Date().toISOString().replace(/[:.]/g, '-') + '.xlsx';
        
        // Write and download
        XLSX.writeFile(wb, fileName);
        
        logger.query('Exported to Excel', { fileName: fileName });
        showStatusCallback('Data exported to Excel', 'success');
    }
    
    function exportToPdf() {
        // Create a new jsPDF instance
        const doc = new jspdf.jsPDF();
        
        // Set up the table data
        const tableData = [];
        currentResults.data.forEach(row => {
            const rowData = [];
            currentResults.columns.forEach(col => {
                rowData.push(row[col] || '');
            });
            tableData.push(rowData);
        });
        
        // Add title
        doc.text('Query Results', 14, 16);
        
        // Add timestamp
        const timestamp = new Date().toLocaleString();
        doc.setFontSize(10);
        doc.text('Generated: ' + timestamp, 14, 22);
        
        // Add the table
        doc.autoTable({
            head: [currentResults.columns],
            body: tableData,
            startY: 25,
            margin: { top: 25 },
            styles: { overflow: 'linebreak' },
            headStyles: { fillColor: [66, 139, 202] }
        });
        
        // Generate filename
        const fileName = 'query_results_' + new Date().toISOString().replace(/[:.]/g, '-') + '.pdf';
        
        // Save the PDF
        doc.save(fileName);
        
        logger.query('Exported to PDF', { fileName: fileName });
        showStatusCallback('Data exported to PDF', 'success');
    }
    
    // Make sure to include the export functions in the return object
    return {
        executeQuery: executeQuery,
        navigatePage: navigatePage,
        displayResults: displayResults,
        exportResults: exportResults
    };
})();