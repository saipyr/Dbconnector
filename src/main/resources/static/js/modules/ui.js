const UIModule = (function() {
    let toast;
    
    function init() {
        return {
            initializeToast: initializeToast,
            showStatus: showStatus,
            updateConnectionUI: updateConnectionUI,
            clearResults: clearResults
        };
    }
    
    function initializeToast() {
        const toastEl = document.getElementById('statusToast');
        toast = new bootstrap.Toast(toastEl);
    }
    
    function showStatus(message, type) {
        const statusMessage = document.getElementById('statusMessage');
        statusMessage.textContent = message;
        
        const toastEl = document.getElementById('statusToast');
        toastEl.className = toastEl.className.replace(/bg-\w+/, '');
        toastEl.classList.add(`bg-${type}`);
        
        toast = bootstrap.Toast.getInstance(toastEl) || new bootstrap.Toast(toastEl);
        toast.show();
    }
    
    function updateConnectionUI(connected) {
        const startBtn = document.getElementById('startConnection');
        const stopBtn = document.getElementById('stopConnection');
        const formInputs = document.querySelectorAll('#connectionForm input, #connectionForm select');
        
        if (connected) {
            startBtn.disabled = true;
            stopBtn.disabled = false;
            formInputs.forEach(input => input.disabled = true);
            document.getElementById('executeQuery').disabled = false;
        } else {
            startBtn.disabled = false;
            stopBtn.disabled = true;
            formInputs.forEach(input => input.disabled = false);
            document.getElementById('executeQuery').disabled = true;
        }
    }
    
    function clearResults() {
        const table = document.getElementById('resultsTable');
        const thead = table.querySelector('thead');
        const tbody = table.querySelector('tbody');
        
        thead.innerHTML = '';
        tbody.innerHTML = '';
        
        document.getElementById('pageInfo').textContent = 'No results';
    }
    
    return {
        init: init
    };
})();