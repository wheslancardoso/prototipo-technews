package br.com.technews.repository;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Testes de integração para NewsArticleRepository
 * Testa operações de persistência e consultas de artigos
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("NewsArticle Repository Tests")
class NewsArticleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category techCategory;
    private Category businessCategory;
    private NewsArticle publishedArticle;
    private NewsArticle draftArticle;
    private NewsArticle approvedArticle;

    @BeforeEach
    void setUp() {
        // Limpar dados existentes
        entityManager.getEntityManager().createQuery("DELETE FROM NewsArticle").executeUpdate();
        entityManager.getEntityManager().createQuery("DELETE FROM Category").executeUpdate();
        entityManager.flush();

        // Criar categorias
        techCategory = new Category();
        techCategory.setName("Tecnologia");
        techCategory.setDescription("Categoria de tecnologia");
        techCategory.setSlug("tecnologia");
        techCategory.setActive(true);
        techCategory.setCreatedAt(LocalDateTime.now());
        techCategory = entityManager.persistAndFlush(techCategory);

        businessCategory = new Category();
        businessCategory.setName("Negócios");
        businessCategory.setDescription("Categoria de negócios");
        businessCategory.setSlug("negocios");
        businessCategory.setActive(true);
        businessCategory.setCreatedAt(LocalDateTime.now());
        businessCategory = entityManager.persistAndFlush(businessCategory);

        // Criar artigos para teste
        publishedArticle = new NewsArticle();
        publishedArticle.setTitle("Artigo Publicado sobre IA");
        publishedArticle.setContent("Conteúdo completo sobre inteligência artificial...");
        publishedArticle.setSummary("Resumo sobre IA");
        publishedArticle.setAuthor("João Silva");
        publishedArticle.setUrl("https://example.com/artigo-ia");
        publishedArticle.setImageUrl("https://example.com/image1.jpg");
        publishedArticle.setSourceDomain("example.com");
        publishedArticle.setCategoryEntity(techCategory);
        publishedArticle.setCategory("Tecnologia");
        publishedArticle.setPublished(true);
        publishedArticle.setStatus(ArticleStatus.PUBLICADO);
        publishedArticle.setPublishedAt(LocalDateTime.now().minusDays(2));
        publishedArticle.setCreatedAt(LocalDateTime.now().minusDays(3));

        draftArticle = new NewsArticle();
        draftArticle.setTitle("Artigo em Rascunho");
        draftArticle.setContent("Conteúdo em desenvolvimento...");
        draftArticle.setSummary("Resumo do rascunho");
        draftArticle.setAuthor("Maria Santos");
        draftArticle.setUrl("https://example.com/rascunho");
        draftArticle.setCategoryEntity(businessCategory);
        draftArticle.setCategory("Negócios");
        draftArticle.setPublished(false);
        draftArticle.setStatus(ArticleStatus.PENDENTE_REVISAO);
        draftArticle.setCreatedAt(LocalDateTime.now().minusDays(1));

        approvedArticle = new NewsArticle();
        approvedArticle.setTitle("Artigo Aprovado");
        approvedArticle.setContent("Conteúdo aprovado para publicação...");
        approvedArticle.setSummary("Resumo do artigo aprovado");
        approvedArticle.setAuthor("Pedro Costa");
        approvedArticle.setUrl("https://example.com/aprovado");
        approvedArticle.setCategoryEntity(techCategory);
        approvedArticle.setCategory("Tecnologia");
        approvedArticle.setPublished(false);
        approvedArticle.setStatus(ArticleStatus.APROVADO);
        approvedArticle.setCreatedAt(LocalDateTime.now().minusHours(6));

        // Persistir artigos
        entityManager.persistAndFlush(publishedArticle);
        entityManager.persistAndFlush(draftArticle);
        entityManager.persistAndFlush(approvedArticle);
    }

    @Test
    @DisplayName("Deve encontrar artigo por URL")
    void shouldFindByUrl() {
        // When
        Optional<NewsArticle> found = newsArticleRepository.findByUrl("https://example.com/artigo-ia");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Artigo Publicado sobre IA");
        assertThat(found.get().getPublished()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar vazio quando URL não existe")
    void shouldReturnEmptyWhenUrlNotExists() {
        // When
        Optional<NewsArticle> found = newsArticleRepository.findByUrl("https://nonexistent.com/article");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve verificar se URL existe")
    void shouldCheckIfUrlExists() {
        // When & Then
        assertThat(newsArticleRepository.existsByUrl("https://example.com/artigo-ia")).isTrue();
        assertThat(newsArticleRepository.existsByUrl("https://nonexistent.com/article")).isFalse();
    }

    @Test
    @DisplayName("Deve buscar apenas artigos publicados")
    void shouldFindOnlyPublishedArticles() {
        // When
        List<NewsArticle> publishedArticles = newsArticleRepository.findByPublishedTrueOrderByPublishedAtDesc();

        // Then
        assertThat(publishedArticles).hasSize(1);
        assertThat(publishedArticles.get(0).getTitle()).isEqualTo("Artigo Publicado sobre IA");
        assertThat(publishedArticles.get(0).getPublished()).isTrue();
    }

    @Test
    @DisplayName("Deve buscar artigos por status")
    void shouldFindByStatus() {
        // When
        List<NewsArticle> pendingArticles = newsArticleRepository.findByStatus(ArticleStatus.PENDENTE_REVISAO);
        List<NewsArticle> approvedArticles = newsArticleRepository.findByStatus(ArticleStatus.APROVADO);
        List<NewsArticle> publishedArticles = newsArticleRepository.findByStatus(ArticleStatus.PUBLICADO);

        // Then
        assertThat(pendingArticles).hasSize(1);
        assertThat(pendingArticles.get(0).getTitle()).isEqualTo("Artigo em Rascunho");

        assertThat(approvedArticles).hasSize(1);
        assertThat(approvedArticles.get(0).getTitle()).isEqualTo("Artigo Aprovado");

        assertThat(publishedArticles).hasSize(1);
        assertThat(publishedArticles.get(0).getTitle()).isEqualTo("Artigo Publicado sobre IA");
    }

    @Test
    @DisplayName("Deve buscar artigos por categoria")
    void shouldFindByCategory() {
        // When
        List<NewsArticle> techArticles = newsArticleRepository.findByCategoryOrderByCreatedAtDesc("Tecnologia");
        List<NewsArticle> businessArticles = newsArticleRepository.findByCategoryOrderByCreatedAtDesc("Negócios");

        // Then
        assertThat(techArticles).hasSize(2);
        assertThat(techArticles)
            .extracting(NewsArticle::getTitle)
            .containsExactlyInAnyOrder("Artigo Publicado sobre IA", "Artigo Aprovado");

        assertThat(businessArticles).hasSize(1);
        assertThat(businessArticles.get(0).getTitle()).isEqualTo("Artigo em Rascunho");
    }

    @Test
    @DisplayName("Deve buscar artigos por categoria com paginação")
    void shouldFindByCategoryWithPagination() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by("createdAt").descending());

        // When
        Page<NewsArticle> techPage = newsArticleRepository.findByPublishedTrueAndCategoryOrderByPublishedAtDesc("Tecnologia", pageRequest);

        // Then
        assertThat(techPage.getTotalElements()).isEqualTo(1); // Apenas 1 artigo publicado de tecnologia
        assertThat(techPage.getContent()).hasSize(1);
        // Deve retornar o mais recente primeiro
        assertThat(techPage.getContent().get(0).getTitle()).isEqualTo("Artigo Publicado sobre IA");
    }

    @Test
    @DisplayName("Deve buscar artigos por autor")
    void shouldFindByAuthor() {
        // When
        List<NewsArticle> joaoArticles = newsArticleRepository.findByAuthorContainingIgnoreCase("João Silva");
        List<NewsArticle> mariaArticles = newsArticleRepository.findByAuthorContainingIgnoreCase("Maria Santos");

        // Then
        assertThat(joaoArticles).hasSize(1);
        assertThat(joaoArticles.get(0).getTitle()).isEqualTo("Artigo Publicado sobre IA");

        assertThat(mariaArticles).hasSize(1);
        assertThat(mariaArticles.get(0).getTitle()).isEqualTo("Artigo em Rascunho");
    }

    @Test
    @DisplayName("Deve buscar artigos por domínio da fonte")
    void shouldFindBySourceDomain() {
        // When
        List<NewsArticle> exampleArticles = newsArticleRepository.findBySourceDomain("example.com");

        // Then
        assertThat(exampleArticles).hasSize(1);
        assertThat(exampleArticles.get(0).getTitle()).isEqualTo("Artigo Publicado sobre IA");
    }

    @Test
    @DisplayName("Deve buscar artigos por período de publicação")
    void shouldFindByPublishedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(5);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        List<NewsArticle> recentArticles = newsArticleRepository.findRecentPublishedArticles(startDate, PageRequest.of(0, 10));

        // Then
        assertThat(recentArticles).hasSize(1);
        assertThat(recentArticles.get(0).getTitle()).isEqualTo("Artigo Publicado sobre IA");
    }

    @Test
    @DisplayName("Deve buscar artigos por período de criação")
    void shouldFindByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(2);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        List<NewsArticle> recentArticles = newsArticleRepository.findByCreatedAtAfter(startDate);

        // Then
        assertThat(recentArticles).hasSize(3);
        assertThat(recentArticles)
            .extracting(NewsArticle::getTitle)
            .containsExactlyInAnyOrder("Artigo Publicado sobre IA", "Artigo em Rascunho", "Artigo Aprovado");
    }

    @Test
    @DisplayName("Deve contar artigos por status")
    void shouldCountByStatus() {
        // When
        long pendingCount = newsArticleRepository.countByStatus(ArticleStatus.PENDENTE_REVISAO);
        long approvedCount = newsArticleRepository.countByStatus(ArticleStatus.APROVADO);
        long publishedCount = newsArticleRepository.countByStatus(ArticleStatus.PUBLICADO);

        // Then
        assertThat(pendingCount).isEqualTo(1);
        assertThat(approvedCount).isEqualTo(1);
        assertThat(publishedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve contar artigos publicados")
    void shouldCountPublishedArticles() {
        // When
        long publishedCount = newsArticleRepository.countByPublishedTrue();

        // Then
        assertThat(publishedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve salvar novo artigo")
    void shouldSaveNewArticle() {
        // Given
        NewsArticle newArticle = new NewsArticle();
        newArticle.setTitle("Novo Artigo de Teste");
        newArticle.setContent("Conteúdo do novo artigo...");
        newArticle.setSummary("Resumo do novo artigo");
        newArticle.setAuthor("Autor Teste");
        newArticle.setUrl("https://example.com/novo-artigo");
        newArticle.setCategoryEntity(techCategory);
        newArticle.setCategory("Tecnologia");
        newArticle.setPublished(false);
        newArticle.setStatus(ArticleStatus.PENDENTE_REVISAO);
        newArticle.setCreatedAt(LocalDateTime.now());

        // When
        NewsArticle saved = newsArticleRepository.save(newArticle);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("Novo Artigo de Teste");
        
        // Verificar se foi persistido
        Optional<NewsArticle> found = newsArticleRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAuthor()).isEqualTo("Autor Teste");
    }

    @Test
    @DisplayName("Deve atualizar artigo existente")
    void shouldUpdateExistingArticle() {
        // Given
        NewsArticle article = newsArticleRepository.findByUrl("https://example.com/rascunho").orElseThrow();
        String originalTitle = article.getTitle();

        // When
        article.setTitle("Título Atualizado");
        article.setStatus(ArticleStatus.APROVADO);
        NewsArticle updated = newsArticleRepository.save(article);

        // Then
        assertThat(updated.getTitle()).isNotEqualTo(originalTitle);
        assertThat(updated.getTitle()).isEqualTo("Título Atualizado");
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.APROVADO);
        
        // Verificar persistência
        NewsArticle reloaded = newsArticleRepository.findById(updated.getId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("Título Atualizado");
        assertThat(reloaded.getStatus()).isEqualTo(ArticleStatus.APROVADO);
    }

    @Test
    @DisplayName("Deve publicar artigo")
    void shouldPublishArticle() {
        // Given
        NewsArticle article = newsArticleRepository.findByUrl("https://example.com/aprovado").orElseThrow();
        assertThat(article.getPublished()).isFalse();
        assertThat(article.getPublishedAt()).isNull();

        // When
        article.setPublished(true);
        article.setStatus(ArticleStatus.PUBLICADO);
        article.setPublishedAt(LocalDateTime.now());
        NewsArticle updated = newsArticleRepository.save(article);

        // Then
        assertThat(updated.getPublished()).isTrue();
        assertThat(updated.getStatus()).isEqualTo(ArticleStatus.PUBLICADO);
        assertThat(updated.getPublishedAt()).isNotNull();
        
        // Verificar que aparece na lista de publicados
        List<NewsArticle> publishedArticles = newsArticleRepository.findByPublishedTrueOrderByPublishedAtDesc();
        assertThat(publishedArticles).hasSize(2);
    }

    @Test
    @DisplayName("Deve deletar artigo")
    void shouldDeleteArticle() {
        // Given
        NewsArticle article = newsArticleRepository.findByUrl("https://example.com/rascunho").orElseThrow();
        Long articleId = article.getId();

        // When
        newsArticleRepository.delete(article);

        // Then
        Optional<NewsArticle> deleted = newsArticleRepository.findById(articleId);
        assertThat(deleted).isEmpty();
        
        // Verificar que outros artigos ainda existem
        assertThat(newsArticleRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve buscar todos os artigos ordenados por data de criação")
    void shouldFindAllArticlesOrderedByCreationDate() {
        // When
        List<NewsArticle> allArticles = newsArticleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Then
        assertThat(allArticles).hasSize(3);
        // Deve estar ordenado do mais recente para o mais antigo
        assertThat(allArticles.get(0).getTitle()).isEqualTo("Artigo Aprovado");
        assertThat(allArticles.get(1).getTitle()).isEqualTo("Artigo em Rascunho");
        assertThat(allArticles.get(2).getTitle()).isEqualTo("Artigo Publicado sobre IA");
    }

    @Test
    @DisplayName("Deve validar URL única")
    void shouldValidateUniqueUrl() {
        // Given
        NewsArticle duplicateUrl = new NewsArticle();
        duplicateUrl.setTitle("Artigo com URL Duplicada");
        duplicateUrl.setContent("Conteúdo...");
        duplicateUrl.setUrl("https://example.com/artigo-ia"); // URL já existe
        duplicateUrl.setCategoryEntity(techCategory);
        duplicateUrl.setStatus(ArticleStatus.PENDENTE_REVISAO);
        duplicateUrl.setCreatedAt(LocalDateTime.now());

        // When & Then
        assertThatThrownBy(() -> {
            newsArticleRepository.save(duplicateUrl);
            entityManager.flush(); // Força a validação
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Deve buscar artigos com paginação e ordenação")
    void shouldFindArticlesWithPaginationAndSorting() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 2, Sort.by("title").ascending());

        // When
        Page<NewsArticle> page = newsArticleRepository.findAll(pageRequest);

        // Then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        
        // Verificar ordenação alfabética por título
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Artigo Aprovado");
        assertThat(page.getContent().get(1).getTitle()).isEqualTo("Artigo Publicado sobre IA");
    }
}