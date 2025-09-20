package br.com.technews.repository;

import br.com.technews.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldSaveAndFindByName() {
        // Given
        Category category = new Category();
        category.setName("Tecnologia");
        category.setDescription("Categoria sobre tecnologia");
        category.setSlug("tecnologia");
        category.setColor("#FF0000");
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        // When
        Category savedCategory = categoryRepository.save(category);
        Optional<Category> foundCategory = categoryRepository.findByName("Tecnologia");

        // Then
        assertThat(savedCategory).isNotNull();
        assertThat(savedCategory.getId()).isNotNull();
        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getName()).isEqualTo("Tecnologia");
        assertThat(foundCategory.get().getDescription()).isEqualTo("Categoria sobre tecnologia");
    }

    @Test
    void shouldReturnEmptyWhenCategoryNotExists() {
        // When
        Optional<Category> foundCategory = categoryRepository.findByName("Inexistente");

        // Then
        assertThat(foundCategory).isEmpty();
    }

    @Test
    void shouldCountCategories() {
        // Given
        Category category1 = new Category();
        category1.setName("Tech");
        category1.setSlug("tech");
        category1.setActive(true);
        category1.setCreatedAt(LocalDateTime.now());
        category1.setUpdatedAt(LocalDateTime.now());

        Category category2 = new Category();
        category2.setName("Science");
        category2.setSlug("science");
        category2.setActive(true);
        category2.setCreatedAt(LocalDateTime.now());
        category2.setUpdatedAt(LocalDateTime.now());

        entityManager.persistAndFlush(category1);
        entityManager.persistAndFlush(category2);

        // When
        long count = categoryRepository.count();

        // Then
        assertThat(count).isEqualTo(2);
    }
}