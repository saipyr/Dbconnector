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
    
    // Log application start
    logger.info('DB Connector application initialized');
});