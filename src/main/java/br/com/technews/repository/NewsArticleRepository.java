package br.com.technews.repository;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}