package br.com.technews.repository;

import br.com.technews.entity.Category;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Subscriber.SubscriptionFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Testes de integração para SubscriberRepository
 */
@DataJpaTest
@ActiveProfiles("test")
class SubscriberRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

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
        assertThat(found.get().getFullName()).isEqualTo("João Silva");
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
    void testFindByFrequencyAndActive() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        
        Subscriber dailySubscriber = createSubscriber("daily@example.com", "Daily User");
        dailySubscriber.setActive(true);
        dailySubscriber.setFrequency(Subscriber.SubscriptionFrequency.DAILY);

        Subscriber weeklySubscriber = createSubscriber("weekly@example.com", "Weekly User");
        weeklySubscriber.setActive(true);
        weeklySubscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);

        Subscriber inactiveSubscriber = createSubscriber("inactive@example.com", "Inactive User");
        inactiveSubscriber.setActive(false);
        inactiveSubscriber.setFrequency(Subscriber.SubscriptionFrequency.DAILY);

        subscriberRepository.saveAll(Arrays.asList(dailySubscriber, weeklySubscriber, inactiveSubscriber));

        // When
        Page<Subscriber> dailyActiveSubscribers = subscriberRepository.findByFrequencyAndActive(Subscriber.SubscriptionFrequency.DAILY, true, pageRequest);

        // Then
        assertThat(dailyActiveSubscribers.getContent()).hasSize(1);
        assertThat(dailyActiveSubscribers.getContent().get(0).getEmail()).isEqualTo("daily@example.com");
        assertThat(dailyActiveSubscribers.getContent().get(0).getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.DAILY);
    }

    @Test
    void testFindActiveVerifiedByCategory() {
        // Given
        Category category1 = createCategory("Technology");
        Category category2 = createCategory("Sports");
        
        Subscriber subscriber1 = createSubscriber("tech@example.com", "Tech User");
        subscriber1.setActive(true);
        subscriber1.setEmailVerified(true);
        subscriber1.setSubscribedCategories(new HashSet<>());
        subscriber1.addCategory(category1);

        Subscriber subscriber2 = createSubscriber("sports@example.com", "Sports User");
        subscriber2.setActive(true);
        subscriber2.setEmailVerified(true);
        subscriber2.setSubscribedCategories(new HashSet<>());
        subscriber2.addCategory(category2);

        Subscriber inactiveSubscriber = createSubscriber("inactive@example.com", "Inactive User");
        inactiveSubscriber.setActive(false);
        inactiveSubscriber.setEmailVerified(true);
        inactiveSubscriber.setSubscribedCategories(new HashSet<>());
        inactiveSubscriber.addCategory(category1);

        categoryRepository.saveAll(Arrays.asList(category1, category2));
        subscriberRepository.saveAll(Arrays.asList(subscriber1, subscriber2, inactiveSubscriber));

        // When
        List<Subscriber> techSubscribers = subscriberRepository.findActiveVerifiedByCategory(category1);

        // Then
        assertThat(techSubscribers).hasSize(1);
        assertThat(techSubscribers.get(0).getEmail()).isEqualTo("tech@example.com");
        assertThat(techSubscribers.get(0).isSubscribedToCategory(category1)).isTrue();
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
        assertThat(saved.getFullName()).isEqualTo("Novo Usuário");
        assertThat(saved.getSubscribedAt()).isNotNull();
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
        saved.setFullName("Nome Atualizado");
        saved.setFrequency(SubscriptionFrequency.WEEKLY);
        Subscriber updated = subscriberRepository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(saved.getId());
        assertThat(updated.getFullName()).isEqualTo("Nome Atualizado");
        assertThat(updated.getFrequency()).isEqualTo(SubscriptionFrequency.WEEKLY);
    }

    private Subscriber createSubscriber(String email, String name) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFullName(name);
        subscriber.setActive(true);
        subscriber.setFrequency(SubscriptionFrequency.DAILY);
        // subscribedAt is automatically set by @CreationTimestamp
        subscriber.setSubscribedCategories(new HashSet<>(Set.of(category1)));
        return subscriber;
    }

    private Category createCategory(String name) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(name.toLowerCase());
        category.setActive(true);
        return category;
    }
}