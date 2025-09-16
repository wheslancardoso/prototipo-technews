package br.com.technews.controller.api;

import br.com.technews.entity.Subscriber;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * API REST Controller para gerenciamento de newsletter
 * Fornece endpoints para integração externa com o sistema de newsletter
 */
@RestController
@RequestMapping("/api/newsletter")
@CrossOrigin(origins = "*", maxAge = 3600)
public class NewsletterApiController {

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private EmailService emailService;

    /**
     * Criar nova inscrição via API
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@Valid @RequestBody SubscribeRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Verificar se email já existe
            if (subscriberService.isEmailSubscribed(request.getEmail())) {
                response.put("success", false);
                response.put("message", "Email já está inscrito na newsletter");
                response.put("code", "EMAIL_ALREADY_EXISTS");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Criar nova inscrição
            Subscriber subscriber = subscriberService.subscribe(
                request.getEmail(),
                request.getNome(),
                request.getFrequencia(),
                request.getCategorias()
            );

            response.put("success", true);
            response.put("message", "Inscrição realizada com sucesso. Verifique seu email para confirmar.");
            response.put("subscriberId", subscriber.getId());
            response.put("verificationRequired", true);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro interno do servidor: " + e.getMessage());
            response.put("code", "INTERNAL_ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verificar status de inscrição
     */
    @GetMapping("/status/{email}")
    public ResponseEntity<Map<String, Object>> getSubscriptionStatus(@PathVariable String email) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Subscriber> subscriberOpt = subscriberService.findByEmail(email);
            
