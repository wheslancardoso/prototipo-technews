package br.com.technews.repository;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Category;
import br.com.technews.entity.TrustedSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

/**
 * Testes unitários para NewsArticleRepository usando Mockito
 */
@DisplayName("NewsArticle Repository Unit Tests")
class NewsArticleRepositoryUnitTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve encontrar artigo por URL")
    void shouldFindArticleByUrl() {
        // Given
        NewsArticle article = createTestNewsArticle("Test Article", "https://example.com/test");
        when(newsArticleRepository.findByUrl("https://example.com/test")).thenReturn(Optional.of(article));

        // When
        Optional<NewsArticle> result = newsArticleRepository.findByUrl("https://example.com/test");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Test Article");
        assertThat(result.get().getUrl()).isEqualTo("https://example.com/test");
        verify(newsArticleRepository).findByUrl("https://example.com/test");
    }

    @Test
    @DisplayName("Deve verificar se URL existe")
    void shouldCheckIfUrlExists() {
        // Given
        when(newsArticleRepository.existsByUrl("https://example.com/test")).thenReturn(true);

        // When
        boolean exists = newsArticleRepository.existsByUrl("https://example.com/test");

        // Then
        assertThat(exists).isTrue();
        verify(newsArticleRepository).existsByUrl("https://example.com/test");
    }

    @Test
    @DisplayName("Deve encontrar artigos por categoria")
    void shouldFindArticlesByCategory() {
        // Given
        String category = "AI";
        List<NewsArticle> articles = Arrays.asList(
            createTestNewsArticle("AI Article 1", "https://example.com/ai1"),
            createTestNewsArticle("AI Article 2", "https://example.com/ai2")
        );
        
        when(newsArticleRepository.findByCategoryOrderByCreatedAtDesc(category)).thenReturn(articles);

        // When
        List<NewsArticle> result = newsArticleRepository.findByCategoryOrderByCreatedAtDesc(category);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NewsArticle::getTitle)
                .containsExactlyInAnyOrder("AI Article 1", "AI Article 2");
        verify(newsArticleRepository).findByCategoryOrderByCreatedAtDesc(category);
    }

    @Test
    @DisplayName("Deve encontrar artigos por domínio da fonte")
    void shouldFindArticlesBySourceDomain() {
        // Given
        String sourceDomain = "techcrunch.com";
        List<NewsArticle> articles = Arrays.asList(
            createTestNewsArticle("Tech News 1", "https://techcrunch.com/news1"),
            createTestNewsArticle("Tech News 2", "https://techcrunch.com/news2")
        );
        
        when(newsArticleRepository.findBySourceDomain(sourceDomain)).thenReturn(articles);

        // When
        List<NewsArticle> result = newsArticleRepository.findBySourceDomain(sourceDomain);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NewsArticle::getTitle)
                .containsExactlyInAnyOrder("Tech News 1", "Tech News 2");
        verify(newsArticleRepository).findBySourceDomain(sourceDomain);
    }

    @Test
    @DisplayName("Deve buscar artigos publicados")
    void shouldSearchPublishedArticles() {
        // Given
        List<NewsArticle> articles = Arrays.asList(
            createTestNewsArticle("AI Revolution", "https://example.com/ai-revolution"),
            createTestNewsArticle("Machine Learning and AI", "https://example.com/ml-ai")
        );
        
        when(newsArticleRepository.findByPublishedTrueOrderByPublishedAtDesc()).thenReturn(articles);

        // When
        List<NewsArticle> result = newsArticleRepository.findByPublishedTrueOrderByPublishedAtDesc();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(NewsArticle::getTitle)
                .containsExactlyInAnyOrder("AI Revolution", "Machine Learning and AI");
        verify(newsArticleRepository).findByPublishedTrueOrderByPublishedAtDesc();
    }

    @Test
    @DisplayName("Deve contar artigos publicados")
    void shouldCountPublishedArticles() {
        // Given
        when(newsArticleRepository.countByPublishedTrue()).thenReturn(15L);

        // When
        long count = newsArticleRepository.countByPublishedTrue();

        // Then
        assertThat(count).isEqualTo(15L);
        verify(newsArticleRepository).countByPublishedTrue();
    }

    @Test
    @DisplayName("Deve salvar artigo")
    void shouldSaveArticle() {
        // Given
        NewsArticle article = createTestNewsArticle("New Article", "https://example.com/new");
        NewsArticle savedArticle = createTestNewsArticle("New Article", "https://example.com/new");
        savedArticle.setId(1L);
        
        when(newsArticleRepository.save(article)).thenReturn(savedArticle);

        // When
        NewsArticle result = newsArticleRepository.save(article);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("New Article");
        assertThat(result.getUrl()).isEqualTo("https://example.com/new");
        verify(newsArticleRepository).save(article);
    }

    @Test
    @DisplayName("Deve deletar artigo por ID")
    void shouldDeleteArticleById() {
        // Given
        Long articleId = 1L;
        doNothing().when(newsArticleRepository).deleteById(articleId);

        // When
        newsArticleRepository.deleteById(articleId);

        // Then
        verify(newsArticleRepository).deleteById(articleId);
    }

    @Test
    @DisplayName("Deve contar total de artigos")
    void shouldCountAllArticles() {
        // Given
        when(newsArticleRepository.count()).thenReturn(100L);

        // When
        long count = newsArticleRepository.count();

        // Then
        assertThat(count).isEqualTo(100L);
        verify(newsArticleRepository).count();
    }

    private NewsArticle createTestNewsArticle(String title, String url) {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setUrl(url);
        article.setContent("Test content for " + title);
        article.setSummary("Test summary");
        article.setPublishedAt(LocalDateTime.now());
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        return article;
    }

    private Category createTestCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setDescription("Test category");
        category.setSlug(name.toLowerCase());
        category.setColor("#FF0000");
        category.setActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    private TrustedSource createTestTrustedSource(String name) {
        TrustedSource source = new TrustedSource();
        source.setName(name);
        source.setDomainName(name.toLowerCase() + ".com");
        source.setDescription("Test trusted source");
        source.setActive(true);
        source.setCreatedAt(LocalDateTime.now());
        source.setUpdatedAt(LocalDateTime.now());
        return source;
    }
}