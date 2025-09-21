package br.com.technews.repository;

import br.com.technews.entity.TrustedSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com a entidade TrustedSource
 */
@Repository
public interface TrustedSourceRepository extends JpaRepository<TrustedSource, Long> {
    
    /**
     * Busca uma fonte confiável pelo nome do domínio
     */
    Optional<TrustedSource> findByDomainName(String domainName);
    
    /**
     * Busca uma fonte confiável pelo nome
     */
    Optional<TrustedSource> findByName(String name);
    
    /**
     * Verifica se existe uma fonte confiável com o domínio especificado
     */
    boolean existsByDomainName(String domainName);
    
    /**
     * Verifica se existe uma fonte confiável com o nome especificado
     */
    boolean existsByName(String name);
    
    /**
     * Verifica se existe uma fonte confiável com o domínio especificado, excluindo um ID específico
     */
    boolean existsByDomainNameAndIdNot(String domainName, Long id);
    
    /**
     * Verifica se existe uma fonte confiável com o nome especificado, excluindo um ID específico
     */
    boolean existsByNameAndIdNot(String name, Long id);
    
    /**
     * Busca todas as fontes confiáveis ativas
     */
    List<TrustedSource> findByActiveTrue();
    
    /**
     * Busca todas as fontes confiáveis inativas
     */
    List<TrustedSource> findByActiveFalse();
    
    /**
     * Busca fontes confiáveis por status (ativo/inativo) com paginação
     */
    Page<TrustedSource> findByActive(Boolean active, Pageable pageable);
    
    /**
     * Busca fontes confiáveis por nome ou domínio (busca parcial)
     */
    @Query("SELECT ts FROM TrustedSource ts WHERE " +
           "LOWER(ts.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ts.domainName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ts.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<TrustedSource> findBySearchTerm(@Param("search") String search, Pageable pageable);
    
    /**
     * Busca fontes confiáveis por nome ou domínio e status
     */
    @Query("SELECT ts FROM TrustedSource ts WHERE " +
           "(LOWER(ts.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ts.domainName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(ts.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND ts.active = :active")
    Page<TrustedSource> findBySearchTermAndActive(@Param("search") String search, 
                                                  @Param("active") Boolean active, 
                                                  Pageable pageable);
    
    /**
     * Conta o número total de fontes confiáveis ativas
     */
    long countByActiveTrue();
    
    /**
     * Conta o número total de fontes confiáveis inativas
     */
    long countByActiveFalse();
    
    /**
     * Busca as fontes confiáveis mais recentes
     */
    @Query("SELECT ts FROM TrustedSource ts ORDER BY ts.createdAt DESC")
    List<TrustedSource> findRecentSources(Pageable pageable);
}