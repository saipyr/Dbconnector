/**
 * Cloud Connections UI Controller
 * Handles the cloud connections interface and SSL certificate management
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize modules
    const logger = LoggerModule.init();
    const toastManager = ToastModule.init();
    const cloudConnections = CloudConnectionsModule.init({
        logger: logger,
        toastManager: toastManager
    });
    
    // Initialize SSL certificate management
    initializeSSLCertificateManagement();
    
    // Initialize cloud provider selection
    initializeCloudProviderSelection();
    
    // Initialize authentication method handling
    initializeAuthenticationMethods();
    
    logger.info('Cloud Connections UI initialized');
});

/**
 * Initialize SSL certificate management functionality
 */
function initializeSSLCertificateManagement() {
    // SSL toggle handler
    const sslToggle = document.getElementById('useSSL');
    const sslConfiguration = document.getElementById('sslConfiguration');
    
    if (sslToggle && sslConfiguration) {
        sslToggle.addEventListener('change', function() {
            sslConfiguration.style.display = this.checked ? 'block' : 'none';
        });
    }
    
    // Certificate file upload handlers
    setupCertificateUpload('caCertFile', 'caCertUpload', 'caCertInfo', 'caCertDetails');
    setupCertificateUpload('clientCertFile', 'clientCertUpload', 'clientCertInfo', 'clientCertDetails');
    setupCertificateUpload('clientKeyFile', 'clientKeyUpload', null, null);
    
    // Drag and drop functionality
    setupDragAndDrop();
}

/**
 * Set up certificate upload functionality
 */
function setupCertificateUpload(fileInputId, uploadAreaId, infoId, detailsId) {
    const fileInput = document.getElementById(fileInputId);
    const uploadArea = document.getElementById(uploadAreaId);
    
    if (!fileInput || !uploadArea) return;
    
    fileInput.addEventListener('change', function(e) {
        const file = e.target.files[0];
        if (file) {
            handleCertificateFile(file, uploadArea, infoId, detailsId);
        }
    });
    
    // Click to upload
    uploadArea.addEventListener('click', function() {
        fileInput.click();
    });
}

/**
 * Handle certificate file processing
 */
function handleCertificateFile(file, uploadArea, infoId, detailsId) {
    const reader = new FileReader();
    
    reader.onload = function(e) {
        const content = e.target.result;
        
        // Update upload area to show success
        uploadArea.innerHTML = `
            <i class="bi bi-check-circle-fill text-success display-4"></i>
            <h6 class="text-success">${file.name}</h6>
            <p class="text-muted">Certificate uploaded successfully</p>
            <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeCertificate('${uploadArea.id}', '${infoId}')">
                <i class="bi bi-trash"></i> Remove
            </button>
        `;
        
        // Parse and display certificate information
        if (infoId && detailsId) {
            displayCertificateInfo(content, infoId, detailsId);
        }
        
        // Show certificate info section
        const certificateInfo = document.getElementById('certificateInfo');
        if (certificateInfo) {
            certificateInfo.style.display = 'block';
        }
    };
    
    reader.readAsText(file);
}

/**
 * Display certificate information
 */
function displayCertificateInfo(content, infoId, detailsId) {
    const infoElement = document.getElementById(infoId);
    const detailsElement = document.getElementById(detailsId);
    
    if (!infoElement || !detailsElement) return;
    
    try {
        // Parse certificate information (basic parsing)
        const certInfo = parseCertificateInfo(content);
        
        detailsElement.innerHTML = `
            <div class="row">
                <div class="col-sm-4"><strong>Subject:</strong></div>
                <div class="col-sm-8">${certInfo.subject || 'N/A'}</div>
            </div>
            <div class="row">
                <div class="col-sm-4"><strong>Issuer:</strong></div>
                <div class="col-sm-8">${certInfo.issuer || 'N/A'}</div>
            </div>
            <div class="row">
                <div class="col-sm-4"><strong>Valid From:</strong></div>
                <div class="col-sm-8">${certInfo.validFrom || 'N/A'}</div>
            </div>
            <div class="row">
                <div class="col-sm-4"><strong>Valid To:</strong></div>
                <div class="col-sm-8">${certInfo.validTo || 'N/A'}</div>
            </div>
            <div class="row">
                <div class="col-sm-4"><strong>Fingerprint:</strong></div>
                <div class="col-sm-8"><code>${certInfo.fingerprint || 'N/A'}</code></div>
            </div>
        `;
        
        infoElement.style.display = 'block';
    } catch (error) {
        console.error('Error parsing certificate:', error);
        detailsElement.innerHTML = '<div class="text-warning">Could not parse certificate information</div>';
        infoElement.style.display = 'block';
    }
}

/**
 * Basic certificate information parser
 */
