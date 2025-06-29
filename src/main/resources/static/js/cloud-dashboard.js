/**
 * Cloud Dashboard Controller
 * Manages the cloud database dashboard interface
 */
document.addEventListener('DOMContentLoaded', function() {
    // Initialize modules
    const logger = LoggerModule.init();
    const toastManager = ToastModule.init();
    const cloudManagement = CloudManagementModule.init({
        logger: logger,
        toastManager: toastManager
    });
    
    // Initialize dashboard
    initializeDashboard();
    
    // Set up event listeners
    setupEventListeners();
    
    // Load initial data
    loadInitialData();
    
    logger.info('Cloud Dashboard initialized');
});

/**
 * Initialize dashboard components
 */
function initializeDashboard() {
    // Initialize charts
    initializeMetricsChart();
    initializeAnalyticsChart();
    
    // Set up auto-refresh
    setInterval(refreshMonitoringData, 30000); // Refresh every 30 seconds
}

/**
 * Set up event listeners
 */
function setupEventListeners() {
    // Discovery
    document.getElementById('discoverInstances').addEventListener('click', discoverInstances);
    
    // Monitoring
    document.getElementById('refreshDashboard').addEventListener('click', refreshDashboard);
    
    // Multi-cloud management
    document.getElementById('generateUnifiedView').addEventListener('click', generateUnifiedView);
    document.getElementById('compareProviders').addEventListener('click', compareProviders);
    document.getElementById('loadAnalytics').addEventListener('click', loadAnalytics);
    document.getElementById('optimizeDeployment').addEventListener('click', optimizeDeployment);
    
    // Pool statistics
    document.getElementById('refreshPoolStats').addEventListener('click', refreshPoolStatistics);
}

/**
 * Load initial dashboard data
 */
function loadInitialData() {
    refreshMonitoringData();
    refreshPoolStatistics();
}

/**
 * Discover cloud database instances
 */
function discoverInstances() {
    const button = document.getElementById('discoverInstances');
    const originalText = button.innerHTML;
    
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Discovering...';
    button.disabled = true;
    
    // Mock credentials - in production, get from user input
    const credentials = {
        aws: { accessKey: 'mock', secretKey: 'mock' },
        azure: { clientId: 'mock', clientSecret: 'mock' },
        gcp: { serviceAccount: 'mock' }
    };
    
    cloudManagement.discoverInstances(credentials)
        .then(instances => {
            displayDiscoveryResults(instances);
        })
        .catch(error => {
            console.error('Discovery failed:', error);
        })
        .finally(() => {
            button.innerHTML = originalText;
            button.disabled = false;
        });
}

/**
 * Display discovery results
 */
