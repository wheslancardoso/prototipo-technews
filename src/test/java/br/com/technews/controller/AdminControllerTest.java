package br.com.technews.controller;

import br.com.technews.entity.Subscriber;
import br.com.technews.service.SubscriberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriberService subscriberService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListSubscribersSuccessfully() throws Exception {
        // Given
        Subscriber subscriber1 = createTestSubscriber(1L, "João", "joao@example.com");
        Subscriber subscriber2 = createTestSubscriber(2L, "Maria", "maria@example.com");
        List<Subscriber> subscribers = Arrays.asList(subscriber1, subscriber2);
        Page<Subscriber> subscriberPage = new PageImpl<>(subscribers, PageRequest.of(0, 10), 2);
        
        SubscriberService.SubscriberStats stats = new SubscriberService.SubscriberStats(2L, 2L, 0L, 0L, List.of());
        
        when(subscriberService.findAll(anyString(), any(), any(), any(PageRequest.class)))
            .thenReturn(subscriberPage);
        when(subscriberService.getStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/admin/subscribers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/subscribers/list"))
                .andExpect(model().attribute("subscribers", subscriberPage))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1))
                .andExpect(model().attribute("totalElements", 2L))
                .andExpect(model().attribute("activeCount", 2L))
                .andExpect(model().attribute("totalCount", 2L));

        verify(subscriberService).findAll("", null, null, PageRequest.of(0, 10));
        verify(subscriberService).getStats();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleListSubscribersWithException() throws Exception {
        // Given
        when(subscriberService.findAll(anyString(), any(), any(), any(PageRequest.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/admin/subscribers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/subscribers/list"))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 0))
                .andExpect(model().attribute("totalElements", 0L))
                .andExpect(model().attribute("activeCount", 0L))
                .andExpect(model().attribute("totalCount", 0L))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldShowEditSubscriberForm() throws Exception {
        // Given
        Subscriber subscriber = createTestSubscriber(1L, "João", "joao@example.com");
        when(subscriberService.findById(1L)).thenReturn(Optional.of(subscriber));

        // When & Then
        mockMvc.perform(get("/admin/subscribers/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/subscribers/form"))
                .andExpect(model().attribute("subscriber", subscriber));

        verify(subscriberService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectWhenSubscriberNotFoundForEdit() throws Exception {
        // Given
        when(subscriberService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/admin/subscribers/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subscribers"))
                .andExpect(flash().attribute("error", "Assinante não encontrado!"));

        verify(subscriberService).findById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateSubscriberSuccessfully() throws Exception {
        // Given
        Subscriber subscriber = createTestSubscriber(1L, "João Updated", "joao@example.com");
        when(subscriberService.save(any(Subscriber.class))).thenReturn(subscriber);

        // When & Then
        mockMvc.perform(post("/admin/subscribers/1")
                .with(csrf())
                .param("id", "1")
                .param("fullName", "João Updated")
                .param("email", "joao@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subscribers"))
                .andExpect(flash().attribute("success", "Assinante atualizado com sucesso!"));

        verify(subscriberService).save(any(Subscriber.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleUpdateSubscriberWithException() throws Exception {
        // Given
        when(subscriberService.save(any(Subscriber.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/admin/subscribers/1")
                .with(csrf())
                .param("id", "1")
                .param("fullName", "João Updated")
                .param("email", "joao@example.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subscribers"))
                .andExpect(flash().attribute("error", "Erro ao atualizar assinante: Database error"));

        verify(subscriberService).save(any(Subscriber.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteSubscriberSuccessfully() throws Exception {
        // Given
        doNothing().when(subscriberService).delete(1L);

        // When & Then
        mockMvc.perform(post("/admin/subscribers/1/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subscribers"))
                .andExpect(flash().attribute("success", "Assinante removido com sucesso!"));

        verify(subscriberService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleDeleteSubscriberWithException() throws Exception {
        // Given
        doThrow(new RuntimeException("Database error"))
            .when(subscriberService).delete(1L);

        // When & Then
        mockMvc.perform(post("/admin/subscribers/1/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/subscribers"))
                .andExpect(flash().attribute("error", "Erro ao remover assinante: Database error"));

        verify(subscriberService).delete(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExportSubscribersAsCsv() throws Exception {
        // Given
        Subscriber subscriber1 = createTestSubscriber(1L, "João", "joao@example.com");
        Subscriber subscriber2 = createTestSubscriber(2L, "Maria", "maria@example.com");
        List<Subscriber> subscribers = Arrays.asList(subscriber1, subscriber2);
        
        when(subscriberService.findActiveAndVerifiedSubscribers()).thenReturn(subscribers);

        // When & Then
        mockMvc.perform(get("/admin/subscribers/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"subscribers.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ID,Nome,Email,Ativo,Data de Inscrição")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1,João,joao@example.com,Sim")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("2,Maria,maria@example.com,Sim")));

        verify(subscriberService).findActiveAndVerifiedSubscribers();
    }

    private Subscriber createTestSubscriber(Long id, String name, String email) {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(id);
        subscriber.setFullName(name);
        subscriber.setEmail(email);
        subscriber.setActive(true);
        subscriber.setSubscribedAt(LocalDateTime.now());
        return subscriber;
    }
}