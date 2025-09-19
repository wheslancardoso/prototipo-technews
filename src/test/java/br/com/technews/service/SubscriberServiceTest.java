package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Subscriber.SubscriptionFrequency;
import br.com.technews.repository.SubscriberRepository;
import br.com.technews.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Testes unitários para SubscriberService
 */
@ExtendWith(MockitoExtension.class)
class SubscriberServiceTest {

    @Mock
    private SubscriberRepository subscriberRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private SubscriberService subscriberService;

    private Subscriber subscriber;
    private Category category;

    @BeforeEach
    void setUp() {
        category = new Category();
        category.setId(1L);
        category.setName("Tecnologia");
        category.setDescription("Categoria de tecnologia");

        subscriber = Subscriber.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("João Silva")
                .active(true)
                .frequency(SubscriptionFrequency.DAILY)
                .build();
    }

    @Test
    void testGetAllSubscribers() {
        // Given
        List<Subscriber> subscribers = Arrays.asList(subscriber);
        when(subscriberRepository.findAll()).thenReturn(subscribers);

        // When
        List<Subscriber> result = subscriberService.getAllSubscribers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(subscriber);
        verify(subscriberRepository).findAll();
    }

    @Test
    void testFindActiveSubscribers() {
        // Given
        List<Subscriber> activeSubscribers = Arrays.asList(subscriber);
        when(subscriberRepository.findByActiveTrue()).thenReturn(activeSubscribers);

        // When
        List<Subscriber> result = subscriberService.findActiveSubscribers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(subscriber);
        verify(subscriberRepository).findByActiveTrue();
    }

    @Test
    void testFindByEmailWithSubscriber() {
        // Given
        String email = "test@example.com";
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(subscriber));

