package br.com.technews.controller.admin;

import br.com.technews.entity.Category;
import br.com.technews.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerUnitTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private Model model;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private CategoryController categoryController;

    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Technology");
        testCategory.setSlug("TECHNOLOGY");
        testCategory.setDescription("Technology category");
        testCategory.setActive(true);
        testCategory.setCreatedAt(LocalDateTime.now());
        testCategory.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void shouldListCategoriesWithPagination() {
        // Given
        List<Category> categories = Arrays.asList(testCategory);
        Page<Category> categoryPage = new PageImpl<>(categories, PageRequest.of(0, 10), 1);
        when(categoryService.findAll(any(Pageable.class))).thenReturn(categoryPage);

        // When
        String viewName = categoryController.listCategories(0, 10, "name", "asc", null, model);

        // Then
        assertThat(viewName).isEqualTo("admin/categories/list");
        verify(model).addAttribute("categories", categoryPage);
        verify(model).addAttribute("currentPage", 0);
        verify(model).addAttribute("totalPages", 1);
        verify(categoryService).findAll(any(Pageable.class));
    }

    @Test
    void shouldShowCreateForm() {
        // When
        String viewName = categoryController.showCreateForm(model);

        // Then
        assertThat(viewName).isEqualTo("admin/categories/form");
        verify(model).addAttribute(eq("category"), any(Category.class));
        verify(model).addAttribute("isEdit", false);
    }

    @Test
    void shouldCreateCategorySuccessfully() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(categoryService.save(any(Category.class))).thenReturn(testCategory);

        // When
        String viewName = categoryController.createCategory(testCategory, bindingResult, redirectAttributes, model);

        // Then
        assertThat(viewName).isEqualTo("redirect:/admin/categories");
        verify(categoryService).save(testCategory);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void shouldCreateCategoryWithValidationErrors() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(true);

        // When
        String viewName = categoryController.createCategory(testCategory, bindingResult, redirectAttributes, model);

        // Then
        assertThat(viewName).isEqualTo("admin/categories/form");
        verify(model).addAttribute("isEdit", false);
        verify(categoryService, never()).save(any(Category.class));
    }

    @Test
    void shouldShowEditForm() {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        String viewName = categoryController.showEditForm(1L, model, redirectAttributes);

        // Then
        assertThat(viewName).isEqualTo("admin/categories/form");
        verify(model).addAttribute("category", testCategory);
        verify(model).addAttribute("isEdit", true);
        verify(categoryService).findById(1L);
    }

    @Test
    void shouldHandleEditFormCategoryNotFound() {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.empty());

        // When
        String viewName = categoryController.showEditForm(1L, model, redirectAttributes);

        // Then
        assertThat(viewName).isEqualTo("redirect:/admin/categories");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Categoria não encontrada!");
        verify(categoryService).findById(1L);
    }

    @Test
    void shouldUpdateCategorySuccessfully() {
        // Given
        when(bindingResult.hasErrors()).thenReturn(false);
        when(categoryService.update(eq(1L), any(Category.class))).thenReturn(testCategory);

        // When
        String viewName = categoryController.updateCategory(1L, testCategory, bindingResult, redirectAttributes, model);

        // Then
        assertThat(viewName).isEqualTo("redirect:/admin/categories");
        verify(categoryService).update(1L, testCategory);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void shouldViewCategoryDetails() {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        String viewName = categoryController.viewCategory(1L, model, redirectAttributes);

        // Then
        assertThat(viewName).isEqualTo("admin/categories/view");
        verify(model).addAttribute("category", testCategory);
        verify(categoryService).findById(1L);
    }

    @Test
    void shouldHandleViewCategoryNotFound() {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.empty());

        // When
        String viewName = categoryController.viewCategory(1L, model, redirectAttributes);

        // Then
        assertThat(viewName).isEqualTo("redirect:/admin/categories");
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Categoria não encontrada!");
        verify(categoryService).findById(1L);
    }

    @Test
    void shouldToggleCategoryStatus() {
        // Given
        when(categoryService.toggleActive(1L)).thenReturn(testCategory);

        // When
        String viewName = categoryController.toggleCategory(1L, redirectAttributes);

        // Then
        assertThat(viewName).isEqualTo("redirect:/admin/categories");
        verify(categoryService).toggleActive(1L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }

    @Test
    void shouldDeleteCategory() {
        // Given
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        // When
        String viewName = categoryController.deleteCategory(1L, redirectAttributes);

        // Then
        assertThat(viewName).isEqualTo("redirect:/admin/categories");
        verify(categoryService).findById(1L);
        verify(categoryService).delete(1L);
        verify(redirectAttributes).addFlashAttribute(eq("successMessage"), anyString());
    }
}