package br.com.technews.repository;

import br.com.technews.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Testes de integração para CategoryRepository
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Category Repository Tests")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("Deve salvar e buscar categoria por nome")
    void shouldSaveAndFindByName() {
        // Given
        Category category = new Category();
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");
        category.setSlug("tecnologia");
        category.setColor("#FF0000");
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        // When
        Category saved = categoryRepository.save(category);
        Optional<Category> found = categoryRepository.findByName("Tecnologia");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Tecnologia");
        assertThat(found.get().getDescription()).isEqualTo("Categoria de tecnologia");
    }

    @Test
    @DisplayName("Deve retornar vazio quando categoria não existe")
    void shouldReturnEmptyWhenCategoryNotExists() {
        // When
        Optional<Category> found = categoryRepository.findByName("Inexistente");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve contar total de categorias")
    void shouldCountCategories() {
        // Given
        Category category1 = new Category();
        category1.setName("AI");
        category1.setDescription("Inteligência Artificial");
        category1.setSlug("ai");
        category1.setColor("#00FF00");
        category1.setActive(true);
        category1.setCreatedAt(LocalDateTime.now());
        category1.setUpdatedAt(LocalDateTime.now());

        Category category2 = new Category();
        category2.setName("Blockchain");
        category2.setDescription("Tecnologia Blockchain");
        category2.setSlug("blockchain");
        category2.setColor("#0000FF");
        category2.setActive(true);
        category2.setCreatedAt(LocalDateTime.now());
        category2.setUpdatedAt(LocalDateTime.now());

        // When
        categoryRepository.save(category1);
        categoryRepository.save(category2);
        long count = categoryRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}