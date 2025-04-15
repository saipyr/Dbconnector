const SavedConnectionsModule = (function() {
    let savedConnections = {};
    let connectionModule;
    let showStatusCallback;
    let saveConnectionModal;
    
    function init(connection, statusCallback) {
        connectionModule = connection;
        showStatusCallback = statusCallback;
        savedConnections = loadSavedConnections();
        
        // Initialize save connection modal
        saveConnectionModal = new bootstrap.Modal(document.getElementById('saveConnectionModal'));
        
        return {
            openSaveConnectionModal: openSaveConnectionModal,
            saveConnection: saveConnection,
            displaySavedConnections: displaySavedConnections,
            filterConnections: filterConnections
        };
    }
    
    function loadSavedConnections() {
        const saved = localStorage.getItem('savedConnections');
        return saved ? JSON.parse(saved) : {};
    }
    
    function openSaveConnectionModal() {
        const connectionName = document.getElementById('connectionName').value || 'My Connection';
        const saveFolder = document.getElementById('saveFolder').value || 'default';
        
        document.getElementById('modalConnectionName').value = connectionName;
        document.getElementById('modalSaveFolder').value = saveFolder;
        
        saveConnectionModal.show();
    }
    
    function saveConnection() {
        const connectionData = {
            name: document.getElementById('modalConnectionName').value,
            folder: document.getElementById('modalSaveFolder').value || 'default',
            dbType: document.getElementById('dbType').value,
            host: document.getElementById('host').value,
            port: document.getElementById('port').value,
            database: document.getElementById('database').value,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value, // Note: In a real app, consider encrypting this
            timestamp: new Date().toISOString()
        };
        
        // Validate connection data
        if (!connectionData.name || !connectionData.host || !connectionData.port || !connectionData.database || !connectionData.username) {
            showStatusCallback('Please fill in all required fields', 'warning');
            return;
        }
        
        // Add to saved connections
        if (!savedConnections[connectionData.folder]) {
            savedConnections[connectionData.folder] = [];
        }
        
        // Check if connection with same name exists in this folder
        const existingIndex = savedConnections[connectionData.folder].findIndex(conn => conn.name === connectionData.name);
        if (existingIndex >= 0) {
            savedConnections[connectionData.folder][existingIndex] = connectionData;
        } else {
            savedConnections[connectionData.folder].push(connectionData);
        }
        
        // Save to localStorage
        localStorage.setItem('savedConnections', JSON.stringify(savedConnections));
        
        // Update UI
        displaySavedConnections();
        
        // Close modal
        saveConnectionModal.hide();
        
        // Show success message
        showStatusCallback('Connection saved successfully', 'success');
        
        // Switch to Saved tab
        const savedTab = document.getElementById('saved-tab');
        bootstrap.Tab.getInstance(savedTab) || new bootstrap.Tab(savedTab);
        savedTab.click();
    }
    
    function displaySavedConnections() {
        const connectionsList = document.getElementById('savedConnectionsList');
        connectionsList.innerHTML = '';
        
        // Check if there are any saved connections
        const folders = Object.keys(savedConnections);
        if (folders.length === 0) {
            connectionsList.innerHTML = `
                <div class="text-center text-muted p-3">
                    <i>No saved connections found</i>
                </div>
            `;
            return;
        }
        
        // Create accordion items for each folder
        folders.forEach((folder, index) => {
            const connections = savedConnections[folder];
            if (connections.length === 0) return;
            
            const folderId = `folder-${folder.replace(/\s+/g, '-').toLowerCase()}`;
            const folderItem = document.createElement('div');
            folderItem.className = 'accordion-item';
            folderItem.innerHTML = `
                <h2 class="accordion-header" id="heading-${folderId}">
                    <button class="accordion-button ${index === 0 ? '' : 'collapsed'}" type="button" 
                            data-bs-toggle="collapse" data-bs-target="#collapse-${folderId}" 
                            aria-expanded="${index === 0 ? 'true' : 'false'}" aria-controls="collapse-${folderId}">
                        ${folder} (${connections.length})
                    </button>
                </h2>
                <div id="collapse-${folderId}" class="accordion-collapse collapse ${index === 0 ? 'show' : ''}" 
                     aria-labelledby="heading-${folderId}" data-bs-parent="#savedConnectionsList">
                    <div class="accordion-body p-0">
                        <div class="list-group list-group-flush" id="connections-${folderId}">
                        </div>
                    </div>
                </div>
            `;
            connectionsList.appendChild(folderItem);
            
            // Add connections to the folder
            const connectionListEl = document.getElementById(`connections-${folderId}`);
            connections.forEach(conn => {
                const connItem = document.createElement('a');
                connItem.className = 'list-group-item list-group-item-action d-flex justify-content-between align-items-center';
                connItem.href = '#';
                connItem.innerHTML = `
                    <div>
                        <strong>${conn.name}</strong>
                        <div class="text-muted small">${conn.dbType} | ${conn.host}:${conn.port} | ${conn.database}</div>
                    </div>
                    <div class="btn-group btn-group-sm" role="group">
                        <button type="button" class="btn btn-outline-primary load-connection" data-folder="${folder}" data-name="${conn.name}">
                            <i class="bi bi-box-arrow-in-right"></i>
                        </button>
                        <button type="button" class="btn btn-outline-danger delete-connection" data-folder="${folder}" data-name="${conn.name}">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                `;
                connectionListEl.appendChild(connItem);
            });
        });
        
        // Add event listeners for load and delete buttons
        document.querySelectorAll('.load-connection').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                loadSavedConnection(btn.dataset.folder, btn.dataset.name);
            });
        });
        
        document.querySelectorAll('.delete-connection').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                deleteSavedConnection(btn.dataset.folder, btn.dataset.name);
            });
        });
    }
    
    function loadSavedConnection(folder, name) {
        if (!savedConnections[folder]) return;
        
        const connection = savedConnections[folder].find(conn => conn.name === name);
        if (!connection) return;
        
        // Fill the form with connection data
        document.getElementById('connectionName').value = connection.name;
        document.getElementById('saveFolder').value = connection.folder;
        document.getElementById('dbType').value = connection.dbType;
        document.getElementById('host').value = connection.host;
        document.getElementById('port').value = connection.port;
        document.getElementById('database').value = connection.database;
        document.getElementById('username').value = connection.username;
        document.getElementById('password').value = connection.password;
        
        // Switch to Connection tab
        const connectionTab = document.getElementById('connection-tab');
        bootstrap.Tab.getInstance(connectionTab) || new bootstrap.Tab(connectionTab);
        connectionTab.click();
        
        showStatusCallback(`Loaded connection: ${connection.name}`, 'success');
    }
    
    function deleteSavedConnection(folder, name) {
        if (!savedConnections[folder]) return;
        
        // Find and remove the connection
        const index = savedConnections[folder].findIndex(conn => conn.name === name);
        if (index >= 0) {
            savedConnections[folder].splice(index, 1);
            
            // Remove folder if empty
            if (savedConnections[folder].length === 0) {
                delete savedConnections[folder];
            }
            
            // Save to localStorage
            localStorage.setItem('savedConnections', JSON.stringify(savedConnections));
            
            // Update UI
            displaySavedConnections();
            
            showStatusCallback(`Deleted connection: ${name}`, 'success');
        }
    }
    
    function filterConnections() {
        const searchTerm = document.getElementById('connectionSearch').value.toLowerCase();
        const folderItems = document.querySelectorAll('#savedConnectionsList .accordion-item');
        
        folderItems.forEach(folderItem => {
            const folderHeader = folderItem.querySelector('.accordion-header button').textContent;
            const connectionItems = folderItem.querySelectorAll('.list-group-item');
            let hasVisibleConnections = false;
            
            connectionItems.forEach(item => {
                const connectionName = item.querySelector('strong').textContent.toLowerCase();
                const connectionDetails = item.querySelector('.text-muted').textContent.toLowerCase();
                
                if (connectionName.includes(searchTerm) || connectionDetails.includes(searchTerm)) {
                    item.style.display = '';
                    hasVisibleConnections = true;
                } else {
                    item.style.display = 'none';
                }
            });
            
            // Show/hide folder based on whether it has visible connections
            folderItem.style.display = hasVisibleConnections ? '' : 'none';
        });
    }
    
    return {
        init: init
    };
})();