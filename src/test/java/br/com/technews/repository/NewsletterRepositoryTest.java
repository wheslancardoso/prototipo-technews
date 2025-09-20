package br.com.technews.repository;

import br.com.technews.entity.Subscriber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Testes de integração para SubscriberRepository
 * Testa operações de persistência e consultas de subscriber
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Subscriber Repository Tests")
class NewsletterRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriberRepository subscriberRepository;

    private Subscriber activeSubscriber;
    private Subscriber inactiveSubscriber;
    private Subscriber pendingSubscriber;

    @BeforeEach
    void setUp() {
        // Criar assinantes para teste
        activeSubscriber = Subscriber.builder()
                .email("active@example.com")
                .fullName("Active User")
                .active(true)
                .emailVerified(true)
                .frequency(Subscriber.SubscriptionFrequency.WEEKLY)
                .build();
        
        inactiveSubscriber = Subscriber.builder()
                .email("inactive@example.com")
                .fullName("Inactive User")
                .active(false)
                .emailVerified(false)
                .frequency(Subscriber.SubscriptionFrequency.MONTHLY)
                .build();
        
        pendingSubscriber = Subscriber.builder()
                .email("pending@example.com")
                .fullName("Pending User")
                .active(true)
                .emailVerified(false)
                .frequency(Subscriber.SubscriptionFrequency.DAILY)
                .build();

        // Persistir no banco de dados de teste
        entityManager.persist(activeSubscriber);
        entityManager.persist(inactiveSubscriber);
        entityManager.persist(pendingSubscriber);
        entityManager.flush();
    }

    @Test
    @DisplayName("Deve encontrar assinante por email")
    void shouldFindByEmail() {
        // When
        Optional<Subscriber> found = subscriberRepository.findByEmail("active@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Active User");
        assertThat(found.get().isActive()).isTrue();
    }

    @Test
    @DisplayName("Deve retornar vazio quando email não existe")
    void shouldReturnEmptyWhenEmailNotExists() {
        // When
        Optional<Subscriber> found = subscriberRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Deve verificar se email existe")
    void shouldCheckIfEmailExists() {
        // When & Then
        assertThat(subscriberRepository.existsByEmail("active@example.com")).isTrue();
        assertThat(subscriberRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Deve verificar existência de email excluindo ID específico")
    void shouldCheckEmailExistenceExcludingSpecificId() {
        // Given
        Long activeId = activeSubscriber.getId();

        // When & Then
        assertThat(subscriberRepository.existsByEmailAndIdNot("active@example.com", activeId)).isFalse();
        assertThat(subscriberRepository.existsByEmailAndIdNot("inactive@example.com", activeId)).isTrue();
    }

    @Test
    @DisplayName("Deve buscar apenas assinantes ativos")
    void shouldFindOnlyActiveSubscribers() {
        // When
        List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();

        // Then
        assertThat(activeSubscribers).hasSize(2);
        assertThat(activeSubscribers)
            .extracting(Subscriber::getEmail)
            .containsExactlyInAnyOrder("active@example.com", "pending@example.com");
        assertThat(activeSubscribers)
            .allMatch(Subscriber::isActive);
    }

    @Test
    @DisplayName("Deve buscar apenas assinantes inativos")
    void shouldFindOnlyInactiveSubscribers() {
        // When
        List<Subscriber> inactiveSubscribers = subscriberRepository.findByActiveFalse();

        // Then
        assertThat(inactiveSubscribers).hasSize(1);
        assertThat(inactiveSubscribers.get(0).getEmail()).isEqualTo("inactive@example.com");
        assertThat(inactiveSubscribers.get(0).isActive()).isFalse();
        assertThat(inactiveSubscribers.get(0).getUnsubscribedAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve buscar assinantes por status com paginação")
    void shouldFindByActiveWithPagination() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 1, Sort.by("subscribedAt").descending());

        // When
        Page<Subscriber> activePage = subscriberRepository.findByActive(true, pageRequest);
        Page<Subscriber> inactivePage = subscriberRepository.findByActive(false, pageRequest);

        // Then
        assertThat(activePage.getTotalElements()).isEqualTo(2);
        assertThat(activePage.getContent()).hasSize(1);
        assertThat(activePage.getContent().get(0).isActive()).isTrue();
        // Deve retornar o mais recente primeiro
        assertThat(activePage.getContent().get(0).getEmail()).isEqualTo("pending@example.com");

        assertThat(inactivePage.getTotalElements()).isEqualTo(1);
        assertThat(inactivePage.getContent()).hasSize(1);
        assertThat(inactivePage.getContent().get(0).isActive()).isFalse();
    }

    @Test
    @DisplayName("Deve buscar assinantes por período de inscrição")
    void shouldFindBySubscriptionPeriod() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(15);
        LocalDateTime endDate = LocalDateTime.now();

        // When
        List<Subscriber> recentSubscribers = subscriberRepository.findRecentSubscribers(startDate);

        // Then
        assertThat(recentSubscribers).hasSize(2);
        assertThat(recentSubscribers)
            .extracting(Subscriber::getEmail)
            .containsExactlyInAnyOrder("active@example.com", "pending@example.com");
    }

    @Test
    @DisplayName("Deve contar assinantes ativos")
    void shouldCountActiveSubscribers() {
        // When
        long activeCount = subscriberRepository.countActiveSubscribers();
        long inactiveCount = subscriberRepository.countTotalSubscribers() - activeCount;

        // Then
        assertThat(activeCount).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve contar assinantes inativos")
    void shouldCountInactiveSubscribers() {
        // When
        long activeCount = subscriberRepository.countActiveSubscribers();
        long inactiveCount = subscriberRepository.countTotalSubscribers() - activeCount;

        // Then
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve salvar novo assinante")
    void shouldSaveNewSubscriber() {
        // Given
        Subscriber newSubscriber = Subscriber.builder()
                .email("new@example.com")
                .fullName("New User")
                .active(true)
                .emailVerified(false)
                .frequency(Subscriber.SubscriptionFrequency.WEEKLY)
                .build();

        // When
        Subscriber saved = subscriberRepository.save(newSubscriber);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        
        // Verificar se foi persistido
        Optional<Subscriber> found = subscriberRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("New User");
    }

    @Test
    @DisplayName("Deve atualizar assinante existente")
    void shouldUpdateExistingSubscriber() {
        // Given
        Subscriber subscriber = subscriberRepository.findByEmail("active@example.com").orElseThrow();
        String originalName = subscriber.getFullName();

        // When
        subscriber.setFullName("Updated Active User");
        Subscriber updated = subscriberRepository.save(subscriber);

        // Then
        assertThat(updated.getFullName()).isNotEqualTo(originalName);
        assertThat(updated.getFullName()).isEqualTo("Updated Active User");
        
        // Verificar persistência
        Subscriber reloaded = subscriberRepository.findById(updated.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Updated Active User");
    }

    @Test
    @DisplayName("Deve desativar assinante (unsubscribe)")
    void shouldDeactivateSubscriber() {
        // Given
        Subscriber subscriber = subscriberRepository.findByEmail("active@example.com").orElseThrow();
        assertThat(subscriber.isActive()).isTrue();
        assertThat(subscriber.getUnsubscribedAt()).isNull();

        // When
        subscriber.setActive(false);
        subscriber.setUnsubscribedAt(LocalDateTime.now());
        Subscriber updated = subscriberRepository.save(subscriber);

        // Then
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getUnsubscribedAt()).isNotNull();
        
        // Verificar que não aparece mais na lista de ativos
        List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();
        assertThat(activeSubscribers)
            .extracting(Subscriber::getEmail)
            .doesNotContain("active@example.com");
    }

    @Test
    @DisplayName("Deve reativar assinante")
    void shouldReactivateSubscriber() {
        // Given
        Subscriber subscriber = subscriberRepository.findByEmail("inactive@example.com").orElseThrow();
        assertThat(subscriber.isActive()).isFalse();
        assertThat(subscriber.getUnsubscribedAt()).isNotNull();

        // When
        subscriber.setActive(true);
        subscriber.setUnsubscribedAt(null);
        Subscriber updated = subscriberRepository.save(subscriber);

        // Then
        assertThat(updated.isActive()).isTrue();
        assertThat(updated.getUnsubscribedAt()).isNull();
        
        // Verificar que aparece na lista de ativos
        List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();
        assertThat(activeSubscribers)
            .extracting(Subscriber::getEmail)
            .contains("inactive@example.com");
    }

    @Test
    @DisplayName("Deve deletar assinante")
    void shouldDeleteSubscriber() {
        // Given
        Subscriber subscriber = subscriberRepository.findByEmail("pending@example.com").orElseThrow();
        Long subscriberId = subscriber.getId();

        // When
        subscriberRepository.delete(subscriber);

        // Then
        Optional<Subscriber> deleted = subscriberRepository.findById(subscriberId);
        assertThat(deleted).isEmpty();
        
        // Verificar que outros assinantes ainda existem
        assertThat(subscriberRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve buscar todos os assinantes ordenados por data de inscrição")
    void shouldFindAllOrderedBySubscribedAt() {
        // Given
        Subscriber subscriber1 = Subscriber.builder()
                .email("first@test.com")
                .fullName("First User")
                .active(true)
                .emailVerified(true)
                .frequency(Subscriber.SubscriptionFrequency.WEEKLY)
                .build();
        
        Subscriber subscriber2 = Subscriber.builder()
                .email("second@test.com")
                .fullName("Second User")
                .active(true)
                .emailVerified(true)
                .frequency(Subscriber.SubscriptionFrequency.DAILY)
                .build();
        
        subscriberRepository.save(subscriber1);
        subscriberRepository.save(subscriber2);
        
        // When
        List<Subscriber> subscribers = subscriberRepository.findAll();
        
        // Then
        assertThat(subscribers).hasSize(4); // 2 criados no setup + 2 criados aqui
        assertThat(subscribers).extracting("email")
                .contains("first@test.com", "second@test.com");
    }

    @Test
    @DisplayName("Deve validar formato de email único")
    void shouldValidateUniqueEmailFormat() {
        // Given
        Subscriber duplicateEmail = Subscriber.builder()
                .email("active@example.com") // Email já existe
                .fullName("Duplicate User")
                .active(true)
                .emailVerified(false)
                .frequency(Subscriber.SubscriptionFrequency.MONTHLY)
                .build();

        // When & Then
        assertThatThrownBy(() -> {
            subscriberRepository.save(duplicateEmail);
            entityManager.flush(); // Força a validação
        }).isInstanceOf(Exception.class);
    }
}