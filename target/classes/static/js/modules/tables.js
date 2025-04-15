const TablesModule = (function() {
    let currentTable = '';
    let connectionModule;
    let editorModule;
    let showStatusCallback;
    
    function init(connection, editor, statusCallback) {
        connectionModule = connection;
        editorModule = editor;
        showStatusCallback = statusCallback;
        
        return {
            loadTables: loadTables,
            selectTable: selectTable,
            filterTables: filterTables,
            clearTables: clearTables,
            displayResults: displayResults,
            getCurrentTable: function() { return currentTable; }
        };
    }
    
    function loadTables() {
        if (!connectionModule.isConnected()) {
            showStatusCallback('Not connected to any database', 'warning');
            return;
        }
        
        fetch('/tables')
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const tableList = document.getElementById('tableList');
                tableList.innerHTML = '';
                
                if (data.tables.length === 0) {
                    tableList.innerHTML = `
                        <div class="text-center text-muted p-3">
                            <i>No tables found in this database</i>
                        </div>
                    `;
                    return;
                }
                
                data.tables.forEach(table => {
                    const item = document.createElement('a');
                    item.className = 'list-group-item list-group-item-action';
                    item.textContent = table;
                    item.href = '#';
                    item.addEventListener('click', (e) => {
                        e.preventDefault();
                        selectTable(table);
                    });
                    tableList.appendChild(item);
                });
                
                if (data.tables.length > 0) {
                    selectTable(data.tables[0]);
                }
            } else {
                showStatusCallback(data.message, 'danger');
            }
        })
        .catch(error => {
            showStatusCallback('Error loading tables: ' + error, 'danger');
        });
    }
    
    function selectTable(tableName) {
        currentTable = tableName;
        QueryModule.init().setCurrentPage(1);
        
        // Highlight selected table
        document.querySelectorAll('#tableList a').forEach(item => {
            item.classList.remove('active');
            if (item.textContent === tableName) {
                item.classList.add('active');
            }
        });
        
        // Set query
        const pageSize = QueryModule.init().getPageSize();
        editorModule.setValue(`SELECT * FROM ${tableName} LIMIT ${pageSize} OFFSET 0`);
        
        // Execute query
        QueryModule.init().executeQuery();
    }
    
    function filterTables() {
        const searchTerm = document.getElementById('tableSearch').value.toLowerCase();
        const tableItems = document.querySelectorAll('#tableList a');
        
        tableItems.forEach(item => {
            const tableName = item.textContent.toLowerCase();
            if (tableName.includes(searchTerm)) {
                item.style.display = '';
            } else {
                item.style.display = 'none';
            }
        });
    }
    
    function clearTables() {
        document.getElementById('tableList').innerHTML = `
            <div class="text-center text-muted p-3">
                <i>Connect to a database to view tables</i>
            </div>
        `;
        currentTable = '';
        editorModule.setValue('');
    }
    
    function displayResults(result) {
        const table = document.getElementById('resultsTable');
        const thead = table.querySelector('thead');
        const tbody = table.querySelector('tbody');
        
        // Clear previous results
        thead.innerHTML = '';
        tbody.innerHTML = '';
        
        // Set pagination info
        QueryModule.init().setTotalPages(result.totalPages);
        QueryModule.init().updatePaginationInfo(result.totalRows);
        
        // Add column headers
        const headerRow = document.createElement('tr');
        result.columns.forEach(column => {
            const th = document.createElement('th');
            th.textContent = column;
            headerRow.appendChild(th);
        });
        thead.appendChild(headerRow);
        
        // Add data rows
        result.data.forEach(row => {
            const tr = document.createElement('tr');
            result.columns.forEach(column => {
                const td = document.createElement('td');
                td.textContent = row[column] !== null ? row[column] : 'NULL';
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
    }
    
    return {
        init: init
    };
})();