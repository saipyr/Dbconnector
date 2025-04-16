// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', function() {
    // Initialize modules in the correct order
    const logger = LoggerModule.init();
    
    // Initialize toast manager
    const toastManager = ToastModule.init();
    
    // Initialize connection manager with dependencies
    const connectionManager = ConnectionManager.init({
        logger: logger,
        toastManager: toastManager
    });
    
    // Initialize database drivers module with dependencies
    const dbDriversModule = DbDriversModule.init({
        logger: logger,
        toastManager: toastManager
    });
    
    // Initialize query module with dependencies
    const queryModule = QueryModule.init({
        logger: logger,
        toastManager: toastManager,
        connectionManager: connectionManager
    });
    
    // Add direct event listeners for critical buttons
    setupDirectEventListeners(logger, toastManager, connectionManager);
    
    // Log application start
    logger.info('DB Connector application initialized');
    
    // Check if Bootstrap is loaded
    if (typeof bootstrap === 'undefined') {
        console.error('Bootstrap is not loaded. Some UI components may not work correctly.');
    }
});

// Setup direct event listeners for critical functionality
function setupDirectEventListeners(logger, toastManager, connectionManager) {
    // Connect button
    const connectBtn = document.getElementById('startConnection');
    if (connectBtn) {
        connectBtn.addEventListener('click', function() {
            logger.info('Connect button clicked - Starting connection process');
            toastManager.showToast('Attempting to connect...', 'info');
            
            logger.info('Collecting connection form data');
            const connectionData = getConnectionFormData();
            logger.info(`Connection data collected: ${JSON.stringify(connectionData)}`);
            
            // Update UI to show progress
            connectBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Connecting...';
            connectBtn.disabled = true;
            
            logger.info('Sending connection request to server');
            // Make direct API call to connect
            fetch('/api/connections/connect', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(connectionData)
            })
            .then(response => {
                logger.info(`Server responded with status: ${response.status}`);
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                logger.info('Parsing JSON response');
                return response.json();
            })
            .then(data => {
                logger.info(`Response data received: ${JSON.stringify(data)}`);
                if (data.success) {
                    logger.info('Connection successful');
                    toastManager.showToast(`Connected successfully to ${connectionData.dbType} at ${connectionData.host}!`, 'success');
                    
                    logger.info('Switching to tables tab');
                    // Switch to tables tab if it exists
                    const tablesTab = document.getElementById('tables-tab');
                    if (tablesTab) {
                        logger.info('Tables tab found, activating');
                        bootstrap.Tab.getOrCreateInstance(tablesTab).show();
                    } else {
                        logger.warn('Tables tab not found');
                    }
                } else {
                    logger.error(`Connection failed: ${data.message}`);
                    throw new Error(data.message || 'Failed to connect');
                }
            })
            .catch(error => {
                logger.error('Connection error: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            })
            .finally(() => {
                logger.info('Connection process completed');
                // Reset button
                connectBtn.innerHTML = 'Connect';
                connectBtn.disabled = false;
            });
        });
    } else {
        logger.error('Connect button not found in DOM');
    }
    
    // Save connection button
    const saveBtn = document.getElementById('saveConnectionBtn');
    if (saveBtn) {
        saveBtn.addEventListener('click', function() {
            logger.info('Save button clicked - Opening save connection modal');
            
            // Validate form data before showing modal
            logger.info('Validating connection form data');
            const connectionData = getConnectionFormData();
            
            if (!connectionData.dbType || !connectionData.host) {
                logger.warn('Incomplete connection data, showing warning');
                toastManager.showToast('Please fill in required connection details first', 'warning');
                return;
            }
            
            logger.info('Connection data valid, preparing to show modal');
            
            // Show the modal
            const saveModal = new bootstrap.Modal(document.getElementById('saveConnectionModal'));
            if (saveModal) {
                logger.info('Save connection modal found, showing');
                saveModal.show();
                logger.info('Modal displayed');
                
                // Pre-fill connection name if empty
                const nameField = document.getElementById('modalConnectionName');
                if (nameField && !nameField.value) {
                    const suggestedName = `${connectionData.dbType} - ${connectionData.host}`;
                    logger.info(`Pre-filling connection name: ${suggestedName}`);
                    nameField.value = suggestedName;
                }
            } else {
                logger.error('Save connection modal not found or Bootstrap not loaded');
                toastManager.showToast('Could not open save dialog', 'danger');
            }
        });
    } else {
        logger.error('Save button not found in DOM');
    }
    
    // Confirm save in modal
    const confirmSaveBtn = document.getElementById('confirmSaveConnection');
    if (confirmSaveBtn) {
        confirmSaveBtn.addEventListener('click', function() {
            logger.info('Confirm save button clicked - Starting save process');
            
            logger.info('Collecting connection form data');
            const connectionData = getConnectionFormData();
            
            logger.info('Getting connection name and folder from modal');
            connectionData.name = document.getElementById('modalConnectionName').value.trim();
            connectionData.folder = document.getElementById('modalSaveFolder').value.trim() || 'default';
            
            logger.info(`Saving connection "${connectionData.name}" to folder "${connectionData.folder}"`);
            
            // Update UI to show progress
            confirmSaveBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Saving...';
            confirmSaveBtn.disabled = true;
            
            logger.info('Sending save request to server');
            // Make direct API call to save
            fetch('/api/connections/save', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(connectionData)
            })
            .then(response => {
                logger.info(`Server responded with status: ${response.status}`);
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                logger.info('Parsing JSON response');
                return response.json();
            })
            .then(data => {
                logger.info(`Response data received: ${JSON.stringify(data)}`);
                if (data.success) {
                    logger.info('Connection saved successfully');
                    toastManager.showToast(`Connection "${connectionData.name}" saved successfully!`, 'success');
                    
                    logger.info('Closing modal');
                    // Close the modal
                    const saveModal = bootstrap.Modal.getInstance(document.getElementById('saveConnectionModal'));
                    if (saveModal) {
                        saveModal.hide();
                        logger.info('Modal closed');
                    } else {
                        logger.warn('Could not find modal instance to close');
                    }
                    
                    logger.info('Refreshing saved connections list');
                    // Refresh connections list if the function exists
                    if (typeof connectionManager.refreshConnectionsList === 'function') {
                        connectionManager.refreshConnectionsList();
                        logger.info('Connections list refreshed');
                    } else {
                        logger.warn('refreshConnectionsList function not found');
                    }
                } else {
                    logger.error(`Save failed: ${data.message}`);
                    throw new Error(data.message || 'Failed to save connection');
                }
            })
            .catch(error => {
                logger.error('Save error: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            })
            .finally(() => {
                logger.info('Save process completed');
                // Reset button
                confirmSaveBtn.innerHTML = 'Save';
                confirmSaveBtn.disabled = false;
            });
        });
    } else {
        logger.error('Confirm save button not found in DOM');
    }
    
    // Add custom driver button
    const addDriverBtn = document.getElementById('addCustomDriver');
    if (addDriverBtn) {
        addDriverBtn.addEventListener('click', function() {
            logger.info('Add custom driver button clicked - Opening driver modal');
            
            // Show the modal
            const driverModal = new bootstrap.Modal(document.getElementById('customDbDriverModal'));
            if (driverModal) {
                logger.info('Custom driver modal found, showing');
                driverModal.show();
                logger.info('Driver modal displayed');
            } else {
                logger.error('Custom driver modal not found or Bootstrap not loaded');
                toastManager.showToast('Could not open driver dialog', 'danger');
            }
        });
    } else {
        logger.error('Add custom driver button not found in DOM');
    }
    
    // Save custom driver button
    const saveDriverBtn = document.getElementById('saveCustomDbDriver');
    if (saveDriverBtn) {
        saveDriverBtn.addEventListener('click', function() {
            logger.info('Save custom driver button clicked - Starting driver upload process');
            
            // Get form data
            logger.info('Collecting driver form data');
            const driverName = document.getElementById('customDbName').value.trim();
            const driverClassName = document.getElementById('customDbClassName').value.trim();
            const urlTemplate = document.getElementById('customDbUrlTemplate').value.trim();
            const defaultPort = document.getElementById('customDbDefaultPort').value.trim();
            const driverFile = document.getElementById('customDbDriver').files[0];
            
            logger.info(`Driver details: ${driverName}, ${driverClassName}, port ${defaultPort}`);
            
            if (!driverName || !driverClassName || !urlTemplate || !defaultPort || !driverFile) {
                logger.warn('Incomplete driver data, showing warning');
                toastManager.showToast('Please fill in all required fields', 'warning');
                return;
            }
            
            // Update UI to show progress
            saveDriverBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Uploading...';
            saveDriverBtn.disabled = true;
            
            // Create FormData object
            logger.info('Creating FormData for file upload');
            const formData = new FormData();
            formData.append('name', driverName);
            formData.append('className', driverClassName);
            formData.append('urlTemplate', urlTemplate);
            formData.append('defaultPort', defaultPort);
            formData.append('driverFile', driverFile);
            
            logger.info('Sending driver upload request to server');
            // Make API call to upload driver
            fetch('/api/drivers', {
                method: 'POST',
                body: formData
            })
            .then(response => {
                logger.info(`Server responded with status: ${response.status}`);
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                logger.info('Parsing JSON response');
                return response.json();
            })
            .then(data => {
                logger.info(`Response data received: ${JSON.stringify(data)}`);
                if (data.success) {
                    logger.info('Driver uploaded successfully');
                    toastManager.showToast(`Driver "${driverName}" uploaded successfully!`, 'success');
                    
                    logger.info('Closing modal');
                    // Close the modal
                    const driverModal = bootstrap.Modal.getInstance(document.getElementById('customDbDriverModal'));
                    if (driverModal) {
                        driverModal.hide();
                        logger.info('Modal closed');
                    } else {
                        logger.warn('Could not find modal instance to close');
                    }
                    
                    // Reset form
                    logger.info('Resetting driver form');
                    document.getElementById('customDbDriverForm').reset();
                } else {
                    logger.error(`Upload failed: ${data.message}`);
                    throw new Error(data.message || 'Failed to upload driver');
                }
            })
            .catch(error => {
                logger.error('Driver upload error: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            })
            .finally(() => {
                logger.info('Driver upload process completed');
                // Reset button
                saveDriverBtn.innerHTML = 'Add Driver';
                saveDriverBtn.disabled = false;
            });
        });
    } else {
        logger.error('Save custom driver button not found in DOM');
    }
}

// Helper function to get connection form data
function getConnectionFormData() {
    return {
        dbType: document.getElementById('dbType').value,
        host: document.getElementById('host').value,
        port: document.getElementById('port').value,
        database: document.getElementById('database').value,
        username: document.getElementById('username').value,
        password: document.getElementById('password').value
    };
}