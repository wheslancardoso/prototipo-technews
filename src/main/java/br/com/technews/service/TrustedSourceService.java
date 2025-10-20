package br.com.technews.service;

import br.com.technews.entity.TrustedSource;
import br.com.technews.repository.TrustedSourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service para gerenciamento de fontes confiáveis
 */
@Service
@Transactional
public class TrustedSourceService {

    @Autowired
    private TrustedSourceRepository trustedSourceRepository;

    /**
     * Busca todas as fontes confiáveis com paginação
     */
    @Transactional(readOnly = true)
    public Page<TrustedSource> findAll(Pageable pageable) {
        return trustedSourceRepository.findAll(pageable);
    }

    /**
     * Busca fonte confiável por ID
     */
    @Transactional(readOnly = true)
    public Optional<TrustedSource> findById(Long id) {
        return trustedSourceRepository.findById(id);
    }

    /**
     * Busca fonte confiável por domínio
     */
    @Transactional(readOnly = true)
    public Optional<TrustedSource> findByDomainName(String domainName) {
        return trustedSourceRepository.findByDomainName(domainName);
    }

    /**
     * Busca fonte confiável por nome
     */
    @Transactional(readOnly = true)
    public Optional<TrustedSource> findByName(String name) {
        return trustedSourceRepository.findByName(name);
    }

    /**
     * Busca fontes confiáveis por termo de pesquisa
     */
    @Transactional(readOnly = true)
    public Page<TrustedSource> findBySearchTerm(String search, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return findAll(pageable);
        }
        return trustedSourceRepository.findBySearchTerm(search.trim(), pageable);
    }

    /**
     * Busca fontes confiáveis por termo de pesquisa e status
     */
    @Transactional(readOnly = true)
    public Page<TrustedSource> findBySearchTermAndActive(String search, Boolean active, Pageable pageable) {
        if (search == null || search.trim().isEmpty()) {
            return trustedSourceRepository.findByActive(active, pageable);
        }
        return trustedSourceRepository.findBySearchTermAndActive(search.trim(), active, pageable);
    }

    /**
     * Busca todas as fontes confiáveis ativas
     */
    @Transactional(readOnly = true)
    public List<TrustedSource> findActiveSource() {
        return trustedSourceRepository.findByActiveTrue();
    }

    /**
     * Busca fontes confiáveis por status
     */
    @Transactional(readOnly = true)
    public Page<TrustedSource> findByActive(Boolean active, Pageable pageable) {
        return trustedSourceRepository.findByActive(active, pageable);
    }

    /**
     * Busca as fontes confiáveis mais recentes
     */
    @Transactional(readOnly = true)
    public List<TrustedSource> findRecentSources(int limit) {
        return trustedSourceRepository.findRecentSources(Pageable.ofSize(limit));
    }

    /**
     * Salva uma nova fonte confiável
     */
    public TrustedSource save(TrustedSource trustedSource) {
        validateTrustedSource(trustedSource);
        return trustedSourceRepository.save(trustedSource);
    }

    /**
     * Atualiza uma fonte confiável existente
     */
    public TrustedSource update(Long id, TrustedSource trustedSource) {
        Optional<TrustedSource> existingSource = findById(id);
        if (existingSource.isEmpty()) {
            throw new RuntimeException("Fonte confiável não encontrada com ID: " + id);
        }

        TrustedSource existing = existingSource.get();
        
        // Validar se nome e domínio não estão sendo usados por outra fonte
        validateTrustedSourceForUpdate(trustedSource, id);
        
        // Atualizar campos
        existing.setName(trustedSource.getName());
        existing.setDomainName(trustedSource.getDomainName());
        existing.setDescription(trustedSource.getDescription());
        existing.setActive(trustedSource.getActive());

        return trustedSourceRepository.save(existing);
    }

    /**
     * Alterna o status ativo/inativo de uma fonte confiável
     */
    public TrustedSource toggleActive(Long id) {
        Optional<TrustedSource> sourceOpt = findById(id);
        if (sourceOpt.isEmpty()) {
            throw new RuntimeException("Fonte confiável não encontrada com ID: " + id);
        }

        TrustedSource source = sourceOpt.get();
        source.toggleActive();
        return trustedSourceRepository.save(source);
    }

    /**
     * Remove uma fonte confiável
     */
    public void delete(Long id) {
        if (!trustedSourceRepository.existsById(id)) {
            throw new RuntimeException("Fonte confiável não encontrada com ID: " + id);
        }
        trustedSourceRepository.deleteById(id);
    }

    /**
     * Conta o total de fontes confiáveis
     */
    @Transactional(readOnly = true)
    public long count() {
        return trustedSourceRepository.count();
    }

    /**
     * Conta fontes confiáveis ativas
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return trustedSourceRepository.countByActiveTrue();
    }

    /**
     * Conta fontes confiáveis inativas
     */
    @Transactional(readOnly = true)
    public long countInactive() {
        return trustedSourceRepository.countByActiveFalse();
    }

    /**
     * Verifica se um domínio já está cadastrado
     */
    @Transactional(readOnly = true)
    public boolean isDomainNameExists(String domainName) {
        return trustedSourceRepository.existsByDomainName(domainName);
    }

    /**
     * Verifica se um nome já está cadastrado
     */
    @Transactional(readOnly = true)
    public boolean isNameExists(String name) {
        return trustedSourceRepository.existsByName(name);
    }

    /**
     * Verifica se um domínio é de uma fonte confiável ativa
     */
    @Transactional(readOnly = true)
    public boolean isTrustedDomain(String domainName) {
        Optional<TrustedSource> source = findByDomainName(domainName);
        return source.isPresent() && source.get().isActive();
    }

    /**
     * Valida uma fonte confiável antes de salvar
     */
    private void validateTrustedSource(TrustedSource trustedSource) {
        if (trustedSource.getName() == null || trustedSource.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da fonte é obrigatório");
        }

        if (trustedSource.getDomainName() == null || trustedSource.getDomainName().trim().isEmpty()) {
            throw new IllegalArgumentException("Domínio é obrigatório");
        }

        // Verificar se nome já existe
        if (isNameExists(trustedSource.getName())) {
            throw new IllegalArgumentException("Já existe uma fonte confiável com este nome");
        }

        // Verificar se domínio já existe
        if (isDomainNameExists(trustedSource.getDomainName())) {
            throw new IllegalArgumentException("Já existe uma fonte confiável com este domínio");
        }
    }

    /**
     * Valida uma fonte confiável antes de atualizar
     */
    private void validateTrustedSourceForUpdate(TrustedSource trustedSource, Long id) {
        if (trustedSource.getName() == null || trustedSource.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da fonte é obrigatório");
        }

        if (trustedSource.getDomainName() == null || trustedSource.getDomainName().trim().isEmpty()) {
            throw new IllegalArgumentException("Domínio é obrigatório");
        }

        // Verificar se nome já existe (excluindo o próprio registro)
        if (trustedSourceRepository.existsByNameAndIdNot(trustedSource.getName(), id)) {
            throw new IllegalArgumentException("Já existe uma fonte confiável com este nome");
        }

        // Verificar se domínio já existe (excluindo o próprio registro)
        if (trustedSourceRepository.existsByDomainNameAndIdNot(trustedSource.getDomainName(), id)) {
            throw new IllegalArgumentException("Já existe uma fonte confiável com este domínio");
        }
    }
}