            if (subscriberOpt.isPresent()) {
                Subscriber subscriber = subscriberOpt.get();
                response.put("subscribed", true);
                response.put("active", subscriber.isAtivo());
                response.put("verified", subscriber.isEmailVerificado());
                response.put("frequency", subscriber.getFrequencia().toString());
                response.put("subscriptionDate", subscriber.getDataInscricao());
                response.put("categories", subscriber.getCategorias());
            } else {
                response.put("subscribed", false);
                response.put("active", false);
                response.put("verified", false);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", "Erro ao verificar status: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Cancelar inscrição via API
     */
    @DeleteMapping("/unsubscribe/{email}")
    public ResponseEntity<Map<String, Object>> unsubscribe(@PathVariable String email, 
                                                          @RequestParam(required = false) String reason) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = subscriberService.unsubscribe(email, reason);
            
            if (success) {
                response.put("success", true);
                response.put("message", "Inscrição cancelada com sucesso");
            } else {
                response.put("success", false);
                response.put("message", "Email não encontrado ou já cancelado");
                response.put("code", "EMAIL_NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao cancelar inscrição: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Atualizar preferências do assinante
     */
    @PutMapping("/preferences/{email}")
    public ResponseEntity<Map<String, Object>> updatePreferences(@PathVariable String email,
                                                               @Valid @RequestBody PreferencesRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Subscriber> subscriberOpt = subscriberService.findByEmail(email);
            
            if (!subscriberOpt.isPresent()) {
                response.put("success", false);
                response.put("message", "Assinante não encontrado");
                response.put("code", "SUBSCRIBER_NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Subscriber subscriber = subscriberOpt.get();
            
            // Atualizar preferências
            if (request.getNome() != null) {
                subscriber.setNome(request.getNome());
            }
            if (request.getFrequencia() != null) {
                subscriber.setFrequencia(request.getFrequencia());
            }
            if (request.getCategorias() != null) {
                subscriber.setCategorias(request.getCategorias());
            }
            
            subscriber.setDataAtualizacao(LocalDateTime.now());
            subscriberService.save(subscriber);

            response.put("success", true);
            response.put("message", "Preferências atualizadas com sucesso");
            response.put("subscriber", createSubscriberResponse(subscriber));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao atualizar preferências: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Listar assinantes (com paginação)
     */
    @GetMapping("/subscribers")
    public ResponseEntity<Map<String, Object>> getSubscribers(
            @RequestParam(defaultValue = "true") Boolean activeOnly,
            @RequestParam(required = false) String frequency,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Page<Subscriber> subscribers;
            
            if (search != null && !search.trim().isEmpty()) {
                subscribers = subscriberService.findByEmailContainingOrNomeContaining(search, search, pageable);
            } else if (frequency != null) {
                Subscriber.SubscriptionFrequency freq = Subscriber.SubscriptionFrequency.valueOf(frequency.toUpperCase());
                subscribers = subscriberService.findByFrequenciaAndAtivo(freq, activeOnly, pageable);
            } else {
                subscribers = activeOnly ? 
                    subscriberService.findByAtivo(true, pageable) : 
                    subscriberService.findAll(pageable);
            }

            response.put("success", true);
            response.put("subscribers", subscribers.getContent().stream()
                .map(this::createSubscriberResponse)
                .toArray());
            response.put("totalElements", subscribers.getTotalElements());
            response.put("totalPages", subscribers.getTotalPages());
            response.put("currentPage", subscribers.getNumber());
            response.put("size", subscribers.getSize());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao listar assinantes: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obter estatísticas da newsletter
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            SubscriberService.SubscriberStats stats = subscriberService.getStats();
            
            response.put("success", true);
            response.put("stats", Map.of(
                "totalSubscribers", stats.getTotalSubscribers(),
                "activeSubscribers", stats.getActiveSubscribers(),
                "verifiedSubscribers", stats.getVerifiedSubscribers(),
                "unverifiedSubscribers", stats.getUnverifiedSubscribers(),
                "frequencyDistribution", stats.getFrequencyDistribution(),
                "categoryDistribution", stats.getCategoryDistribution(),
                "recentSubscriptions", stats.getRecentSubscriptions(),
                "monthlyGrowth", stats.getMonthlyGrowth()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao obter estatísticas: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Enviar newsletter manualmente
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNewsletter(@RequestBody SendNewsletterRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validar parâmetros
            if (request.getFrequency() == null) {
                response.put("success", false);
                response.put("message", "Frequência é obrigatória");
                return ResponseEntity.badRequest().body(response);
            }

            // Enviar newsletter
            int emailsSent = emailService.sendNewsletterToSubscribers(
                request.getFrequency(),
                request.getCategoryIds(),
                request.isTestMode()
            );

            response.put("success", true);
            response.put("message", "Newsletter enviada com sucesso");
            response.put("emailsSent", emailsSent);
            response.put("testMode", request.isTestMode());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao enviar newsletter: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Reativar inscrição
     */
    @PostMapping("/reactivate")
    public ResponseEntity<Map<String, Object>> reactivate(@RequestBody ReactivateRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = subscriberService.reactivateSubscription(request.getEmail());
            
            if (success) {
                response.put("success", true);
                response.put("message", "Inscrição reativada com sucesso");
            } else {
                response.put("success", false);
                response.put("message", "Email não encontrado ou já está ativo");
                response.put("code", "EMAIL_NOT_FOUND_OR_ACTIVE");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao reativar inscrição: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Verificar email via API
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyEmail(@RequestBody VerifyEmailRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = subscriberService.verifyEmail(request.getToken());
            
            if (success) {
                response.put("success", true);
                response.put("message", "Email verificado com sucesso");
            } else {
                response.put("success", false);
                response.put("message", "Token inválido ou expirado");
                response.put("code", "INVALID_TOKEN");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao verificar email: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Métodos auxiliares
    private Map<String, Object> createSubscriberResponse(Subscriber subscriber) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", subscriber.getId());
        response.put("email", subscriber.getEmail());
        response.put("nome", subscriber.getNome());
        response.put("ativo", subscriber.isAtivo());
        response.put("emailVerificado", subscriber.isEmailVerificado());
        response.put("frequencia", subscriber.getFrequencia().toString());
        response.put("categorias", subscriber.getCategorias());
        response.put("dataInscricao", subscriber.getDataInscricao());
        response.put("dataAtualizacao", subscriber.getDataAtualizacao());
        response.put("emailsEnviados", subscriber.getEmailsEnviados());
        return response;
    }

    // Classes de Request
    public static class SubscribeRequest {
        private String email;
        private String nome;
        private Subscriber.SubscriptionFrequency frequencia;
        private String categorias;

        // Getters e Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        
        public Subscriber.SubscriptionFrequency getFrequencia() { return frequencia; }
        public void setFrequencia(Subscriber.SubscriptionFrequency frequencia) { this.frequencia = frequencia; }
        
        public String getCategorias() { return categorias; }
        public void setCategorias(String categorias) { this.categorias = categorias; }
    }

    public static class PreferencesRequest {
        private String nome;
        private Subscriber.SubscriptionFrequency frequencia;
        private String categorias;

        // Getters e Setters
        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }
        
        public Subscriber.SubscriptionFrequency getFrequencia() { return frequencia; }
        public void setFrequencia(Subscriber.SubscriptionFrequency frequencia) { this.frequencia = frequencia; }
        
        public String getCategorias() { return categorias; }
        public void setCategorias(String categorias) { this.categorias = categorias; }
    }

    public static class SendNewsletterRequest {
        private Subscriber.SubscriptionFrequency frequency;
        private String categoryIds;
        private boolean testMode = false;

        // Getters e Setters
        public Subscriber.SubscriptionFrequency getFrequency() { return frequency; }
        public void setFrequency(Subscriber.SubscriptionFrequency frequency) { this.frequency = frequency; }
        
        public String getCategoryIds() { return categoryIds; }
        public void setCategoryIds(String categoryIds) { this.categoryIds = categoryIds; }
        
        public boolean isTestMode() { return testMode; }
        public void setTestMode(boolean testMode) { this.testMode = testMode; }
    }

    public static class ReactivateRequest {
        private String email;

        // Getters e Setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class VerifyEmailRequest {
        private String token;

        // Getters e Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}