package br.com.technews.service;

import br.com.technews.config.ApiConfig;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.NewsArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    
    private final ApiConfig apiConfig;
    private final WebClient webClient;
    
    public EmailService(ApiConfig apiConfig, WebClient webClient) {
        this.apiConfig = apiConfig;
        this.webClient = webClient;
    }
    
    /**
     * Envia newsletter através da API do Mailgun
     * @param recipientEmail Email do destinatário
     * @param subject Assunto do email
     * @param htmlBody Conteúdo HTML do email
     */
    public void sendNewsletter(String recipientEmail, String subject, String htmlBody) {
        try {
            // Prepara os dados do formulário
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("from", "TechNews <mailgun@" + apiConfig.getMailgunDomain() + ">");
            formData.add("to", recipientEmail);
            formData.add("subject", subject);
            formData.add("html", htmlBody);
            
            // Prepara a autenticação Basic Auth
            String auth = "api:" + apiConfig.getMailgunApiKey();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            
            // URL da API do Mailgun
            String url = "https://api.mailgun.net/v3/" + apiConfig.getMailgunDomain() + "/messages";
            
            // Faz a requisição POST assíncrona
            webClient.post()
                    .uri(url)
                    .header("Authorization", "Basic " + encodedAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                        response -> {
                            log.info("Email enviado com sucesso para {}: {}", recipientEmail, response);
                        },
                        error -> {
                            log.error("Erro ao enviar email para {}: {}", recipientEmail, error.getMessage());
                        }
                    );
                    
        } catch (Exception e) {
            log.error("Erro ao preparar envio de email para {}: {}", recipientEmail, e.getMessage());
        }
    }
    
    /**
     * Métodos de compatibilidade com o código existente
     */
    public CompletableFuture<Boolean> sendVerificationEmail(Subscriber subscriber) {
        String subject = "Confirme sua inscrição na newsletter - TechNews";
        String htmlBody = "<h1>Confirme sua inscrição</h1><p>Clique no link para confirmar sua inscrição.</p>";
        sendNewsletter(subscriber.getEmail(), subject, htmlBody);
        return CompletableFuture.completedFuture(true);
    }
    
    public CompletableFuture<Boolean> sendWelcomeEmail(Subscriber subscriber) {
        String subject = "Bem-vindo à nossa newsletter! - TechNews";
        String htmlBody = "<h1>Bem-vindo!</h1><p>Obrigado por se inscrever em nossa newsletter.</p>";
        sendNewsletter(subscriber.getEmail(), subject, htmlBody);
        return CompletableFuture.completedFuture(true);
    }
    
    public CompletableFuture<Boolean> sendUnsubscribeConfirmationEmail(Subscriber subscriber) {
        String subject = "Cancelamento confirmado - TechNews";
        String htmlBody = "<h1>Cancelamento confirmado</h1><p>Sua inscrição foi cancelada com sucesso.</p>";
        sendNewsletter(subscriber.getEmail(), subject, htmlBody);
        return CompletableFuture.completedFuture(true);
    }
    
    public CompletableFuture<Boolean> sendNewsletterToSubscriber(Subscriber subscriber, List<NewsArticle> articles) {
        String subject = "TechNews - Novos artigos para você!";
        StringBuilder htmlBody = new StringBuilder("<h1>Newsletter TechNews</h1>");
        for (NewsArticle article : articles) {
            htmlBody.append("<h2>").append(article.getTitle()).append("</h2>");
            htmlBody.append("<p>").append(article.getSummary()).append("</p>");
        }
        sendNewsletter(subscriber.getEmail(), subject, htmlBody.toString());
        return CompletableFuture.completedFuture(true);
    }
    
    public void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        sendNewsletter(toEmail, subject, htmlContent);
    }
    
    public int sendNewsletterToSubscribers(Subscriber.SubscriptionFrequency frequency, String categoryIds, boolean testMode) {
        log.info("Método sendNewsletterToSubscribers chamado com frequência: {}, categorias: {}, modo teste: {}", 
                frequency, categoryIds, testMode);
        // Implementação simplificada - retorna 0 por enquanto
        return 0;
    }
}