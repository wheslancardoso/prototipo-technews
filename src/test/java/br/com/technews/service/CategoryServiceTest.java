package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Testes unitários para CategoryService
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");
        
        category1 = new Category();
        category1.setId(1L);
        category1.setName("Tecnologia");
        category1.setSlug("tecnologia");
        category1.setActive(true);
        
        category2 = new Category();
        category2.setId(2L);
        category2.setName("Ciência");
        category2.setSlug("ciencia");
        category2.setActive(true);
    }

    @Test
    void testFindAllActive() {
        // Given
        List<Category> categories = Arrays.asList(category1, category2);
        when(categoryRepository.findByActiveTrueOrderByNameAsc()).thenReturn(categories);

        // When
        List<Category> result = categoryService.findAllActive();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(category1, category2);
        verify(categoryRepository).findByActiveTrueOrderByNameAsc();
    }

    @Test
    void testFindBySlug() {
        // Given
        when(categoryRepository.findBySlug(anyString())).thenReturn(Optional.of(category1));

        // When
        Optional<Category> result = categoryService.findBySlug("technology");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(category1);
        verify(categoryRepository).findBySlug("technology");
    }

    @Test
    void testFindBySlugNotFound() {
        // Given
        when(categoryRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryService.findBySlug("inexistente");

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findBySlug("inexistente");
    }

    @Test
    void testSaveCategory() {
        // Given
        Category newCategory = new Category();
        newCategory.setName("Nova Categoria");
        newCategory.setDescription("Descrição da nova categoria");

        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);

        // When
        Category result = categoryService.save(newCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Nova Categoria");
        verify(categoryRepository).save(newCategory);
    }

    @Test
    void testSaveCategoryAlreadyExists() {
        // Given
        Category existingCategory = new Category();
        existingCategory.setName("Tecnologia");

        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        // When
        Category result = categoryService.save(existingCategory);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository).save(existingCategory);
    }

    @Test
    void testUpdateCategory() {
        // Given
        Long categoryId = 1L;
        Category updatedCategory = new Category();
        updatedCategory.setName("Categoria Atualizada");
        updatedCategory.setDescription("Descrição atualizada");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(updatedCategory);

        // When
        Category result = categoryService.update(categoryId, updatedCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Categoria Atualizada");
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testUpdateCategoryNotFound() {
        // Given
        Long categoryId = 999L;
        Category updatedCategory = new Category();
        updatedCategory.setName("Categoria Atualizada");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.update(categoryId, updatedCategory))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testDeleteCategory() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        categoryService.delete(categoryId);

        // Then
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).delete(category);
    }

    @Test
    void testDeleteCategoryNotFound() {
        // Given
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.delete(categoryId))
                .isInstanceOf(RuntimeException.class);

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void testSearch() {
        // Given
        String searchTerm = "Tecnologia";
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findBySearchTerm(searchTerm)).thenReturn(categories);

        // When
        List<Category> result = categoryService.search(searchTerm);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(category);
        verify(categoryRepository).findBySearchTerm(searchTerm);
    }

    @Test
    void testFindCategoriesWithPublishedArticles() {
        // Given
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findCategoriesWithPublishedArticles()).thenReturn(categories);

        // When
        List<Category> result = categoryService.findCategoriesWithPublishedArticles();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(category);
        verify(categoryRepository).findCategoriesWithPublishedArticles();
    }

    @Test
    void testFindById() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        Optional<Category> result = categoryService.findById(categoryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(category);
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void testFindBySlugWithCategory() {
        // Given
        String slug = "tecnologia";
        when(categoryRepository.findBySlug(slug)).thenReturn(Optional.of(category));

        // When
        Optional<Category> result = categoryService.findBySlug(slug);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(category);
        verify(categoryRepository).findBySlug(slug);
    }
}