// Newsletter Subscription JavaScript

document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('subscriptionForm');
    const submitButton = form.querySelector('button[type="submit"]');
    const emailInput = document.getElementById('email');
    const nameInput = document.getElementById('fullName');
    
    // Form validation
    function validateForm() {
        let isValid = true;
        const errors = [];
        
        // Validate name
        const name = nameInput.value.trim();
        if (name.length < 2) {
            errors.push('Nome deve ter pelo menos 2 caracteres');
            isValid = false;
        }
        
        // Validate email
        const email = emailInput.value.trim();
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            errors.push('Email inválido');
            isValid = false;
        }
        
        // Show errors if any
        if (!isValid) {
            showValidationErrors(errors);
        }
        
        return isValid;
    }
    
    function showValidationErrors(errors) {
        // Remove existing error alerts
        const existingErrors = document.querySelectorAll('.validation-error');
        existingErrors.forEach(error => error.remove());
        
        // Create new error alert
        const errorAlert = document.createElement('div');
        errorAlert.className = 'alert alert-danger validation-error';
        errorAlert.innerHTML = `
            <i class="fas fa-exclamation-circle me-2"></i>
            <ul class="mb-0">
                ${errors.map(error => `<li>${error}</li>`).join('')}
            </ul>
        `;
        
        // Insert before form
        form.parentNode.insertBefore(errorAlert, form);
        
        // Scroll to error
        errorAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    
    // Real-time email validation
    emailInput.addEventListener('blur', function() {
        const email = this.value.trim();
        if (email && !isValidEmail(email)) {
            this.classList.add('is-invalid');
            showFieldError(this, 'Email inválido');
        } else {
            this.classList.remove('is-invalid');
            hideFieldError(this);
        }
    });
    
    // Real-time name validation
    nameInput.addEventListener('blur', function() {
        const name = this.value.trim();
        if (name && name.length < 2) {
            this.classList.add('is-invalid');
            showFieldError(this, 'Nome deve ter pelo menos 2 caracteres');
        } else {
            this.classList.remove('is-invalid');
            hideFieldError(this);
        }
    });
    
    function isValidEmail(email) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }
    
    function showFieldError(field, message) {
        hideFieldError(field);
        const errorDiv = document.createElement('div');
        errorDiv.className = 'invalid-feedback';
        errorDiv.textContent = message;
        field.parentNode.appendChild(errorDiv);
    }
    
    function hideFieldError(field) {
        const existingError = field.parentNode.querySelector('.invalid-feedback');
        if (existingError) {
            existingError.remove();
        }
    }
    
    // Form submission
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        if (!validateForm()) {
            return;
        }
        
        // Show loading state
        submitButton.classList.add('loading');
        submitButton.disabled = true;
        
        // Simulate form submission (replace with actual AJAX call if needed)
        const formData = new FormData(form);
        
        // Get selected categories
        const selectedCategories = [];
        const categoryCheckboxes = document.querySelectorAll('input[name="categoryIds"]:checked');
        categoryCheckboxes.forEach(checkbox => {
            selectedCategories.push(checkbox.value);
        });
        
        // Submit form
        fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.ok) {
                return response.text();
            }
            throw new Error('Erro na requisição');
        })
        .then(data => {
            // Check if response contains success or error
            if (data.includes('alert-success')) {
                showSuccessMessage('Inscrição realizada com sucesso! Verifique seu email.');
                form.reset();
                
                // Update stats if available
                updateStats();
            } else if (data.includes('alert-danger')) {
                // Extract error message from response
                const parser = new DOMParser();
                const doc = parser.parseFromString(data, 'text/html');
                const errorAlert = doc.querySelector('.alert-danger');
                const errorMessage = errorAlert ? errorAlert.textContent.trim() : 'Erro ao processar inscrição';
                showErrorMessage(errorMessage);
            } else {
                // Redirect to success page or reload
                window.location.reload();
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showErrorMessage('Erro ao processar inscrição. Tente novamente.');
        })
        .finally(() => {
            // Remove loading state
            submitButton.classList.remove('loading');
            submitButton.disabled = false;
        });
    });
    
    function showSuccessMessage(message) {
        showMessage(message, 'success');
    }
    
    function showErrorMessage(message) {
        showMessage(message, 'danger');
    }
    
    function showMessage(message, type) {
        // Remove existing alerts
        const existingAlerts = document.querySelectorAll('.alert');
        existingAlerts.forEach(alert => {
            if (alert.classList.contains('validation-error') || 
                alert.classList.contains('dynamic-alert')) {
                alert.remove();
            }
        });
        
        // Create new alert
        const alert = document.createElement('div');
        alert.className = `alert alert-${type} alert-dismissible fade show dynamic-alert`;
        alert.innerHTML = `
            <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        // Insert before form
        form.parentNode.insertBefore(alert, form);
        
        // Scroll to alert
        alert.scrollIntoView({ behavior: 'smooth', block: 'center' });
        
        // Auto-dismiss success messages after 5 seconds
        if (type === 'success') {
            setTimeout(() => {
                if (alert.parentNode) {
                    alert.remove();
                }
            }, 5000);
        }
    }
    
    function updateStats() {
        // Update subscriber count if stats section exists
        const subscriberStat = document.querySelector('.stat-item .stat-number');
        if (subscriberStat) {
            const currentCount = parseInt(subscriberStat.textContent.replace(/[^\d]/g, ''));
            const newCount = currentCount + 1;
            animateNumber(subscriberStat, currentCount, newCount);
        }
    }
    
    function animateNumber(element, start, end) {
        const duration = 1000;
        const startTime = performance.now();
        
        function update(currentTime) {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);
            
            const current = Math.floor(start + (end - start) * progress);
            element.textContent = current.toLocaleString();
            
            if (progress < 1) {
                requestAnimationFrame(update);
            }
        }
        
        requestAnimationFrame(update);
    }
    
    // Category selection enhancement
    const categoryCheckboxes = document.querySelectorAll('input[name="categoryIds"]');
    categoryCheckboxes.forEach(checkbox => {
        checkbox.addEventListener('change', function() {
            const card = this.closest('.form-check');
            if (this.checked) {
                card.style.background = 'linear-gradient(135deg, #e3f2fd, #bbdefb)';
                card.style.borderColor = 'var(--bs-primary)';
            } else {
                card.style.background = '#f8f9fa';
                card.style.borderColor = 'transparent';
            }
        });
    });
    
    // Email domain suggestions
    const commonDomains = ['gmail.com', 'hotmail.com', 'yahoo.com', 'outlook.com', 'uol.com.br'];
    
    emailInput.addEventListener('input', function() {
        const email = this.value;
        const atIndex = email.indexOf('@');
        
        if (atIndex > 0 && atIndex < email.length - 1) {
            const domain = email.substring(atIndex + 1);
            const suggestions = commonDomains.filter(d => d.startsWith(domain.toLowerCase()));
            
            if (suggestions.length > 0 && domain.length > 0 && domain.length < suggestions[0].length) {
                showEmailSuggestion(email, suggestions[0]);
            } else {
                hideEmailSuggestion();
            }
        } else {
            hideEmailSuggestion();
        }
    });
    
    function showEmailSuggestion(currentEmail, suggestedDomain) {
        hideEmailSuggestion();
        
        const atIndex = currentEmail.indexOf('@');
        const username = currentEmail.substring(0, atIndex + 1);
        const suggestedEmail = username + suggestedDomain;
        
        const suggestion = document.createElement('div');
        suggestion.className = 'email-suggestion';
        suggestion.innerHTML = `
            <small class="text-muted">
                Você quis dizer: 
                <a href="#" class="text-primary suggestion-link">${suggestedEmail}</a>?
            </small>
        `;
        
        emailInput.parentNode.appendChild(suggestion);
        
        // Handle suggestion click
        suggestion.querySelector('.suggestion-link').addEventListener('click', function(e) {
            e.preventDefault();
            emailInput.value = suggestedEmail;
            hideEmailSuggestion();
            emailInput.focus();
        });
    }
    
    function hideEmailSuggestion() {
        const existingSuggestion = document.querySelector('.email-suggestion');
        if (existingSuggestion) {
            existingSuggestion.remove();
        }
    }
    
    // Smooth scrolling for internal links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
    
    // Add loading animation to form elements
    const formElements = form.querySelectorAll('input, select, textarea');
    formElements.forEach(element => {
        element.addEventListener('focus', function() {
            this.parentNode.classList.add('focused');
        });
        
        element.addEventListener('blur', function() {
            this.parentNode.classList.remove('focused');
        });
    });
});

// CSS for email suggestions and focus states
const style = document.createElement('style');
style.textContent = `
    .email-suggestion {
        margin-top: 0.25rem;
        padding: 0.5rem;
        background: #f8f9fa;
        border-radius: 5px;
        border: 1px solid #dee2e6;
    }
    
    .suggestion-link {
        text-decoration: none;
        font-weight: 600;
    }
    
    .suggestion-link:hover {
        text-decoration: underline;
    }
    
    .focused {
        transform: scale(1.02);
        transition: transform 0.2s ease;
    }
    
    .form-check {
        cursor: pointer;
    }
    
    .form-check:hover {
        transform: translateX(5px);
    }
`;
document.head.appendChild(style);