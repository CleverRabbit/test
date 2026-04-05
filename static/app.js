// AI Developer - Общие JavaScript функции

// Базовая утилита для HTTP запросов
async function apiRequest(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const response = await fetch(url, {...defaultOptions, ...options});
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({error: 'Ошибка сервера'}));
        throw new Error(error.error || `HTTP ${response.status}`);
    }
    
    return response.json();
}

// Форматирование даты
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Форматирование размера файла
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Debounce функция
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Проверка валидности JSON
function isValidJSON(str) {
    try {
        JSON.parse(str);
        return true;
    } catch (e) {
        return false;
    }
}

// Копирование в буфер обмена
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        return true;
    } catch (err) {
        // Fallback для старых браузеров
        const textArea = document.createElement('textarea');
        textArea.value = text;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        return true;
    }
}

// Показ уведомления
function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 25px;
        border-radius: 8px;
        background: ${type === 'success' ? '#22c55e' : type === 'error' ? '#ef4444' : '#6366f1'};
        color: white;
        z-index: 9999;
        animation: slideIn 0.3s ease;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Добавление CSS анимаций
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
`;
document.head.appendChild(style);

// Экспорт функций
window.AIUtils = {
    apiRequest,
    formatDate,
    formatFileSize,
    debounce,
    isValidJSON,
    copyToClipboard,
    showNotification
};

console.log('AI Developer utilities loaded');

// ==================== Модуль самотестирования ====================

async function runSelfTest() {
    const selftestModal = document.getElementById('selftestModal');
    const selftestResults = document.getElementById('selftestResults');
    
    if (!selftestModal || !selftestResults) return;
    
    selftestModal.style.display = 'flex';
    selftestResults.innerHTML = '<div class="loading">Выполнение тестов...</div>';
    
    try {
        const results = await AIUtils.apiRequest('/api/system/selftest');
        
        let html = '<div class="selftest-summary">';
        html += `<h3>Общий статус: <span class="status-${results.status}">${results.status === 'ok' ? 'OK' : results.status.toUpperCase()}</span></h3>`;
        html += '</div><div class="selftest-details">';
        
        for (const [component, result] of Object.entries(results.components)) {
            const icon = result.status === 'ok' ? '✓' : result.status === 'warning' ? '⚠' : '✗';
            html += `
                <div class="selftest-item status-${result.status}">
                    <span class="selftest-icon">${icon}</span>
                    <span class="selftest-name">${component}</span>
                    <span class="selftest-message">${result.message}</span>
                </div>
            `;
        }
        
        html += '</div>';
        html += '<button onclick="closeSelfTestModal()" class="btn btn-primary" style="margin-top: 15px;">Закрыть</button>';
        
        selftestResults.innerHTML = html;
    } catch (error) {
        selftestResults.innerHTML = `
            <div class="selftest-item status-error">
                <span class="selftest-icon">✗</span>
                <span class="selftest-name">Ошибка теста</span>
                <span class="selftest-message">${error.message}</span>
            </div>
            <button onclick="closeSelfTestModal()" class="btn btn-primary" style="margin-top: 15px;">Закрыть</button>
        `;
    }
}

function closeSelfTestModal() {
    const selftestModal = document.getElementById('selftestModal');
    if (selftestModal) {
        selftestModal.style.display = 'none';
    }
}

// Закрытие модального окна по клику вне его
window.addEventListener('click', (e) => {
    const selftestModal = document.getElementById('selftestModal');
    if (e.target === selftestModal) {
        selftestModal.style.display = 'none';
    }
});

console.log('AI Developer self-test module loaded');
