package com.dbconnector.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class CloudMonitoringService {
    
    @Autowired
    private LoggingService loggingService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    // Connection metrics
    private final Map<String, AtomicInteger> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> totalConnections = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failedConnections = new ConcurrentHashMap<>();
    
    // Performance metrics
    private final Map<String, Timer> connectionTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer> queryTimers = new ConcurrentHashMap<>();
    
    // Health metrics
    private final Map<String, AtomicInteger> healthScores = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHealthCheck = new ConcurrentHashMap<>();
    
    /**
     * Initialize monitoring for a cloud provider
     */
    public void initializeProviderMonitoring(String cloudProvider) {
        loggingService.logInfo("Initializing monitoring for cloud provider: " + cloudProvider);
        
        // Initialize connection counters
        activeConnections.put(cloudProvider, new AtomicInteger(0));
        totalConnections.put(cloudProvider, new AtomicLong(0));
        failedConnections.put(cloudProvider, new AtomicLong(0));
        healthScores.put(cloudProvider, new AtomicInteger(100));
        
        // Register Micrometer gauges
        Gauge.builder("cloud.connections.active", activeConnections.get(cloudProvider), AtomicInteger::get)
            .tag("provider", cloudProvider)
            .register(meterRegistry);
            
        Gauge.builder("cloud.connections.total", totalConnections.get(cloudProvider), AtomicLong::get)
            .tag("provider", cloudProvider)
            .register(meterRegistry);
            
        Gauge.builder("cloud.connections.failed", failedConnections.get(cloudProvider), AtomicLong::get)
            .tag("provider", cloudProvider)
            .register(meterRegistry);
            
        Gauge.builder("cloud.health.score", healthScores.get(cloudProvider), AtomicInteger::get)
            .tag("provider", cloudProvider)
            .register(meterRegistry);
        
        // Initialize timers
        connectionTimers.put(cloudProvider, 
            Timer.builder("cloud.connection.duration")
                .tag("provider", cloudProvider)
                .register(meterRegistry));
                
        queryTimers.put(cloudProvider,
            Timer.builder("cloud.query.duration")
                .tag("provider", cloudProvider)
                .register(meterRegistry));
    }
    
    /**
     * Record connection attempt
     */
    public void recordConnectionAttempt(String cloudProvider, boolean successful, long durationMs) {
        totalConnections.get(cloudProvider).incrementAndGet();
        
        if (successful) {
            activeConnections.get(cloudProvider).incrementAndGet();
            connectionTimers.get(cloudProvider).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // Update health score positively
            updateHealthScore(cloudProvider, 5);
        } else {
            failedConnections.get(cloudProvider).incrementAndGet();
            
            // Update health score negatively
            updateHealthScore(cloudProvider, -10);
        }
        
        loggingService.logInfo("Connection attempt recorded for " + cloudProvider + 
            " - Success: " + successful + ", Duration: " + durationMs + "ms");
    }
    
    /**
     * Record connection closure
     */
    public void recordConnectionClosure(String cloudProvider) {
        activeConnections.get(cloudProvider).decrementAndGet();
        loggingService.logInfo("Connection closure recorded for " + cloudProvider);
    }
    
    /**
     * Record query execution
     */
    public void recordQueryExecution(String cloudProvider, long durationMs, boolean successful) {
        queryTimers.get(cloudProvider).record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        if (successful) {
            updateHealthScore(cloudProvider, 1);
        } else {
            updateHealthScore(cloudProvider, -5);
        }
        
        loggingService.logInfo("Query execution recorded for " + cloudProvider + 
            " - Success: " + successful + ", Duration: " + durationMs + "ms");
    }
    
    /**
     * Update health score for a provider
     */
    private void updateHealthScore(String cloudProvider, int delta) {
        AtomicInteger currentScore = healthScores.get(cloudProvider);
        int newScore = Math.max(0, Math.min(100, currentScore.get() + delta));
        currentScore.set(newScore);
        
        lastHealthCheck.put(cloudProvider, System.currentTimeMillis());
    }
    
    /**
     * Get comprehensive monitoring metrics
     */
    public Map<String, Object> getMonitoringMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        for (String provider : activeConnections.keySet()) {
            Map<String, Object> providerMetrics = new HashMap<>();
            
            providerMetrics.put("activeConnections", activeConnections.get(provider).get());
            providerMetrics.put("totalConnections", totalConnections.get(provider).get());
            providerMetrics.put("failedConnections", failedConnections.get(provider).get());
            providerMetrics.put("healthScore", healthScores.get(provider).get());
            providerMetrics.put("lastHealthCheck", lastHealthCheck.get(provider));
            
            // Calculate success rate
            long total = totalConnections.get(provider).get();
            long failed = failedConnections.get(provider).get();
            double successRate = total > 0 ? ((double)(total - failed) / total) * 100 : 0;
            providerMetrics.put("successRate", successRate);
            
            // Get timer statistics
            Timer connectionTimer = connectionTimers.get(provider);
            if (connectionTimer != null) {
                providerMetrics.put("avgConnectionTime", connectionTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                providerMetrics.put("maxConnectionTime", connectionTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
            }
            
            Timer queryTimer = queryTimers.get(provider);
            if (queryTimer != null) {
                providerMetrics.put("avgQueryTime", queryTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                providerMetrics.put("maxQueryTime", queryTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
            }
            
            metrics.put(provider, providerMetrics);
        }
        
        return metrics;
    }
    
    /**
     * Record service discovery metrics
     */
    public void recordDiscoveryMetrics(Map<String, List<CloudServiceDiscoveryService.CloudDatabaseInstance>> discoveredInstances) {
        for (Map.Entry<String, List<CloudServiceDiscoveryService.CloudDatabaseInstance>> entry : discoveredInstances.entrySet()) {
            String provider = entry.getKey();
            int instanceCount = entry.getValue().size();
            
            Gauge.builder("cloud.discovery.instances", () -> instanceCount)
                .tag("provider", provider)
                .register(meterRegistry);
                
            loggingService.logInfo("Discovery metrics recorded for " + provider + ": " + instanceCount + " instances");
        }
    }
    
    /**
     * Get health status for all providers
     */
    public Map<String, String> getHealthStatus() {
        Map<String, String> healthStatus = new HashMap<>();
        
        for (Map.Entry<String, AtomicInteger> entry : healthScores.entrySet()) {
            String provider = entry.getKey();
            int score = entry.getValue().get();
            
            String status;
            if (score >= 80) {
                status = "HEALTHY";
            } else if (score >= 60) {
                status = "WARNING";
            } else if (score >= 40) {
                status = "DEGRADED";
            } else {
                status = "CRITICAL";
            }
            
            healthStatus.put(provider, status);
        }
        
        return healthStatus;
    }
    
    /**
     * Generate monitoring report
     */
    public Map<String, Object> generateMonitoringReport() {
        Map<String, Object> report = new HashMap<>();
        
        report.put("timestamp", new Date());
        report.put("metrics", getMonitoringMetrics());
        report.put("healthStatus", getHealthStatus());
        
        // Calculate overall system health
        double avgHealthScore = healthScores.values().stream()
            .mapToInt(AtomicInteger::get)
            .average()
            .orElse(0.0);
        report.put("overallHealthScore", avgHealthScore);
        
        // Calculate total active connections
        int totalActiveConnections = activeConnections.values().stream()
            .mapToInt(AtomicInteger::get)
            .sum();
        report.put("totalActiveConnections", totalActiveConnections);
        
        loggingService.logInfo("Monitoring report generated - Overall health: " + avgHealthScore + 
            ", Active connections: " + totalActiveConnections);
        
        return report;
    }
    
    /**
     * Reset metrics for a provider
     */
    public void resetProviderMetrics(String cloudProvider) {
        activeConnections.get(cloudProvider).set(0);
        totalConnections.get(cloudProvider).set(0);
        failedConnections.get(cloudProvider).set(0);
        healthScores.get(cloudProvider).set(100);
        
        loggingService.logInfo("Metrics reset for cloud provider: " + cloudProvider);
    }
}