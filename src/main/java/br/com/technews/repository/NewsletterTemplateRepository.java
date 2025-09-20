package br.com.technews.repository;

import br.com.technews.entity.NewsletterTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositório para NewsletterTemplate
 */
@Repository
public interface NewsletterTemplateRepository extends JpaRepository<NewsletterTemplate, Long> {

    /**
     * Buscar templates ativos
     */
    List<NewsletterTemplate> findByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Buscar template por chave
     */
    Optional<NewsletterTemplate> findByTemplateKeyAndIsActiveTrue(String templateKey);

    /**
     * Buscar template padrão
     */
    Optional<NewsletterTemplate> findByIsDefaultTrueAndIsActiveTrue();

    /**
     * Verificar se existe template com a chave
     */
    boolean existsByTemplateKey(String templateKey);

    /**
     * Buscar templates por nome (busca parcial)
     */
    @Query("SELECT t FROM NewsletterTemplate t WHERE t.isActive = true AND LOWER(t.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY t.createdAt DESC")
    List<NewsletterTemplate> findByNameContainingIgnoreCaseAndIsActiveTrue(@Param("name") String name);

    /**
     * Contar templates ativos
     */
    long countByIsActiveTrue();

    /**
     * Buscar templates criados por usuário
     */
    List<NewsletterTemplate> findByCreatedByAndIsActiveTrueOrderByCreatedAtDesc(String createdBy);
}