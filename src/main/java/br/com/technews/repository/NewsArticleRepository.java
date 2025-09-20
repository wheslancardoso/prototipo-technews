package br.com.technews.repository;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com a entidade NewsArticle
 */
@Repository
public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {
    
    /**
     * Busca artigos por status
     */
    List<NewsArticle> findByStatus(ArticleStatus status);
    
    /**
     * Busca artigos por status ordenados por data de criação
     */
    List<NewsArticle> findByStatusOrderByCreatedAtDesc(ArticleStatus status);
    
    /**
     * Busca artigo por URL
     */
    Optional<NewsArticle> findByUrl(String url);
    
    /**
     * Verifica se existe um artigo com a URL especificada
     */
    boolean existsByUrl(String url);
    
    /**
     * Busca artigos por domínio da fonte
     */
    List<NewsArticle> findBySourceDomain(String sourceDomain);
    
    /**
     * Busca artigos aprovados ordenados por data de publicação
     */
    List<NewsArticle> findByStatusOrderByPublishedAtDesc(ArticleStatus status);
    
    /**
     * Busca os 10 artigos mais recentes por data de publicação
     */
    List<NewsArticle> findTop10ByOrderByPublishedAtDesc();
    
    /**
     * Busca artigos publicados ordenados por data de publicação
     */
    List<NewsArticle> findByPublishedTrueOrderByPublishedAtDesc();
    
    /**
     * Busca artigos publicados com paginação
     */
    Page<NewsArticle> findByPublishedTrueOrderByPublishedAtDesc(Pageable pageable);
    
    /**
     * Busca artigos por categoria ordenados por data de criação
     */
    List<NewsArticle> findByCategoryOrderByCreatedAtDesc(String category);
    
    /**
     * Conta artigos publicados
     */
    long countByPublishedTrue();
    
    /**
     * Conta artigos por status
     */
    long countByStatus(ArticleStatus status);

    // Métodos adicionais para integração com GNews API

    /**
     * Remove artigos não publicados criados antes de uma data específica
     */
    @Modifying
    @Query("DELETE FROM NewsArticle n WHERE n.published = false AND n.createdAt < :cutoffDate")
    int deleteByPublishedFalseAndCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Busca artigos criados após uma data específica
     */
    List<NewsArticle> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Busca artigos por título contendo texto (case insensitive)
     */
    @Query("SELECT n FROM NewsArticle n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<NewsArticle> findByTitleContainingIgnoreCase(@Param("title") String title);

    /**
     * Busca artigos por autor contendo texto (case insensitive)
     */
    @Query("SELECT n FROM NewsArticle n WHERE LOWER(n.author) LIKE LOWER(CONCAT('%', :author, '%'))")
    List<NewsArticle> findByAuthorContainingIgnoreCase(@Param("author") String author);

    /**
     * Busca artigos publicados com busca por título ou conteúdo
     */
    @Query("SELECT n FROM NewsArticle n WHERE n.published = true AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    Page<NewsArticle> findByPublishedTrueAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByPublishedAtDesc(
            @Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Busca artigos publicados recentemente
     */
    @Query("SELECT n FROM NewsArticle n WHERE n.publishedAt >= :since ORDER BY n.publishedAt DESC")
    List<NewsArticle> findRecentPublishedArticles(@Param("since") LocalDateTime since, Pageable pageable);
    
    /**
     * Busca artigos publicados por categoria ordenados por data de publicação
     */
    Page<NewsArticle> findByPublishedTrueAndCategoryOrderByPublishedAtDesc(String category, Pageable pageable);
    
    /**
     * Busca artigos com filtros avançados combinados
     */
    @Query("SELECT n FROM NewsArticle n WHERE n.published = true " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(n.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:category IS NULL OR :category = '' OR n.category = :category) " +
           "AND (:dateFrom IS NULL OR :dateFrom = '' OR n.publishedAt >= CAST(:dateFrom AS timestamp)) " +
           "AND (:dateTo IS NULL OR :dateTo = '' OR n.publishedAt <= CAST(:dateTo AS timestamp)) " +
           "AND (:author IS NULL OR :author = '' OR LOWER(n.author) LIKE LOWER(CONCAT('%', :author, '%')))")
    Page<NewsArticle> findArticlesWithFilters(@Param("search") String search,
                                            @Param("category") String category,
                                            @Param("dateFrom") String dateFrom,
                                            @Param("dateTo") String dateTo,
                                            @Param("author") String author,
                                            Pageable pageable);
    
    /**
     * Obtém lista de autores distintos
     */
    @Query("SELECT DISTINCT n.author FROM NewsArticle n WHERE n.published = true AND n.author IS NOT NULL ORDER BY n.author")
    List<String> findDistinctAuthors();
}