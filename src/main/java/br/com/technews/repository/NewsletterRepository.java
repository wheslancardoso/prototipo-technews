package br.com.technews.repository;

import br.com.technews.entity.Newsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repositório para Newsletter
 */
@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {

    /**
     * Buscar newsletter por slug
     */
    Optional<Newsletter> findBySlug(String slug);
    
    /**
     * Buscar newsletter por data
     */
    Optional<Newsletter> findByNewsletterDate(LocalDate date);
    
    /**
     * Verificar se existe newsletter para uma data específica
     */
    boolean existsByNewsletterDate(LocalDate date);
    
    /**
     * Buscar a newsletter mais recente publicada
     */
    Optional<Newsletter> findFirstByPublishedTrueOrderByNewsletterDateDesc();
    
    /**
     * Buscar newsletters publicadas ordenadas por data (mais recentes primeiro)
     */
    List<Newsletter> findByPublishedTrueOrderByNewsletterDateDesc();
    
    /**
     * Buscar newsletters publicadas com paginação
     */
    Page<Newsletter> findByPublishedTrueOrderByNewsletterDateDesc(Pageable pageable);
    
    /**
     * Buscar todas as newsletters ordenadas por data (mais recentes primeiro)
     */
    List<Newsletter> findAllByOrderByNewsletterDateDesc();
    
    /**
     * Buscar newsletters por período
     */
    @Query("SELECT n FROM Newsletter n WHERE n.newsletterDate BETWEEN :startDate AND :endDate ORDER BY n.newsletterDate DESC")
    List<Newsletter> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * Buscar newsletters do mês atual
     */
    @Query("SELECT n FROM Newsletter n WHERE EXTRACT(YEAR FROM n.newsletterDate) = :year AND EXTRACT(MONTH FROM n.newsletterDate) = :month ORDER BY n.newsletterDate DESC")
    List<Newsletter> findByYearAndMonth(@Param("year") int year, @Param("month") int month);
    
    /**
     * Buscar newsletters do ano atual
     */
    @Query("SELECT n FROM Newsletter n WHERE EXTRACT(YEAR FROM n.newsletterDate) = :year ORDER BY n.newsletterDate DESC")
    List<Newsletter> findByYear(@Param("year") int year);
    
    /**
     * Contar newsletters publicadas
     */
    long countByPublishedTrue();
    
    /**
     * Buscar newsletters mais visualizadas
     */
    @Query("SELECT n FROM Newsletter n WHERE n.published = true ORDER BY n.views DESC")
    List<Newsletter> findMostViewed(Pageable pageable);
    
    /**
     * Buscar newsletters recentes (últimos 30 dias)
     */
    @Query("SELECT n FROM Newsletter n WHERE n.newsletterDate >= :thirtyDaysAgo ORDER BY n.newsletterDate DESC")
    List<Newsletter> findRecentNewsletters(@Param("thirtyDaysAgo") LocalDate thirtyDaysAgo);
    
    /**
     * Buscar newsletters com artigos
     */
    @Query("SELECT DISTINCT n FROM Newsletter n JOIN n.articles a WHERE n.published = true ORDER BY n.newsletterDate DESC")
    List<Newsletter> findNewslettersWithArticles();
    
    /**
     * Buscar newsletters por título (busca parcial)
     */
    @Query("SELECT n FROM Newsletter n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%')) ORDER BY n.newsletterDate DESC")
    List<Newsletter> findByTitleContaining(@Param("title") String title);
}