package br.com.technews.controller.api;

import br.com.technews.entity.Category;
import br.com.technews.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para CategoryApiController
 * Testa endpoints REST da API de categorias
 */
@WebMvcTest(CategoryApiController.class)
@Import(br.com.technews.controller.TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.thymeleaf.check-template=false",
    "spring.thymeleaf.check-template-location=false",
    "spring.thymeleaf.mode=HTML",
    "spring.thymeleaf.enabled=false"
})
@DisplayName("Category API Controller Tests")
class CategoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category techCategory;
    private Category businessCategory;
    private Category inactiveCategory;

    @BeforeEach
    void setUp() {
        techCategory = createTestCategory(1L, "Technology", "TECHNOLOGY", true);
        businessCategory = createTestCategory(2L, "Business", "BUSINESS", true);
        inactiveCategory = createTestCategory(3L, "Inactive", "INACTIVE", false);
    }

    @Test
    @DisplayName("GET /api/categories - Deve retornar todas as categorias ativas com paginação")
    void shouldGetAllCategoriesWithPagination() throws Exception {
        // Given
        List<Category> activeCategories = Arrays.asList(techCategory, businessCategory);
        when(categoryService.findAllActive()).thenReturn(activeCategories);

        // When & Then
        mockMvc.perform(get("/api/categories")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "name")
                .param("sortDir", "asc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].name", is("Technology")))
                .andExpect(jsonPath("$.categories[1].name", is("Business")))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.totalPages", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrevious", is(false)));

        verify(categoryService).findAllActive();
    }

    @Test
    @DisplayName("GET /api/categories - Deve retornar erro quando serviço falha")
    void shouldReturnErrorWhenServiceFails() throws Exception {
        // Given
        when(categoryService.findAllActive()).thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao buscar categorias")));

        verify(categoryService).findAllActive();
    }

    @Test
    @DisplayName("GET /api/categories/all - Deve retornar todas as categorias ativas sem paginação")
    void shouldGetAllCategoriesSimple() throws Exception {
        // Given
        List<Category> activeCategories = Arrays.asList(techCategory, businessCategory);
        when(categoryService.findAllActive()).thenReturn(activeCategories);

        // When & Then
        mockMvc.perform(get("/api/categories/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].name", is("Technology")))
                .andExpect(jsonPath("$.categories[1].name", is("Business")))
                .andExpect(jsonPath("$.count", is(2)));

        verify(categoryService).findAllActive();
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Deve retornar categoria ativa por ID")
    void shouldGetCategoryByIdWhenActive() throws Exception {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.of(techCategory));

        // When & Then
        mockMvc.perform(get("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.category.id", is(1)))
                .andExpect(jsonPath("$.category.name", is("Technology")))
                .andExpect(jsonPath("$.category.slug", is("TECHNOLOGY")))
                .andExpect(jsonPath("$.category.active", is(true)));

        verify(categoryService).findById(1L);
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Deve retornar 404 quando categoria não existe")
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        // Given
        when(categoryService.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/categories/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categoryService).findById(999L);
    }

    @Test
    @DisplayName("GET /api/categories/{id} - Deve retornar 404 quando categoria está inativa")
    void shouldReturn404WhenCategoryIsInactive() throws Exception {
        // Given
        when(categoryService.findById(3L)).thenReturn(Optional.of(inactiveCategory));

        // When & Then
        mockMvc.perform(get("/api/categories/3")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categoryService).findById(3L);
    }

    @Test
    @DisplayName("GET /api/categories/slug/{slug} - Deve retornar categoria ativa por slug")
    void shouldGetCategoryBySlugWhenActive() throws Exception {
        // Given
        when(categoryService.findBySlug("TECHNOLOGY")).thenReturn(Optional.of(techCategory));

        // When & Then
        mockMvc.perform(get("/api/categories/slug/TECHNOLOGY")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.category.id", is(1)))
                .andExpect(jsonPath("$.category.name", is("Technology")))
                .andExpect(jsonPath("$.category.slug", is("TECHNOLOGY")))
                .andExpect(jsonPath("$.category.active", is(true)));

        verify(categoryService).findBySlug("TECHNOLOGY");
    }

    @Test
    @DisplayName("GET /api/categories/slug/{slug} - Deve retornar 404 quando slug não existe")
    void shouldReturn404WhenSlugNotFound() throws Exception {
        // Given
        when(categoryService.findBySlug("NONEXISTENT")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/categories/slug/NONEXISTENT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(categoryService).findBySlug("NONEXISTENT");
    }

    @Test
    @DisplayName("GET /api/categories/search - Deve pesquisar categorias por termo")
    void shouldSearchCategoriesByQuery() throws Exception {
        // Given
        List<Category> searchResults = Arrays.asList(techCategory);
        when(categoryService.search("tech")).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/api/categories/search")
                .param("query", "tech")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.categories[0].name", is("Technology")))
                .andExpect(jsonPath("$.query", is("tech")))
                .andExpect(jsonPath("$.count", is(1)));

        verify(categoryService).search("tech");
    }

    @Test
    @DisplayName("GET /api/categories/search - Deve retornar erro quando pesquisa falha")
    void shouldReturnErrorWhenSearchFails() throws Exception {
        // Given
        when(categoryService.search(anyString())).thenThrow(new RuntimeException("Search error"));

        // When & Then
        mockMvc.perform(get("/api/categories/search")
                .param("query", "tech")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao pesquisar categorias")));

        verify(categoryService).search("tech");
    }

    @Test
    @DisplayName("GET /api/categories/with-articles - Deve retornar categorias com artigos publicados")
    void shouldGetCategoriesWithArticles() throws Exception {
        // Given
        List<Category> categoriesWithArticles = Arrays.asList(techCategory, businessCategory);
        when(categoryService.findCategoriesWithPublishedArticles()).thenReturn(categoriesWithArticles);

        // When & Then
        mockMvc.perform(get("/api/categories/with-articles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.categories", hasSize(2)))
                .andExpect(jsonPath("$.categories[0].name", is("Technology")))
                .andExpect(jsonPath("$.categories[1].name", is("Business")))
                .andExpect(jsonPath("$.count", is(2)));

        verify(categoryService).findCategoriesWithPublishedArticles();
    }

    @Test
    @DisplayName("GET /api/categories/stats - Deve retornar estatísticas das categorias")
    void shouldGetCategoryStats() throws Exception {
        // Given
        when(categoryService.count()).thenReturn(10L);
        when(categoryService.countActive()).thenReturn(8L);

        // When & Then
        mockMvc.perform(get("/api/categories/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.totalCategories", is(10)))
                .andExpect(jsonPath("$.activeCategories", is(8)))
                .andExpect(jsonPath("$.inactiveCategories", is(2)));

        verify(categoryService).count();
        verify(categoryService).countActive();
    }

    @Test
    @DisplayName("GET /api/categories/stats - Deve retornar erro quando falha ao obter estatísticas")
    void shouldReturnErrorWhenStatsFailure() throws Exception {
        // Given
        when(categoryService.count()).thenThrow(new RuntimeException("Stats error"));

        // When & Then
        mockMvc.perform(get("/api/categories/stats")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Erro ao obter estatísticas")));

        verify(categoryService).count();
    }

    @Test
    @DisplayName("GET /api/categories - Deve aplicar ordenação descendente")
    void shouldApplyDescendingSort() throws Exception {
        // Given
        List<Category> activeCategories = Arrays.asList(businessCategory, techCategory);
        when(categoryService.findAllActive()).thenReturn(activeCategories);

        // When & Then
        mockMvc.perform(get("/api/categories")
                .param("sortBy", "name")
                .param("sortDir", "desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.categories", hasSize(2)));

        verify(categoryService).findAllActive();
    }

    @Test
    @DisplayName("GET /api/categories - Deve aplicar paginação com página 1")
    void shouldApplyPaginationSecondPage() throws Exception {
        // Given
        List<Category> activeCategories = Arrays.asList(techCategory, businessCategory);
        when(categoryService.findAllActive()).thenReturn(activeCategories);

        // When & Then
        mockMvc.perform(get("/api/categories")
                .param("page", "1")
                .param("size", "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.categories", hasSize(1)))
                .andExpect(jsonPath("$.currentPage", is(1)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.hasNext", is(false)))
                .andExpect(jsonPath("$.hasPrevious", is(true)));

        verify(categoryService).findAllActive();
    }

    /**
     * Método auxiliar para criar categorias de teste
     */
    private Category createTestCategory(Long id, String name, String slug, boolean active) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription("Description for " + name);
        category.setActive(active);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
}