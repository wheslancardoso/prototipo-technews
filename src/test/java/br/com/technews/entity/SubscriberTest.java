package br.com.technews.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Testes unitários para a entidade Subscriber
 */
class SubscriberTest {

    private Subscriber subscriber;
    private Category category;

    @BeforeEach
    void setUp() {
        subscriber = new Subscriber();
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
    }

    @Test
    void testSubscriberCreation() {
        // Given
        String email = "test@example.com";
        String nome = "João Silva";
        Subscriber.SubscriptionFrequency frequency = Subscriber.SubscriptionFrequency.WEEKLY;

        // When
        subscriber.setEmail(email);
        subscriber.setNome(nome);
        subscriber.setFrequencia(frequency);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.getNome()).isEqualTo(nome);
        assertThat(subscriber.getFrequencia()).isEqualTo(frequency);
        assertThat(subscriber.getId()).isNull();
        assertThat(subscriber.isActive()).isTrue(); // Default should be true
    }

    @Test
    void testSubscriberWithCategories() {
        // Given
        String email = "user@test.com";
        Set<Category> categories = new HashSet<>();
        categories.add(category);

        // When
        subscriber.setEmail(email);
        subscriber.setCategorias(categories);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.getCategorias()).hasSize(1);
        assertThat(subscriber.getCategorias()).contains(category);
    }

    @Test
    void testSubscriberEquality() {
        // Given
        Subscriber subscriber1 = new Subscriber();
        subscriber1.setId(1L);
        subscriber1.setEmail("test@example.com");

        Subscriber subscriber2 = new Subscriber();
        subscriber2.setId(1L);
        subscriber2.setEmail("test@example.com");

        // Then
        assertThat(subscriber1).isEqualTo(subscriber2);
        assertThat(subscriber1.hashCode()).isEqualTo(subscriber2.hashCode());
    }

    @Test
    void testSubscriberActiveStatus() {
        // Given
        String email = "active@test.com";

        // When
        subscriber.setEmail(email);
        subscriber.setActive(false);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.isActive()).isFalse();
    }

    @Test
    void testSubscriberVerificationToken() {
        // Given
        String email = "verify@test.com";
        String token = "verification-token-123";

        // When
        subscriber.setEmail(email);
        subscriber.setVerificationToken(token);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.getVerificationToken()).isEqualTo(token);
        assertThat(subscriber.isVerified()).isFalse(); // Default should be false
    }

    @Test
    void testSubscriberVerification() {
        // Given
        String email = "verified@test.com";

        // When
        subscriber.setEmail(email);
        subscriber.setVerified(true);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.isVerified()).isTrue();
    }

    @Test
    void testSubscriptionFrequencyEnum() {
        // Test all frequency options
        assertThat(Subscriber.SubscriptionFrequency.DAILY).isNotNull();
        assertThat(Subscriber.SubscriptionFrequency.WEEKLY).isNotNull();
        assertThat(Subscriber.SubscriptionFrequency.MONTHLY).isNotNull();
    }

    @Test
    void testSubscriberToString() {
        // Given
        subscriber.setId(1L);
        subscriber.setEmail("test@example.com");
        subscriber.setNome("Test User");

        // When
        String toString = subscriber.toString();

        // Then
        assertThat(toString).contains("test@example.com");
        assertThat(toString).contains("1");
    }

    @Test
    void testSubscriberCreatedAtTimestamp() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        
        // When
        Subscriber newSubscriber = new Subscriber();
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

        // Then - createdAt should be set automatically if using @PrePersist
        assertThat(newSubscriber.getCreatedAt()).isBetween(beforeCreation, afterCreation);
    }
}