package br.com.technews.repository;

import br.com.technews.model.TrustedSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
     * Verifica se existe uma fonte confiável com o domínio especificado
     */
    boolean existsByDomainName(String domainName);
}