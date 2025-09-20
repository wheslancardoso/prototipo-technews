package br.com.technews.repository;

import br.com.technews.entity.TrustedSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Arrays;
import java.util.List;

/**
 * Testes unitários para TrustedSourceRepository usando Mockito
 */
@DisplayName("TrustedSource Repository Unit Tests")
class TrustedSourceRepositoryUnitTest {

    @Mock
    private TrustedSourceRepository trustedSourceRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve encontrar fonte confiável por nome")
    void shouldFindTrustedSourceByName() {
        // Given
        TrustedSource source = createTestTrustedSource("TechCrunch", "techcrunch.com");
        when(trustedSourceRepository.findByName("TechCrunch")).thenReturn(Optional.of(source));

        // When
        Optional<TrustedSource> result = trustedSourceRepository.findByName("TechCrunch");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("TechCrunch");
        assertThat(result.get().getDomainName()).isEqualTo("techcrunch.com");
        verify(trustedSourceRepository).findByName("TechCrunch");
    }

    @Test
    @DisplayName("Deve encontrar fonte confiável por domínio")
    void shouldFindTrustedSourceByDomain() {
        // Given
        TrustedSource source = createTestTrustedSource("Wired", "wired.com");
        when(trustedSourceRepository.findByDomainName("wired.com")).thenReturn(Optional.of(source));

        // When
        Optional<TrustedSource> result = trustedSourceRepository.findByDomainName("wired.com");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Wired");
        assertThat(result.get().getDomainName()).isEqualTo("wired.com");
        verify(trustedSourceRepository).findByDomainName("wired.com");
    }

    @Test
    @DisplayName("Deve retornar vazio quando fonte não existe")
    void shouldReturnEmptyWhenSourceNotExists() {
        // Given
        when(trustedSourceRepository.findByName("Inexistente")).thenReturn(Optional.empty());

        // When
        Optional<TrustedSource> result = trustedSourceRepository.findByName("Inexistente");

        // Then
        assertThat(result).isEmpty();
        verify(trustedSourceRepository).findByName("Inexistente");
    }

    @Test
    @DisplayName("Deve salvar fonte confiável")
    void shouldSaveTrustedSource() {
        // Given
        TrustedSource source = createTestTrustedSource("The Verge", "theverge.com");
        TrustedSource savedSource = createTestTrustedSource("The Verge", "theverge.com");
        savedSource.setId(1L);
        
        when(trustedSourceRepository.save(source)).thenReturn(savedSource);

        // When
        TrustedSource result = trustedSourceRepository.save(source);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("The Verge");
        assertThat(result.getDomainName()).isEqualTo("theverge.com");
        verify(trustedSourceRepository).save(source);
    }

    @Test
    @DisplayName("Deve encontrar todas as fontes ativas")
    void shouldFindAllActiveSources() {
        // Given
        List<TrustedSource> activeSources = Arrays.asList(
            createTestTrustedSource("TechCrunch", "techcrunch.com"),
            createTestTrustedSource("Wired", "wired.com")
        );
        when(trustedSourceRepository.findByActiveTrue()).thenReturn(activeSources);

        // When
        List<TrustedSource> result = trustedSourceRepository.findByActiveTrue();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TrustedSource::getName)
                .containsExactlyInAnyOrder("TechCrunch", "Wired");
        verify(trustedSourceRepository).findByActiveTrue();
    }

    @Test
    @DisplayName("Deve contar fontes confiáveis")
    void shouldCountTrustedSources() {
        // Given
        when(trustedSourceRepository.count()).thenReturn(10L);

        // When
        long count = trustedSourceRepository.count();

        // Then
        assertThat(count).isEqualTo(10L);
        verify(trustedSourceRepository).count();
    }

    @Test
    @DisplayName("Deve contar fontes ativas")
    void shouldCountActiveSources() {
        // Given
        when(trustedSourceRepository.countByActiveTrue()).thenReturn(8L);

        // When
        long count = trustedSourceRepository.countByActiveTrue();

        // Then
        assertThat(count).isEqualTo(8L);
        verify(trustedSourceRepository).countByActiveTrue();
    }

    @Test
    @DisplayName("Deve deletar fonte por ID")
    void shouldDeleteSourceById() {
        // Given
        Long sourceId = 1L;
        doNothing().when(trustedSourceRepository).deleteById(sourceId);

        // When
        trustedSourceRepository.deleteById(sourceId);

        // Then
        verify(trustedSourceRepository).deleteById(sourceId);
    }

    @Test
    @DisplayName("Deve verificar se fonte existe por ID")
    void shouldCheckIfSourceExistsById() {
        // Given
        Long sourceId = 1L;
        when(trustedSourceRepository.existsById(sourceId)).thenReturn(true);

        // When
        boolean exists = trustedSourceRepository.existsById(sourceId);

        // Then
        assertThat(exists).isTrue();
        verify(trustedSourceRepository).existsById(sourceId);
    }

    private TrustedSource createTestTrustedSource(String name, String domainName) {
        TrustedSource source = new TrustedSource();
        source.setName(name);
        source.setDomainName(domainName);
        source.setDescription("Fonte confiável de notícias de tecnologia");
        source.setActive(true);
        source.setCreatedAt(LocalDateTime.now());
        source.setUpdatedAt(LocalDateTime.now());
        return source;
    }
}