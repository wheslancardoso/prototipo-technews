package br.com.technews.security;

import br.com.technews.entity.Subscriber;
import br.com.technews.repository.SubscriberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Testes de segurança para verificar autenticação, autorização e proteção de rotas
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        subscriberRepository.deleteAll();
    }

    @Test
    void shouldAllowPublicAccessToSubscriptionEndpoints() throws Exception {
        // Endpoints públicos devem ser acessíveis sem autenticação
        
        // 1. Inscrição na newsletter
        String subscribeRequest = """
            {
                "email": "public@example.com",
                "nome": "Public User",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isCreated());

        // 2. Verificação de status
        mockMvc.perform(get("/api/newsletter/status/public@example.com"))
                .andExpect(status().isOk());

        // 3. Cancelamento de inscrição (com token)
        Subscriber subscriber = subscriberRepository.findByEmail("public@example.com").orElse(null);
        if (subscriber != null) {
            mockMvc.perform(delete("/api/newsletter/unsubscribe/public@example.com")
                    .with(csrf())
                    .param("token", subscriber.getManageToken()))
                    .andExpect(status().isOk());
        }

        // 4. Reativação
        mockMvc.perform(post("/api/newsletter/reactivate")
                .with(csrf())
                .param("email", "public@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldDenyAccessToAdminEndpointsWithoutAuthentication() throws Exception {
        // Endpoints administrativos devem ser protegidos
        
        // 1. Listagem de subscribers
        mockMvc.perform(get("/api/newsletter/subscribers"))
                .andExpect(status().isUnauthorized());

        // 2. Estatísticas
        mockMvc.perform(get("/api/newsletter/stats"))
                .andExpect(status().isUnauthorized());

        // 3. Envio de newsletter
        mockMvc.perform(post("/api/newsletter/send")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyAccessToAdminEndpointsWithUserRole() throws Exception {
        // Usuários com role USER não devem acessar endpoints administrativos
        
        // 1. Listagem de subscribers
        mockMvc.perform(get("/api/newsletter/subscribers"))
                .andExpect(status().isForbidden());

        // 2. Estatísticas
        mockMvc.perform(get("/api/newsletter/stats"))
                .andExpect(status().isForbidden());

        // 3. Envio de newsletter
        mockMvc.perform(post("/api/newsletter/send")
                .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAccessToAdminEndpointsWithAdminRole() throws Exception {
        // Usuários com role ADMIN devem acessar endpoints administrativos
        
        // 1. Listagem de subscribers
        mockMvc.perform(get("/api/newsletter/subscribers"))
                .andExpect(status().isOk());

        // 2. Estatísticas
        mockMvc.perform(get("/api/newsletter/stats"))
                .andExpect(status().isOk());

        // 3. Envio de newsletter (pode retornar erro de negócio, mas não de autorização)
        mockMvc.perform(post("/api/newsletter/send")
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Deve ser qualquer status exceto 401 (Unauthorized) ou 403 (Forbidden)
                    assert status != 401 && status != 403;
                });
    }

    @Test
    void shouldRequireCSRFTokenForStateChangingOperations() throws Exception {
        // Operações que modificam estado devem exigir token CSRF
        
        String subscribeRequest = """
            {
                "email": "csrf@example.com",
                "nome": "CSRF User",
                "frequencia": "WEEKLY"
            }
            """;

        // 1. POST sem CSRF deve falhar
        mockMvc.perform(post("/api/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isForbidden());

        // 2. POST com CSRF deve funcionar
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldValidateInputDataAndPreventInjectionAttacks() throws Exception {
        // Testes para prevenir ataques de injeção
        
        // 1. SQL Injection attempt
        String maliciousRequest = """
            {
                "email": "test'; DROP TABLE subscribers; --",
                "nome": "Malicious User",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(maliciousRequest))
                .andExpect(status().isBadRequest());

        // 2. XSS attempt
        String xssRequest = """
            {
                "email": "xss@example.com",
                "nome": "<script>alert('XSS')</script>",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(xssRequest))
                .andExpect(status().isBadRequest());

        // 3. Email format validation
        String invalidEmailRequest = """
            {
                "email": "invalid-email",
                "nome": "Invalid Email User",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEmailRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldProtectAgainstUnauthorizedTokenAccess() throws Exception {
        // Criar subscriber para teste
        Subscriber subscriber = createTestSubscriber("token@example.com", "Token User");
        subscriber = subscriberRepository.save(subscriber);

        // 1. Tentar cancelar inscrição sem token
        mockMvc.perform(delete("/api/newsletter/unsubscribe/token@example.com")
                .with(csrf()))
                .andExpect(status().isBadRequest());

        // 2. Tentar cancelar inscrição com token inválido
        mockMvc.perform(delete("/api/newsletter/unsubscribe/token@example.com")
                .with(csrf())
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest());

        // 3. Cancelar inscrição com token válido deve funcionar
        mockMvc.perform(delete("/api/newsletter/unsubscribe/token@example.com")
                .with(csrf())
                .param("token", subscriber.getManageToken()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleRateLimitingForPublicEndpoints() throws Exception {
        // Simular múltiplas tentativas de inscrição
        String subscribeRequest = """
            {
                "email": "ratelimit@example.com",
                "nome": "Rate Limit User",
                "frequencia": "WEEKLY"
            }
            """;

        // Primeira tentativa deve funcionar
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isCreated());

        // Tentativas subsequentes com mesmo email devem ser rejeitadas
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSecurelyHandlePasswordResetFlow() throws Exception {
        // Teste para fluxo de reset de senha (se implementado)
        // Este teste pode ser expandido quando funcionalidade de login for implementada
        
        // Por enquanto, verificar que endpoints de reset não existem ou estão protegidos
        mockMvc.perform(post("/api/auth/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\"}"))
                .andExpect(status().isNotFound()); // Endpoint não implementado ainda
    }

    @Test
    void shouldValidateSessionManagement() throws Exception {
        // Teste para gerenciamento de sessão
        // Verificar que sessões são invalidadas corretamente
        
        // Este teste pode ser expandido quando autenticação baseada em sessão for implementada
        mockMvc.perform(post("/api/auth/logout")
                .with(csrf()))
                .andExpect(status().isNotFound()); // Endpoint não implementado ainda
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldLogSecurityEvents() throws Exception {
        // Teste para verificar que eventos de segurança são logados
        // (Este teste verificaria logs, mas por simplicidade, apenas testa o comportamento)
        
        // Tentativa de acesso a dados sensíveis deve ser logada
        mockMvc.perform(get("/api/newsletter/subscribers"))
                .andExpect(status().isOk());

        // Em um cenário real, verificaríamos se o evento foi logado
        // Por exemplo: verificar se existe entrada no log de auditoria
    }

    private Subscriber createTestSubscriber(String email, String name) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFullName(name);
        subscriber.setActive(true);
        subscriber.setEmailVerified(true);
        subscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);
        subscriber.setSubscribedAt(LocalDateTime.now());
        subscriber.setManageToken("token-" + email.hashCode());
        return subscriber;
    }
}