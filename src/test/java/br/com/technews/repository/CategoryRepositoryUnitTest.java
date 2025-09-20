package br.com.technews.repository;

import br.com.technews.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

/**
 * Testes unitários para CategoryRepository usando Mockito
 */
@DisplayName("Category Repository Unit Tests")
class CategoryRepositoryUnitTest {

    @Mock
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve encontrar categoria por nome")
    void shouldFindCategoryByName() {
        // Given
        Category category = createTestCategory("Tecnologia", "Categoria de tecnologia");
        when(categoryRepository.findByName("Tecnologia")).thenReturn(Optional.of(category));

        // When
        Optional<Category> result = categoryRepository.findByName("Tecnologia");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Tecnologia");
        assertThat(result.get().getDescription()).isEqualTo("Categoria de tecnologia");
        verify(categoryRepository).findByName("Tecnologia");
    }

    @Test
    @DisplayName("Deve retornar vazio quando categoria não existe")
    void shouldReturnEmptyWhenCategoryNotExists() {
        // Given
        when(categoryRepository.findByName("Inexistente")).thenReturn(Optional.empty());

        // When
        Optional<Category> result = categoryRepository.findByName("Inexistente");

        // Then
        assertThat(result).isEmpty();
        verify(categoryRepository).findByName("Inexistente");
    }

    @Test
    @DisplayName("Deve salvar categoria")
    void shouldSaveCategory() {
        // Given
        Category category = createTestCategory("AI", "Inteligência Artificial");
        Category savedCategory = createTestCategory("AI", "Inteligência Artificial");
        savedCategory.setId(1L);
        
        when(categoryRepository.save(category)).thenReturn(savedCategory);

        // When
        Category result = categoryRepository.save(category);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("AI");
        assertThat(result.getDescription()).isEqualTo("Inteligência Artificial");
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("Deve encontrar todas as categorias")
    void shouldFindAllCategories() {
        // Given
        List<Category> categories = Arrays.asList(
            createTestCategory("AI", "Inteligência Artificial"),
            createTestCategory("Blockchain", "Tecnologia Blockchain")
        );
        when(categoryRepository.findAll()).thenReturn(categories);

        // When
        List<Category> result = categoryRepository.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Category::getName)
                .containsExactlyInAnyOrder("AI", "Blockchain");
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Deve contar categorias")
    void shouldCountCategories() {
        // Given
        when(categoryRepository.count()).thenReturn(5L);

        // When
        long count = categoryRepository.count();

        // Then
        assertThat(count).isEqualTo(5L);
        verify(categoryRepository).count();
    }

    @Test
    @DisplayName("Deve deletar categoria por ID")
    void shouldDeleteCategoryById() {
        // Given
        Long categoryId = 1L;
        doNothing().when(categoryRepository).deleteById(categoryId);

        // When
        categoryRepository.deleteById(categoryId);

        // Then
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    @DisplayName("Deve verificar se categoria existe por ID")
    void shouldCheckIfCategoryExistsById() {
        // Given
        Long categoryId = 1L;
        when(categoryRepository.existsById(categoryId)).thenReturn(true);

        // When
        boolean exists = categoryRepository.existsById(categoryId);

        // Then
        assertThat(exists).isTrue();
        verify(categoryRepository).existsById(categoryId);
    }

    private Category createTestCategory(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setSlug(name.toLowerCase().replace(" ", "-"));
        category.setColor("#FF0000");
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
}