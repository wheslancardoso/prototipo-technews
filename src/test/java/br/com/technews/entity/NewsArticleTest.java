package br.com.technews.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDateTime;

/**
 * Testes unitários para a entidade NewsArticle
 */
class NewsArticleTest {

    private NewsArticle article;
    private Category category;

    @BeforeEach
    void setUp() {
        article = new NewsArticle();
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
    }

    @Test
    void testNewsArticleCreation() {
        // Given
        String title = "Nova tecnologia revoluciona o mercado";
        String content = "Conteúdo do artigo sobre a nova tecnologia...";
        String url = "https://example.com/news/1";
        String source = "TechNews";
        LocalDateTime publishedAt = LocalDateTime.now();

        // When
        article.setTitle(title);
        article.setContent(content);
        article.setUrl(url);
        article.setSource(source);
        article.setPublishedAt(publishedAt);
        article.setCategoryEntity(category);

        // Then
        assertThat(article.getTitle()).isEqualTo(title);
        assertThat(article.getContent()).isEqualTo(content);
        assertThat(article.getUrl()).isEqualTo(url);
        assertThat(article.getSource()).isEqualTo(source);
        assertThat(article.getPublishedAt()).isEqualTo(publishedAt);
        assertThat(article.getCategoryEntity()).isEqualTo(category);
        assertThat(article.getId()).isNull();
    }

    @Test
    void testNewsArticleWithId() {
        // Given
        Long id = 1L;
        String title = "Artigo de teste";

        // When
        article.setId(id);
        article.setTitle(title);

        // Then
        assertThat(article.getId()).isEqualTo(id);
        assertThat(article.getTitle()).isEqualTo(title);
    }

    @Test
    void testNewsArticleEquality() {
        // Given
        NewsArticle article1 = new NewsArticle();
        article1.setId(1L);
        article1.setTitle("Mesmo título");
        article1.setUrl("https://same-url.com");

        NewsArticle article2 = new NewsArticle();
        article2.setId(1L);
        article2.setTitle("Mesmo título");
        article2.setUrl("https://same-url.com");

        // Then
        assertThat(article1).isEqualTo(article2);
        assertThat(article1.hashCode()).isEqualTo(article2.hashCode());
    }

    @Test
    void testNewsArticleToString() {
        // Given
        article.setId(1L);
        article.setTitle("Artigo de teste");
        article.setSource("TestSource");

        // When
        String toString = article.toString();

        // Then
        assertThat(toString).contains("Artigo de teste");
        assertThat(toString).contains("1");
    }

    @Test
    void testCategoryAssociation() {
        // Given
        Category techCategory = new Category();
        techCategory.setId(2L);
        techCategory.setName("Tech");

        // When
        article.setCategoryEntity(techCategory);

        // Then
        assertThat(article.getCategoryEntity()).isEqualTo(techCategory);
        assertThat(article.getCategoryEntity().getName()).isEqualTo("Tech");
    }

    @Test
    void testPublishedAtHandling() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime past = now.minusDays(1);

        // When
        article.setPublishedAt(past);

        // Then
        assertThat(article.getPublishedAt()).isEqualTo(past);
        assertThat(article.getPublishedAt()).isBefore(now);
    }
}