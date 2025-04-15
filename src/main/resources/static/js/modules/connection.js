const ConnectionModule = (function() {
    let isConnected = false;
    let showStatusCallback;
    let logger;
    
    function init(statusCallback, loggerModule) {
        showStatusCallback = statusCallback;
        // Ensure logger is properly initialized
        logger = loggerModule || {
            connection: console.log,
            monitoring: console.log
        };
        
        logger.connection('Connection module initialized');
        
        // Initialize saved connections dropdown
        loadSavedConnectionsDropdown();
        
        // Add event listeners for connection management
        document.getElementById('dbType').addEventListener('change', function() {
            updatePortForDbType();
            filterSavedConnectionsByType(this.value);
        });
        
        document.getElementById('savedConnectionSelect').addEventListener('change', loadSelectedConnection);
        document.getElementById('saveConnectionBtn').addEventListener('click', saveCurrentConnection);
        
        return {
            isConnected: function() { return isConnected; },
            startConnection: startConnection,
            stopConnection: stopConnection,
            updatePortForDbType: updatePortForDbType,
            loadSavedConnectionsDropdown: loadSavedConnectionsDropdown,
            saveCurrentConnection: saveCurrentConnection
        };
    }
    
    // Load saved connections into dropdown
    function loadSavedConnectionsDropdown() {
        logger.connection('Loading saved connections dropdown');
        
        fetch('/api/connections')
        .then(response => response.json())
        .then(data => {
            const select = document.getElementById('savedConnectionSelect');
            select.innerHTML = '<option value="">-- Select a saved connection --</option>';
            
            if (data && data.connections) {
                data.connections.forEach(conn => {
                    const option = document.createElement('option');
                    option.value = conn.id;
                    option.textContent = `${conn.name} (${conn.dbType} - ${conn.host})`;
                    option.dataset.dbType = conn.dbType;
                    select.appendChild(option);
                });
            }
        })
        .catch(error => {
            logger.connection('Error loading saved connections', { error: error.toString() });
        });
    }
    
    // Filter saved connections dropdown by database type
    function filterSavedConnectionsByType(dbType) {
        const select = document.getElementById('savedConnectionSelect');
        const options = select.querySelectorAll('option');
        
        options.forEach(option => {
            if (!option.value || option.dataset.dbType === dbType) {
                option.style.display = '';
            } else {
                option.style.display = 'none';
            }
        });
    }
    
    // Load selected connection details
    function loadSelectedConnection() {
        const connectionId = this.value;
        if (!connectionId) return;
        
        logger.connection('Loading saved connection', { id: connectionId });
        
        fetch(`/api/connections/${connectionId}`)
        .then(response => response.json())
        .then(data => {
            if (data && data.connection) {
                const conn = data.connection;
                
                document.getElementById('dbType').value = conn.dbType;
                document.getElementById('host').value = conn.host;
                document.getElementById('port').value = conn.port;
                document.getElementById('database').value = conn.database;
                document.getElementById('username').value = conn.username;
                document.getElementById('password').value = ''; // Password is not stored in plaintext
                document.getElementById('connectionName').value = conn.name;
                
                logger.connection('Loaded saved connection details', { name: conn.name });
            }
        })
        .catch(error => {
            logger.connection('Error loading connection details', { error: error.toString() });
            showStatusCallback('Error loading connection details', 'danger');
        });
    }
    
    // Save current connection details
    function saveCurrentConnection() {
        const connectionData = {
            name: document.getElementById('connectionName').value || 'Unnamed Connection',
            dbType: document.getElementById('dbType').value,
            host: document.getElementById('host').value,
            port: document.getElementById('port').value,
            database: document.getElementById('database').value,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value
        };
        
        // Validate connection data
        if (!connectionData.host || !connectionData.port || !connectionData.database || !connectionData.username) {
            showStatusCallback('Please fill in all required fields', 'warning');
            return;
        }
        
        logger.connection('Saving connection details', { 
            name: connectionData.name,
            dbType: connectionData.dbType,
            host: connectionData.host
        });
        
        fetch('/api/connections', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(connectionData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                showStatusCallback('Connection saved successfully', 'success');
                loadSavedConnectionsDropdown();
            } else {
                showStatusCallback(data.message || 'Failed to save connection', 'danger');
            }
        })
        .catch(error => {
            logger.connection('Error saving connection', { error: error.toString() });
            showStatusCallback('Error saving connection', 'danger');
        });
    }
    
    function startConnection() {
        if (isConnected) {
            logger.connection('Connection attempt while already connected');
            showStatusCallback('Already connected to database', 'warning');
            return;
        }
        
        const formData = {
            dbType: document.getElementById('dbType').value,
            host: document.getElementById('host').value,
            port: document.getElementById('port').value,
            database: document.getElementById('database').value,
            username: document.getElementById('username').value,
            password: '********' // Masked for logging
        };
        
        logger.connection('Attempting to connect to database', {
            dbType: formData.dbType,
            host: formData.host,
            port: formData.port,
            database: formData.database,
            username: formData.username
        });
        
        // Validate form data
        if (!formData.host || !formData.port || !formData.database || !formData.username) {
            logger.connection('Connection validation failed - missing required fields');
            showStatusCallback('Please fill in all required fields', 'warning');
            return;
        }
        
        // Show connecting status
        showStatusCallback('Connecting to database...', 'info');
        
        // Add password back for the actual request
        formData.password = document.getElementById('password').value;
        
        fetch('/connect', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        })
        .then(response => {
            logger.connection('Connection response received', { status: response.status });
            return response.json();
        })
        .then(data => {
            if (data.success) {
                isConnected = true;
                logger.connection('Connection successful', { message: data.message });
                UIModule.init(logger).updateConnectionUI(true);
                showStatusCallback(data.message, 'success');
                TablesModule.init(null, null, null, logger).loadTables();
                
                // Switch to Tables tab
                const tablesTab = document.getElementById('tables-tab');
                bootstrap.Tab.getInstance(tablesTab) || new bootstrap.Tab(tablesTab);
                tablesTab.click();
                
                logger.monitoring('Database connection established', {
                    dbType: formData.dbType,
                    host: formData.host,
                    database: formData.database
                });
            } else {
                logger.connection('Connection failed', { message: data.message });
                showStatusCallback(data.message, 'danger');
            }
        })
        .catch(error => {
            logger.connection('Connection error', { error: error.toString() });
            showStatusCallback('Error: ' + error, 'danger');
        });
    }
    
    function stopConnection() {
        if (!isConnected) {
            logger.connection('Disconnect attempt while not connected');
            showStatusCallback('Not connected to any database', 'warning');
            return;
        }
        
        logger.connection('Attempting to disconnect from database');
        
        fetch('/disconnect', {
            method: 'POST'
        })
        .then(response => {
            logger.connection('Disconnect response received', { status: response.status });
            return response.json();
        })
        .then(data => {
            if (data.success) {
                isConnected = false;
                logger.connection('Disconnection successful', { message: data.message });
                UIModule.init(logger).updateConnectionUI(false);
                UIModule.init(logger).clearResults();
                TablesModule.init(null, null, null, logger).clearTables();
                showStatusCallback(data.message, 'success');
                
                // Switch to Connection tab
                const connectionTab = document.getElementById('connection-tab');
                bootstrap.Tab.getInstance(connectionTab) || new bootstrap.Tab(connectionTab);
                connectionTab.click();
                
                logger.monitoring('Database connection closed');
            } else {
                logger.connection('Disconnection failed', { message: data.message });
                showStatusCallback(data.message, 'danger');
            }
        })
        .catch(error => {
            logger.connection('Disconnection error', { error: error.toString() });
            showStatusCallback('Error: ' + error, 'danger');
        });
    }
    
    function updatePortForDbType() {
        const dbType = document.getElementById('dbType').value;
        const portInput = document.getElementById('port');
        let newPort;
        
        switch (dbType) {
            case 'postgresql':
                newPort = '5432';
                break;
            case 'mysql':
                newPort = '3306';
                break;
            case 'sqlserver':
                newPort = '1433';
                break;
            case 'oracle':
                newPort = '1521';
                break;
        }
        
        logger.connection('Port updated for database type', { dbType: dbType, port: newPort });
        portInput.value = newPort;
    }
    
    return {
        init: init
    };
})();