/**
 * Cloud Connections Module
 * Handles cloud database connections (AWS, Azure, GCP)
 */
const CloudConnectionsModule = (function() {
    let logger;
    let toastManager;
    
    /**
     * Initialize the cloud connections module
     * @param {Object} dependencies - Module dependencies
     * @returns {Object} Cloud connections interface
     */
    function init(dependencies) {
        logger = dependencies.logger || console;
        toastManager = dependencies.toastManager || {
            showToast: (message, type) => { console.log(message); }
        };
        
        logger.info('Cloud Connections module initialized');
        
        // Set up event listeners
        setupEventListeners();
        
        // Load supported cloud providers
        loadCloudProviders();
        
        return {
            testCloudConnection,
            connectToCloud,
            disconnectFromCloud,
            getCloudProviders
        };
    }
    
    /**
     * Set up event listeners for cloud connection UI
     */
    function setupEventListeners() {
        // Cloud provider selection
        const cloudProviderSelect = document.getElementById('cloudProvider');
        if (cloudProviderSelect) {
            cloudProviderSelect.addEventListener('change', function() {
                updateCloudProviderFields(this.value);
            });
        }
        
        // Test cloud connection button
        const testCloudBtn = document.getElementById('testCloudConnection');
        if (testCloudBtn) {
            testCloudBtn.addEventListener('click', function() {
                const connectionData = getCloudConnectionFormData();
                testCloudConnection(connectionData);
            });
        }
        
        // Connect to cloud button
        const connectCloudBtn = document.getElementById('connectToCloud');
        if (connectCloudBtn) {
            connectCloudBtn.addEventListener('click', function() {
                const connectionData = getCloudConnectionFormData();
                connectToCloud(connectionData);
            });
        }
        
        // SSL/TLS toggle
        const sslToggle = document.getElementById('useSSL');
        if (sslToggle) {
            sslToggle.addEventListener('change', function() {
                toggleSSLFields(this.checked);
            });
        }
        
        // Authentication method selection
        const authMethodSelect = document.getElementById('authMethod');
        if (authMethodSelect) {
            authMethodSelect.addEventListener('change', function() {
                updateAuthenticationFields(this.value);
            });
        }
    }
    
    /**
     * Load supported cloud providers
     */
    function loadCloudProviders() {
        fetch('/api/cloud-connections/providers')
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    populateCloudProviders(data.providers);
                    logger.info('Loaded cloud providers successfully');
                } else {
                    throw new Error('Failed to load cloud providers');
                }
            })
            .catch(error => {
                logger.error('Error loading cloud providers: ' + error.message);
                toastManager.showToast('Failed to load cloud providers', 'warning');
            });
    }
    
    /**
     * Populate cloud providers dropdown
     * @param {Object} providers - Cloud providers data
     */
    function populateCloudProviders(providers) {
        const select = document.getElementById('cloudProvider');
        if (!select) return;
        
        // Clear existing options
        select.innerHTML = '<option value="">Select Cloud Provider...</option>';
        
        // Add cloud providers
        Object.keys(providers).forEach(key => {
            const provider = providers[key];
            const option = document.createElement('option');
            option.value = key;
            option.textContent = provider.name;
            option.dataset.services = JSON.stringify(provider.services);
            option.dataset.authMethods = JSON.stringify(provider.authMethods);
            select.appendChild(option);
        });
    }
    
    /**
     * Update fields based on selected cloud provider
     * @param {string} provider - Selected cloud provider
     */
    function updateCloudProviderFields(provider) {
        const serviceSelect = document.getElementById('cloudService');
        const authMethodSelect = document.getElementById('authMethod');
        const endpointField = document.getElementById('cloudEndpoint');
        const portField = document.getElementById('cloudPort');
        
        if (!serviceSelect || !authMethodSelect) return;
        
        // Clear existing options
        serviceSelect.innerHTML = '<option value="">Select Service...</option>';
        authMethodSelect.innerHTML = '<option value="">Select Authentication...</option>';
        
        if (!provider) return;
        
        // Get provider data
        const option = document.querySelector(`#cloudProvider option[value="${provider}"]`);
        if (!option) return;
        
        const services = JSON.parse(option.dataset.services || '[]');
        const authMethods = JSON.parse(option.dataset.authMethods || '[]');
        
        // Populate services
        services.forEach(service => {
            const serviceOption = document.createElement('option');
            serviceOption.value = service.toLowerCase().replace(/\s+/g, '-');
            serviceOption.textContent = service;
            serviceSelect.appendChild(serviceOption);
        });
        
        // Populate authentication methods
        authMethods.forEach(method => {
            const authOption = document.createElement('option');
            authOption.value = method.toLowerCase().replace(/\s+/g, '-');
            authOption.textContent = method;
            authMethodSelect.appendChild(authOption);
        });
        
        // Set default values based on provider
        updateProviderDefaults(provider, endpointField, portField);
    }
    
    /**
     * Update provider-specific defaults
     * @param {string} provider - Cloud provider
     * @param {Element} endpointField - Endpoint input field
     * @param {Element} portField - Port input field
     */
    function updateProviderDefaults(provider, endpointField, portField) {
        const defaults = {
            aws: {
                endpoint: 'your-rds-instance.region.rds.amazonaws.com',
                port: 5432
            },
            azure: {
                endpoint: 'your-server.database.windows.net',
                port: 1433
            },
            gcp: {
                endpoint: 'your-project:region:instance',
                port: 5432
            }
        };
        
        if (defaults[provider]) {
            if (endpointField) endpointField.placeholder = defaults[provider].endpoint;
            if (portField) portField.value = defaults[provider].port;
        }
    }
    
    /**
     * Toggle SSL/TLS configuration fields
     * @param {boolean} enabled - Whether SSL is enabled
     */
    function toggleSSLFields(enabled) {
        const sslModeField = document.getElementById('sslMode');
        const sslCertField = document.getElementById('sslCertificate');
        
        if (sslModeField) sslModeField.disabled = !enabled;
        if (sslCertField) sslCertField.disabled = !enabled;
    }
    
    /**
     * Update authentication fields based on selected method
     * @param {string} method - Authentication method
     */
    function updateAuthenticationFields(method) {
        const usernameField = document.getElementById('cloudUsername');
        const passwordField = document.getElementById('cloudPassword');
        const serviceAccountField = document.getElementById('serviceAccountFile');
        const iamRoleField = document.getElementById('iamRole');
        
        // Hide all auth-specific fields first
        hideElement('usernamePasswordAuth');
        hideElement('serviceAccountAuth');
        hideElement('iamAuth');
        hideElement('azureAdAuth');
        
        // Show relevant fields based on method
        switch (method) {
            case 'username-password':
                showElement('usernamePasswordAuth');
                break;
            case 'service-account':
                showElement('serviceAccountAuth');
                break;
            case 'iam':
                showElement('iamAuth');
                break;
            case 'azure-ad':
                showElement('azureAdAuth');
                break;
        }
    }
    
    /**
     * Get cloud connection form data
     * @returns {Object} Cloud connection data
     */
    function getCloudConnectionFormData() {
        return {
            name: document.getElementById('cloudConnectionName')?.value || '',
            cloudProvider: document.getElementById('cloudProvider')?.value || '',
            service: document.getElementById('cloudService')?.value || '',
            endpoint: document.getElementById('cloudEndpoint')?.value || '',
            port: parseInt(document.getElementById('cloudPort')?.value) || 0,
            database: document.getElementById('cloudDatabase')?.value || '',
            username: document.getElementById('cloudUsername')?.value || '',
            password: document.getElementById('cloudPassword')?.value || '',
            dbType: getDbTypeFromService(),
            
            // SSL/TLS settings
            useSSL: document.getElementById('useSSL')?.checked || false,
            sslMode: document.getElementById('sslMode')?.value || 'require',
            sslCertPath: document.getElementById('sslCertificate')?.value || '',
            
            // Authentication settings
            authMethod: document.getElementById('authMethod')?.value || '',
            useIAM: document.getElementById('authMethod')?.value === 'iam',
            useAzureAD: document.getElementById('authMethod')?.value === 'azure-ad',
            useServiceAccount: document.getElementById('authMethod')?.value === 'service-account',
            serviceAccountPath: document.getElementById('serviceAccountFile')?.value || '',
            
            // Connection settings
            connectionTimeout: parseInt(document.getElementById('connectionTimeout')?.value) || 30,
            autoReconnect: document.getElementById('autoReconnect')?.checked || true,
            
            // Cloud SQL specific
            useCloudSQLProxy: document.getElementById('useCloudSQLProxy')?.checked || false,
            cloudSQLInstance: document.getElementById('cloudSQLInstance')?.value || ''
        };
    }
    
    /**
     * Get database type from selected service
     * @returns {string} Database type
     */
    function getDbTypeFromService() {
        const service = document.getElementById('cloudService')?.value || '';
        const provider = document.getElementById('cloudProvider')?.value || '';
        
        // Map services to database types
        const serviceMapping = {
            'rds': 'postgresql', // Default, could be mysql, postgres, etc.
            'aurora': 'postgresql',
            'sql-database': 'sqlserver',
            'cloud-sql': 'postgresql',
            'database-for-postgresql': 'postgresql',
            'database-for-mysql': 'mysql'
        };
        
        return serviceMapping[service] || 'postgresql';
    }
    
    /**
     * Test cloud database connection
     * @param {Object} connectionData - Connection data
     */
    function testCloudConnection(connectionData) {
        if (!validateCloudConnectionData(connectionData)) {
            return;
        }
        
        logger.info(`Testing cloud connection to ${connectionData.cloudProvider}`);
        
        // Show loading state
        const testBtn = document.getElementById('testCloudConnection');
        if (testBtn) {
            testBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Testing...';
            testBtn.disabled = true;
        }
        
        fetch('/api/cloud-connections/test', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(connectionData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                toastManager.showToast(`Connection test successful for ${data.provider}`, 'success');
                logger.info(`Cloud connection test successful: ${data.provider}`);
            } else {
                toastManager.showToast(`Connection test failed: ${data.message}`, 'danger');
                logger.error(`Cloud connection test failed: ${data.message}`);
            }
        })
        .catch(error => {
            logger.error('Error testing cloud connection: ' + error.message);
            toastManager.showToast(`Connection test error: ${error.message}`, 'danger');
        })
        .finally(() => {
            // Reset button state
            if (testBtn) {
                testBtn.innerHTML = 'Test Connection';
                testBtn.disabled = false;
            }
        });
    }
    
    /**
     * Connect to cloud database
     * @param {Object} connectionData - Connection data
     */
    function connectToCloud(connectionData) {
        if (!validateCloudConnectionData(connectionData)) {
            return;
        }
        
        logger.info(`Connecting to cloud database: ${connectionData.cloudProvider}`);
        
        // Show loading state
        const connectBtn = document.getElementById('connectToCloud');
        if (connectBtn) {
            connectBtn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Connecting...';
            connectBtn.disabled = true;
        }
        
        fetch('/api/cloud-connections/connect', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(connectionData)
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                toastManager.showToast(`Successfully connected to ${data.provider}`, 'success');
                logger.info(`Cloud connection successful: ${data.provider}`);
                
                // Update UI to show connected state
                updateConnectionStatus(true, data);
                
                // Switch to appropriate tab (tables, query, etc.)
                switchToMainInterface();
                
            } else {
                toastManager.showToast(`Connection failed: ${data.message}`, 'danger');
                logger.error(`Cloud connection failed: ${data.message}`);
            }
        })
        .catch(error => {
            logger.error('Error connecting to cloud database: ' + error.message);
            toastManager.showToast(`Connection error: ${error.message}`, 'danger');
        })
        .finally(() => {
            // Reset button state
            if (connectBtn) {
                connectBtn.innerHTML = 'Connect';
                connectBtn.disabled = false;
            }
        });
    }
    
    /**
     * Disconnect from cloud database
     * @param {string} connectionId - Connection ID
     */
    function disconnectFromCloud(connectionId) {
        logger.info(`Disconnecting from cloud database: ${connectionId}`);
        
        fetch(`/api/cloud-connections/disconnect/${connectionId}`, {
            method: 'POST'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                toastManager.showToast('Disconnected from cloud database', 'success');
                logger.info('Cloud disconnection successful');
                
                // Update UI to show disconnected state
                updateConnectionStatus(false);
                
            } else {
                toastManager.showToast(`Disconnection failed: ${data.message}`, 'warning');
                logger.error(`Cloud disconnection failed: ${data.message}`);
            }
        })
        .catch(error => {
            logger.error('Error disconnecting from cloud database: ' + error.message);
            toastManager.showToast(`Disconnection error: ${error.message}`, 'danger');
        });
    }
    
    /**
     * Validate cloud connection data
     * @param {Object} connectionData - Connection data to validate
     * @returns {boolean} Whether data is valid
     */
    function validateCloudConnectionData(connectionData) {
        const required = ['cloudProvider', 'endpoint', 'database'];
        
        for (const field of required) {
            if (!connectionData[field]) {
                toastManager.showToast(`Please fill in the ${field.replace(/([A-Z])/g, ' $1').toLowerCase()}`, 'warning');
                return false;
            }
        }
        
        // Validate authentication
        if (connectionData.authMethod === 'username-password') {
            if (!connectionData.username || !connectionData.password) {
                toastManager.showToast('Please provide username and password', 'warning');
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Update connection status in UI
     * @param {boolean} connected - Whether connected
     * @param {Object} data - Connection data
     */
    function updateConnectionStatus(connected, data = {}) {
        const statusElement = document.getElementById('cloudConnectionStatus');
        if (statusElement) {
            if (connected) {
                statusElement.innerHTML = `
                    <span class="badge bg-success">Connected to ${data.provider || 'Cloud'}</span>
                    <small class="text-muted">${data.endpoint || ''}</small>
                `;
            } else {
                statusElement.innerHTML = '<span class="badge bg-secondary">Not Connected</span>';
            }
        }
    }
    
    /**
     * Switch to main interface after successful connection
     */
    function switchToMainInterface() {
        // Switch to tables or query tab
        const tablesTab = document.getElementById('tables-tab');
        if (tablesTab) {
            bootstrap.Tab.getOrCreateInstance(tablesTab).show();
        }
    }
    
    /**
     * Utility function to show element
     * @param {string} elementId - Element ID
     */
    function showElement(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.style.display = 'block';
        }
    }
    
    /**
     * Utility function to hide element
     * @param {string} elementId - Element ID
     */
    function hideElement(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.style.display = 'none';
        }
    }
    
    /**
     * Get cloud providers
     * @returns {Promise} Promise resolving to cloud providers data
     */
    function getCloudProviders() {
        return fetch('/api/cloud-connections/providers')
            .then(response => response.json())
            .then(data => data.success ? data.providers : {});
    }
    
    return {
        init
    };
})();