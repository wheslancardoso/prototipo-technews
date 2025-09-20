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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(br.com.technews.controller.TestSecurityConfig.class)
@TestPropertySource(properties = {
    "spring.thymeleaf.check-template=false",
    "spring.thymeleaf.check-template-location=false",
    "spring.thymeleaf.mode=HTML",
    "spring.thymeleaf.enabled=false"
})
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

        // When & Then
        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("categories", categoryPage))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 1));

        verify(categoryService).findAll(any(Pageable.class));
    }

    @Test
    void shouldShowCreateForm() throws Exception {
        // When & Then
        mockMvc.perform(get("/admin/categories/new"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attributeExists("category"));
    }

    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        // Given
        Category category = createTestCategory(1L, "Technology", "TECHNOLOGY");
        when(categoryService.save(any(Category.class))).thenReturn(category);

        // When & Then
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "Technology")
                .param("slug", "TECHNOLOGY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("successMessage", 
                    "Categoria 'Technology' criada com sucesso!"));

        verify(categoryService).save(any(Category.class));
    }

    @Test
    void shouldCreateCategoryWithValidationErrors() throws Exception {
        // When & Then
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "")
                .param("slug", ""))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isEdit", false))
                .andExpect(model().attributeHasFieldErrors("category", "name"));

        verify(categoryService, never()).save(any(Category.class));
    }

    @Test
    void shouldShowEditForm() throws Exception {
        // Given
        Category category = createTestCategory(1L, "Technology", "TECHNOLOGY");
        when(categoryService.findById(1L)).thenReturn(Optional.of(category));

        // When & Then
        mockMvc.perform(get("/admin/categories/edit/1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("category", category))
                .andExpect(model().attribute("isEdit", true));

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
        
        when(categoryService.update(eq(1L), any(Category.class))).thenReturn(updatedCategory);

        // When & Then
        mockMvc.perform(post("/admin/categories/edit/1")
                .param("id", "1")
                .param("name", "Technology Updated")
                .param("slug", "TECHNOLOGY"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"))
                .andExpect(flash().attribute("successMessage", 
                    "Categoria 'Technology Updated' atualizada com sucesso!"));

        verify(categoryService).update(eq(1L), any(Category.class));
    }

    @Test
    void shouldHandleUpdateCategoryNotFound() throws Exception {
        // Given
        when(categoryService.update(eq(1L), any(Category.class)))
                .thenThrow(new RuntimeException("Categoria não encontrada com ID: 1"));

        // When & Then
        mockMvc.perform(post("/admin/categories/edit/1")
                .param("id", "1")
                .param("name", "Technology Updated")
                .param("slug", "TECHNOLOGY"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attribute("errorMessage", "Erro ao atualizar categoria: Categoria não encontrada com ID: 1"));

        verify(categoryService).update(eq(1L), any(Category.class));
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

    private Category createTestCategory(Long id, String name, String slug) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setSlug(slug);
        return category;
    }
}