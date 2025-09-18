package br.com.technews.repository;

import br.com.technews.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

/**
 * Testes de integração para CategoryRepository
 */
@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void testFindByName() {
        // Given
        Category category = new Category();
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");
        entityManager.persistAndFlush(category);

        // When
        Optional<Category> found = categoryRepository.findByName("Tecnologia");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Tecnologia");
        assertThat(found.get().getDescription()).isEqualTo("Categoria de tecnologia");
    }

    @Test
    void testFindByNameNotFound() {
        // When
        Optional<Category> found = categoryRepository.findByName("Inexistente");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testSaveCategory() {
        // Given
        Category category = new Category();
        category.setName("Inteligência Artificial");
        category.setDescription("Notícias sobre IA");

        // When
        Category saved = categoryRepository.save(category);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Inteligência Artificial");
        assertThat(saved.getDescription()).isEqualTo("Notícias sobre IA");
    }

    @Test
    void testFindAll() {
        // Given
        Category category1 = new Category();
        category1.setName("Blockchain");
        category1.setDescription("Tecnologia blockchain");

        Category category2 = new Category();
        category2.setName("Machine Learning");
        category2.setDescription("Aprendizado de máquina");

        entityManager.persist(category1);
        entityManager.persist(category2);
        entityManager.flush();

        // When
        List<Category> categories = categoryRepository.findAll();

        // Then
        assertThat(categories).hasSize(2);
        assertThat(categories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Blockchain", "Machine Learning");
    }

    @Test
    void testDeleteCategory() {
        // Given
        Category category = new Category();
        category.setName("Categoria Temporária");
        category.setDescription("Para teste de exclusão");
        Category saved = entityManager.persistAndFlush(category);

        // When
        categoryRepository.deleteById(saved.getId());
        entityManager.flush();

        // Then
        Optional<Category> found = categoryRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void testUpdateCategory() {
        // Given
        Category category = new Category();
        category.setName("Nome Original");
        category.setDescription("Descrição Original");
        Category saved = entityManager.persistAndFlush(category);

        // When
        saved.setName("Nome Atualizado");
        saved.setDescription("Descrição Atualizada");
        Category updated = categoryRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getName()).isEqualTo("Nome Atualizado");
        assertThat(updated.getDescription()).isEqualTo("Descrição Atualizada");
    }
}