/**
 * Database Drivers Module
 * Handles custom database driver management
 */
const DbDriversModule = (function() {
    // Store for custom drivers
    let customDrivers = [];
    let logger;
    let toastManager;
    
    /**
     * Initialize the database drivers module
     * @param {Object} dependencies - Module dependencies
     * @returns {Object} Database drivers interface
     */
    function init(dependencies) {
        logger = dependencies.logger || console;
        toastManager = dependencies.toastManager || {
            showToast: (message, type) => { console.log(message); }
        };
        
        logger.info('Database Drivers module initialized');
        
        // Load custom drivers
        loadCustomDrivers();
        
        // Set up event listeners
        document.getElementById('saveCustomDbDriver').addEventListener('click', uploadCustomDriver);
        
        // Update port when database type changes
        document.getElementById('dbType').addEventListener('change', function() {
            updateDefaultPort(this.value);
        });
        
        return {
            addCustomDriver,
            getCustomDrivers,
            loadCustomDrivers,
            getDriverById,
            getDefaultPortForDriver
        };
    }
    
    /**
     * Upload a custom database driver
     */
    function uploadCustomDriver() {
        const name = document.getElementById('customDbName').value;
        const driverFile = document.getElementById('customDbDriver').files[0];
        const className = document.getElementById('customDbClassName').value;
        const urlTemplate = document.getElementById('customDbUrlTemplate').value;
        const defaultPort = parseInt(document.getElementById('customDbDefaultPort').value);
        
        if (!name || !driverFile || !className || !urlTemplate || isNaN(defaultPort)) {
            toastManager.showToast('Please fill in all fields', 'warning');
            return;
        }
        
        logger.info('Uploading custom database driver: ' + name);
        
        const formData = new FormData();
        formData.append('name', name);
        formData.append('className', className);
        formData.append('urlTemplate', urlTemplate);
        formData.append('defaultPort', defaultPort);
        formData.append('driverFile', driverFile);
        
        fetch('/api/drivers', {
            method: 'POST',
            body: formData
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                toastManager.showToast(`Database driver "${name}" added successfully`, 'success');
                
                // Add to local store
                customDrivers.push(data.driver);
                
                // Update UI
                updateDriversDropdown();
                
                // Close modal
                const modal = bootstrap.Modal.getInstance(document.getElementById('customDbDriverModal'));
                modal.hide();
                
                // Clear form
                document.getElementById('customDbDriverForm').reset();
            } else {
                throw new Error(data.message || 'Failed to add database driver');
            }
        })
        .catch(error => {
            logger.error('Error adding custom database driver: ' + error.message);
            toastManager.showToast(`Error adding driver: ${error.message}`, 'danger');
        });
    }
    
    /**
     * Load custom database drivers from the server
     */
    function loadCustomDrivers() {
        fetch('/api/drivers')
            .then(response => response.json())
            .then(data => {
                customDrivers = data;
                updateDriversDropdown();
                logger.info(`Loaded ${customDrivers.length} custom database drivers`);
            })
            .catch(error => {
                logger.error('Error loading custom database drivers: ' + error.message);
            });
    }
    
    /**
     * Update the database type dropdown with custom drivers
     */
    function updateDriversDropdown() {
        const dbTypeSelect = document.getElementById('dbType');
        
        // Remove existing custom options
        Array.from(dbTypeSelect.options).forEach(option => {
            if (option.dataset.custom === 'true') {
                dbTypeSelect.removeChild(option);
            }
        });
        
        // Add custom drivers
        if (customDrivers.length > 0) {
            // Add separator
            const separator = document.createElement('option');
            separator.disabled = true;
            separator.dataset.custom = 'true';
            separator.textContent = '──────────────';
            dbTypeSelect.appendChild(separator);
            
            // Add custom drivers
            customDrivers.forEach(driver => {
                const option = document.createElement('option');
                option.value = 'custom:' + driver.id;
                option.textContent = driver.name;
                option.dataset.custom = 'true';
                dbTypeSelect.appendChild(option);
            });
        }
    }
    
    /**
     * Get all custom drivers
     * @returns {Array} Array of custom drivers
     */
    function getCustomDrivers() {
        return [...customDrivers];
    }
    
    /**
     * Get a driver by ID
     * @param {string} id - Driver ID
     * @returns {Object|null} Driver object or null if not found
     */
    function getDriverById(id) {
        return customDrivers.find(driver => driver.id === id) || null;
    }
    
    /**
     * Update the port field with the default port for the selected database type
     * @param {string} dbType - Database type
     */
    function updateDefaultPort(dbType) {
        const portInput = document.getElementById('port');
        
        if (dbType.startsWith('custom:')) {
            const driverId = dbType.split(':')[1];
            const driver = getDriverById(driverId);
            if (driver) {
                portInput.value = driver.defaultPort;
            }
        } else {
            // Default ports for standard database types
            const defaultPorts = {
                'postgresql': 5432,
                'mysql': 3306,
                'sqlserver': 1433,
                'oracle': 1521
            };
            
            portInput.value = defaultPorts[dbType] || '';
        }
    }
    
    /**
     * Get the default port for a driver
     * @param {string} dbType - Database type
     * @returns {number} Default port
     */
    function getDefaultPortForDriver(dbType) {
        if (dbType.startsWith('custom:')) {
            const driverId = dbType.split(':')[1];
            const driver = getDriverById(driverId);
            if (driver) {
                return driver.defaultPort;
            }
        } else {
            // Default ports for standard database types
            const defaultPorts = {
                'postgresql': 5432,
                'mysql': 3306,
                'sqlserver': 1433,
                'oracle': 1521
            };
            
            return defaultPorts[dbType] || 0;
        }
        
        return 0;
    }
    
    return {
        init
    };
})();