        // When
        Optional<Subscriber> result = subscriberService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(subscriber);
        verify(subscriberRepository).findByEmail(email);
    }

    @Test
    void testFindActiveAndVerifiedSubscribers() {
        // Given
        List<Subscriber> activeVerifiedSubscribers = Arrays.asList(subscriber);
        when(subscriberRepository.findActiveAndVerifiedSubscribers()).thenReturn(activeVerifiedSubscribers);

        // When
        List<Subscriber> result = subscriberService.findActiveAndVerifiedSubscribers();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(subscriber);
        verify(subscriberRepository).findActiveAndVerifiedSubscribers();
    }

    @Test
    void testSubscribe() {
        // Given
        String email = "new@example.com";
        String fullName = "Novo Usuário";
        SubscriptionFrequency frequency = SubscriptionFrequency.WEEKLY;
        Set<Long> categoryIds = Set.of(1L);

        when(subscriberRepository.existsByEmail(email)).thenReturn(false);
        when(categoryRepository.findAllById(categoryIds)).thenReturn(Arrays.asList(category));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        // When
        Subscriber result = subscriberService.subscribe(email, fullName, frequency, categoryIds);

        // Then
        assertThat(result).isNotNull();
        verify(subscriberRepository).existsByEmail(email);
         verify(categoryRepository).findAllById(categoryIds);
         verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void testSaveSubscriberAlreadyExists() {
        // Given
        Subscriber newSubscriber = Subscriber.builder()
                .email("test@example.com")
                .fullName("Test User")
                .build();

        when(subscriberRepository.existsByEmail("test@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> subscriberService.save(newSubscriber))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Este email já está inscrito");

        verify(subscriberRepository).existsByEmail("test@example.com");
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    void testSaveSubscriber() {
        // Given
        Subscriber subscriber = Subscriber.builder()
                .email("test@example.com")
                .fullName("Test User")
                .frequency(SubscriptionFrequency.WEEKLY)
                .active(true)
                .emailVerified(false)
                .build();

        when(subscriberRepository.existsByEmail(anyString())).thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        // When
        Subscriber result = subscriberService.save(subscriber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void testSaveSubscriberUpdate() {
        // Given
        Long subscriberId = 1L;
        Subscriber existingSubscriber = Subscriber.builder()
                .id(subscriberId)
                .email("old@example.com")
                .fullName("Old Name")
                .frequency(SubscriptionFrequency.WEEKLY)
                .active(true)
                .emailVerified(true)
                .build();

        when(subscriberRepository.existsByEmailAndIdNot(anyString(), eq(subscriberId))).thenReturn(false);
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(existingSubscriber);

        // When
        Subscriber result = subscriberService.save(existingSubscriber);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(subscriberId);
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void testDeleteSubscriber() {
        // Given
        Long subscriberId = 1L;
        when(subscriberRepository.findById(subscriberId)).thenReturn(Optional.of(subscriber));

        // When
        subscriberService.delete(subscriberId);

        // Then
        verify(subscriberRepository).findById(subscriberId);
        verify(subscriberRepository).delete(subscriber);
    }

    @Test
    void testDeleteSubscriberNotFound() {
        // Given
        Long subscriberId = 999L;
        when(subscriberRepository.findById(subscriberId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriberService.delete(subscriberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assinante não encontrado");

        verify(subscriberRepository).findById(subscriberId);
        verify(subscriberRepository, never()).delete(any(Subscriber.class));
    }

    @Test
    void testToggleActiveSubscriber() {
        // Given
        Long subscriberId = 1L;
        subscriber.setActive(true);
        when(subscriberRepository.findById(subscriberId)).thenReturn(Optional.of(subscriber));
        when(subscriberRepository.save(any(Subscriber.class))).thenReturn(subscriber);

        // When
        Subscriber result = subscriberService.toggleActive(subscriberId);

        // Then
        assertThat(result).isNotNull();
        verify(subscriberRepository).findById(subscriberId);
        verify(subscriberRepository).save(any(Subscriber.class));
    }

    @Test
    void testVerifyEmailInvalidToken() {
        // Given
        String invalidToken = "invalidtoken";
        when(subscriberRepository.findByVerificationToken(invalidToken)).thenReturn(Optional.empty());

        // When
        boolean result = subscriberService.verifyEmail(invalidToken);

        // Then
        assertThat(result).isFalse();
        verify(subscriberRepository).findByVerificationToken(invalidToken);
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    void testUnsubscribe() {
        // Given
        String unsubscribeToken = "unsubscribe123";
        subscriber.setUnsubscribeToken(unsubscribeToken);
        when(subscriberRepository.findByUnsubscribeToken(unsubscribeToken)).thenReturn(Optional.of(subscriber));
        when(subscriberRepository.save(subscriber)).thenReturn(subscriber);

        // When
        boolean result = subscriberService.unsubscribe(unsubscribeToken);

        // Then
        assertThat(result).isTrue();
        assertThat(subscriber.isActive()).isFalse();
        verify(subscriberRepository).findByUnsubscribeToken(unsubscribeToken);
        verify(subscriberRepository).save(subscriber);
    }

    @Test
    void testUnsubscribeNotFound() {
        // Given
        String unsubscribeToken = "invalidtoken";
        when(subscriberRepository.findByUnsubscribeToken(unsubscribeToken)).thenReturn(Optional.empty());

        // When
        boolean result = subscriberService.unsubscribe(unsubscribeToken);

        // Then
        assertThat(result).isFalse();
        verify(subscriberRepository).findByUnsubscribeToken(unsubscribeToken);
        verify(subscriberRepository, never()).save(any(Subscriber.class));
    }

    @Test
    void testFindByEmail() {
        // Given
        String email = "test@example.com";
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.of(subscriber));

        // When
        Optional<Subscriber> result = subscriberService.findByEmail(email);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(subscriber);
        verify(subscriberRepository).findByEmail(email);
    }

    @Test
    void testFindByEmailNotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(subscriberRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When
        Optional<Subscriber> result = subscriberService.findByEmail(email);

        // Then
        assertThat(result).isEmpty();
        verify(subscriberRepository).findByEmail(email);
    }

    @Test
    void testGetStats() {
        // Given
        when(subscriberRepository.countTotalSubscribers()).thenReturn(100L);
        when(subscriberRepository.countActiveSubscribers()).thenReturn(80L);
        when(subscriberRepository.countVerifiedSubscribers()).thenReturn(70L);
        when(subscriberRepository.countActiveAndVerifiedSubscribers()).thenReturn(60L);
        when(subscriberRepository.countByFrequency()).thenReturn(List.of());

        // When
        SubscriberService.SubscriberStats stats = subscriberService.getStats();

        // Then
        assertThat(stats).isNotNull();
        verify(subscriberRepository).countTotalSubscribers();
        verify(subscriberRepository).countActiveSubscribers();
        verify(subscriberRepository).countVerifiedSubscribers();
        verify(subscriberRepository).countActiveAndVerifiedSubscribers();
        verify(subscriberRepository).countByFrequency();
    }
}