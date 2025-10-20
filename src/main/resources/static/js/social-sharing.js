/**
 * Sistema de Compartilhamento Social
 * Funcionalidades para compartilhar artigos em redes sociais
 */

class SocialSharing {
    constructor() {
        this.init();
    }

    init() {
        this.bindEvents();
        this.setupCopyLink();
    }

    bindEvents() {
        // Eventos para botões de compartilhamento
        document.addEventListener('click', (e) => {
            if (e.target.matches('[data-share]')) {
                e.preventDefault();
                this.handleShare(e.target);
            }
        });
    }

    handleShare(button) {
        const platform = button.dataset.share;
        const url = this.getCurrentUrl();
        const title = this.getArticleTitle();
        const description = this.getArticleDescription();

        switch (platform) {
            case 'facebook':
                this.shareOnFacebook(url, title);
                break;
            case 'twitter':
                this.shareOnTwitter(url, title);
                break;
            case 'linkedin':
                this.shareOnLinkedIn(url, title, description);
                break;
            case 'whatsapp':
                this.shareOnWhatsApp(url, title);
                break;
            case 'telegram':
                this.shareOnTelegram(url, title);
                break;
            case 'copy':
                this.copyToClipboard(url);
                break;
            default:
                console.warn('Plataforma de compartilhamento não suportada:', platform);
        }

        // Analytics (opcional)
        this.trackShare(platform);
    }

    shareOnFacebook(url, title) {
        const shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`;
        this.openShareWindow(shareUrl, 'Facebook');
    }

    shareOnTwitter(url, title) {
        const text = `${title} ${url}`;
        const shareUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(text)}`;
        this.openShareWindow(shareUrl, 'Twitter');
    }

    shareOnLinkedIn(url, title, description) {
        const shareUrl = `https://www.linkedin.com/sharing/share-offsite/?url=${encodeURIComponent(url)}`;
        this.openShareWindow(shareUrl, 'LinkedIn');
    }

    shareOnWhatsApp(url, title) {
        const text = `${title} ${url}`;
        const shareUrl = `https://wa.me/?text=${encodeURIComponent(text)}`;
        this.openShareWindow(shareUrl, 'WhatsApp');
    }

    shareOnTelegram(url, title) {
        const text = `${title} ${url}`;
        const shareUrl = `https://t.me/share/url?url=${encodeURIComponent(url)}&text=${encodeURIComponent(title)}`;
        this.openShareWindow(shareUrl, 'Telegram');
    }

    copyToClipboard(url) {
        if (navigator.clipboard && window.isSecureContext) {
            // Método moderno
            navigator.clipboard.writeText(url).then(() => {
                this.showCopySuccess();
            }).catch(err => {
                console.error('Erro ao copiar:', err);
                this.fallbackCopyToClipboard(url);
            });
        } else {
            // Fallback para navegadores mais antigos
            this.fallbackCopyToClipboard(url);
        }
    }

    fallbackCopyToClipboard(text) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        
        try {
            document.execCommand('copy');
            this.showCopySuccess();
        } catch (err) {
            console.error('Erro ao copiar:', err);
            this.showCopyError();
        } finally {
            document.body.removeChild(textArea);
        }
    }

    showCopySuccess() {
        this.showNotification('Link copiado para a área de transferência!', 'success');
    }

    showCopyError() {
        this.showNotification('Erro ao copiar o link. Tente novamente.', 'error');
    }

    showNotification(message, type = 'info') {
        // Remove notificação anterior se existir
        const existingNotification = document.querySelector('.share-notification');
        if (existingNotification) {
            existingNotification.remove();
        }

        // Cria nova notificação
        const notification = document.createElement('div');
        notification.className = `share-notification share-notification--${type}`;
        notification.textContent = message;
        
        // Estilos inline para garantir que funcione
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${type === 'success' ? '#28a745' : type === 'error' ? '#dc3545' : '#007bff'};
            color: white;
            padding: 12px 20px;
            border-radius: 4px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            z-index: 10000;
            font-size: 14px;
            transition: all 0.3s ease;
        `;

        document.body.appendChild(notification);

        // Remove após 3 segundos
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }

    openShareWindow(url, platform) {
        const width = 600;
        const height = 400;
        const left = (window.innerWidth - width) / 2;
        const top = (window.innerHeight - height) / 2;

        const features = `
            width=${width},
            height=${height},
            left=${left},
            top=${top},
            scrollbars=yes,
            resizable=yes
        `.replace(/\s/g, '');

        window.open(url, `share-${platform}`, features);
    }

    getCurrentUrl() {
        return window.location.href;
    }

    getArticleTitle() {
        // Tenta pegar o título do artigo
        const titleElement = document.querySelector('h1.article-title, .article-title, h1');
        if (titleElement) {
            return titleElement.textContent.trim();
        }
        
        // Fallback para o título da página
        return document.title || 'TechNews';
    }

    getArticleDescription() {
        // Tenta pegar a descrição do artigo
        const descElement = document.querySelector('.article-summary, .article-description, meta[name="description"]');
        if (descElement) {
            return descElement.textContent || descElement.getAttribute('content') || '';
        }
        
        return 'Confira este artigo interessante no TechNews!';
    }

    setupCopyLink() {
        // Adiciona funcionalidade de copiar link ao clicar no URL
        const urlElements = document.querySelectorAll('.article-url, .share-url');
        urlElements.forEach(element => {
            element.style.cursor = 'pointer';
            element.addEventListener('click', (e) => {
                e.preventDefault();
                this.copyToClipboard(this.getCurrentUrl());
            });
        });
    }

    trackShare(platform) {
        // Implementação futura para analytics
        console.log(`Compartilhamento realizado: ${platform}`);
        
        // Exemplo de integração com Google Analytics
        if (typeof gtag !== 'undefined') {
            gtag('event', 'share', {
                method: platform,
                content_type: 'article',
                content_id: this.getArticleId()
            });
        }
    }

    getArticleId() {
        // Tenta extrair o ID do artigo da URL ou de um elemento
        const match = window.location.pathname.match(/\/article\/(\d+)/);
        return match ? match[1] : 'unknown';
    }
}

// Inicializa o sistema quando o DOM estiver carregado
document.addEventListener('DOMContentLoaded', () => {
    new SocialSharing();
});

// Exporta para uso em outros scripts se necessário
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SocialSharing;
}