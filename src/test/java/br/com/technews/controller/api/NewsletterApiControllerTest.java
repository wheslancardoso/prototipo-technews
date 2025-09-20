package br.com.technews.controller.api;

import br.com.technews.entity.Subscriber;
import br.com.technews.service.EmailService;
import br.com.technews.service.SubscriberService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsletterApiController.class)
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email inválido"));

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
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Erro interno do servidor"));

        verify(subscriberService).subscribe(eq("joao@example.com"), eq("João Silva"), isNull(), isNull());
    }

    @Test
    void shouldUnsubscribeSuccessfully() throws Exception {
        // Given
        when(subscriberService.unsubscribe("joao@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/newsletter/unsubscribe")
                .param("email", "joao@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cancelamento realizado com sucesso!"));

        verify(subscriberService).unsubscribe("joao@example.com");
    }

    @Test
    void shouldHandleUnsubscribeWithNonExistentEmail() throws Exception {
        // Given
        when(subscriberService.unsubscribe("nonexistent@example.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/newsletter/unsubscribe")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email não encontrado"));

        verify(subscriberService).unsubscribe("nonexistent@example.com");
    }

    @Test
    void shouldGetSubscriberPreferences() throws Exception {
        // Given
        Subscriber subscriber = createTestSubscriber(1L, "João Silva", "joao@example.com");
        subscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);
        
        when(subscriberService.findByEmail("joao@example.com")).thenReturn(Optional.of(subscriber));

        // When & Then
        mockMvc.perform(get("/api/newsletter/preferences")
                .param("email", "joao@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("joao@example.com"))
                .andExpect(jsonPath("$.data.nome").value("João Silva"))
                .andExpect(jsonPath("$.data.frequencia").value("WEEKLY"))
                .andExpect(jsonPath("$.data.categorias").value("TECHNOLOGY,SCIENCE"));

        verify(subscriberService).findByEmail("joao@example.com");
    }

    @Test
    void shouldHandleGetPreferencesWithNonExistentEmail() throws Exception {
        // Given
        when(subscriberService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/newsletter/preferences")
                .param("email", "nonexistent@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assinante não encontrado"));

        verify(subscriberService).findByEmail("nonexistent@example.com");
    }

    @Test
    void shouldUpdatePreferencesSuccessfully() throws Exception {
        // Given
        NewsletterApiController.PreferencesRequest request = new NewsletterApiController.PreferencesRequest();
        request.setNome("João Silva Updated");
        request.setFrequencia(Subscriber.SubscriptionFrequency.DAILY);
        request.setCategorias("TECHNOLOGY,BUSINESS");

        Subscriber existingSubscriber = createTestSubscriber(1L, "João Silva", "joao@example.com");
        Subscriber updatedSubscriber = createTestSubscriber(1L, "João Silva Updated", "joao@example.com");
        updatedSubscriber.setFrequency(Subscriber.SubscriptionFrequency.DAILY);

        when(subscriberService.findByEmail("joao@example.com")).thenReturn(Optional.of(existingSubscriber));
        when(subscriberService.save(any(Subscriber.class))).thenReturn(updatedSubscriber);

        // When & Then
        mockMvc.perform(put("/api/newsletter/preferences")
                .param("email", "joao@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Preferências atualizadas com sucesso!"))
                .andExpect(jsonPath("$.data.nome").value("João Silva Updated"))
                .andExpect(jsonPath("$.data.frequencia").value("DAILY"))
                .andExpect(jsonPath("$.data.categorias").value("TECHNOLOGY,BUSINESS"));

        verify(subscriberService).findByEmail("joao@example.com");
        verify(subscriberService).save(any(Subscriber.class));
    }

    @Test
    void shouldHandleUpdatePreferencesWithNonExistentEmail() throws Exception {
        // Given
        NewsletterApiController.PreferencesRequest request = new NewsletterApiController.PreferencesRequest();
        request.setNome("João Silva");

        when(subscriberService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(put("/api/newsletter/preferences")
                .param("email", "nonexistent@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Assinante não encontrado"));

        verify(subscriberService).findByEmail("nonexistent@example.com");
        verify(subscriberService, never()).save(any(Subscriber.class));
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