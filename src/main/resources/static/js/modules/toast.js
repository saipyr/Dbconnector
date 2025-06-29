/**
 * Toast Manager Module
 * Handles toast notifications throughout the application
 */
const ToastModule = (function() {
    let toastContainer;
    
    /**
     * Initialize the toast manager
     * @returns {Object} Toast manager interface
     */
    function init() {
        // Create toast container if it doesn't exist
        if (!document.getElementById('toastContainer')) {
            createToastContainer();
        }
        
        toastContainer = document.getElementById('toastContainer');
        
        return {
            showToast,
            showSuccess: (message) => showToast(message, 'success'),
            showError: (message) => showToast(message, 'danger'),
            showWarning: (message) => showToast(message, 'warning'),
            showInfo: (message) => showToast(message, 'info')
        };
    }
    
    /**
     * Create the toast container
     */
    function createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }
    
    /**
     * Show a toast notification
     * @param {string} message - Toast message
     * @param {string} type - Toast type (success, danger, warning, info)
     * @param {number} duration - Duration in milliseconds (default: 5000)
     */
    function showToast(message, type = 'info', duration = 5000) {
        const toastId = 'toast-' + Date.now();
        
        const toastElement = document.createElement('div');
        toastElement.id = toastId;
        toastElement.className = `toast align-items-center text-white bg-${type} border-0`;
        toastElement.setAttribute('role', 'alert');
        toastElement.setAttribute('aria-live', 'assertive');
        toastElement.setAttribute('aria-atomic', 'true');
        
        toastElement.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">
                    ${getIcon(type)} ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" 
                        data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        `;
        
        toastContainer.appendChild(toastElement);
        
        // Initialize Bootstrap toast
        const toast = new bootstrap.Toast(toastElement, {
            autohide: true,
            delay: duration
        });
        
        // Show the toast
        toast.show();
        
        // Remove from DOM after hiding
        toastElement.addEventListener('hidden.bs.toast', () => {
            if (toastContainer.contains(toastElement)) {
                toastContainer.removeChild(toastElement);
            }
        });
    }
    
    /**
     * Get icon for toast type
     * @param {string} type - Toast type
     * @returns {string} Icon HTML
     */
    function getIcon(type) {
        const icons = {
            success: '<i class="bi bi-check-circle-fill me-2"></i>',
            danger: '<i class="bi bi-exclamation-triangle-fill me-2"></i>',
            warning: '<i class="bi bi-exclamation-triangle-fill me-2"></i>',
            info: '<i class="bi bi-info-circle-fill me-2"></i>'
        };
        return icons[type] || icons.info;
    }
    
    return {
        init
    };
})();