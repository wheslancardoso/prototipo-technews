package br.com.technews.repository;

import br.com.technews.model.NewsArticle;
import br.com.technews.model.ArticleStatus;
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
}