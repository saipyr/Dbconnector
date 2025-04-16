/**
 * Logger Module
 * Handles all logging operations with different log types and retention policies
 */
const LoggerModule = (function() {
    // Log types
    const LOG_TYPES = {
        ACCESS: 'access',
        MONITORING: 'monitoring',
        CONNECTION: 'connection',
        STDOUT: 'stdout',
        STDIN: 'stdin',
        COMMON: 'common'
    };
    
    // Maximum number of log files to retain per type
    const MAX_LOG_FILES = 10;
    
    // Initialize the logger
    function init() {
        console.log('Logger module initialized');
        
        // Create initial log entry for each log type
        Object.values(LOG_TYPES).forEach(type => {
            log(type, 'Logger initialized', { timestamp: new Date().toISOString() });
        });
        
        // Clean up old logs to maintain retention policy
        cleanupOldLogs();
        
        return {
            log: log,
            access: (message, data) => log(LOG_TYPES.ACCESS, message, data),
            monitoring: (message, data) => log(LOG_TYPES.MONITORING, message, data),
            connection: (message, data) => log(LOG_TYPES.CONNECTION, message, data),
            stdout: (message, data) => log(LOG_TYPES.STDOUT, message, data),
            stdin: (message, data) => log(LOG_TYPES.STDIN, message, data),
            common: (message, data) => log(LOG_TYPES.COMMON, message, data),
            getLogTypes: () => ({ ...LOG_TYPES }),
            viewLogs: viewLogs,
            downloadLogs: downloadLogs,
            clearLogs: clearLogs
        };
    }
    
    // Log a message to the specified log type
    function log(type, message, data = {}) {
        if (!Object.values(LOG_TYPES).includes(type)) {
            console.error(`Invalid log type: ${type}`);
            return;
        }
        
        const logEntry = {
            timestamp: data.timestamp || new Date().toISOString(),
            message: message,
            data: { ...data }
        };
        
        // Store log in localStorage with type prefix
        const logs = getLogs(type);
        logs.push(logEntry);
        
        // Ensure we don't exceed storage limits (keep last 1000 entries per type)
        if (logs.length > 1000) {
            logs.shift(); // Remove oldest entry
        }
        
        // Save back to localStorage
        localStorage.setItem(`logs_${type}`, JSON.stringify(logs));
        
        // Also log to console for debugging
        console.log(`[${type.toUpperCase()}] ${message}`, data);
        
        // Send log to server for persistent storage
        sendLogToServer(type, logEntry);
    }
    
    // Get logs of a specific type from localStorage
    function getLogs(type) {
        const logsJson = localStorage.getItem(`logs_${type}`);
        return logsJson ? JSON.parse(logsJson) : [];
    }
    
    // Send log to server for persistent storage
    function sendLogToServer(type, logEntry) {
        fetch('/api/logs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                type: type,
                entry: logEntry
            })
        }).catch(error => {
            console.error('Failed to send log to server:', error);
            // Store failed logs to retry later
            const failedLogs = JSON.parse(localStorage.getItem('failed_logs') || '[]');
            failedLogs.push({ type, entry: logEntry });
            localStorage.setItem('failed_logs', JSON.stringify(failedLogs));
        });
    }
    
    // Clean up old logs to maintain retention policy
    function cleanupOldLogs() {
        fetch('/api/logs/cleanup', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                maxFiles: MAX_LOG_FILES
            })
        }).catch(error => {
            console.error('Failed to clean up old logs:', error);
        });
    }
    
    // View logs of a specific type
    function viewLogs(type) {
        if (!type) {
            return Object.values(LOG_TYPES).reduce((allLogs, logType) => {
                allLogs[logType] = getLogs(logType);
                return allLogs;
            }, {});
        }
        
        return getLogs(type);
    }
    
    // Download logs as a JSON file
    function downloadLogs(type) {
        const logs = type ? { [type]: getLogs(type) } : viewLogs();
        const blob = new Blob([JSON.stringify(logs, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        
        const a = document.createElement('a');
        a.href = url;
        a.download = type ? `logs_${type}.json` : 'all_logs.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
    
    // Clear logs of a specific type or all logs
    function clearLogs(type) {
        if (type) {
            localStorage.removeItem(`logs_${type}`);
        } else {
            Object.values(LOG_TYPES).forEach(logType => {
                localStorage.removeItem(`logs_${logType}`);
            });
        }
        
        // Also clear on server
        fetch('/api/logs/clear', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                type: type || 'all'
            })
        }).catch(error => {
            console.error('Failed to clear logs on server:', error);
        });
    }
    
    return {
        init: init
    };
})();
/**
 * Enhanced Logger Module
 * Provides comprehensive logging functionality with different log levels and categories
 */