function displayDiscoveryResults(instances) {
    const resultsDiv = document.getElementById('discoveryResults');
    
    let html = '<div class="discovery-results">';
    
    for (const [provider, providerInstances] of Object.entries(instances)) {
        html += `
            <div class="provider-section mb-3">
                <h6 class="text-capitalize">
                    <i class="bi bi-cloud me-2"></i>${provider} 
                    <span class="badge bg-primary">${providerInstances.length}</span>
                </h6>
                <div class="row">
        `;
        
        providerInstances.forEach(instance => {
            const statusClass = getStatusClass(instance.status);
            html += `
                <div class="col-md-6 mb-2">
                    <div class="card card-sm">
                        <div class="card-body p-2">
                            <div class="d-flex justify-content-between align-items-center">
                                <div>
                                    <strong>${instance.instanceId}</strong>
                                    <div class="text-muted small">${instance.engine} | ${instance.region}</div>
                                </div>
                                <span class="badge ${statusClass}">${instance.status}</span>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
        
        html += '</div></div>';
    }
    
    html += '</div>';
    resultsDiv.innerHTML = html;
}

/**
 * Get status badge class
 */
function getStatusClass(status) {
    const statusMap = {
        'available': 'bg-success',
        'online': 'bg-success',
        'runnable': 'bg-success',
        'active': 'bg-success',
        'ready': 'bg-success',
        'stopped': 'bg-secondary',
        'error': 'bg-danger',
        'warning': 'bg-warning'
    };
    
    return statusMap[status.toLowerCase()] || 'bg-secondary';
}

/**
 * Refresh dashboard data
 */
function refreshDashboard() {
    refreshMonitoringData();
    refreshPoolStatistics();
}

/**
 * Refresh monitoring data
 */
function refreshMonitoringData() {
    cloudManagement.getMonitoringMetrics()
        .then(metrics => {
            updateMonitoringOverview(metrics);
            updateMetricsChart(metrics);
        })
        .catch(error => {
            console.error('Error loading monitoring data:', error);
        });
    
    cloudManagement.getHealthStatus()
        .then(healthStatus => {
            updateHealthStatus(healthStatus);
        })
        .catch(error => {
            console.error('Error loading health status:', error);
        });
}

/**
 * Update monitoring overview
 */
function updateMonitoringOverview(metrics) {
    let totalActiveConnections = 0;
    let totalHealthScore = 0;
    let providerCount = 0;
    
    for (const [provider, providerMetrics] of Object.entries(metrics)) {
        totalActiveConnections += providerMetrics.activeConnections || 0;
        totalHealthScore += providerMetrics.healthScore || 0;
        providerCount++;
    }
    
    const avgHealthScore = providerCount > 0 ? Math.round(totalHealthScore / providerCount) : 0;
    
    document.getElementById('activeConnections').textContent = totalActiveConnections;
    document.getElementById('healthScore').textContent = avgHealthScore + '%';
}

/**
 * Update health status
 */
function updateHealthStatus(healthStatus) {
    // Update health indicators in the UI
    for (const [provider, status] of Object.entries(healthStatus)) {
        const statusElement = document.getElementById(`${provider}Status`);
        if (statusElement) {
            statusElement.className = `badge ${getHealthStatusClass(status)}`;
            statusElement.textContent = status;
        }
    }
}

/**
 * Get health status badge class
 */
function getHealthStatusClass(status) {
    const statusMap = {
        'HEALTHY': 'bg-success',
        'WARNING': 'bg-warning',
        'DEGRADED': 'bg-warning',
        'CRITICAL': 'bg-danger'
    };
    
    return statusMap[status] || 'bg-secondary';
}

/**
 * Initialize metrics chart
 */
function initializeMetricsChart() {
    const ctx = document.getElementById('metricsChart').getContext('2d');
    
    window.metricsChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Active Connections',
                data: [],
                borderColor: 'rgb(75, 192, 192)',
                backgroundColor: 'rgba(75, 192, 192, 0.2)',
                tension: 0.1
            }, {
                label: 'Health Score',
                data: [],
                borderColor: 'rgb(255, 99, 132)',
                backgroundColor: 'rgba(255, 99, 132, 0.2)',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}

/**
 * Update metrics chart
 */
function updateMetricsChart(metrics) {
    if (!window.metricsChart) return;
    
    const now = new Date().toLocaleTimeString();
    const chart = window.metricsChart;
    
    // Calculate totals
    let totalConnections = 0;
    let avgHealthScore = 0;
    let providerCount = 0;
    
    for (const [provider, providerMetrics] of Object.entries(metrics)) {
        totalConnections += providerMetrics.activeConnections || 0;
        avgHealthScore += providerMetrics.healthScore || 0;
        providerCount++;
    }
    
    avgHealthScore = providerCount > 0 ? avgHealthScore / providerCount : 0;
    
    // Update chart data
    chart.data.labels.push(now);
    chart.data.datasets[0].data.push(totalConnections);
    chart.data.datasets[1].data.push(avgHealthScore);
    
    // Keep only last 10 data points
    if (chart.data.labels.length > 10) {
        chart.data.labels.shift();
        chart.data.datasets[0].data.shift();
        chart.data.datasets[1].data.shift();
    }
    
    chart.update();
}

/**
 * Generate unified cloud view
 */
function generateUnifiedView() {
    const button = document.getElementById('generateUnifiedView');
    const originalText = button.innerHTML;
    
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Generating...';
    button.disabled = true;
    
    // Mock credentials
    const credentials = {
        aws: { accessKey: 'mock', secretKey: 'mock' },
        azure: { clientId: 'mock', clientSecret: 'mock' },
        gcp: { serviceAccount: 'mock' }
    };
    
    cloudManagement.getUnifiedCloudView(credentials)
        .then(unifiedView => {
            displayUnifiedView(unifiedView);
        })
        .catch(error => {
            console.error('Error generating unified view:', error);
        })
        .finally(() => {
            button.innerHTML = originalText;
            button.disabled = false;
        });
}

/**
 * Display unified cloud view
 */
function displayUnifiedView(unifiedView) {
    const contentDiv = document.getElementById('unifiedViewContent');
    
    let html = `
        <div class="unified-view">
            <div class="row mb-3">
                <div class="col-md-3">
                    <div class="metric-card">
                        <h6>Total Instances</h6>
                        <div class="metric-value">${unifiedView.totalInstances}</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="metric-card">
                        <h6>Providers</h6>
                        <div class="metric-value">${Object.keys(unifiedView.instancesByProvider).length}</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="metric-card">
                        <h6>Database Engines</h6>
                        <div class="metric-value">${Object.keys(unifiedView.instancesByEngine).length}</div>
                    </div>
                </div>
                <div class="col-md-3">
                    <div class="metric-card">
                        <h6>Regions</h6>
                        <div class="metric-value">${Object.keys(unifiedView.instancesByRegion).length}</div>
                    </div>
                </div>
            </div>
            
            <div class="row">
                <div class="col-md-6">
                    <h6>Instances by Provider</h6>
                    <div class="provider-breakdown">
    `;
    
    for (const [provider, instances] of Object.entries(unifiedView.instancesByProvider)) {
        html += `
            <div class="d-flex justify-content-between align-items-center mb-2">
                <span class="text-capitalize">${provider}</span>
                <span class="badge bg-primary">${instances.length}</span>
            </div>
        `;
    }
    
    html += `
                    </div>
                </div>
                <div class="col-md-6">
                    <h6>Cost Comparison</h6>
                    <div class="cost-breakdown">
    `;
    
    if (unifiedView.costComparison && unifiedView.costComparison.monthlyCosts) {
        for (const [provider, cost] of Object.entries(unifiedView.costComparison.monthlyCosts)) {
            html += `
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="text-capitalize">${provider}</span>
                    <span class="text-success">$${cost.toFixed(2)}/month</span>
                </div>
            `;
        }
    }
    
    html += `
                    </div>
                </div>
            </div>
        </div>
    `;
    
    contentDiv.innerHTML = html;
}

/**
 * Compare cloud providers
 */
function compareProviders() {
    const select = document.getElementById('providerSelect');
    const selectedProviders = Array.from(select.selectedOptions).map(option => option.value);
    
    if (selectedProviders.length < 2) {
        toastManager.showToast('Please select at least 2 providers to compare', 'warning');
        return;
    }
    
    const button = document.getElementById('compareProviders');
    const originalText = button.innerHTML;
    
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Comparing...';
    button.disabled = true;
    
    cloudManagement.compareCloudProviders(selectedProviders)
        .then(comparison => {
            displayProviderComparison(comparison);
        })
        .catch(error => {
            console.error('Error comparing providers:', error);
        })
        .finally(() => {
            button.innerHTML = originalText;
            button.disabled = false;
        });
}

/**
 * Display provider comparison
 */
function displayProviderComparison(comparison) {
    const contentDiv = document.getElementById('comparisonContent');
    
    let html = '<div class="provider-comparison">';
    
    // Summary
    if (comparison.summary) {
        html += `
            <div class="comparison-summary mb-4">
                <h6>Comparison Summary</h6>
                <div class="row">
                    <div class="col-md-3">
                        <strong>Recommended:</strong> ${comparison.summary.recommendedProvider}
                    </div>
                    <div class="col-md-3">
                        <strong>Cost Leader:</strong> ${comparison.summary.costLeader}
                    </div>
                    <div class="col-md-3">
                        <strong>Performance:</strong> ${comparison.summary.performanceLeader}
                    </div>
                    <div class="col-md-3">
                        <strong>Reliability:</strong> ${comparison.summary.reliabilityLeader}
                    </div>
                </div>
            </div>
        `;
    }
    
    // Detailed comparison
    html += '<div class="detailed-comparison"><div class="row">';
    
    for (const [provider, details] of Object.entries(comparison)) {
        if (provider === 'summary') continue;
        
        html += `
            <div class="col-md-4">
                <div class="card">
                    <div class="card-header">
                        <h6 class="text-capitalize">${provider}</h6>
                    </div>
                    <div class="card-body">
                        <div class="mb-2">
                            <strong>Health:</strong> 
                            <span class="badge ${getHealthStatusClass(details.healthStatus)}">${details.healthStatus}</span>
                        </div>
                        <div class="mb-2">
                            <strong>Services:</strong> ${details.services ? details.services.length : 0}
                        </div>
                        <div class="mb-2">
                            <strong>Regions:</strong> ${details.regions ? details.regions.length : 0}
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    html += '</div></div></div>';
    
    contentDiv.innerHTML = html;
}

/**
 * Load analytics
 */
function loadAnalytics() {
    const button = document.getElementById('loadAnalytics');
    const originalText = button.innerHTML;
    
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Loading...';
    button.disabled = true;
    
    cloudManagement.getCrossCloudAnalytics()
        .then(analytics => {
            displayAnalytics(analytics);
        })
        .catch(error => {
            console.error('Error loading analytics:', error);
        })
        .finally(() => {
            button.innerHTML = originalText;
            button.disabled = false;
        });
}

/**
 * Display analytics
 */
function displayAnalytics(analytics) {
    // Update metric cards
    if (analytics.cost) {
        document.getElementById('totalCost').textContent = `$${analytics.cost.totalMonthlyCost.toFixed(2)}`;
    }
    
    if (analytics.performance) {
        document.getElementById('avgResponseTime').textContent = `${analytics.performance.averageResponseTime}ms`;
        document.getElementById('uptime').textContent = `${analytics.performance.uptime}%`;
        document.getElementById('errorRate').textContent = `${(analytics.performance.errorRate * 100).toFixed(2)}%`;
    }
    
    // Update analytics chart
    updateAnalyticsChart(analytics);
}

/**
 * Initialize analytics chart
 */
function initializeAnalyticsChart() {
    const ctx = document.getElementById('analyticsChart').getContext('2d');
    
    window.analyticsChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['AWS', 'Azure', 'GCP'],
            datasets: [{
                label: 'Cost Distribution',
                data: [0, 0, 0],
                backgroundColor: [
                    'rgba(255, 99, 132, 0.8)',
                    'rgba(54, 162, 235, 0.8)',
                    'rgba(255, 205, 86, 0.8)'
                ]
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false
        }
    });
}

/**
 * Update analytics chart
 */
function updateAnalyticsChart(analytics) {
    if (!window.analyticsChart || !analytics.cost || !analytics.cost.costPerProvider) return;
    
    const chart = window.analyticsChart;
    const costData = analytics.cost.costPerProvider;
    
    chart.data.datasets[0].data = [
        costData.aws || 0,
        costData.azure || 0,
        costData.gcp || 0
    ];
    
    chart.update();
}

/**
 * Optimize deployment
 */
function optimizeDeployment() {
    const requirements = {
        workloadType: document.getElementById('workloadType').value,
        budgetLimit: parseFloat(document.getElementById('budgetLimit').value),
        regions: Array.from(document.getElementById('preferredRegions').selectedOptions).map(option => option.value)
    };
    
    const button = document.getElementById('optimizeDeployment');
    const originalText = button.innerHTML;
    
    button.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Optimizing...';
    button.disabled = true;
    
    cloudManagement.optimizeDeployment(requirements)
        .then(optimization => {
            displayOptimizationResults(optimization);
        })
        .catch(error => {
            console.error('Error optimizing deployment:', error);
        })
        .finally(() => {
            button.innerHTML = originalText;
            button.disabled = false;
        });
}

/**
 * Display optimization results
 */
function displayOptimizationResults(optimization) {
    const resultsDiv = document.getElementById('optimizationResults');
    
    let html = '<div class="optimization-results">';
    
    if (optimization.deploymentOptions) {
        html += '<h6>Deployment Options</h6>';
        
        optimization.deploymentOptions.forEach((option, index) => {
            html += `
                <div class="card mb-2">
                    <div class="card-body">
                        <h6 class="card-title">${option.name}</h6>
                        <div class="row">
                            <div class="col-md-6">
                                <small><strong>Provider:</strong> ${option.provider}</small><br>
                                <small><strong>Cost:</strong> $${option.estimatedCost}/month</small>
                            </div>
                            <div class="col-md-6">
                                <small><strong>Performance:</strong> ${option.performance}</small><br>
                                <small><strong>Reliability:</strong> ${option.reliability}</small>
                            </div>
                        </div>
                    </div>
                </div>
            `;
        });
    }
    
    if (optimization.recommendations) {
        html += '<h6 class="mt-3">Recommendations</h6><ul>';
        optimization.recommendations.forEach(rec => {
            html += `<li>${rec}</li>`;
        });
        html += '</ul>';
    }
    
    html += '</div>';
    
    resultsDiv.innerHTML = html;
}

/**
 * Refresh pool statistics
 */
function refreshPoolStatistics() {
    cloudManagement.getPoolStatistics()
        .then(statistics => {
            displayPoolStatistics(statistics);
        })
        .catch(error => {
            console.error('Error loading pool statistics:', error);
        });
}

/**
 * Display pool statistics
 */
function displayPoolStatistics(statistics) {
    const statsDiv = document.getElementById('poolStatistics');
    
    if (Object.keys(statistics).length === 0) {
        statsDiv.innerHTML = `
            <div class="text-center text-muted p-4">
                <i class="bi bi-diagram-3 display-4"></i>
                <p>No active connection pools found</p>
            </div>
        `;
        return;
    }
    
    let html = '<div class="pool-statistics"><div class="row">';
    
    for (const [connectionId, stats] of Object.entries(statistics)) {
        html += `
            <div class="col-md-6 mb-3">
                <div class="card">
                    <div class="card-header">
                        <h6>${connectionId}</h6>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-6">
                                <small><strong>Active:</strong> ${stats.activeConnections || 0}</small><br>
                                <small><strong>Idle:</strong> ${stats.idleConnections || 0}</small>
                            </div>
                            <div class="col-6">
                                <small><strong>Total:</strong> ${stats.totalConnections || 0}</small><br>
                                <small><strong>Waiting:</strong> ${stats.threadsAwaitingConnection || 0}</small>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }
    
    html += '</div></div>';
    
    statsDiv.innerHTML = html;
}

// Add CSS for metric cards
const style = document.createElement('style');
style.textContent = `
    .metric-card {
        background: #f8f9fa;
        border: 1px solid #dee2e6;
        border-radius: 8px;
        padding: 15px;
        text-align: center;
        margin-bottom: 15px;
    }
    
    .metric-card h6 {
        margin-bottom: 10px;
        color: #6c757d;
        font-size: 0.9rem;
    }
    
    .metric-value {
        font-size: 1.5rem;
        font-weight: bold;
        color: #495057;
    }
    
    .card-sm .card-body {
        padding: 0.5rem;
    }
    
    .provider-section {
        border-left: 3px solid #007bff;
        padding-left: 15px;
    }
`;
document.head.appendChild(style);