/**
 * Connection Manager Module
 * Handles database connections and saved connections
 */
const ConnectionManager = (function() {
    let connections = [];
    let currentConnection = null;
    let logger;
    let toastManager;
    
    /**
     * Initialize the connection manager
     * @param {Object} dependencies - Module dependencies
     * @returns {Object} Connection manager interface
     */
    function init(dependencies) {
        logger = dependencies.logger || console;
        toastManager = dependencies.toastManager || {
            showToast: (message, type) => { console.log(message); }
        };
        
        // Load saved connections
        loadConnections();
        
        // Set up event listeners
        document.getElementById('startConnection').addEventListener('click', connectToDatabase);
        document.getElementById('saveConnectionBtn').addEventListener('click', showSaveConnectionModal);
        document.getElementById('confirmSaveConnection').addEventListener('click', saveConnection);
        document.getElementById('savedConnectionSelect').addEventListener('change', loadSavedConnection);
        document.getElementById('refreshConnections').addEventListener('click', refreshConnectionsList);
        
        // Initialize saved connections dropdown
        updateSavedConnectionsDropdown();
        
        logger.info('Connection manager initialized');
        
        return {
            getConnections,
            getCurrentConnection,
            saveConnection,
            deleteConnection,
            connectToDatabase
        };
    }
    
    /**
     * Connect to a database
     */
    function connectToDatabase() {
        const connectionData = getConnectionFormData();
        
        if (!validateConnectionData(connectionData)) {
            return;
        }
        
        logger.info(`Connecting to ${connectionData.dbType} database at ${connectionData.host}:${connectionData.port}`);
        
        // Show loading indicator
        document.getElementById('startConnection').innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Connecting...';
        document.getElementById('startConnection').disabled = true;
        
        // Make API call to connect
        fetch('/api/connections/connect', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(connectionData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                currentConnection = {
                    ...connectionData,
                    id: data.connectionId
                };
                
                toastManager.showToast(`Connected to ${connectionData.dbType} database`, 'success');
                
                // Switch to tables tab
                const tablesTab = document.getElementById('tables-tab');
                bootstrap.Tab.getOrCreateInstance(tablesTab).show();
                
                // Load tables
                loadTables(currentConnection.id);
            } else {
                throw new Error(data.message || 'Failed to connect to database');
            }
        })
        .catch(error => {
            logger.error('Error connecting to database: ' + error.message);
            toastManager.showToast(`Error: ${error.message}`, 'danger');
        })
        .finally(() => {
            // Reset button
            document.getElementById('startConnection').innerHTML = 'Connect';
            document.getElementById('startConnection').disabled = false;
        });
    }
    
    /**
     * Show save connection modal
     */
    function showSaveConnectionModal() {
        const connectionData = getConnectionFormData();
        
        if (!validateConnectionData(connectionData, false)) {
            return;
        }
        
        // Populate modal fields
        document.getElementById('modalConnectionName').value = connectionData.name || '';
        document.getElementById('modalSaveFolder').value = '';
        
        // Show modal
        const modal = new bootstrap.Modal(document.getElementById('saveConnectionModal'));
        modal.show();
    }
    
    /**
     * Save a database connection
     */
    function saveConnection() {
        const connectionData = getConnectionFormData();
        const modalConnectionName = document.getElementById('modalConnectionName').value.trim();
        const modalSaveFolder = document.getElementById('modalSaveFolder').value.trim();
        
        if (!modalConnectionName) {
            toastManager.showToast('Please enter a connection name', 'warning');
            return;
        }
        
        connectionData.name = modalConnectionName;
        connectionData.folder = modalSaveFolder || 'default';
        
        logger.info(`Saving connection: ${connectionData.name}`);
        
        // Make API call to save connection
        fetch('/api/connections/save', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(connectionData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                // Add to local connections list
                connections.push({
                    ...connectionData,
                    id: data.connectionId
                });
                
                // Update UI
                updateSavedConnectionsDropdown();
                refreshConnectionsList();
                
                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('saveConnectionModal'));
                modal.hide();
                
                toastManager.showToast(`Connection "${connectionData.name}" saved successfully`, 'success');
            } else {
                throw new Error(data.message || 'Failed to save connection');
            }
        })
        .catch(error => {
            logger.error('Error saving connection: ' + error.message);
            toastManager.showToast(`Error: ${error.message}`, 'danger');
        });
    }
    
    // Rest of your code...