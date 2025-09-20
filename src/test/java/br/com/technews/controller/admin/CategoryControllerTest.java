package br.com.technews.controller.admin;

import br.com.technews.entity.Category;
import br.com.technews.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(br.com.technews.controller.TestSecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void shouldListCategoriesSuccessfully() throws Exception {
        // Given
        Category category1 = createTestCategory(1L, "Technology", "TECHNOLOGY");
        Category category2 = createTestCategory(2L, "Science", "SCIENCE");
        List<Category> categories = Arrays.asList(category1, category2);
        Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 2);
        
        when(categoryService.findAll(any(Pageable.class))).thenReturn(categoryPage);

        // When & Then - Testando apenas a lógica do controller
        MvcResult result = mockMvc.perform(get("/admin/categories"))
                .andReturn();

        // Verificando se o modelo foi populado corretamente
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        assertThat(modelAndView.getModel().get("categories")).isEqualTo(categoryPage);
        assertThat(modelAndView.getModel().get("currentPage")).isEqualTo(0);
        assertThat(modelAndView.getModel().get("totalPages")).isEqualTo(1);

        verify(categoryService).findAll(any(Pageable.class));
    }

    @Test
    void shouldShowCreateForm() throws Exception {
        // When & Then - Testando apenas a lógica do controller
        MvcResult result = mockMvc.perform(get("/admin/categories/new"))
                .andReturn();

        // Verificando se o modelo foi populado corretamente
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        assertThat(modelAndView.getModel().get("isEdit")).isEqualTo(false);
        assertThat(modelAndView.getModel().get("category")).isNotNull();
    }

    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        // Given
        Category category = createTestCategory(1L, "Technology", "TECHNOLOGY");
        when(categoryService.save(any(Category.class))).thenReturn(category);

        // When & Then
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "Technology")
                .param("slug", "TECHNOLOGY")
                .param("description", "Technology category")
                .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("successMessage", 
                    "Categoria 'Technology' criada com sucesso!"));

        verify(categoryService).save(any(Category.class));
    }

    @Test
    void shouldHandleCreateCategoryWithValidationErrors() throws Exception {
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "") // Nome vazio para gerar erro de validação
                .param("slug", ""))
                .andExpect(model().attribute("isEdit", false));

        verify(categoryService, never()).save(any(Category.class));
    }

    @Test
    void shouldHandleCreateCategoryWithException() throws Exception {
        // Given
        when(categoryService.save(any(Category.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "Technology")
                .param("slug", "TECHNOLOGY")
                .param("description", "Technology category")
                .param("active", "true"))
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attribute("errorMessage", "Erro ao criar categoria: Database error"));

        verify(categoryService).save(any(Category.class));
    }

    @Test
    void shouldShowEditForm() throws Exception {
        // Given
        Category category = createTestCategory(1L, "Technology", "TECHNOLOGY");
        when(categoryService.findById(1L)).thenReturn(Optional.of(category));

        // When & Then - Testando apenas a lógica do controller
        MvcResult result = mockMvc.perform(get("/admin/categories/edit/1"))
                .andReturn();

        // Verificando se o modelo foi populado corretamente
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        assertThat(modelAndView.getModel().get("category")).isEqualTo(category);
        assertThat(modelAndView.getModel().get("isEdit")).isEqualTo(true);

        verify(categoryService).findById(1L);
    }

    @Test
    void shouldRedirectWhenCategoryNotFoundForEdit() throws Exception {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/admin/categories/edit/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("errorMessage", "Categoria não encontrada!"));

        verify(categoryService).findById(1L);
    }

    @Test
    void shouldUpdateCategorySuccessfully() throws Exception {
        // Given
        Category existingCategory = createTestCategory(1L, "Technology", "TECHNOLOGY");
        Category updatedCategory = createTestCategory(1L, "Technology Updated", "TECHNOLOGY");
        
        when(categoryService.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryService.save(any(Category.class))).thenReturn(updatedCategory);

        // When & Then - Apenas verificando se o controller processa corretamente
        mockMvc.perform(post("/admin/categories/edit/1")
                .param("id", "1")
                .param("name", "Technology Updated")
                .param("slug", "TECHNOLOGY")
                .param("description", "Updated technology category")
                .param("active", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("successMessage", 
                    "Categoria 'Technology Updated' atualizada com sucesso!"));

        verify(categoryService).findById(1L);
        verify(categoryService).save(any(Category.class));
    }

    @Test
    void shouldHandleUpdateCategoryNotFound() throws Exception {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/admin/categories/edit/1")
                .param("id", "1")
                .param("name", "Technology Updated")
                .param("slug", "TECHNOLOGY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("errorMessage", "Categoria não encontrada!"));

        verify(categoryService).findById(1L);
        verify(categoryService, never()).save(any(Category.class));
    }

    @Test
    void shouldDeleteCategorySuccessfully() throws Exception {
        // Given
        Category category = createTestCategory(1L, "Technology", "TECHNOLOGY");
        when(categoryService.findById(1L)).thenReturn(Optional.of(category));
        doNothing().when(categoryService).delete(1L);

        // When & Then
        mockMvc.perform(post("/admin/categories/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("successMessage", 
                    "Categoria 'Technology' removida com sucesso!"));

        verify(categoryService).findById(1L);
        verify(categoryService).delete(1L);
    }

    @Test
    void shouldHandleDeleteCategoryNotFound() throws Exception {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/admin/categories/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("errorMessage", "Categoria não encontrada!"));

        verify(categoryService).findById(1L);
        verify(categoryService, never()).delete(1L);
    }

    @Test
    void shouldViewCategoryDetails() throws Exception {
        // Given
        Category category = createTestCategory(1L, "Technology", "TECHNOLOGY");
        when(categoryService.findById(1L)).thenReturn(Optional.of(category));

        // When & Then - Testando apenas a lógica do controller
        MvcResult result = mockMvc.perform(get("/admin/categories/view/1"))
                .andReturn();

        // Verificando se o modelo foi populado corretamente
        ModelAndView modelAndView = result.getModelAndView();
        assertThat(modelAndView).isNotNull();
        assertThat(modelAndView.getModel().get("category")).isEqualTo(category);

        verify(categoryService).findById(1L);
    }

    @Test
    void shouldHandleViewCategoryNotFound() throws Exception {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/admin/categories/view/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("errorMessage", "Categoria não encontrada!"));

        verify(categoryService).findById(1L);
    }

    @Test
    void shouldSearchCategories() throws Exception {
        // Given
        List<Category> searchResults = Arrays.asList(
            createTestCategory(1L, "Technology", "TECHNOLOGY")
        );
        when(categoryService.search("tech")).thenReturn(searchResults);

        // When & Then
        mockMvc.perform(get("/admin/categories/api/search")
                .param("term", "tech"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));

        verify(categoryService).search("tech");
    }

    private Category createTestCategory(Long id, String name, String slug) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(name + " category");
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
}