/**
 * Logger Module
 * Provides logging functionality for the application
 */
const LoggerModule = (function() {
    const LOG_LEVELS = {
        DEBUG: 0,
        INFO: 1,
        WARN: 2,
        ERROR: 3
    };
    
    let currentLogLevel = LOG_LEVELS.INFO;
    let logHistory = [];
    
    /**
     * Initialize the logger module
     * @param {Object} options - Logger options
     * @returns {Object} Logger interface
     */
    function init(options = {}) {
        if (options.logLevel && LOG_LEVELS[options.logLevel] !== undefined) {
            currentLogLevel = LOG_LEVELS[options.logLevel];
        }
        
        console.log('Logger initialized with log level: ' + getLogLevelName(currentLogLevel));
        
        return {
            debug,
            info,
            warn,
            error,
            getLogHistory,
            setLogLevel
        };
    }
    
    /**
     * Log a debug message
     * @param {string} message - Message to log
     */
    function debug(message) {
        if (currentLogLevel <= LOG_LEVELS.DEBUG) {
            console.debug(message);
            addToHistory('DEBUG', message);
        }
    }
    
    /**
     * Log an info message
     * @param {string} message - Message to log
     */
    function info(message) {
        if (currentLogLevel <= LOG_LEVELS.INFO) {
            console.info(message);
            addToHistory('INFO', message);
        }
    }
    
    /**
     * Log a warning message
     * @param {string} message - Message to log
     */
    function warn(message) {
        if (currentLogLevel <= LOG_LEVELS.WARN) {
            console.warn(message);
            addToHistory('WARN', message);
        }
    }
    
    /**
     * Log an error message
     * @param {string} message - Message to log
     */
    function error(message) {
        if (currentLogLevel <= LOG_LEVELS.ERROR) {
            console.error(message);
            addToHistory('ERROR', message);
        }
    }
    
    /**
     * Add a log entry to history
     * @param {string} level - Log level
     * @param {string} message - Log message
     */
    function addToHistory(level, message) {
        const timestamp = new Date().toISOString();
        logHistory.push({ timestamp, level, message });
        
        // Keep history size manageable
        if (logHistory.length > 1000) {
            logHistory.shift();
        }
    }
    
    /**
     * Get log history
     * @returns {Array} Log history
     */
    function getLogHistory() {
        return [...logHistory];
    }
    
    /**
     * Set log level
     * @param {string} level - Log level name
     */
    function setLogLevel(level) {
        if (LOG_LEVELS[level] !== undefined) {
            currentLogLevel = LOG_LEVELS[level];
            console.log('Log level set to: ' + level);
        } else {
            console.error('Invalid log level: ' + level);
        }
    }
    
    /**
     * Get log level name
     * @param {number} level - Log level value
     * @returns {string} Log level name
     */
    function getLogLevelName(level) {
        for (const [name, value] of Object.entries(LOG_LEVELS)) {
            if (value === level) {
                return name;
            }
        }
        return 'UNKNOWN';
    }
    
    return {
        init
    };
})();
    
    // Log storage
    const logs = {
        access: [],
        audit: [],
        error: [],
        common: [],
        connection: [],
        query: [],
        table: [],
        stdin: []
    };
    
    // Maximum number of logs to keep per category
    const MAX_LOGS = 1000;
    
    // DOM elements
    let loggerPanel;
    let loggerContent;
    let activeTab = 'all';
    
    /**
     * Initialize the logger module
     * @returns {Object} Logger interface
     */
    function init() {
        console.log('Initializing Logger Module');
        
        // Create logger UI if it doesn't exist
        if (!document.getElementById('loggerPanel')) {
            createLoggerUI();
        }
        
        // Return logger interface
        return {
            access: logAccess,
            audit: logAudit,
            error: logError,
            common: logCommon,
            connection: logConnection,
            query: logQuery,
            table: logTable,
            stdin: logStdin,
            setLogLevel: setLogLevel,
            getLogLevel: getLogLevel,
            getLogs: getLogs,
            clearLogs: clearLogs,
            exportLogs: exportLogs
        };
    }
    
    /**
     * Create the logger UI
     */
    function createLoggerUI() {
        // Create logger panel
        loggerPanel = document.createElement('div');
        loggerPanel.id = 'loggerPanel';
        loggerPanel.className = 'logger-panel';
        
        // Create logger header
        const loggerHeader = document.createElement('div');
        loggerHeader.className = 'logger-header';
        loggerHeader.innerHTML = '<h6>Application Logs</h6><span class="toggle-icon">▲</span>';
        loggerHeader.addEventListener('click', toggleLoggerPanel);
        
        // Create logger tabs
        const loggerTabs = document.createElement('div');
        loggerTabs.className = 'logger-tabs';
        
        const tabs = ['all', 'access', 'audit', 'error', 'connection', 'query'];
        tabs.forEach(tab => {
            const tabElement = document.createElement('div');
            tabElement.className = `logger-tab ${tab === 'all' ? 'active' : ''}`;
            tabElement.textContent = tab.charAt(0).toUpperCase() + tab.slice(1);
            tabElement.dataset.tab = tab;
            tabElement.addEventListener('click', function() {
                document.querySelectorAll('.logger-tab').forEach(t => t.classList.remove('active'));
                this.classList.add('active');
                activeTab = tab;
                renderLogs();
            });
            loggerTabs.appendChild(tabElement);
        });
        
        // Create logger content
        loggerContent = document.createElement('div');
        loggerContent.className = 'logger-content';
        
        // Create logger controls
        const loggerControls = document.createElement('div');
        loggerControls.className = 'logger-controls';
        
        const clearButton = document.createElement('button');
        clearButton.className = 'btn btn-sm btn-outline-danger';
        clearButton.textContent = 'Clear Logs';
        clearButton.addEventListener('click', function() {
            clearLogs();
            renderLogs();
        });
        
        const exportButton = document.createElement('button');
        exportButton.className = 'btn btn-sm btn-outline-primary';
        exportButton.textContent = 'Export Logs';
        exportButton.addEventListener('click', function() {
            exportLogs();
        });
        
        loggerControls.appendChild(clearButton);
        loggerControls.appendChild(exportButton);
        
        // Assemble logger panel
        loggerPanel.appendChild(loggerHeader);
        loggerPanel.appendChild(loggerTabs);
        loggerPanel.appendChild(loggerContent);
        loggerPanel.appendChild(loggerControls);
        
        // Add to document
        document.body.appendChild(loggerPanel);
    }
    
    /**
     * Toggle the logger panel visibility
     */
    function toggleLoggerPanel() {
        loggerPanel.classList.toggle('expanded');
        const toggleIcon = loggerPanel.querySelector('.toggle-icon');
        toggleIcon.textContent = loggerPanel.classList.contains('expanded') ? '▼' : '▲';
    }
    
    /**
     * Render logs in the logger content area
     */
    function renderLogs() {
        if (!loggerContent) return;
        
        loggerContent.innerHTML = '';
        
        let logsToRender = [];
        
        if (activeTab === 'all') {
            // Combine all logs and sort by timestamp
            Object.keys(logs).forEach(category => {
                logsToRender = logsToRender.concat(logs[category].map(log => ({
                    ...log,
                    category
                })));
            });
        } else {
            // Only show logs from the selected category
            logsToRender = logs[activeTab].map(log => ({
                ...log,
                category: activeTab
            }));
        }
        
        // Sort by timestamp (newest first)
        logsToRender.sort((a, b) => b.timestamp - a.timestamp);
        
        // Render logs
        logsToRender.forEach(log => {
            const logEntry = document.createElement('div');
            logEntry.className = `logger-entry ${log.category}`;
            
            const timestamp = new Date(log.timestamp).toISOString().replace('T', ' ').substr(0, 19);
            const level = log.level.toUpperCase();
            const message = log.message;
            const details = log.details ? JSON.stringify(log.details) : '';
            
            logEntry.innerHTML = `<span class="log-timestamp">[${timestamp}]</span> <span class="log-level">[${level}]</span> <span class="log-category">[${log.category}]</span> ${message} ${details ? '<span class="log-details">' + details + '</span>' : ''}`;
            
            loggerContent.appendChild(logEntry);
        });
    }
    
    /**
     * Add a log entry
     * @param {string} category - Log category
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     * @param {string} level - Log level
     */
    function addLog(category, message, details, level) {
        // Check if log level is high enough
        if (LOG_LEVELS[level.toUpperCase()] < currentLogLevel) {
            return;
        }
        
        // Create log entry
        const logEntry = {
            timestamp: Date.now(),
            message,
            details,
            level
        };
        
        // Add to logs
        logs[category].unshift(logEntry);
        
        // Trim logs if necessary
        if (logs[category].length > MAX_LOGS) {
            logs[category] = logs[category].slice(0, MAX_LOGS);
        }
        
        // Log to console
        console.log(`[${category.toUpperCase()}] ${message}`, details || '');
        
        // Update UI if visible
        if (loggerPanel && loggerPanel.classList.contains('expanded')) {
            renderLogs();
        }
        
        // Send to server for persistent logging
        sendLogToServer(category, logEntry);
    }
    
    /**
     * Send log to server for persistent storage
     * @param {string} category - Log category
     * @param {Object} logEntry - Log entry
     */
    function sendLogToServer(category, logEntry) {
        fetch('/api/logs', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                category,
                ...logEntry
            })
        }).catch(error => {
            console.error('Failed to send log to server:', error);
        });
    }
    
    /**
     * Log access events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logAccess(message, details) {
        addLog('access', message, details, 'info');
    }
    
    /**
     * Log audit events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logAudit(message, details) {
        addLog('audit', message, details, 'info');
    }
    
    /**
     * Log error events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logError(message, details) {
        addLog('error', message, details, 'error');
    }
    
    /**
     * Log common events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logCommon(message, details) {
        addLog('common', message, details, 'info');
    }
    
    /**
     * Log connection events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logConnection(message, details) {
        addLog('connection', message, details, 'info');
    }
    
    /**
     * Log query events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logQuery(message, details) {
        addLog('query', message, details, 'info');
    }
    
    /**
     * Log table events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logTable(message, details) {
        addLog('table', message, details, 'info');
    }
    
    /**
     * Log user input events
     * @param {string} message - Log message
     * @param {Object} details - Additional log details
     */
    function logStdin(message, details) {
        addLog('stdin', message, details, 'info');
    }
    
    /**
     * Set the log level
     * @param {string} level - Log level
     */
    function setLogLevel(level) {
        if (LOG_LEVELS[level.toUpperCase()] !== undefined) {
            currentLogLevel = LOG_LEVELS[level.toUpperCase()];
            console.log(`Log level set to ${level.toUpperCase()}`);
        } else {
            console.error(`Invalid log level: ${level}`);
        }
    }
    
    /**
     * Get the current log level
     * @returns {string} Current log level
     */
    function getLogLevel() {
        return Object.keys(LOG_LEVELS).find(key => LOG_LEVELS[key] === currentLogLevel);
    }
    
    /**
     * Get logs by category
     * @param {string} category - Log category
     * @returns {Array} Logs for the specified category
     */
    function getLogs(category) {
        return category ? [...logs[category]] : logs;
    }
    
    /**
     * Clear logs
     * @param {string} category - Log category to clear (optional)
     */
    function clearLogs(category) {
        if (category) {
            logs[category] = [];
        } else {
            Object.keys(logs).forEach(key => {
                logs[key] = [];
            });
        }
    }
    
    /**
     * Export logs to a file
     */
    function exportLogs() {
        // Prepare logs for export
        const exportData = {
            timestamp: new Date().toISOString(),
            logs: {}
        };
        
        if (activeTab === 'all') {
            exportData.logs = { ...logs };
        } else {
            exportData.logs[activeTab] = [...logs[activeTab]];
        }
        
        // Create blob and download
        const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        
        const a = document.createElement('a');
        a.href = url;
        a.download = `dbconnector_logs_${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
    
    return {
        init
    };
})();