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

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");
    }

    @Test
    void testGetAllCategories() {
        // Given
        List<Category> categories = Arrays.asList(category);
        when(categoryRepository.findAll()).thenReturn(categories);

        // When
        List<Category> result = categoryService.getAllCategories();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(category);
        verify(categoryRepository).findAll();
    }

    @Test
    void testGetCategoryByName() {
        // Given
        when(categoryRepository.findByName("Tecnologia")).thenReturn(Optional.of(category));

        // When
        Optional<Category> result = categoryService.getCategoryByName("Tecnologia");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(category);
        verify(categoryRepository).findByName("Tecnologia");
    }

    @Test
    void testGetCategoryByNameNotFound() {
        // Given
        when(categoryRepository.findByName("Inexistente")).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryService.getCategoryByName("Inexistente");

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findByName("Inexistente");
    }

    @Test
    void testCreateCategory() {
        // Given
        Category newCategory = new Category();
        newCategory.setName("Nova Categoria");
        newCategory.setDescription("Descrição da nova categoria");

        when(categoryRepository.findByName("Nova Categoria")).thenReturn(Optional.empty());
        when(categoryRepository.save(newCategory)).thenReturn(newCategory);

        // When
        Category result = categoryService.createCategory(newCategory);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(newCategory);
        verify(categoryRepository).findByName("Nova Categoria");
        verify(categoryRepository).save(newCategory);
    }

    @Test
    void testCreateCategoryAlreadyExists() {
        // Given
        Category existingCategory = new Category();
        existingCategory.setName("Tecnologia");
        existingCategory.setDescription("Nova descrição");

        when(categoryRepository.findByName("Tecnologia")).thenReturn(Optional.of(category));

        // When & Then
        assertThatThrownBy(() -> categoryService.createCategory(existingCategory))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoria já existe");

        verify(categoryRepository).findByName("Tecnologia");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testUpdateCategory() {
        // Given
        Long categoryId = 1L;
        Category updatedCategory = new Category();
        updatedCategory.setName("Tecnologia Atualizada");
        updatedCategory.setDescription("Descrição atualizada");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class))).thenReturn(category);

        // When
        Category result = categoryService.updateCategory(categoryId, updatedCategory);

        // Then
        assertThat(result).isNotNull();
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testUpdateCategoryNotFound() {
        // Given
        Long categoryId = 999L;
        Category updatedCategory = new Category();
        updatedCategory.setName("Categoria Inexistente");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, updatedCategory))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoria não encontrada");

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testDeleteCategory() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        categoryService.deleteCategory(categoryId);

        // Then
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void testDeleteCategoryNotFound() {
        // Given
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoria não encontrada");

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    void testFindOrCreateCategory() {
        // Given
        String categoryName = "Nova Categoria";
        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.empty());
        
        Category newCategory = new Category();
        newCategory.setName(categoryName);
        newCategory.setDescription("Categoria criada automaticamente");
        
        when(categoryRepository.save(any(Category.class))).thenReturn(newCategory);

        // When
        Category result = categoryService.findOrCreateCategory(categoryName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(categoryName);
        verify(categoryRepository).findByName(categoryName);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void testFindOrCreateCategoryExisting() {
        // Given
        String categoryName = "Tecnologia";
        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(category));

        // When
        Category result = categoryService.findOrCreateCategory(categoryName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(category);
        verify(categoryRepository).findByName(categoryName);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testGetCategoryById() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        // When
        Optional<Category> result = categoryService.getCategoryById(categoryId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(category);
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    void testGetCategoryByIdNotFound() {
        // Given
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryService.getCategoryById(categoryId);

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findById(categoryId);
    }
}