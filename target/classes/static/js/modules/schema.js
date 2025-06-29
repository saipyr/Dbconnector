/**
 * Schema Explorer Module
 * Handles database schema exploration and visualization
 */
const SchemaModule = (function() {
    let logger;
    let toastManager;
    let currentConnectionId;
    
    /**
     * Initialize the schema module
     * @param {Object} dependencies - Module dependencies
     * @returns {Object} Schema module interface
     */
    function init(dependencies) {
        logger = dependencies.logger || console;
        toastManager = dependencies.toastManager || {
            showToast: (message, type) => { console.log(message); }
        };
        
        logger.info('Schema module initialized');
        
        return {
            loadSchemaTree,
            showTableStructure,
            showTableIndexes,
            showForeignKeys
        };
    }
    
    /**
     * Load the database schema tree
     * @param {string} connectionId - Connection ID
     */
    function loadSchemaTree(connectionId) {
        currentConnectionId = connectionId;
        logger.info('Loading schema tree for connection: ' + connectionId);
        
        // Load databases first
        fetch(`/api/schema/databases?connectionId=${connectionId}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    renderSchemaTree(data.databases);
                } else {
                    throw new Error(data.message || 'Failed to load databases');
                }
            })
            .catch(error => {
                logger.error('Error loading schema tree: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            });
    }
    
    /**
     * Render the schema tree in the UI
     * @param {Array} databases - List of databases
     */
    function renderSchemaTree(databases) {
        const schemaTree = document.getElementById('schemaTree');
        if (!schemaTree) return;
        
        schemaTree.innerHTML = '';
        
        databases.forEach(database => {
            const dbNode = createTreeNode(database.name, 'database', () => {
                loadSchemas(database.name, dbNode);
            });
            schemaTree.appendChild(dbNode);
        });
    }
    
    /**
     * Load schemas for a database
     * @param {string} databaseName - Database name
     * @param {Element} parentNode - Parent tree node
     */
    function loadSchemas(databaseName, parentNode) {
        fetch(`/api/schema/schemas?connectionId=${currentConnectionId}&database=${databaseName}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    renderSchemas(data.schemas, parentNode);
                } else {
                    throw new Error(data.message || 'Failed to load schemas');
                }
            })
            .catch(error => {
                logger.error('Error loading schemas: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            });
    }
    
    /**
     * Render schemas under a database node
     * @param {Array} schemas - List of schemas
     * @param {Element} parentNode - Parent tree node
     */
    function renderSchemas(schemas, parentNode) {
        const container = parentNode.querySelector('.tree-children');
        container.innerHTML = '';
        
        schemas.forEach(schema => {
            const schemaNode = createTreeNode(schema.name, 'schema', () => {
                loadTables(schema.name, schemaNode);
            });
            container.appendChild(schemaNode);
        });
    }
    
    /**
     * Load tables for a schema
     * @param {string} schemaName - Schema name
     * @param {Element} parentNode - Parent tree node
     */
    function loadTables(schemaName, parentNode) {
        fetch(`/api/schema/tables?connectionId=${currentConnectionId}&schema=${schemaName}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    renderTables(data.tables, parentNode);
                } else {
                    throw new Error(data.message || 'Failed to load tables');
                }
            })
            .catch(error => {
                logger.error('Error loading tables: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            });
    }
    
    /**
     * Render tables under a schema node
     * @param {Array} tables - List of tables
     * @param {Element} parentNode - Parent tree node
     */
    function renderTables(tables, parentNode) {
        const container = parentNode.querySelector('.tree-children');
        container.innerHTML = '';
        
        // Group tables by type
        const tablesByType = tables.reduce((acc, table) => {
            const type = table.type || 'TABLE';
            if (!acc[type]) acc[type] = [];
            acc[type].push(table);
            return acc;
        }, {});
        
        Object.keys(tablesByType).forEach(type => {
            const typeNode = createTreeNode(`${type}S (${tablesByType[type].length})`, 'folder');
            const typeContainer = typeNode.querySelector('.tree-children');
            
            tablesByType[type].forEach(table => {
                const tableNode = createTreeNode(table.name, 'table', () => {
                    showTableStructure(table.name, table.schema);
                });
                
                // Add context menu for table operations
                tableNode.addEventListener('contextmenu', (e) => {
                    e.preventDefault();
                    showTableContextMenu(e, table);
                });
                
                typeContainer.appendChild(tableNode);
            });
            
            container.appendChild(typeNode);
        });
    }
    
    /**
     * Create a tree node element
     * @param {string} text - Node text
     * @param {string} type - Node type (database, schema, table, etc.)
     * @param {Function} onClick - Click handler
     * @returns {Element} Tree node element
     */
    function createTreeNode(text, type, onClick) {
        const node = document.createElement('div');
        node.className = 'tree-node';
        
        const header = document.createElement('div');
        header.className = 'tree-node-header';
        
        const icon = document.createElement('i');
        icon.className = getIconClass(type);
        
        const label = document.createElement('span');
        label.textContent = text;
        label.className = 'tree-node-label';
        
        const toggle = document.createElement('i');
        toggle.className = 'bi bi-chevron-right tree-toggle';
        
        header.appendChild(toggle);
        header.appendChild(icon);
        header.appendChild(label);
        
        const children = document.createElement('div');
        children.className = 'tree-children';
        children.style.display = 'none';
        
        node.appendChild(header);
        node.appendChild(children);
        
        // Add click handlers
        if (onClick) {
            header.addEventListener('click', (e) => {
                e.stopPropagation();
                toggleNode(node);
                onClick();
            });
        } else {
            toggle.addEventListener('click', (e) => {
                e.stopPropagation();
                toggleNode(node);
            });
        }
        
        return node;
    }
    
    /**
     * Toggle tree node expansion
     * @param {Element} node - Tree node element
     */
    function toggleNode(node) {
        const children = node.querySelector('.tree-children');
        const toggle = node.querySelector('.tree-toggle');
        
        if (children.style.display === 'none') {
            children.style.display = 'block';
            toggle.className = 'bi bi-chevron-down tree-toggle';
        } else {
            children.style.display = 'none';
            toggle.className = 'bi bi-chevron-right tree-toggle';
        }
    }
    
    /**
     * Get icon class for node type
     * @param {string} type - Node type
     * @returns {string} Icon class
     */
    function getIconClass(type) {
        const icons = {
            database: 'bi bi-database',
            schema: 'bi bi-folder',
            table: 'bi bi-table',
            view: 'bi bi-eye',
            procedure: 'bi bi-gear',
            folder: 'bi bi-folder'
        };
        return icons[type] || 'bi bi-file';
    }
    
    /**
     * Show table structure in detail panel
     * @param {string} tableName - Table name
     * @param {string} schema - Schema name
     */
    function showTableStructure(tableName, schema) {
        logger.info(`Loading structure for table: ${tableName}`);
        
        fetch(`/api/schema/table-structure?connectionId=${currentConnectionId}&tableName=${tableName}&schema=${schema}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    renderTableStructure(tableName, data);
                } else {
                    throw new Error(data.message || 'Failed to load table structure');
                }
            })
            .catch(error => {
                logger.error('Error loading table structure: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            });
    }
    
    /**
     * Render table structure in the detail panel
     * @param {string} tableName - Table name
     * @param {Object} structure - Table structure data
     */
    function renderTableStructure(tableName, structure) {
        const detailPanel = document.getElementById('tableDetailPanel');
        if (!detailPanel) return;
        
        let html = `
            <div class="table-structure">
                <h5>${tableName} - Structure</h5>
                <div class="table-responsive">
                    <table class="table table-sm table-striped">
                        <thead>
                            <tr>
                                <th>Column</th>
                                <th>Type</th>
                                <th>Size</th>
                                <th>Nullable</th>
                                <th>Default</th>
                                <th>Auto Inc</th>
                                <th>Primary Key</th>
                            </tr>
                        </thead>
                        <tbody>
        `;
        
        const pkColumns = structure.primaryKeys.map(pk => pk.columnName);
        
        structure.columns.forEach(column => {
            const isPrimaryKey = pkColumns.includes(column.name);
            html += `
                <tr>
                    <td><strong>${column.name}</strong></td>
                    <td>${column.type}</td>
                    <td>${column.size || '-'}</td>
                    <td>${column.nullable ? 'Yes' : 'No'}</td>
                    <td>${column.defaultValue || '-'}</td>
                    <td>${column.autoIncrement ? 'Yes' : 'No'}</td>
                    <td>${isPrimaryKey ? '<i class="bi bi-key text-warning"></i>' : ''}</td>
                </tr>
            `;
        });
        
        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        
        detailPanel.innerHTML = html;
    }
    
    /**
     * Show table context menu
     * @param {Event} event - Context menu event
     * @param {Object} table - Table object
     */
    function showTableContextMenu(event, table) {
        // Create context menu
        const menu = document.createElement('div');
        menu.className = 'context-menu';
        menu.style.position = 'fixed';
        menu.style.left = event.clientX + 'px';
        menu.style.top = event.clientY + 'px';
        menu.style.zIndex = '9999';
        
        const menuItems = [
            { text: 'View Structure', action: () => showTableStructure(table.name, table.schema) },
            { text: 'View Indexes', action: () => showTableIndexes(table.name, table.schema) },
            { text: 'View Foreign Keys', action: () => showForeignKeys(table.name, table.schema) },
            { text: 'Select Data', action: () => selectTableData(table.name, table.schema) },
            { text: 'Generate SQL', action: () => generateSQL(table.name, table.schema) }
        ];
        
        menuItems.forEach(item => {
            const menuItem = document.createElement('div');
            menuItem.className = 'context-menu-item';
            menuItem.textContent = item.text;
            menuItem.addEventListener('click', () => {
                item.action();
                document.body.removeChild(menu);
            });
            menu.appendChild(menuItem);
        });
        
        document.body.appendChild(menu);
        
        // Remove menu when clicking elsewhere
        setTimeout(() => {
            document.addEventListener('click', function removeMenu() {
                if (document.body.contains(menu)) {
                    document.body.removeChild(menu);
                }
                document.removeEventListener('click', removeMenu);
            });
        }, 100);
    }
    
    /**
     * Show table indexes
     * @param {string} tableName - Table name
     * @param {string} schema - Schema name
     */
    function showTableIndexes(tableName, schema) {
        fetch(`/api/schema/indexes?connectionId=${currentConnectionId}&tableName=${tableName}&schema=${schema}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    renderTableIndexes(tableName, data.indexes);
                } else {
                    throw new Error(data.message || 'Failed to load table indexes');
                }
            })
            .catch(error => {
                logger.error('Error loading table indexes: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            });
    }
    
    /**
     * Show foreign keys
     * @param {string} tableName - Table name
     * @param {string} schema - Schema name
     */
    function showForeignKeys(tableName, schema) {
        fetch(`/api/schema/foreign-keys?connectionId=${currentConnectionId}&tableName=${tableName}&schema=${schema}`)
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    renderForeignKeys(tableName, data.foreignKeys);
                } else {
                    throw new Error(data.message || 'Failed to load foreign keys');
                }
            })
            .catch(error => {
                logger.error('Error loading foreign keys: ' + error.message);
                toastManager.showToast(`Error: ${error.message}`, 'danger');
            });
    }
    
    /**
     * Render table indexes
     * @param {string} tableName - Table name
     * @param {Array} indexes - List of indexes
     */
    function renderTableIndexes(tableName, indexes) {
        const detailPanel = document.getElementById('tableDetailPanel');
        if (!detailPanel) return;
        
        let html = `
            <div class="table-indexes">
                <h5>${tableName} - Indexes</h5>
                <div class="table-responsive">
                    <table class="table table-sm table-striped">
                        <thead>
                            <tr>
                                <th>Index Name</th>
                                <th>Column</th>
                                <th>Unique</th>
                                <th>Type</th>
                                <th>Position</th>
                            </tr>
                        </thead>
                        <tbody>
        `;
        
        indexes.forEach(index => {
            html += `
                <tr>
                    <td>${index.name || '-'}</td>
                    <td>${index.columnName}</td>
                    <td>${index.unique ? 'Yes' : 'No'}</td>
                    <td>${index.type}</td>
                    <td>${index.position}</td>
                </tr>
            `;
        });
        
        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        
        detailPanel.innerHTML = html;
    }
    
    /**
     * Render foreign keys
     * @param {string} tableName - Table name
     * @param {Array} foreignKeys - List of foreign keys
     */
    function renderForeignKeys(tableName, foreignKeys) {
        const detailPanel = document.getElementById('tableDetailPanel');
        if (!detailPanel) return;
        
        let html = `
            <div class="table-foreign-keys">
                <h5>${tableName} - Foreign Keys</h5>
                <div class="table-responsive">
                    <table class="table table-sm table-striped">
                        <thead>
                            <tr>
                                <th>FK Name</th>
                                <th>Column</th>
                                <th>Referenced Table</th>
                                <th>Referenced Column</th>
                                <th>Update Rule</th>
                                <th>Delete Rule</th>
                            </tr>
                        </thead>
                        <tbody>
        `;
        
        foreignKeys.forEach(fk => {
            html += `
                <tr>
                    <td>${fk.name || '-'}</td>
                    <td>${fk.columnName}</td>
                    <td>${fk.referencedTable}</td>
                    <td>${fk.referencedColumn}</td>
                    <td>${getRuleText(fk.updateRule)}</td>
                    <td>${getRuleText(fk.deleteRule)}</td>
                </tr>
            `;
        });
        
        html += `
                        </tbody>
                    </table>
                </div>
            </div>
        `;
        
        detailPanel.innerHTML = html;
    }
    
    /**
     * Get rule text for foreign key rules
     * @param {number} rule - Rule number
     * @returns {string} Rule text
     */
    function getRuleText(rule) {
        const rules = {
            0: 'CASCADE',
            1: 'RESTRICT',
            2: 'SET NULL',
            3: 'NO ACTION',
            4: 'SET DEFAULT'
        };
        return rules[rule] || 'UNKNOWN';
    }
    
    return {
        init
    };
})();