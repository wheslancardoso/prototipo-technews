package br.com.technews.repository;

import br.com.technews.entity.Category;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Subscriber.SubscriptionFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Testes de integração para SubscriberRepository
 */
@DataJpaTest
class SubscriberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriberRepository subscriberRepository;

    private Category category1;
    private Category category2;

    @BeforeEach
    void setUp() {
        category1 = new Category();
        category1.setName("Tecnologia");
        category1.setDescription("Categoria de tecnologia");
        category1 = entityManager.persistAndFlush(category1);

        category2 = new Category();
        category2.setName("Ciência");
        category2.setDescription("Categoria de ciência");
        category2 = entityManager.persistAndFlush(category2);
    }

    @Test
    void testFindByEmail() {
        // Given
        Subscriber subscriber = createSubscriber("test@example.com", "João Silva");
        entityManager.persistAndFlush(subscriber);

        // When
        Optional<Subscriber> found = subscriberRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getName()).isEqualTo("João Silva");
    }

    @Test
    void testFindByEmailNotFound() {
        // When
        Optional<Subscriber> found = subscriberRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByVerificationToken() {
        // Given
        Subscriber subscriber = createSubscriber("test@example.com", "João Silva");
        subscriber.setVerificationToken("abc123token");
        entityManager.persistAndFlush(subscriber);

        // When
        Optional<Subscriber> found = subscriberRepository.findByVerificationToken("abc123token");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getVerificationToken()).isEqualTo("abc123token");
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void testFindByVerificationTokenNotFound() {
        // When
        Optional<Subscriber> found = subscriberRepository.findByVerificationToken("invalidtoken");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    void testFindByActiveTrue() {
        // Given
        Subscriber activeSubscriber = createSubscriber("active@example.com", "Ativo");
        activeSubscriber.setActive(true);

        Subscriber inactiveSubscriber = createSubscriber("inactive@example.com", "Inativo");
        inactiveSubscriber.setActive(false);

        entityManager.persist(activeSubscriber);
        entityManager.persist(inactiveSubscriber);
        entityManager.flush();

        // When
        List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();

        // Then
        assertThat(activeSubscribers).hasSize(1);
        assertThat(activeSubscribers.get(0).getEmail()).isEqualTo("active@example.com");
        assertThat(activeSubscribers.get(0).isActive()).isTrue();
    }

    @Test
    void testFindByActiveTrueAndFrequency() {
        // Given
        Subscriber dailySubscriber = createSubscriber("daily@example.com", "Daily User");
        dailySubscriber.setActive(true);
        dailySubscriber.setFrequency(SubscriptionFrequency.DAILY);

        Subscriber weeklySubscriber = createSubscriber("weekly@example.com", "Weekly User");
        weeklySubscriber.setActive(true);
        weeklySubscriber.setFrequency(SubscriptionFrequency.WEEKLY);

        Subscriber inactiveDailySubscriber = createSubscriber("inactive@example.com", "Inactive Daily");
        inactiveDailySubscriber.setActive(false);
        inactiveDailySubscriber.setFrequency(SubscriptionFrequency.DAILY);

        entityManager.persist(dailySubscriber);
        entityManager.persist(weeklySubscriber);
        entityManager.persist(inactiveDailySubscriber);
        entityManager.flush();

        // When
        List<Subscriber> dailyActiveSubscribers = subscriberRepository.findByActiveTrueAndFrequency(SubscriptionFrequency.DAILY);

        // Then
        assertThat(dailyActiveSubscribers).hasSize(1);
        assertThat(dailyActiveSubscribers.get(0).getEmail()).isEqualTo("daily@example.com");
        assertThat(dailyActiveSubscribers.get(0).getFrequency()).isEqualTo(SubscriptionFrequency.DAILY);
    }

    @Test
    void testFindByCategoriesContaining() {
        // Given
        Subscriber subscriber1 = createSubscriber("sub1@example.com", "Subscriber 1");
        subscriber1.setCategories(Set.of(category1));

        Subscriber subscriber2 = createSubscriber("sub2@example.com", "Subscriber 2");
        subscriber2.setCategories(Set.of(category1, category2));

        Subscriber subscriber3 = createSubscriber("sub3@example.com", "Subscriber 3");
        subscriber3.setCategories(Set.of(category2));

        entityManager.persist(subscriber1);
        entityManager.persist(subscriber2);
        entityManager.persist(subscriber3);
        entityManager.flush();

        // When
        List<Subscriber> techSubscribers = subscriberRepository.findByCategoriesContaining(category1);

        // Then
        assertThat(techSubscribers).hasSize(2);
        assertThat(techSubscribers).extracting(Subscriber::getEmail)
                .containsExactlyInAnyOrder("sub1@example.com", "sub2@example.com");
    }

    @Test
    void testSaveSubscriber() {
        // Given
        Subscriber subscriber = createSubscriber("new@example.com", "Novo Usuário");

        // When
        Subscriber saved = subscriberRepository.save(subscriber);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("new@example.com");
        assertThat(saved.getName()).isEqualTo("Novo Usuário");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void testDeleteSubscriber() {
        // Given
        Subscriber subscriber = createSubscriber("delete@example.com", "Para Deletar");
        Subscriber saved = entityManager.persistAndFlush(subscriber);

        // When
        subscriberRepository.deleteById(saved.getId());
        entityManager.flush();

        // Then
        Optional<Subscriber> found = subscriberRepository.findById(saved.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void testUpdateSubscriber() {
        // Given
        Subscriber subscriber = createSubscriber("update@example.com", "Nome Original");
        Subscriber saved = entityManager.persistAndFlush(subscriber);

        // When
        saved.setName("Nome Atualizado");
        saved.setFrequency(SubscriptionFrequency.WEEKLY);
        Subscriber updated = subscriberRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getName()).isEqualTo("Nome Atualizado");
        assertThat(updated.getFrequency()).isEqualTo(SubscriptionFrequency.WEEKLY);
    }

    private Subscriber createSubscriber(String email, String name) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setName(name);
        subscriber.setActive(true);
        subscriber.setFrequency(SubscriptionFrequency.DAILY);
        subscriber.setCreatedAt(LocalDateTime.now());
        subscriber.setCategories(Set.of(category1));
        return subscriber;
    }
}