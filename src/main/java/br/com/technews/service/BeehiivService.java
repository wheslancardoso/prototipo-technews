package br.com.technews.service;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.NewsArticle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeehiivService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${beehiiv.api.key}")
    private String apiKey;

    @Value("${beehiiv.api.base-url:https://api.beehiiv.com/v2}")
    private String baseUrl;

    @Value("${beehiiv.publication.id}")
    private String publicationId;

    /**
     * Cria um novo assinante no Beehiiv
     */
    public BeehiivSubscriptionResponse createSubscriber(Subscriber subscriber) {
        try {
            String url = String.format("%s/publications/%s/subscriptions", baseUrl, publicationId);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", subscriber.getEmail());
            requestBody.put("reactivate_existing", true);
            
            // Adicionar campos customizados se necessário
            if (subscriber.getFullName() != null) {
                Map<String, Object> customFields = new HashMap<>();
                customFields.put("name", subscriber.getFullName());
                requestBody.put("custom_fields", List.of(customFields));
            }

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<BeehiivSubscriptionResponse> response = restTemplate.postForEntity(
                url, request, BeehiivSubscriptionResponse.class);

            log.info("Assinante criado no Beehiiv: {} - Status: {}", 
                subscriber.getEmail(), response.getBody().getStatus());
            
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Erro ao criar assinante no Beehiiv: {} - {}", 
                subscriber.getEmail(), e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao criar assinante no Beehiiv", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar assinante no Beehiiv: {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado na integração com Beehiiv", e);
        }
    }

    /**
     * Remove um assinante do Beehiiv
     */
    public void removeSubscriber(String email) {
        try {
            // Primeiro, buscar o assinante pelo email
            BeehiivSubscriptionResponse subscription = findSubscriberByEmail(email);
            if (subscription != null) {
                String url = String.format("%s/publications/%s/subscriptions/%s", 
                    baseUrl, publicationId, subscription.getId());
                
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> request = new HttpEntity<>(headers);
                
                restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
                log.info("Assinante removido do Beehiiv: {}", email);
            }
        } catch (Exception e) {
            log.error("Erro ao remover assinante do Beehiiv: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao remover assinante do Beehiiv", e);
        }
    }

    /**
     * Busca um assinante pelo email
     */
    public BeehiivSubscriptionResponse findSubscriberByEmail(String email) {
        try {
            String url = String.format("%s/publications/%s/subscriptions?email=%s", 
                baseUrl, publicationId, email);
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<BeehiivSubscriptionListResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, request, BeehiivSubscriptionListResponse.class);
            
            BeehiivSubscriptionListResponse listResponse = response.getBody();
            if (listResponse != null && !listResponse.getData().isEmpty()) {
                return listResponse.getData().get(0);
            }
            
            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar assinante no Beehiiv: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Cria um post/newsletter no Beehiiv
     */
    public BeehiivPostResponse createPost(String title, String content, List<NewsArticle> articles) {
        try {
            String url = String.format("%s/publications/%s/posts", baseUrl, publicationId);
            
            // Gerar conteúdo HTML da newsletter
            String htmlContent = generateNewsletterHtml(title, content, articles);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("title", title);
            requestBody.put("content_html", htmlContent);
            requestBody.put("status", "confirmed"); // Publica imediatamente
            
            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<BeehiivPostResponse> response = restTemplate.postForEntity(
                url, request, BeehiivPostResponse.class);

            log.info("Post criado no Beehiiv: {} - ID: {}", title, response.getBody().getId());
            
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Erro ao criar post no Beehiiv: {} - {}", title, e.getResponseBodyAsString());
            throw new RuntimeException("Falha ao criar post no Beehiiv", e);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar post no Beehiiv: {}", e.getMessage(), e);
            throw new RuntimeException("Erro inesperado na criação do post", e);
        }
    }

    /**
     * Gera HTML da newsletter com os artigos
     */
    private String generateNewsletterHtml(String title, String content, List<NewsArticle> articles) {
        StringBuilder html = new StringBuilder();
        
        html.append("<h1>").append(title).append("</h1>");
        
        if (content != null && !content.isEmpty()) {
            html.append("<p>").append(content).append("</p>");
        }
        
        if (articles != null && !articles.isEmpty()) {
            html.append("<h2>Principais Notícias</h2>");
            
            for (NewsArticle article : articles) {
                html.append("<div style='margin-bottom: 20px; border-bottom: 1px solid #eee; padding-bottom: 15px;'>");
                html.append("<h3><a href='").append(article.getUrl()).append("' target='_blank'>")
                    .append(article.getTitle()).append("</a></h3>");
                
                if (article.getSummary() != null) {
                    html.append("<p>").append(article.getSummary()).append("</p>");
                }
                
                html.append("<p><small>Fonte: ").append(article.getSource())
                    .append(" | ").append(article.getPublishedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                    .append("</small></p>");
                html.append("</div>");
            }
        }
        
        return html.toString();
    }

    /**
     * Cria headers para autenticação
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    // DTOs para resposta da API do Beehiiv
    public static class BeehiivSubscriptionResponse {
        private String id;
        private String email;
        private String status;
        private Long created;
        
        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Long getCreated() { return created; }
        public void setCreated(Long created) { this.created = created; }
    }

    public static class BeehiivSubscriptionListResponse {
        private List<BeehiivSubscriptionResponse> data;
        
        public List<BeehiivSubscriptionResponse> getData() { return data; }
        public void setData(List<BeehiivSubscriptionResponse> data) { this.data = data; }
    }

    public static class BeehiivPostResponse {
        private String id;
        private String title;
        private String status;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}