package br.com.technews.repository;

import br.com.technews.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para operações de banco de dados da entidade Category
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    /**
     * Busca categoria por slug
     */
    Optional<Category> findBySlug(String slug);
    
    /**
     * Busca categoria por nome (case insensitive)
     */
    Optional<Category> findByNameIgnoreCase(String name);
    
    /**
     * Busca todas as categorias ativas
     */
    List<Category> findByActiveTrue();
    
    /**
     * Busca todas as categorias ativas ordenadas por nome
     */
    List<Category> findByActiveTrueOrderByNameAsc();
    
    /**
     * Verifica se existe categoria com o nome especificado (excluindo o ID atual)
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND (:id IS NULL OR c.id != :id)")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);
    
    /**
     * Verifica se existe categoria com o slug especificado (excluindo o ID atual)
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.slug = :slug AND (:id IS NULL OR c.id != :id)")
    boolean existsBySlugAndIdNot(@Param("slug") String slug, @Param("id") Long id);
    
    /**
     * Busca categorias que contém o termo no nome ou descrição
     */
    @Query("SELECT c FROM Category c WHERE c.active = true AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Category> findBySearchTerm(@Param("searchTerm") String searchTerm);
    
    /**
     * Conta o número de artigos por categoria
     */
    @Query("SELECT c.id, COUNT(a) FROM Category c LEFT JOIN c.articles a " +
           "WHERE c.active = true GROUP BY c.id")
    List<Object[]> countArticlesByCategory();
    
    /**
     * Busca categorias com pelo menos um artigo publicado
     */
    @Query("SELECT DISTINCT c FROM Category c JOIN c.articles a " +
           "WHERE c.active = true AND a.published = true")
    List<Category> findCategoriesWithPublishedArticles();
}