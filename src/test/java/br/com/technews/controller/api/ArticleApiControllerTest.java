package br.com.technews.controller.api;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.entity.Category;
import br.com.technews.service.NewsArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para ArticleApiController
 * Testa endpoints REST da API de artigos
 */
@WebMvcTest(ArticleApiController.class)
@Import(br.com.technews.controller.TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.thymeleaf.check-template=false",
    "spring.thymeleaf.check-template-location=false",
    "spring.thymeleaf.mode=HTML",
    "spring.thymeleaf.enabled=false"
})
@DisplayName("Article API Controller Tests")
class ArticleApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsArticleService newsArticleService;

    @Autowired
    private ObjectMapper objectMapper;

    private NewsArticle publishedArticle;
    private NewsArticle draftArticle;
    private Category techCategory;

    @BeforeEach
    void setUp() {
        techCategory = createTestCategory(1L, "Technology", "TECHNOLOGY");
        publishedArticle = createTestArticle(1L, "Published Article", true, "Technology");
        draftArticle = createTestArticle(2L, "Draft Article", false, "Technology");
    }

    @Test
    @DisplayName("GET /api/articles - Deve retornar artigos publicados com paginação")
    void shouldGetAllPublishedArticlesWithPagination() throws Exception {
        // Given
        List<NewsArticle> articles = Arrays.asList(publishedArticle);
        Page<NewsArticle> articlePage = new PageImpl<>(articles, PageRequest.of(0, 10), 1);
        when(newsArticleService.findPublishedArticles(any(Pageable.class))).thenReturn(articlePage);

        // When & Then
        mockMvc.perform(get("/api/articles")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "publishedAt")
                .param("sortDir", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.articles", hasSize(1)))
                .andExpect(jsonPath("$.articles[0].title", is("Published Article")))
                .andExpect(jsonPath("$.articles[0].published", is(true)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrevious", is(false)));

        verify(newsArticleService).findPublishedArticles(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles - Deve retornar erro quando serviço falha")
    void shouldReturnErrorWhenServiceFails() throws Exception {
        // Given
        when(newsArticleService.findPublishedArticles(any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/articles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao buscar artigos")));

        verify(newsArticleService).findPublishedArticles(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles/{id} - Deve retornar artigo publicado por ID")
    void shouldGetPublishedArticleById() throws Exception {
        // Given
        when(newsArticleService.findById(1L)).thenReturn(Optional.of(publishedArticle));

        // When & Then
        mockMvc.perform(get("/api/articles/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.article.id", is(1)))
                .andExpect(jsonPath("$.article.title", is("Published Article")))
                .andExpect(jsonPath("$.article.published", is(true)));

        verify(newsArticleService).findById(1L);
    }

    @Test
    @DisplayName("GET /api/articles/{id} - Deve retornar 404 quando artigo não existe")
    void shouldReturn404WhenArticleNotFound() throws Exception {
        // Given
        when(newsArticleService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/articles/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(newsArticleService).findById(999L);
    }

    @Test
    @DisplayName("GET /api/articles/{id} - Deve retornar 404 quando artigo não está publicado")
    void shouldReturn404WhenArticleNotPublished() throws Exception {
        // Given
        when(newsArticleService.findById(2L)).thenReturn(Optional.of(draftArticle));

        // When & Then
        mockMvc.perform(get("/api/articles/2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(newsArticleService).findById(2L);
    }

    @Test
    @DisplayName("GET /api/articles/category/{category} - Deve retornar artigos por categoria")
    void shouldGetArticlesByCategory() throws Exception {
        // Given
        List<NewsArticle> articles = Arrays.asList(publishedArticle);
        Page<NewsArticle> articlePage = new PageImpl<>(articles, PageRequest.of(0, 10), 1);
        when(newsArticleService.findPublishedArticlesByCategory(eq("technology"), any(Pageable.class)))
                .thenReturn(articlePage);

        // When & Then
        mockMvc.perform(get("/api/articles/category/technology")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.articles", hasSize(1)))
                .andExpect(jsonPath("$.articles[0].title", is("Published Article")))
                .andExpect(jsonPath("$.category", is("technology")))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(newsArticleService).findPublishedArticlesByCategory(eq("technology"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles/category/{category} - Deve retornar erro quando busca por categoria falha")
    void shouldReturnErrorWhenCategorySearchFails() throws Exception {
        // Given
        when(newsArticleService.findPublishedArticlesByCategory(anyString(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Category search error"));

        // When & Then
        mockMvc.perform(get("/api/articles/category/technology")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao buscar artigos por categoria")));

        verify(newsArticleService).findPublishedArticlesByCategory(eq("technology"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles/search - Deve pesquisar artigos por termo")
    void shouldSearchArticlesByQuery() throws Exception {
        // Given
        List<NewsArticle> articles = Arrays.asList(publishedArticle);
        Page<NewsArticle> articlePage = new PageImpl<>(articles, PageRequest.of(0, 10), 1);
        when(newsArticleService.searchArticlesWithFilters(
                eq("technology"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(articlePage);

        // When & Then
        mockMvc.perform(get("/api/articles/search")
                .param("query", "technology")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.articles", hasSize(1)))
                .andExpect(jsonPath("$.articles[0].title", is("Published Article")))
                .andExpect(jsonPath("$.query", is("technology")))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        verify(newsArticleService).searchArticlesWithFilters(
                eq("technology"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles/search - Deve retornar erro quando pesquisa falha")
    void shouldReturnErrorWhenSearchFails() throws Exception {
        // Given
        when(newsArticleService.searchArticlesWithFilters(
                anyString(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenThrow(new RuntimeException("Search error"));

        // When & Then
        mockMvc.perform(get("/api/articles/search")
                .param("query", "technology")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao pesquisar artigos")));

        verify(newsArticleService).searchArticlesWithFilters(
                eq("technology"), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/articles/recent - Deve retornar artigos recentes")
    void shouldGetRecentArticles() throws Exception {
        // Given
        List<NewsArticle> recentArticles = Arrays.asList(publishedArticle);
        when(newsArticleService.findRecentArticles(10)).thenReturn(recentArticles);

        // When & Then
        mockMvc.perform(get("/api/articles/recent")
                .param("limit", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.articles", hasSize(1)))
                .andExpect(jsonPath("$.articles[0].title", is("Published Article")))
                .andExpect(jsonPath("$.count", is(1)));

        verify(newsArticleService).findRecentArticles(10);
    }

    @Test
    @DisplayName("GET /api/articles/recent - Deve usar limite padrão quando não especificado")
    void shouldUseDefaultLimitForRecentArticles() throws Exception {
        // Given
        List<NewsArticle> recentArticles = Arrays.asList(publishedArticle);
        when(newsArticleService.findRecentArticles(10)).thenReturn(recentArticles);

        // When & Then
        mockMvc.perform(get("/api/articles/recent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.articles", hasSize(1)))
                .andExpect(jsonPath("$.count", is(1)));

        verify(newsArticleService).findRecentArticles(10);
    }

    @Test
    @DisplayName("GET /api/articles/recent - Deve retornar erro quando busca de recentes falha")
    void shouldReturnErrorWhenRecentArticlesFails() throws Exception {
        // Given
        when(newsArticleService.findRecentArticles(anyInt()))
                .thenThrow(new RuntimeException("Recent articles error"));

        // When & Then
        mockMvc.perform(get("/api/articles/recent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao buscar artigos recentes")));

        verify(newsArticleService).findRecentArticles(10);
    }

    @Test
    @DisplayName("GET /api/articles/stats - Deve retornar estatísticas dos artigos")
    void shouldGetArticleStats() throws Exception {
        // Given
        when(newsArticleService.countAll()).thenReturn(100L);
        when(newsArticleService.countPublished()).thenReturn(80L);

        // When & Then
        mockMvc.perform(get("/api/articles/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalArticles", is(100)))
                .andExpect(jsonPath("$.publishedArticles", is(80)))
                .andExpect(jsonPath("$.pendingArticles", is(20)));

        verify(newsArticleService).countAll();
        verify(newsArticleService).countPublished();
    }

    @Test
    @DisplayName("GET /api/articles/stats - Deve retornar erro quando falha ao obter estatísticas")
    void shouldReturnErrorWhenStatsFailure() throws Exception {
        // Given
        when(newsArticleService.countAll()).thenThrow(new RuntimeException("Stats error"));

        // When & Then
        mockMvc.perform(get("/api/articles/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao obter estatísticas")));

        verify(newsArticleService).countAll();
    }

    @Test
    @DisplayName("GET /api/articles - Deve aplicar ordenação ascendente")
    void shouldApplyAscendingSort() throws Exception {
        // Given
        List<NewsArticle> articles = Arrays.asList(publishedArticle);
        Page<NewsArticle> articlePage = new PageImpl<>(articles, PageRequest.of(0, 10), 1);
        when(newsArticleService.findPublishedArticles(any(Pageable.class))).thenReturn(articlePage);

        // When & Then
        mockMvc.perform(get("/api/articles")
                .param("sortBy", "title")
                .param("sortDir", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.articles", hasSize(1)));

        verify(newsArticleService).findPublishedArticles(any(Pageable.class));
    }

    /**
     * Métodos auxiliares para criar objetos de teste
     */
    private NewsArticle createTestArticle(Long id, String title, boolean published, String categoryName) {
        NewsArticle article = new NewsArticle();
        article.setId(id);
        article.setTitle(title);
        article.setContent("Content for " + title);
        article.setPublished(published);
        article.setCategory(categoryName);
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        if (published) {
            article.setPublishedAt(LocalDateTime.now());
        }
        return article;
    }

    private Category createTestCategory(Long id, String name, String slug) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription("Description for " + name);
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
}