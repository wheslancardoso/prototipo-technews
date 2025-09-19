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
        subscriber.setId(1L);
        subscriber.setEmail("test@example.com");
        subscriber.setFullName("Test User");
        subscriber.setActive(true);
        subscriber.setEmailVerified(false);
        subscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);
        subscriber.setSubscribedAt(LocalDateTime.now());
        subscriber.setUpdatedAt(LocalDateTime.now());
        
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
    }

    @Test
    void testSubscriberCreation() {
        assertThat(subscriber.getId()).isEqualTo(1L);
        assertThat(subscriber.getEmail()).isEqualTo("test@example.com");
        assertThat(subscriber.getFullName()).isEqualTo("Test User");
        assertThat(subscriber.isActive()).isTrue();
        assertThat(subscriber.isEmailVerified()).isFalse();
        assertThat(subscriber.getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.WEEKLY);
        assertThat(subscriber.getSubscribedAt()).isNotNull();
        assertThat(subscriber.getUpdatedAt()).isNotNull();
    }

    @Test
    void testSubscriberWithCategories() {
        // Given
        String email = "user@test.com";
        Set<Category> categories = new HashSet<>();
        categories.add(category);

        // When
        subscriber.setEmail(email);
        subscriber.setSubscribedCategories(categories);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.getSubscribedCategories()).hasSize(1);
        assertThat(subscriber.getSubscribedCategories()).contains(category);
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
        assertThat(subscriber.isEmailVerified()).isFalse(); // Default should be false
    }

    @Test
    void testEmailVerification() {
        // Given
        String email = "verified@test.com";

        // When
        subscriber.setEmail(email);
        subscriber.setEmailVerified(true);

        // Then
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.isEmailVerified()).isTrue();
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
        subscriber.setFullName("Test User");

        // When
        String toString = subscriber.toString();

        // Then
        assertThat(toString).contains("test@example.com");
        assertThat(toString).contains("1");
    }

    @Test
    void testSetId() {
        subscriber.setId(2L);
        assertThat(subscriber.getId()).isEqualTo(2L);
    }

    @Test
    void testSetEmail() {
        subscriber.setEmail("new@example.com");
        assertThat(subscriber.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void testSetFullName() {
        subscriber.setFullName("New Name");
        assertThat(subscriber.getFullName()).isEqualTo("New Name");
    }

    @Test
    void testActivateDeactivate() {
        subscriber.deactivate();
        assertThat(subscriber.isActive()).isFalse();
        
        subscriber.activate();
        assertThat(subscriber.isActive()).isTrue();
    }

    @Test
    void testEmailVerificationMethods() {
        assertThat(subscriber.isEmailVerified()).isFalse();
        
        subscriber.verifyEmail();
        assertThat(subscriber.isEmailVerified()).isTrue();
    }

    @Test
    void testSubscriberCreatedAtTimestamp() {
        // Given
        LocalDateTime beforeCreation = LocalDateTime.now().minusSeconds(1);
        
        // When
        Subscriber newSubscriber = new Subscriber();
        newSubscriber.setSubscribedAt(LocalDateTime.now());
        LocalDateTime afterCreation = LocalDateTime.now().plusSeconds(1);

        // Then - createdAt should be set automatically if using @PrePersist
        assertThat(newSubscriber.getSubscribedAt()).isBetween(beforeCreation, afterCreation);
    }
}