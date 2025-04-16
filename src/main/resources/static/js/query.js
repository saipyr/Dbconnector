/**
 * Query Module
 * Handles database query execution and result display
 */
const QueryModule = (function() {
    let logger;
    let toastManager;
    let connectionManager;
    
    /**
     * Initialize the query module
     * @param {Object} dependencies - Module dependencies
     * @returns {Object} Query module interface
     */
    function init(dependencies) {
        logger = dependencies.logger || console;
        toastManager = dependencies.toastManager || {
            showToast: (message, type) => { console.log(message); }
        };
        connectionManager = dependencies.connectionManager;
        
        // Set up event listeners
        document.getElementById('executeQuery').addEventListener('click', executeQuery);
        
        logger.info('Query module initialized');
        
        return {
            executeQuery,
            displayResults // Export the function so it's accessible
        };
    }
    
    /**
     * Execute a SQL query
     */
    function executeQuery() {
        const connectionId = document.getElementById('connectionSelector').value;
        const query = document.getElementById('queryInput').value.trim();
        
        if (!connectionId) {
            toastManager.showToast('Please select a connection', 'warning');
            return;
        }
        
        if (!query) {
            toastManager.showToast('Please enter a SQL query', 'warning');
            return;
        }
        
        logger.info(`Executing query on connection ${connectionId}`);
        
        // Show loading indicator
        document.getElementById('queryResults').innerHTML = '<div class="text-center"><div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div></div>';
        
        // Execute query
        fetch('/api/query', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                connectionId: connectionId,
                query: query
            })
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                displayResults(data.results);
                toastManager.showToast('Query executed successfully', 'success');
            } else {
                throw new Error(data.message || 'Failed to execute query');
            }
        })
        .catch(error => {
            logger.error('Error executing query: ' + error.message);
            toastManager.showToast(`Error: ${error.message}`, 'danger');
            document.getElementById('queryResults').innerHTML = `<div class="alert alert-danger">${error.message}</div>`;
        });
    }
    
    /**
     * Display query results in a table
     * @param {Object} results - Query results
     */
    function displayResults(results) {
        const resultsDiv = document.getElementById('queryResults');
        
        if (!results || !results.columns || !results.data) {
            resultsDiv.innerHTML = '<div class="alert alert-info">No results returned</div>';
            return;
        }
        
        // Create table
        let html = '<div class="table-responsive"><table class="table table-striped table-hover">';
        
        // Add header
        html += '<thead><tr>';
        results.columns.forEach(column => {
            html += `<th>${column}</th>`;
        });
        html += '</tr></thead>';
        
        // Add body
        html += '<tbody>';
        results.data.forEach(row => {
            html += '<tr>';
            row.forEach(cell => {
                html += `<td>${cell === null ? '<em>NULL</em>' : cell}</td>`;
            });
            html += '</tr>';
        });
        html += '</tbody>';
        
        html += '</table></div>';
        
        // Add result summary
        html += `<div class="mt-2 text-muted small">${results.data.length} rows returned</div>`;
        
        resultsDiv.innerHTML = html;
    }
    
    return {
        init
    };
})();