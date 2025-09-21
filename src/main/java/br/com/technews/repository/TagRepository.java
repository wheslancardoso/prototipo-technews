package br.com.technews.repository;

import br.com.technews.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * Busca tag por nome (case insensitive)
     */
    Optional<Tag> findByNameIgnoreCase(String name);
    
    /**
     * Busca todas as tags ativas
     */
    List<Tag> findByIsActiveTrueOrderByNameAsc();
    
    /**
     * Busca tags por nome contendo texto (case insensitive)
     */
    List<Tag> findByNameContainingIgnoreCaseAndIsActiveTrue(String name);
    
    /**
     * Busca tags mais populares (com mais artigos)
     */
    @Query("SELECT t FROM Tag t LEFT JOIN t.articles a WHERE t.isActive = true " +
           "GROUP BY t ORDER BY COUNT(a) DESC")
    List<Tag> findMostPopularTags();
    
    /**
     * Busca tags mais populares limitadas
     */
    @Query("SELECT t FROM Tag t LEFT JOIN t.articles a WHERE t.isActive = true " +
           "GROUP BY t ORDER BY COUNT(a) DESC LIMIT :limit")
    List<Tag> findMostPopularTags(@Param("limit") int limit);
    
    /**
     * Conta o número de artigos por tag
     */
    @Query("SELECT COUNT(a) FROM Tag t LEFT JOIN t.articles a WHERE t.id = :tagId")
    Long countArticlesByTagId(@Param("tagId") Long tagId);
    
    /**
     * Verifica se existe tag com o nome especificado
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Busca tags usadas em artigos publicados
     */
    @Query("SELECT DISTINCT t FROM Tag t JOIN t.articles a WHERE a.published = true AND t.isActive = true")
    List<Tag> findTagsUsedInPublishedArticles();
}