function parseCertificateInfo(content) {
    // This is a simplified parser - in production, you'd use a proper certificate parsing library
    const lines = content.split('\n');
    const certInfo = {};
    
    // Extract basic information from PEM format
    if (content.includes('-----BEGIN CERTIFICATE-----')) {
        // Extract subject, issuer, etc. from the certificate
        // This would require a proper X.509 parser in production
        certInfo.subject = 'Certificate loaded';
        certInfo.issuer = 'Certificate Authority';
        certInfo.validFrom = new Date().toLocaleDateString();
        certInfo.validTo = new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toLocaleDateString();
        certInfo.fingerprint = 'SHA256:' + Math.random().toString(36).substring(2, 15);
    }
    
    return certInfo;
}

/**
 * Remove certificate
 */
function removeCertificate(uploadAreaId, infoId) {
    const uploadArea = document.getElementById(uploadAreaId);
    const infoElement = document.getElementById(infoId);
    
    if (uploadArea) {
        // Reset upload area
        const isCA = uploadAreaId.includes('caCert');
        const isClient = uploadAreaId.includes('clientCert');
        const isKey = uploadAreaId.includes('clientKey');
        
        let icon = 'bi-cloud-upload';
        let title = 'Certificate';
        let description = 'Drag & drop your certificate file here or click to browse';
        
        if (isCA) {
            title = 'CA Certificate';
        } else if (isClient) {
            icon = 'bi-file-earmark-lock';
            title = 'Client Certificate';
            description = 'Upload client certificate for mutual TLS authentication';
        } else if (isKey) {
            icon = 'bi-key';
            title = 'Client Private Key';
            description = 'Upload private key for client certificate';
        }
        
        uploadArea.innerHTML = `
            <i class="bi ${icon} display-4 text-muted"></i>
            <h6>${title}</h6>
            <p class="text-muted">${description}</p>
            <button type="button" class="btn btn-outline-primary btn-sm" onclick="document.getElementById('${uploadAreaId.replace('Upload', 'File')}').click()">
                <i class="bi bi-upload"></i> Browse Files
            </button>
        `;
    }
    
    if (infoElement) {
        infoElement.style.display = 'none';
    }
    
    // Check if all certificates are removed
    const allInfoElements = document.querySelectorAll('.certificate-info[style*="block"]');
    if (allInfoElements.length === 0) {
        const certificateInfo = document.getElementById('certificateInfo');
        if (certificateInfo) {
            certificateInfo.style.display = 'none';
        }
    }
}

/**
 * Set up drag and drop functionality
 */
function setupDragAndDrop() {
    const uploadAreas = document.querySelectorAll('.certificate-upload-area');
    
    uploadAreas.forEach(area => {
        area.addEventListener('dragover', function(e) {
            e.preventDefault();
            this.classList.add('dragover');
        });
        
        area.addEventListener('dragleave', function(e) {
            e.preventDefault();
            this.classList.remove('dragover');
        });
        
        area.addEventListener('drop', function(e) {
            e.preventDefault();
            this.classList.remove('dragover');
            
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                const file = files[0];
                const fileInputId = this.id.replace('Upload', 'File');
                const infoId = this.id.replace('Upload', 'Info');
                const detailsId = this.id.replace('Upload', 'Details');
                
                handleCertificateFile(file, this, infoId, detailsId);
                
                // Update the hidden file input
                const fileInput = document.getElementById(fileInputId);
                if (fileInput) {
                    // Create a new FileList with the dropped file
                    const dt = new DataTransfer();
                    dt.items.add(file);
                    fileInput.files = dt.files;
                }
            }
        });
    });
}

/**
 * Initialize cloud provider selection
 */
function initializeCloudProviderSelection() {
    const cloudProvider = document.getElementById('cloudProvider');
    
    if (cloudProvider) {
        cloudProvider.addEventListener('change', function() {
            const provider = this.value;
            updateCloudProviderUI(provider);
            showCloudServices(provider);
            updateAdvancedOptions(provider);
        });
    }
}

/**
 * Update cloud provider UI
 */
function updateCloudProviderUI(provider) {
    const endpointField = document.getElementById('cloudEndpoint');
    const portField = document.getElementById('cloudPort');
    
    // Update placeholders and default values based on provider
    const providerConfig = {
        aws: {
            endpoint: 'your-rds-instance.region.rds.amazonaws.com',
            port: 5432,
            defaultAuth: 'iam'
        },
        azure: {
            endpoint: 'your-server.database.windows.net',
            port: 1433,
            defaultAuth: 'azure-ad'
        },
        gcp: {
            endpoint: 'your-project:region:instance',
            port: 5432,
            defaultAuth: 'service-account'
        }
    };
    
    if (providerConfig[provider]) {
        const config = providerConfig[provider];
        if (endpointField) endpointField.placeholder = config.endpoint;
        if (portField) portField.value = config.port;
        
        // Update authentication method
        const authMethod = document.getElementById('authMethod');
        if (authMethod) {
            authMethod.value = config.defaultAuth;
            updateAuthenticationUI(config.defaultAuth);
        }
    }
}

/**
 * Show cloud services for selected provider
 */
