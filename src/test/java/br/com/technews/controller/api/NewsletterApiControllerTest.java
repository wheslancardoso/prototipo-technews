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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(NewsletterApiController.class)
@Import(br.com.technews.controller.TestSecurityConfig.class)
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