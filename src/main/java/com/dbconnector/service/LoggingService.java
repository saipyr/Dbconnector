package com.dbconnector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LoggingService {
    
    private static final Logger logger = LoggerFactory.getLogger("com.dbconnector");
    private static final Logger accessLogger = LoggerFactory.getLogger("com.dbconnector.access");
    private static final Logger auditLogger = LoggerFactory.getLogger("com.dbconnector.audit");
    
    public void logInfo(String message) {
        logger.info(message);
    }
    
    public void logError(String message, Throwable throwable) {
        logger.error(message, throwable);
    }
    
    public void logAccess(String message) {
        accessLogger.info(message);
    }
    
    public void logAudit(String message) {
        auditLogger.info(message);
    }
    
    public void logDebug(String message) {
        logger.debug(message);
    }
}