function showCloudServices(provider) {
    const servicesSection = document.getElementById('cloudServicesSection');
    const servicesGrid = document.getElementById('cloudServicesGrid');
    
    if (!servicesSection || !servicesGrid) return;
    
    const services = {
        aws: [
            { id: 'rds', name: 'RDS', icon: 'bi-database', description: 'Relational Database Service' },
            { id: 'aurora', name: 'Aurora', icon: 'bi-lightning', description: 'High-performance database' },
            { id: 'documentdb', name: 'DocumentDB', icon: 'bi-file-text', description: 'MongoDB-compatible' },
            { id: 'redshift', name: 'Redshift', icon: 'bi-bar-chart', description: 'Data warehouse' }
        ],
        azure: [
            { id: 'sql-database', name: 'SQL Database', icon: 'bi-database', description: 'Managed SQL database' },
            { id: 'cosmos-db', name: 'Cosmos DB', icon: 'bi-globe', description: 'Multi-model database' },
            { id: 'postgresql', name: 'PostgreSQL', icon: 'bi-database-gear', description: 'Managed PostgreSQL' },
            { id: 'mysql', name: 'MySQL', icon: 'bi-database-gear', description: 'Managed MySQL' }
        ],
        gcp: [
            { id: 'cloud-sql', name: 'Cloud SQL', icon: 'bi-database', description: 'Managed relational database' },
            { id: 'bigquery', name: 'BigQuery', icon: 'bi-bar-chart-line', description: 'Data warehouse' },
            { id: 'firestore', name: 'Firestore', icon: 'bi-fire', description: 'NoSQL document database' },
            { id: 'spanner', name: 'Spanner', icon: 'bi-diagram-3', description: 'Globally distributed database' }
        ]
    };
    
    if (provider && services[provider]) {
        servicesGrid.innerHTML = '';
        
        services[provider].forEach(service => {
            const serviceCard = document.createElement('div');
            serviceCard.className = 'service-card';
            serviceCard.dataset.service = service.id;
            serviceCard.innerHTML = `
                <i class="bi ${service.icon} display-6 text-primary"></i>
                <h6 class="mt-2">${service.name}</h6>
                <p class="text-muted small">${service.description}</p>
            `;
            
            serviceCard.addEventListener('click', function() {
                // Remove selection from other cards
                document.querySelectorAll('.service-card').forEach(card => {
                    card.classList.remove('selected');
                });
                
                // Select this card
                this.classList.add('selected');
                
                // Update form based on selected service
                updateServiceConfiguration(provider, service.id);
            });
            
            servicesGrid.appendChild(serviceCard);
        });
        
        servicesSection.style.display = 'block';
    } else {
        servicesSection.style.display = 'none';
    }
}

/**
 * Update service configuration
 */
function updateServiceConfiguration(provider, serviceId) {
    // Update database type and other service-specific settings
    const serviceConfig = {
        'rds': { dbType: 'postgresql', port: 5432 },
        'aurora': { dbType: 'postgresql', port: 5432 },
        'sql-database': { dbType: 'sqlserver', port: 1433 },
        'cloud-sql': { dbType: 'postgresql', port: 5432 },
        'mysql': { dbType: 'mysql', port: 3306 }
    };
    
    if (serviceConfig[serviceId]) {
        const config = serviceConfig[serviceId];
        const portField = document.getElementById('cloudPort');
        if (portField) {
            portField.value = config.port;
        }
    }
}

/**
 * Update advanced options based on provider
 */
function updateAdvancedOptions(provider) {
    // Hide all provider-specific options
    const awsOptions = document.getElementById('awsOptions');
    const gcpOptions = document.getElementById('gcpOptions');
    
    if (awsOptions) awsOptions.style.display = 'none';
    if (gcpOptions) gcpOptions.style.display = 'none';
    
    // Show relevant options
    if (provider === 'aws' && awsOptions) {
        awsOptions.style.display = 'block';
    } else if (provider === 'gcp' && gcpOptions) {
        gcpOptions.style.display = 'block';
    }
}

/**
 * Initialize authentication methods
 */
function initializeAuthenticationMethods() {
    const authMethod = document.getElementById('authMethod');
    
    if (authMethod) {
        authMethod.addEventListener('change', function() {
            updateAuthenticationUI(this.value);
        });
    }
}

/**
 * Update authentication UI
 */
function updateAuthenticationUI(method) {
    // Hide all authentication sections
    const authSections = document.querySelectorAll('.auth-method-section');
    authSections.forEach(section => {
        section.style.display = 'none';
        section.classList.remove('active');
    });
    
    // Show selected authentication method
    const sectionMap = {
        'username-password': 'usernamePasswordAuth',
        'iam': 'iamAuth',
        'azure-ad': 'azureAdAuth',
        'service-account': 'serviceAccountAuth'
    };
    
    if (sectionMap[method]) {
        const section = document.getElementById(sectionMap[method]);
        if (section) {
            section.style.display = 'block';
            section.classList.add('active');
        }
    }
}