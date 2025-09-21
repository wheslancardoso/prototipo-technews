package br.com.technews.controller;

import br.com.technews.service.SubscriberService;
import br.com.technews.service.NewsArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PageController.class)
@Import(TestSecurityConfig.class)
class PageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriberService subscriberService;
    
    @MockBean
    private NewsArticleService newsArticleService;

    @Test
    void shouldDisplayHomePage() throws Exception {
        // Given
        when(subscriberService.getAllSubscribers()).thenReturn(Arrays.asList());
        when(newsArticleService.findPublishedArticles(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(newsArticleService.countPublished()).thenReturn(0L);
        when(newsArticleService.getDistinctCategories()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("subscriberCount", 0));

        verify(subscriberService).getAllSubscribers();
    }

    @Test
    void shouldDisplayHomePageWithSubscriberCount() throws Exception {
        // Given
        when(subscriberService.getAllSubscribers()).thenReturn(Arrays.asList(null, null, null));
        when(newsArticleService.findPublishedArticles(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(newsArticleService.countPublished()).thenReturn(5L);
        when(newsArticleService.getDistinctCategories()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("subscriberCount", 3));

        verify(subscriberService).getAllSubscribers();
    }

    @Test
    void shouldHandleSubscriberServiceException() throws Exception {
        // Given
        when(subscriberService.getAllSubscribers()).thenThrow(new RuntimeException("Database error"));
        when(newsArticleService.findPublishedArticles(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(newsArticleService.countPublished()).thenReturn(0L);
        when(newsArticleService.getDistinctCategories()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("subscriberCount", 0));

        verify(subscriberService).getAllSubscribers();
    }

    @Test
    void shouldRedirectToAdminArticles() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/articles"));
    }

    @Test
    void shouldDisplayLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void shouldProcessSubscriptionSuccessfully() throws Exception {
        // Given
        String nome = "João Silva";
        String email = "joao@example.com";
        
        // When & Then
        mockMvc.perform(post("/subscribe")
                .param("nome", nome)
                .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("successMessage", 
                    "Obrigado, " + nome + "! Sua inscrição foi realizada com sucesso."));

        verify(subscriberService).subscribe(email, nome, null, null);
    }

    @Test
    void shouldHandleSubscriptionWithIllegalArgumentException() throws Exception {
        // Given
        String nome = "João Silva";
        String email = "invalid-email";
        String errorMessage = "Email inválido";
        
        doThrow(new IllegalArgumentException(errorMessage))
            .when(subscriberService).subscribe(email, nome, null, null);

        // When & Then
        mockMvc.perform(post("/subscribe")
                .param("nome", nome)
                .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", errorMessage));

        verify(subscriberService).subscribe(email, nome, null, null);
    }

    @Test
    void shouldHandleSubscriptionWithGenericException() throws Exception {
        // Given
        String nome = "João Silva";
        String email = "joao@example.com";
        
        doThrow(new RuntimeException("Database error"))
            .when(subscriberService).subscribe(email, nome, null, null);

        // When & Then
        mockMvc.perform(post("/subscribe")
                .param("nome", nome)
                .param("email", email))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", 
                    "Erro interno. Tente novamente mais tarde."));

        verify(subscriberService).subscribe(email, nome, null, null);
    }
}