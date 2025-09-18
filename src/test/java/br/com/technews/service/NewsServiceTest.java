package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
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

    @Mock
    private NewsScrapingService newsScrapingService;

    @InjectMocks
    private NewsArticleService newsService;

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
        newsArticle.setCategory(category);
    }

    @Test
    void testGetAllNews() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<NewsArticle> articles = Arrays.asList(newsArticle);
        Page<NewsArticle> page = new PageImpl<>(articles, pageable, 1);

        when(newsArticleRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<NewsArticle> result = newsService.getAllNews(pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(newsArticle);
        verify(newsArticleRepository).findAll(pageable);
    }

    @Test
    void testGetNewsByCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<NewsArticle> articles = Arrays.asList(newsArticle);
        Page<NewsArticle> page = new PageImpl<>(articles, pageable, 1);

        when(categoryRepository.findByName("Tecnologia")).thenReturn(Optional.of(category));
        when(newsArticleRepository.findByCategoryOrderByPublishedAtDesc(category, pageable)).thenReturn(page);

        // When
        Page<NewsArticle> result = newsService.getNewsByCategory("Tecnologia", pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(newsArticle);
        verify(categoryRepository).findByName("Tecnologia");
        verify(newsArticleRepository).findByCategoryOrderByPublishedAtDesc(category, pageable);
    }

    @Test
    void testGetNewsByCategoryNotFound() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(categoryRepository.findByName("Inexistente")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> newsService.getNewsByCategory("Inexistente", pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Categoria não encontrada");

        verify(categoryRepository).findByName("Inexistente");
        verify(newsArticleRepository, never()).findByCategoryOrderByPublishedAtDesc(any(), any());
    }

    @Test
    void testGetRecentNews() {
        // Given
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
        List<NewsArticle> articles = Arrays.asList(newsArticle);

        when(newsArticleRepository.findByPublishedAtAfter(any(LocalDateTime.class))).thenReturn(articles);

        // When
        List<NewsArticle> result = newsService.getRecentNews(7);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(newsArticle);
        verify(newsArticleRepository).findByPublishedAtAfter(any(LocalDateTime.class));
    }

    @Test
    void testFetchAndSaveNews() {
        // Given
        String query = "technology";
        List<NewsArticle> scrapedArticles = Arrays.asList(newsArticle);

        when(newsScrapingService.scrapeNews(query)).thenReturn(scrapedArticles);
        when(newsArticleRepository.findByUrl(newsArticle.getUrl())).thenReturn(Optional.empty());
        when(newsArticleRepository.save(newsArticle)).thenReturn(newsArticle);

        // When
        Map<String, Object> result = newsService.fetchAndSaveNews(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("newArticlesSaved")).isEqualTo(1);
        assertThat(result.get("totalFound")).isEqualTo(1);
        assertThat(result.get("query")).isEqualTo(query);

        verify(newsScrapingService).scrapeNews(query);
        verify(newsArticleRepository).findByUrl(newsArticle.getUrl());
        verify(newsArticleRepository).save(newsArticle);
    }

    @Test
    void testFetchAndSaveNewsWithDuplicates() {
        // Given
        String query = "technology";
        List<NewsArticle> scrapedArticles = Arrays.asList(newsArticle);

        when(newsScrapingService.scrapeNews(query)).thenReturn(scrapedArticles);
        when(newsArticleRepository.findByUrl(newsArticle.getUrl())).thenReturn(Optional.of(newsArticle));

        // When
        Map<String, Object> result = newsService.fetchAndSaveNews(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("newArticlesSaved")).isEqualTo(0);
        assertThat(result.get("totalFound")).isEqualTo(1);
        assertThat(result.get("query")).isEqualTo(query);

        verify(newsScrapingService).scrapeNews(query);
        verify(newsArticleRepository).findByUrl(newsArticle.getUrl());
        verify(newsArticleRepository, never()).save(any());
    }

    @Test
    void testFetchAndSaveNewsWithException() {
        // Given
        String query = "technology";
        when(newsScrapingService.scrapeNews(query)).thenThrow(new RuntimeException("Scraping failed"));

        // When
        Map<String, Object> result = newsService.fetchAndSaveNews(query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(false);
        assertThat(result.get("message")).asString().contains("Erro ao buscar notícias");

        verify(newsScrapingService).scrapeNews(query);
        verify(newsArticleRepository, never()).save(any());
    }

    @Test
    void testSaveNewsArticle() {
        // Given
        when(newsArticleRepository.save(newsArticle)).thenReturn(newsArticle);

        // When
        NewsArticle result = newsService.saveNewsArticle(newsArticle);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(newsArticle);
        verify(newsArticleRepository).save(newsArticle);
    }

    @Test
    void testFindByUrl() {
        // Given
        String url = "https://example.com/news/1";
        when(newsArticleRepository.findByUrl(url)).thenReturn(Optional.of(newsArticle));

        // When
        Optional<NewsArticle> result = newsService.findByUrl(url);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(newsArticle);
        verify(newsArticleRepository).findByUrl(url);
    }

    @Test
    void testFindByUrlNotFound() {
        // Given
        String url = "https://example.com/nonexistent";
        when(newsArticleRepository.findByUrl(url)).thenReturn(Optional.empty());

        // When
        Optional<NewsArticle> result = newsService.findByUrl(url);

        // Then
        assertThat(result).isEmpty();
        verify(newsArticleRepository).findByUrl(url);
    }
}