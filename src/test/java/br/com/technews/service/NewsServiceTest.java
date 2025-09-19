package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.repository.CategoryRepository;
import br.com.technews.repository.NewsArticleRepository;
import br.com.technews.service.NewsArticleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Testes unitários para NewsService
 */
@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsArticleRepository newsArticleRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private NewsArticleService newsArticleService;

    private Category category;
    private NewsArticle newsArticle;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");

        newsArticle = new NewsArticle();
        newsArticle.setId(1L);
        newsArticle.setTitle("Título do Artigo");
        newsArticle.setContent("Conteúdo do artigo");
        newsArticle.setUrl("https://example.com/news/1");
        newsArticle.setSource("Test Source");
        newsArticle.setPublishedAt(LocalDateTime.now());
        newsArticle.setCategory(category.getName());
    }

    @Test
    void testGetAllNews() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<NewsArticle> articles = Arrays.asList(newsArticle);
        Page<NewsArticle> page = new PageImpl<>(articles, pageable, 1);

        when(newsArticleRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<NewsArticle> result = newsArticleService.findAll(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(newsArticle);
        verify(newsArticleRepository).findAll(pageable);
    }

    @Test
    void testFindByCategory() {
        // Given
        String categoryName = "Tecnologia";
        List<NewsArticle> articles = Arrays.asList(newsArticle);

        when(newsArticleRepository.findByCategoryOrderByCreatedAtDesc(categoryName)).thenReturn(articles);

        // When
        List<NewsArticle> result = newsArticleService.findByCategory(categoryName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(newsArticle);
        verify(newsArticleRepository).findByCategoryOrderByCreatedAtDesc(categoryName);
    }

    @Test
    void testFindByCategoryEmpty() {
        // Given
        String categoryName = "Inexistente";
        when(newsArticleRepository.findByCategoryOrderByCreatedAtDesc(categoryName)).thenReturn(Arrays.asList());

        // When
        List<NewsArticle> result = newsArticleService.findByCategory(categoryName);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(newsArticleRepository).findByCategoryOrderByCreatedAtDesc(categoryName);
    }

    @Test
    void testFindByCreatedAtAfter() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<NewsArticle> articles = Arrays.asList(newsArticle);

        when(newsArticleRepository.findByCreatedAtAfter(cutoffDate)).thenReturn(articles);

        // When
        List<NewsArticle> result = newsArticleService.findByCreatedAtAfter(cutoffDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(newsArticle);
        verify(newsArticleRepository).findByCreatedAtAfter(cutoffDate);
    }

    @Test
    void testCreateNewsArticle() {
        // Given
        when(newsArticleRepository.save(any(NewsArticle.class))).thenReturn(newsArticle);

        // When
        NewsArticle result = newsArticleService.create(newsArticle);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(newsArticle.getTitle());
        verify(newsArticleRepository).save(newsArticle);
    }

    @Test
    void testCreateNewsArticleWithDuplicateUrl() {
        // Given
        when(newsArticleRepository.save(any(NewsArticle.class))).thenReturn(newsArticle);

        // When
        NewsArticle result = newsArticleService.create(newsArticle);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ArticleStatus.PENDENTE_REVISAO);
        assertThat(result.getPublished()).isFalse();
        verify(newsArticleRepository).save(newsArticle);
    }

    @Test
    void testCreateNewsArticleWithException() {
        // Given
        when(newsArticleRepository.save(any(NewsArticle.class))).thenThrow(new RuntimeException("Save failed"));

        // When & Then
        assertThatThrownBy(() -> newsArticleService.create(newsArticle))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Save failed");
    }

    @Test
    void testSaveNewsArticle() {
        // Given
        when(newsArticleRepository.save(any(NewsArticle.class))).thenReturn(newsArticle);

        // When
        NewsArticle result = newsArticleService.save(newsArticle);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo(newsArticle.getTitle());
        verify(newsArticleRepository).save(newsArticle);
    }

    @Test
    void testExistsByUrl() {
        // Given
        String url = "https://example.com/news/1";
        when(newsArticleRepository.existsByUrl(url)).thenReturn(true);

        // When
        boolean result = newsArticleService.existsByUrl(url);

        // Then
        assertThat(result).isTrue();
        verify(newsArticleRepository).existsByUrl(url);
    }

    @Test
    void testExistsByUrlNotFound() {
        // Given
        String url = "https://example.com/nonexistent";
        when(newsArticleRepository.existsByUrl(url)).thenReturn(false);

        // When
        boolean result = newsArticleService.existsByUrl(url);

        // Then
        assertThat(result).isFalse();
        verify(newsArticleRepository).existsByUrl(url);
    }
}