package br.com.technews.repository;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
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
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class NewsArticleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    private Category category;

    @BeforeEach
    void setUp() {
        // Limpar dados existentes
        entityManager.getEntityManager().createQuery("DELETE FROM NewsArticle").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM Category").executeUpdate();
        entityManager.flush();
        
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
    void testFindByCategoryOrderByCreatedAtDesc() {
        // Given
        NewsArticle article1 = createNewsArticle("Título 1", "https://example.com/1");
        article1.setCategory(category.getName());
        article1.setCreatedAt(LocalDateTime.now().minusHours(2));
        
        NewsArticle article2 = createNewsArticle("Título 2", "https://example.com/2");
        article2.setCategory(category.getName());
        article2.setCreatedAt(LocalDateTime.now().minusHours(1));
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.flush();

        // When
        List<NewsArticle> articles = newsArticleRepository.findByCategoryOrderByCreatedAtDesc(category.getName());

        // Then
        assertThat(articles).hasSize(2);
        assertThat(articles.get(0).getTitle()).isEqualTo("Título 2"); // Mais recente primeiro
        assertThat(articles.get(1).getTitle()).isEqualTo("Título 1");
    }

    @Test
    void testFindByPublishedTrueAndCategoryOrderByPublishedAtDesc() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        
        NewsArticle article1 = createNewsArticle("Título 1", "https://example.com/1");
        article1.setCategory(category.getName());
        article1.setPublished(true);
        article1.setPublishedAt(LocalDateTime.now().minusHours(2));
        
        NewsArticle article2 = createNewsArticle("Título 2", "https://example.com/2");
        article2.setCategory(category.getName());
        article2.setPublished(true);
        article2.setPublishedAt(LocalDateTime.now().minusHours(1));
        
        entityManager.persist(article1);
        entityManager.persist(article2);
        entityManager.flush();

        // When
        Page<NewsArticle> articles = newsArticleRepository.findByPublishedTrueAndCategoryOrderByPublishedAtDesc(category.getName(), pageRequest);

        // Then
        assertThat(articles.getContent()).hasSize(2);
        assertThat(articles.getContent().get(0).getTitle()).isEqualTo("Título 2"); // Mais recente primeiro
        assertThat(articles.getContent().get(1).getTitle()).isEqualTo("Título 1");
    }

    @Test
    void testFindByCreatedAtAfter() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(1);
        
        // Criar artigo antigo primeiro
        NewsArticle oldArticle = createNewsArticle("Artigo Antigo", "https://example.com/old");
        entityManager.persistAndFlush(oldArticle);
        
        // Aguardar um pouco para garantir diferença de tempo
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Atualizar o cutoff para ser após o primeiro artigo
        cutoffDate = LocalDateTime.now().minusNanos(50_000_000); // 50 milissegundos
        
        // Aguardar mais um pouco
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Criar artigo novo
        NewsArticle newArticle = createNewsArticle("Artigo Novo", "https://example.com/new");
        entityManager.persistAndFlush(newArticle);
        entityManager.clear(); // Limpa o contexto de persistência

        // When
        List<NewsArticle> articles = newsArticleRepository.findByCreatedAtAfter(cutoffDate);

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
        assertThat(saved.getCategoryEntity()).isEqualTo(category);
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
        article.setCategoryEntity(category);
        return article;
    }
}