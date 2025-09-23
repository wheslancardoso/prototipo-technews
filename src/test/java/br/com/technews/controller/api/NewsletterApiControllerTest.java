package br.com.technews.controller.api;

import br.com.technews.entity.Subscriber;
import br.com.technews.service.EmailService;
import br.com.technews.service.SubscriberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(NewsletterApiController.class)
@Import(br.com.technews.config.TestSecurityConfig.class)
@WithMockUser
class NewsletterApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriberService subscriberService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private br.com.technews.repository.CategoryRepository categoryRepository;

    @Test
    void shouldSubscribeSuccessfully() throws Exception {
        // Given
        String email = "test@example.com";
        String fullName = "Test User";
        String frequency = "WEEKLY";
        String categoryIds = "1,2";

        Subscriber subscriber = new Subscriber();
        subscriber.setId(1L);
        subscriber.setEmail(email);
        subscriber.setFullName(fullName);
        subscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);
        subscriber.setActive(true);
        subscriber.setEmailVerified(false);

        when(subscriberService.subscribe(eq(email), eq(fullName), 
                eq(Subscriber.SubscriptionFrequency.WEEKLY), any()))
                .thenReturn(subscriber);

        // When & Then
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"nome\":\"" + fullName + "\",\"frequencia\":\"" + frequency + "\",\"categorias\":\"" + categoryIds + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscrição realizada com sucesso. Verifique seu email para confirmar."))
                .andExpect(jsonPath("$.subscriberId").value(1))
                .andExpect(jsonPath("$.verificationRequired").value(true));

        verify(subscriberService).subscribe(eq(email), eq(fullName), 
                eq(Subscriber.SubscriptionFrequency.WEEKLY), any());
    }

    @Test
    void shouldHandleSubscribeWithInvalidEmail() throws Exception {
        // Given
        NewsletterApiController.SubscribeRequest request = new NewsletterApiController.SubscribeRequest();
        request.setEmail("invalid-email");
        request.setNome("João Silva");

        when(subscriberService.subscribe(anyString(), anyString(), any(), any()))
            .thenThrow(new IllegalArgumentException("Email inválido"));

        // When & Then
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor: Email inválido"));

        verify(subscriberService).subscribe(eq("invalid-email"), eq("João Silva"), isNull(), isNull());
    }

    @Test
    void shouldHandleSubscribeWithServerError() throws Exception {
        // Given
        NewsletterApiController.SubscribeRequest request = new NewsletterApiController.SubscribeRequest();
        request.setEmail("joao@example.com");
        request.setNome("João Silva");

        when(subscriberService.subscribe(anyString(), anyString(), any(), any()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor: Database error"));

        verify(subscriberService).subscribe(eq("joao@example.com"), eq("João Silva"), isNull(), isNull());
    }

    @Test
    void shouldUnsubscribeSuccessfully() throws Exception {
        // Given
        when(subscriberService.unsubscribe("joao@example.com", null)).thenReturn(true);

        // When & Then
        mockMvc.perform(delete("/api/newsletter/unsubscribe/joao@example.com")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscrição cancelada com sucesso"));

        verify(subscriberService).unsubscribe("joao@example.com", null);
    }

    @Test
    void shouldHandleUnsubscribeWithNonExistentEmail() throws Exception {
        // Given
        when(subscriberService.unsubscribe("nonexistent@example.com", null)).thenReturn(false);

        // When & Then
        mockMvc.perform(delete("/api/newsletter/unsubscribe/nonexistent@example.com")
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email não encontrado ou já cancelado"))
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_FOUND"));

        verify(subscriberService).unsubscribe("nonexistent@example.com", null);
    }

    @Test
    void shouldGetSubscriberPreferences() throws Exception {
        // Given
        Subscriber subscriber = createTestSubscriber(1L, "João Silva", "joao@example.com");
        subscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);
        
        when(subscriberService.findByEmail("joao@example.com")).thenReturn(Optional.of(subscriber));

        // When & Then
        mockMvc.perform(get("/api/newsletter/status/joao@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"));

        verify(subscriberService).findByEmail("joao@example.com");
    }

    @Test
    void shouldHandleGetPreferencesWithNonExistentEmail() throws Exception {
        // Given
        when(subscriberService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/newsletter/status/nonexistent@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.verified").value(false));

        verify(subscriberService).findByEmail("nonexistent@example.com");
    }

    @Test
    void shouldUpdateSubscriberPreferences() throws Exception {
        // Given
        Subscriber subscriber = createTestSubscriber(1L, "João Silva", "joao@example.com");
        when(subscriberService.findByEmail("joao@example.com")).thenReturn(Optional.of(subscriber));
        when(subscriberService.save(any(Subscriber.class))).thenReturn(subscriber);

        // When & Then
        mockMvc.perform(put("/api/newsletter/preferences/joao@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"João Silva Updated\",\"frequencia\":\"DAILY\",\"categorias\":\"TECHNOLOGY,SCIENCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Preferências atualizadas com sucesso"));

        verify(subscriberService).findByEmail("joao@example.com");
        verify(subscriberService).save(any(Subscriber.class));
    }

    @Test
    void shouldHandleUpdatePreferencesWithNonExistentEmail() throws Exception {
        // Given
        when(subscriberService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/newsletter/preferences/nonexistent@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"João Silva\",\"frequencia\":\"DAILY\",\"categorias\":\"TECHNOLOGY\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assinante não encontrado"));

        verify(subscriberService).findByEmail("nonexistent@example.com");
        verify(subscriberService, never()).save(any(Subscriber.class));
    }

    @Test
    void shouldHandleInvalidEmailForPreferences() throws Exception {
        // Given
        when(subscriberService.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/newsletter/preferences/invalid@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Test\",\"frequencia\":\"DAILY\",\"categorias\":\"TECHNOLOGY\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assinante não encontrado"));

        verify(subscriberService).findByEmail("invalid@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListSubscribersWithPagination() throws Exception {
        // Given
        Subscriber subscriber1 = createTestSubscriber(1L, "João Silva", "joao@example.com");
        Subscriber subscriber2 = createTestSubscriber(2L, "Maria Santos", "maria@example.com");
        
        Page<Subscriber> subscriberPage = new PageImpl<>(
                java.util.Arrays.asList(subscriber1, subscriber2),
                PageRequest.of(0, 10),
                2L
        );
        
        when(subscriberService.findByAtivo(eq(true), any(Pageable.class)))
                .thenReturn(subscriberPage);

        // When & Then
        mockMvc.perform(get("/api/newsletter/subscribers")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subscribers").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.size").value(10));

        verify(subscriberService).findByAtivo(eq(true), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetNewsletterStatistics() throws Exception {
        // Given
        SubscriberService.SubscriberStats stats = new SubscriberService.SubscriberStats(150L, 120L, 100L, 90L, List.of());
        when(subscriberService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/newsletter/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.totalSubscribers").value(150))
                .andExpect(jsonPath("$.stats.activeSubscribers").value(120))
                .andExpect(jsonPath("$.stats.verifiedSubscribers").value(100));

        verify(subscriberService).getStats();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSendNewsletterSuccessfully() throws Exception {
        // Given
        when(emailService.sendNewsletterToSubscribers(any(), any(), anyBoolean())).thenReturn(10);

        // When & Then
        mockMvc.perform(post("/api/newsletter/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Weekly Tech News\",\"content\":\"<h1>Latest News</h1>\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Newsletter enviada com sucesso"));

        verify(emailService).sendNewsletterToSubscribers(any(), any(), anyBoolean());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleSendNewsletterFailure() throws Exception {
        // Given
        when(emailService.sendNewsletterToSubscribers(
            eq(Subscriber.SubscriptionFrequency.WEEKLY), 
            isNull(), 
            eq(false)
        )).thenReturn(0);

        // When & Then
        mockMvc.perform(post("/api/newsletter/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Newsletter enviada com sucesso"))
                .andExpect(jsonPath("$.emailsSent").value(0))
                .andExpect(jsonPath("$.testMode").value(false));

        verify(emailService).sendNewsletterToSubscribers(
            eq(Subscriber.SubscriptionFrequency.WEEKLY), 
            isNull(), 
            eq(false)
        );
    }

    @Test
    void shouldReactivateSubscriptionSuccessfully() throws Exception {
        // Given
        when(subscriberService.reactivateSubscription("joao@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/newsletter/reactivate")
                .with(csrf())
                .param("email", "joao@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscrição reativada com sucesso"));

        verify(subscriberService).reactivateSubscription("joao@example.com");
    }

    @Test
    void shouldHandleReactivateWithNonExistentEmail() throws Exception {
        // Given
        when(subscriberService.reactivateSubscription("nonexistent@example.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/newsletter/reactivate")
                .with(csrf())
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email não encontrado ou já está ativo"));

        verify(subscriberService).reactivateSubscription("nonexistent@example.com");
    }

    @Test
    void shouldVerifyEmailSuccessfully() throws Exception {
        // Given
        when(subscriberService.verifyEmail("verification-token")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/newsletter/verify")
                .param("token", "verification-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verificado com sucesso"))
                .andExpect(jsonPath("$.verified").value(true));

        verify(subscriberService).verifyEmail("verification-token");
    }

    @Test
    void shouldHandleVerifyEmailWithInvalidToken() throws Exception {
        // Given
        when(subscriberService.verifyEmail("invalid-token")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/api/newsletter/verify")
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token de verificação inválido ou expirado"))
                .andExpect(jsonPath("$.verified").value(false));

        verify(subscriberService).verifyEmail("invalid-token");
    }

    @Test
    void shouldHandleVerifyEmailWithMissingToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/newsletter/verify"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token de verificação é obrigatório"))
                .andExpect(jsonPath("$.verified").value(false));

        verify(subscriberService, never()).verifyEmail(anyString());
    }

    private Subscriber createTestSubscriber(Long id, String name, String email) {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(id);
        subscriber.setFullName(name);
        subscriber.setEmail(email);
        subscriber.setActive(true);
        subscriber.setEmailVerified(true);
        subscriber.setSubscribedAt(LocalDateTime.now());
        return subscriber;
    }
}