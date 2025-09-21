package br.com.technews.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para a entidade Category
 */
class CategoryTest {

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
    }

    @Test
    void testCategoryCreation() {
        // Given
        String name = "Tecnologia";
        String description = "Categoria sobre tecnologia";

        // When
        category.setName(name);
        category.setDescription(description);

        // Then
        assertThat(category.getName()).isEqualTo(name);
        assertThat(category.getDescription()).isEqualTo(description);
        assertThat(category.getId()).isNull();
    }

    @Test
    void testCategoryWithId() {
        // Given
        Long id = 1L;
        String name = "Ciência";

        // When
        category.setId(id);
        category.setName(name);

        // Then
        assertThat(category.getId()).isEqualTo(id);
        assertThat(category.getName()).isEqualTo(name);
    }

    @Test
    void testCategoryEquality() {
        // Given
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Tecnologia");

        Category category2 = new Category();
        category2.setId(1L);
        category2.setName("Tecnologia");

        // Then
        assertThat(category1).isEqualTo(category2);
        assertThat(category1.hashCode()).isEqualTo(category2.hashCode());
    }

    @Test
    void testCategoryToString() {
        // Given
        category.setId(1L);
        category.setName("Tecnologia");
        category.setDescription("Categoria sobre tecnologia");

        // When
        String toString = category.toString();

        // Then
        assertThat(toString).contains("Tecnologia");
        assertThat(toString).contains("1");
    }

    @Test
    void testCategoryWithNullValues() {
        // Given/When
        category.setName(null);
        category.setDescription(null);

        // Then
        assertThat(category.getName()).isNull();
        assertThat(category.getDescription()).isNull();
    }
}