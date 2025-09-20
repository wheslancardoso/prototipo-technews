package br.com.technews.repository;

import br.com.technews.entity.TrustedSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Testes de integração para TrustedSourceRepository
 * Testa operações de persistência e consultas customizadas
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TrustedSource Repository Tests")
class TrustedSourceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TrustedSourceRepository trustedSourceRepository;

    private TrustedSource activeTechSource;
    private TrustedSource inactiveTechSource;
    private TrustedSource activeNewsSource;

    @BeforeEach
    void setUp() {
        // Criar fontes confiáveis para teste
        activeTechSource = new TrustedSource();
        activeTechSource.setName("TechCrunch");
        activeTechSource.setDomainName("techcrunch.com");
        activeTechSource.setDescription("Leading technology news source");
        activeTechSource.setActive(true);
        activeTechSource.setCreatedAt(LocalDateTime.now().minusDays(10));

        inactiveTechSource = new TrustedSource();
        inactiveTechSource.setName("Old Tech Blog");
        inactiveTechSource.setDomainName("oldtech.com");
        inactiveTechSource.setDescription("Inactive tech blog");
        inactiveTechSource.setActive(false);
        inactiveTechSource.setCreatedAt(LocalDateTime.now().minusDays(30));

        activeNewsSource = new TrustedSource();
        activeNewsSource.setName("BBC Technology");
        activeNewsSource.setDomainName("bbc.com");
        activeNewsSource.setDescription("BBC Technology section");
        activeNewsSource.setActive(true);
        activeNewsSource.setCreatedAt(LocalDateTime.now().minusDays(5));

        // Persistir no banco H2 de teste
        entityManager.persistAndFlush(activeTechSource);
        entityManager.persistAndFlush(inactiveTechSource);
        entityManager.persistAndFlush(activeNewsSource);
    }

    @Test
    @DisplayName("Deve encontrar fonte confiável por nome de domínio")
    void shouldFindByDomainName() {
        // When
        Optional<TrustedSource> found = trustedSourceRepository.findByDomainName("techcrunch.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TechCrunch");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar vazio quando domínio não existe")
    void shouldReturnEmptyWhenDomainNotExists() {
        // When
        Optional<TrustedSource> found = trustedSourceRepository.findByDomainName("nonexistent.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve encontrar fonte confiável por nome")
    void shouldFindByName() {
        // When
        Optional<TrustedSource> found = trustedSourceRepository.findByName("TechCrunch");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getDomainName()).isEqualTo("techcrunch.com");
    }

    @Test
    @DisplayName("Deve verificar se existe domínio")
    void shouldCheckIfDomainExists() {
        // When & Then
        assertThat(trustedSourceRepository.existsByDomainName("techcrunch.com")).isTrue();
        assertThat(trustedSourceRepository.existsByDomainName("nonexistent.com")).isFalse();
    }

    @Test
    @DisplayName("Deve verificar se existe nome")
    void shouldCheckIfNameExists() {
        // When & Then
        assertThat(trustedSourceRepository.existsByName("TechCrunch")).isTrue();
        assertThat(trustedSourceRepository.existsByName("Nonexistent Source")).isFalse();
    }

    @Test
    @DisplayName("Deve verificar existência excluindo ID específico")
    void shouldCheckExistenceExcludingSpecificId() {
        // Given
        Long techCrunchId = activeTechSource.getId();

        // When & Then
        assertThat(trustedSourceRepository.existsByDomainNameAndIdNot("techcrunch.com", techCrunchId)).isFalse();
        assertThat(trustedSourceRepository.existsByDomainNameAndIdNot("bbc.com", techCrunchId)).isTrue();
        
        assertThat(trustedSourceRepository.existsByNameAndIdNot("TechCrunch", techCrunchId)).isFalse();
        assertThat(trustedSourceRepository.existsByNameAndIdNot("BBC Technology", techCrunchId)).isTrue();
    }

    @Test
    @DisplayName("Deve buscar apenas fontes ativas")
    void shouldFindOnlyActiveSources() {
        // When
        List<TrustedSource> activeSources = trustedSourceRepository.findByActiveTrue();

        // Then
        assertThat(activeSources).hasSize(2);
        assertThat(activeSources)
            .extracting(TrustedSource::getName)
            .containsExactlyInAnyOrder("TechCrunch", "BBC Technology");
        assertThat(activeSources)
            .allMatch(TrustedSource::isActive);
    }

    @Test
    @DisplayName("Deve buscar apenas fontes inativas")
    void shouldFindOnlyInactiveSources() {
        // When
        List<TrustedSource> inactiveSources = trustedSourceRepository.findByActiveFalse();

        // Then
        assertThat(inactiveSources).hasSize(1);
        assertThat(inactiveSources.get(0).getName()).isEqualTo("Old Tech Blog");
        assertThat(inactiveSources.get(0).isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve buscar fontes por status com paginação")
    void shouldFindByActiveWithPagination() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 1);

        // When
        Page<TrustedSource> activePage = trustedSourceRepository.findByActive(true, pageRequest);
        Page<TrustedSource> inactivePage = trustedSourceRepository.findByActive(false, pageRequest);

        // Then
        assertThat(activePage.getTotalElements()).isEqualTo(2);
        assertThat(activePage.getContent()).hasSize(1);
        assertThat(activePage.getContent().get(0).isActive()).isTrue();

        assertThat(inactivePage.getTotalElements()).isEqualTo(1);
        assertThat(inactivePage.getContent()).hasSize(1);
        assertThat(inactivePage.getContent().get(0).isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve salvar nova fonte confiável")
    void shouldSaveNewTrustedSource() {
        // Given
        TrustedSource newSource = new TrustedSource();
        newSource.setName("Ars Technica");
        newSource.setDomainName("arstechnica.com");
        newSource.setDescription("In-depth technology news and analysis");
        newSource.setActive(true);
        newSource.setCreatedAt(LocalDateTime.now());

        // When
        TrustedSource saved = trustedSourceRepository.save(newSource);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Ars Technica");
        
        // Verificar se foi persistido
        Optional<TrustedSource> found = trustedSourceRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDomainName()).isEqualTo("arstechnica.com");
    }

    @Test
    @DisplayName("Deve atualizar fonte confiável existente")
    void shouldUpdateExistingTrustedSource() {
        // Given
        TrustedSource source = trustedSourceRepository.findByName("TechCrunch").orElseThrow();
        String originalDescription = source.getDescription();

        // When
        source.setDescription("Updated description for TechCrunch");
        TrustedSource updated = trustedSourceRepository.save(source);

        // Then
        assertThat(updated.getDescription()).isNotEqualTo(originalDescription);
        assertThat(updated.getDescription()).isEqualTo("Updated description for TechCrunch");
        
        // Verificar persistência
        TrustedSource reloaded = trustedSourceRepository.findById(updated.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isEqualTo("Updated description for TechCrunch");
    }

    @Test
    @DisplayName("Deve deletar fonte confiável")
    void shouldDeleteTrustedSource() {
        // Given
        TrustedSource source = trustedSourceRepository.findByName("Old Tech Blog").orElseThrow();
        Long sourceId = source.getId();

        // When
        trustedSourceRepository.delete(source);

        // Then
        Optional<TrustedSource> deleted = trustedSourceRepository.findById(sourceId);
        assertThat(deleted).isEmpty();
        
        // Verificar que outras fontes ainda existem
        assertThat(trustedSourceRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve contar total de fontes")
    void shouldCountTotalSources() {
        // When
        long count = trustedSourceRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve buscar todas as fontes ordenadas")
    void shouldFindAllSourcesOrdered() {
        // When
        List<TrustedSource> allSources = trustedSourceRepository.findAll();

        // Then
        assertThat(allSources).hasSize(3);
        assertThat(allSources)
            .extracting(TrustedSource::getName)
            .containsExactlyInAnyOrder("TechCrunch", "Old Tech Blog", "BBC Technology");
    }
}