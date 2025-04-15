document.addEventListener('DOMContentLoaded', function() {
    // Initialize logger first
    const logger = LoggerModule.init();
    logger.common('Application starting up');
    
    // Initialize modules
    const editor = EditorModule.init(logger);
    const ui = UIModule.init(logger);
    const connection = ConnectionModule.init(ui.showStatus, logger);
    const tables = TablesModule.init(connection, editor, ui.showStatus, logger);
    const query = QueryModule.init(editor, ui.showStatus, tables.displayResults, logger);
    const savedConnections = SavedConnectionsModule.init(connection, ui.showStatus, logger);
    const dbDrivers = DbDriversModule.init(ui.showStatus, logger);
    
    logger.common('All modules initialized');
    
    // Set up event listeners
    setupEventListeners(connection, tables, query, savedConnections, dbDrivers, editor, logger);
    
    // Initialize UI components
    ui.initializeToast();
    
    // Display saved connections on load
    savedConnections.displaySavedConnections();
    
    logger.access('Application UI ready');
    
    function setupEventListeners(connection, tables, query, savedConnections, dbDrivers, editor, logger) {
        logger.common('Setting up event listeners');
        
        // Form submission
        document.getElementById('connectionForm').addEventListener('submit', function(e) {
            e.preventDefault();
            logger.stdin('Connection form submitted', { preventDefault: true });
        });
        
        // Connection controls
        document.getElementById('startConnection').addEventListener('click', function() {
            logger.stdin('Connect button clicked');
            connection.startConnection();
        });
        
        // Save connection button
        document.getElementById('saveConnectionBtn').addEventListener('click', function() {
            logger.stdin('Save button clicked');
            connection.saveCurrentConnection();
        });
        
        document.getElementById('stopConnection').addEventListener('click', function() {
            logger.stdin('Stop connection button clicked');
            connection.stopConnection();
        });
        
        // Custom database driver
        document.getElementById('saveCustomDbDriver').addEventListener('click', function() {
            logger.stdin('Save custom database driver button clicked');
            
            const driverData = {
                name: document.getElementById('customDbName').value,
                className: document.getElementById('customDbClassName').value,
                urlTemplate: document.getElementById('customDbUrlTemplate').value,
                defaultPort: document.getElementById('customDbDefaultPort').value
            };
            
            const driverFile = document.getElementById('customDbDriver').files[0];
            
            if (!driverData.name || !driverData.className || !driverData.urlTemplate || !driverData.defaultPort || !driverFile) {
                ui.showStatus('Please fill in all fields', 'warning');
                logger.error('Custom driver form validation failed', driverData);
                return;
            }
            
            logger.audit('Adding custom database driver', driverData);
            
            dbDrivers.addCustomDriver(driverData, driverFile)
                .then(driver => {
                    ui.showStatus(`Database driver "${driver.name}" added successfully`, 'success');
                    logger.audit('Custom database driver added successfully', { id: driver.id, name: driver.name });
                    
                    // Close modal
                    const modal = bootstrap.Modal.getInstance(document.getElementById('customDbDriverModal'));
                    modal.hide();
                    
                    // Clear form
                    document.getElementById('customDbDriverForm').reset();
                })
                .catch(error => {
                    ui.showStatus(`Failed to add database driver: ${error.message}`, 'danger');
                    logger.error('Error adding custom database driver', { error: error.toString() });
                });
        });
        
        // Query execution
        document.getElementById('executeQuery').addEventListener('click', function() {
            logger.stdin('Execute query button clicked');
            query.executeQuery();
        });
        
        // Export query results
        document.getElementById('exportExcel').addEventListener('click', function() {
            logger.stdin('Export query to Excel button clicked');
            query.exportResults('excel');
        });
        
        document.getElementById('exportPdf').addEventListener('click', function() {
            logger.stdin('Export query to PDF button clicked');
            query.exportResults('pdf');
        });
        
        // Export table results
        document.getElementById('exportTableExcel').addEventListener('click', function() {
            logger.stdin('Export table to Excel button clicked');
            tables.exportTable('excel');
        });
        
        document.getElementById('exportTablePdf').addEventListener('click', function() {
            logger.stdin('Export table to PDF button clicked');
            tables.exportTable('pdf');
        });
        
        // Custom database driver
        document.getElementById('saveCustomDbDriver').addEventListener('click', function() {
            logger.stdin('Save custom database driver button clicked');
            saveCustomDatabaseDriver();
        });
        
        // Pagination
        document.getElementById('prevPage').addEventListener('click', function() {
            logger.stdin('Previous page button clicked');
            query.navigatePage(-1);
        });
        
        document.getElementById('nextPage').addEventListener('click', function() {
            logger.stdin('Next page button clicked');
            query.navigatePage(1);
        });
        
        // These event listeners might be for a modal that doesn't exist yet
        if (document.getElementById('saveConnection')) {
            document.getElementById('saveConnection').addEventListener('click', function() {
                logger.stdin('Save connection button clicked');
                savedConnections.openSaveConnectionModal();
            });
        }
        
        if (document.getElementById('confirmSaveConnection')) {
            document.getElementById('confirmSaveConnection').addEventListener('click', function() {
                logger.stdin('Confirm save connection button clicked');
                savedConnections.saveConnection();
            });
        }
        
        // Refresh buttons
        document.getElementById('refreshTables').addEventListener('click', function() {
            logger.stdin('Refresh tables button clicked');
            tables.loadTables();
        });
        
        document.getElementById('refreshConnections').addEventListener('click', function() {
            logger.stdin('Refresh connections button clicked');
            savedConnections.displaySavedConnections();
        });
        
        // Search filters
        document.getElementById('tableSearch').addEventListener('input', function() {
            logger.stdin('Table search input', { value: this.value });
            tables.filterTables();
        });
        
        document.getElementById('connectionSearch').addEventListener('input', function() {
            logger.stdin('Connection search input', { value: this.value });
            savedConnections.filterConnections();
        });
        
        // Database type change
        document.getElementById('dbType').addEventListener('change', function() {
            logger.stdin('Database type changed', { value: this.value });
            connection.updatePortForDbType();
        });
        
        logger.common('Event listeners setup complete');
    }
    
    // Function to save custom database driver
    function saveCustomDatabaseDriver() {
        const dbName = document.getElementById('customDbName').value;
        const driverFile = document.getElementById('customDbDriver').files[0];
        const className = document.getElementById('customDbClassName').value;
        const urlTemplate = document.getElementById('customDbUrlTemplate').value;
        const defaultPort = document.getElementById('customDbDefaultPort').value;
        
        if (!dbName || !driverFile || !className || !urlTemplate || !defaultPort) {
            ui.showStatus('Please fill in all fields', 'warning');
            return;
        }
        
        // Create FormData to send the file
        const formData = new FormData();
        formData.append('name', dbName);
        formData.append('driverFile', driverFile);
        formData.append('className', className);
        formData.append('urlTemplate', urlTemplate);
        formData.append('defaultPort', defaultPort);
        
        // Send to backend
        fetch('/api/drivers', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                ui.showStatus('Database driver added successfully', 'success');
                
                // Add to dropdown
                const option = document.createElement('option');
                option.value = data.id || dbName.toLowerCase();
                option.textContent = dbName;
                document.getElementById('dbType').appendChild(option);
                
                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('customDbDriverModal'));
                modal.hide();
                
                // Clear form
                document.getElementById('customDbName').value = '';
                document.getElementById('customDbDriver').value = '';
                document.getElementById('customDbClassName').value = '';
                document.getElementById('customDbUrlTemplate').value = '';
                document.getElementById('customDbDefaultPort').value = '';
            } else {
                ui.showStatus(data.message || 'Failed to add database driver', 'danger');
            }
        })
        .catch(error => {
            logger.connection('Error adding database driver', { error: error.toString() });
            ui.showStatus('Error adding database driver', 'danger');
        });
    }
    
    // Function to load custom database drivers
    function loadCustomDatabaseDrivers() {
        fetch('/api/drivers')
        .then(response => response.json())
        .then(data => {
            if (data && data.drivers && data.drivers.length > 0) {
                const dbTypeSelect = document.getElementById('dbType');
                
                data.drivers.forEach(driver => {
                    const option = document.createElement('option');
                    option.value = driver.id;
                    option.textContent = driver.name;
                    dbTypeSelect.appendChild(option);
                });
            }
        })
        .catch(error => {
            logger.connection('Error loading custom database drivers', { error: error.toString() });
        });
    }
});