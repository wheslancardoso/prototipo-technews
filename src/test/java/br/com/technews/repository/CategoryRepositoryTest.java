package br.com.technews.repository;

import br.com.technews.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para CategoryRepository
 * Testa operações de banco de dados com dados reais
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Category Repository Integration Tests")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category techCategory;
    private Category businessCategory;
    private Category inactiveCategory;

    @BeforeEach
    void setUp() {
        // Limpar dados existentes
        categoryRepository.deleteAll();
        entityManager.flush();
        
        // Criar categorias de teste
        techCategory = createCategory("Tecnologia", "tecnologia", "Categoria sobre tecnologia", "#FF0000", true);
        businessCategory = createCategory("Negócios", "negocios", "Categoria sobre negócios", "#00FF00", true);
        inactiveCategory = createCategory("Inativa", "inativa", "Categoria inativa", "#0000FF", false);
        
        entityManager.persistAndFlush(techCategory);
        entityManager.persistAndFlush(businessCategory);
        entityManager.persistAndFlush(inactiveCategory);
    }

    @Test
    @DisplayName("Deve salvar e encontrar categoria por nome")
    void shouldSaveAndFindByName() {
        // When
        Optional<Category> foundCategory = categoryRepository.findByName("Tecnologia");

        // Then
        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getName()).isEqualTo("Tecnologia");
        assertThat(foundCategory.get().getDescription()).isEqualTo("Categoria sobre tecnologia");
        assertThat(foundCategory.get().getSlug()).isEqualTo("tecnologia");
    }

    @Test
    @DisplayName("Deve retornar vazio quando categoria não existe")
    void shouldReturnEmptyWhenCategoryNotExists() {
        // When
        Optional<Category> foundCategory = categoryRepository.findByName("Inexistente");

        // Then
        assertThat(foundCategory).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar categoria por slug")
    void shouldFindBySlug() {
        // When
        Optional<Category> foundCategory = categoryRepository.findBySlug("tecnologia");

        // Then
        assertThat(foundCategory).isPresent();
        assertThat(foundCategory.get().getName()).isEqualTo("Tecnologia");
        assertThat(foundCategory.get().getSlug()).isEqualTo("tecnologia");
    }

    @Test
    @DisplayName("Deve encontrar categoria por nome ignorando case")
    void shouldFindByNameIgnoreCase() {
        // When
        Optional<Category> foundCategory1 = categoryRepository.findByNameIgnoreCase("TECNOLOGIA");
        Optional<Category> foundCategory2 = categoryRepository.findByNameIgnoreCase("tecnologia");
        Optional<Category> foundCategory3 = categoryRepository.findByNameIgnoreCase("TeCnOlOgIa");

        // Then
        assertThat(foundCategory1).isPresent();
        assertThat(foundCategory2).isPresent();
        assertThat(foundCategory3).isPresent();
        assertThat(foundCategory1.get().getName()).isEqualTo("Tecnologia");
        assertThat(foundCategory2.get().getName()).isEqualTo("Tecnologia");
        assertThat(foundCategory3.get().getName()).isEqualTo("Tecnologia");
    }

    @Test
    @DisplayName("Deve encontrar apenas categorias ativas")
    void shouldFindByActiveTrue() {
        // When
        List<Category> activeCategories = categoryRepository.findByActiveTrue();

        // Then
        assertThat(activeCategories).hasSize(2);
        assertThat(activeCategories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Tecnologia", "Negócios");
        assertThat(activeCategories).allMatch(category -> category.getActive());
    }

    @Test
    @DisplayName("Deve encontrar categorias ativas ordenadas por nome")
    void shouldFindByActiveTrueOrderByNameAsc() {
        // When
        List<Category> activeCategories = categoryRepository.findByActiveTrueOrderByNameAsc();

        // Then
        assertThat(activeCategories).hasSize(2);
        assertThat(activeCategories.get(0).getName()).isEqualTo("Negócios");
        assertThat(activeCategories.get(1).getName()).isEqualTo("Tecnologia");
        assertThat(activeCategories).allMatch(category -> category.getActive());
    }

    @Test
    @DisplayName("Deve verificar se existe categoria por nome ignorando case e ID")
    void shouldCheckExistsByNameIgnoreCaseAndIdNot() {
        // When & Then
        // Deve retornar true para nome existente com ID diferente
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("TECNOLOGIA", 999L)).isTrue();
        
        // Deve retornar false para o mesmo ID
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("Tecnologia", techCategory.getId())).isFalse();
        
        // Deve retornar false para nome inexistente
        assertThat(categoryRepository.existsByNameIgnoreCaseAndIdNot("Inexistente", null)).isFalse();
    }

    @Test
    @DisplayName("Deve verificar se existe categoria por slug e ID")
    void shouldCheckExistsBySlugAndIdNot() {
        // When & Then
        // Deve retornar true para slug existente com ID diferente
        assertThat(categoryRepository.existsBySlugAndIdNot("tecnologia", 999L)).isTrue();
        
        // Deve retornar false para o mesmo ID
        assertThat(categoryRepository.existsBySlugAndIdNot("tecnologia", techCategory.getId())).isFalse();
        
        // Deve retornar false para slug inexistente
        assertThat(categoryRepository.existsBySlugAndIdNot("inexistente", null)).isFalse();
    }

    @Test
    @DisplayName("Deve buscar categorias por termo de pesquisa")
    void shouldFindBySearchTerm() {
        // When
        List<Category> techResults = categoryRepository.findBySearchTerm("tecno");
        List<Category> businessResults = categoryRepository.findBySearchTerm("negócio");
        List<Category> descriptionResults = categoryRepository.findBySearchTerm("sobre");

        // Then
        assertThat(techResults).hasSize(1);
        assertThat(techResults.get(0).getName()).isEqualTo("Tecnologia");
        
        assertThat(businessResults).hasSize(1);
        assertThat(businessResults.get(0).getName()).isEqualTo("Negócios");
        
        // Deve encontrar por descrição (ambas contêm "sobre")
        assertThat(descriptionResults).hasSize(2);
        assertThat(descriptionResults).extracting(Category::getName)
                .containsExactlyInAnyOrder("Tecnologia", "Negócios");
    }

    @Test
    @DisplayName("Deve contar categorias")
    void shouldCountCategories() {
        // When
        long count = categoryRepository.count();

        // Then
        assertThat(count).isEqualTo(3); // 2 ativas + 1 inativa
    }

    @Test
    @DisplayName("Deve contar artigos por categoria")
    void shouldCountArticlesByCategory() {
        // When
        List<Object[]> results = categoryRepository.countArticlesByCategory();

        // Then
        assertThat(results).isNotNull();
        // Como não temos artigos nos dados de teste, cada categoria deve ter 0 artigos
        assertThat(results).hasSize(2); // Apenas categorias ativas
        
        for (Object[] result : results) {
            Long categoryId = (Long) result[0];
            Long articleCount = (Long) result[1];
            assertThat(categoryId).isNotNull();
            assertThat(articleCount).isEqualTo(0L);
        }
    }

    @Test
    @DisplayName("Deve buscar categorias com artigos publicados")
    void shouldFindCategoriesWithPublishedArticles() {
        // When
        List<Category> categoriesWithArticles = categoryRepository.findCategoriesWithPublishedArticles();

        // Then
        // Como não temos artigos nos dados de teste, deve retornar lista vazia
        assertThat(categoriesWithArticles).isEmpty();
    }

    private Category createCategory(String name, String slug, String description, String color, boolean active) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setColor(color);
        category.setActive(active);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }
}