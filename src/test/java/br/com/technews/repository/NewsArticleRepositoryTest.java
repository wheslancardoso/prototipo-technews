package br.com.technews.repository;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Testes de integração para NewsArticleRepository
 */
@DataJpaTest
class NewsArticleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");
        category = entityManager.persistAndFlush(category);
    }

    @Test
    void testFindByUrl() {
        // Given
        NewsArticle article = createNewsArticle("Título", "https://example.com/news/1");
        entityManager.persistAndFlush(article);

        // When
        Optional<NewsArticle> found = newsArticleRepository.findByUrl("https://example.com/news/1");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUrl()).isEqualTo("https://example.com/news/1");
        assertThat(found.get().getTitle()).isEqualTo("Título");
    }

    @Test
    void testFindByUrlNotFound() {
        // When
        Optional<NewsArticle> found = newsArticleRepository.findByUrl("https://nonexistent.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByCategory() {
        // Given
        NewsArticle article1 = createNewsArticle("Artigo 1", "https://example.com/1");
        NewsArticle article2 = createNewsArticle("Artigo 2", "https://example.com/2");
        
        Category otherCategory = new Category();
        otherCategory.setName("Ciência");
        otherCategory = entityManager.persist(otherCategory);
        
        NewsArticle article3 = createNewsArticle("Artigo 3", "https://example.com/3");
        article3.setCategory(otherCategory);

        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.persist(article3);
        entityManager.flush();

        // When
        List<NewsArticle> articles = newsArticleRepository.findByCategory(category);

        // Then
        assertThat(articles).hasSize(2);
        assertThat(articles).extracting(NewsArticle::getTitle)
                .containsExactlyInAnyOrder("Artigo 1", "Artigo 2");
    }

    @Test
    void testFindByCategoryOrderByPublishedAtDesc() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        
        NewsArticle article1 = createNewsArticle("Artigo Antigo", "https://example.com/old");
        article1.setPublishedAt(now.minusDays(2));
        
        NewsArticle article2 = createNewsArticle("Artigo Novo", "https://example.com/new");
        article2.setPublishedAt(now);

        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.flush();

        PageRequest pageRequest = PageRequest.of(0, 10);

        // When
        Page<NewsArticle> articles = newsArticleRepository.findByCategoryOrderByPublishedAtDesc(category, pageRequest);

        // Then
        assertThat(articles.getContent()).hasSize(2);
        assertThat(articles.getContent().get(0).getTitle()).isEqualTo("Artigo Novo");
        assertThat(articles.getContent().get(1).getTitle()).isEqualTo("Artigo Antigo");
    }

    @Test
    void testFindByPublishedAtAfter() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
        
        NewsArticle oldArticle = createNewsArticle("Artigo Antigo", "https://example.com/old");
        oldArticle.setPublishedAt(cutoffDate.minusHours(1));
        
        NewsArticle newArticle = createNewsArticle("Artigo Novo", "https://example.com/new");
        newArticle.setPublishedAt(cutoffDate.plusHours(1));

        entityManager.persist(oldArticle);
        entityManager.persist(newArticle);
        entityManager.flush();

        // When
        List<NewsArticle> articles = newsArticleRepository.findByPublishedAtAfter(cutoffDate);

        // Then
        assertThat(articles).hasSize(1);
        assertThat(articles.get(0).getTitle()).isEqualTo("Artigo Novo");
    }

    @Test
    void testSaveNewsArticle() {
        // Given
        NewsArticle article = createNewsArticle("Novo Artigo", "https://example.com/new-article");

        // When
        NewsArticle saved = newsArticleRepository.save(article);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Novo Artigo");
        assertThat(saved.getUrl()).isEqualTo("https://example.com/new-article");
        assertThat(saved.getCategory()).isEqualTo(category);
    }

    @Test
    void testDeleteNewsArticle() {
        // Given
        NewsArticle article = createNewsArticle("Artigo para Deletar", "https://example.com/delete");
        NewsArticle saved = entityManager.persistAndFlush(article);

        // When
        newsArticleRepository.deleteById(saved.getId());
        entityManager.flush();

        // Then
        Optional<NewsArticle> found = newsArticleRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    private NewsArticle createNewsArticle(String title, String url) {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setContent("Conteúdo do artigo: " + title);
        article.setUrl(url);
        article.setSource("Test Source");
        article.setPublishedAt(LocalDateTime.now());
        article.setCategory(category);
        return article;
    }
}