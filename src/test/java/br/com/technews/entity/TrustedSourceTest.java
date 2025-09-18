package br.com.technews.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

/**
 * Testes unitários para a entidade TrustedSource
 */
class TrustedSourceTest {

    private TrustedSource trustedSource;

    @BeforeEach
    void setUp() {
        trustedSource = new TrustedSource();
    }

    @Test
    void testTrustedSourceCreation() {
        // Given
        String name = "TechCrunch";
        String domain = "techcrunch.com";
        String description = "Leading technology media property";

        // When
        trustedSource.setName(name);
        trustedSource.setDomain(domain);
        trustedSource.setDescription(description);

        // Then
        assertThat(trustedSource.getName()).isEqualTo(name);
        assertThat(trustedSource.getDomain()).isEqualTo(domain);
        assertThat(trustedSource.getDescription()).isEqualTo(description);
        assertThat(trustedSource.getId()).isNull();
        assertThat(trustedSource.isActive()).isTrue(); // Default should be true
    }

    @Test
    void testTrustedSourceWithId() {
        // Given
        Long id = 1L;
        String name = "Wired";
        String domain = "wired.com";

        // When
        trustedSource.setId(id);
        trustedSource.setName(name);
        trustedSource.setDomain(domain);

        // Then
        assertThat(trustedSource.getId()).isEqualTo(id);
        assertThat(trustedSource.getName()).isEqualTo(name);
        assertThat(trustedSource.getDomain()).isEqualTo(domain);
    }

    @Test
    void testTrustedSourceActiveStatus() {
        // Given
        String name = "Inactive Source";
        String domain = "inactive.com";

        // When
        trustedSource.setName(name);
        trustedSource.setDomain(domain);
        trustedSource.setActive(false);

        // Then
        assertThat(trustedSource.getName()).isEqualTo(name);
        assertThat(trustedSource.getDomain()).isEqualTo(domain);
        assertThat(trustedSource.isActive()).isFalse();
    }

    @Test
    void testTrustedSourceEquality() {
        // Given
        TrustedSource source1 = new TrustedSource();
        source1.setId(1L);
        source1.setName("Test Source");
        source1.setDomain("test.com");

        TrustedSource source2 = new TrustedSource();
        source2.setId(1L);
        source2.setName("Test Source");
        source2.setDomain("test.com");

        // Then
        assertThat(source1).isEqualTo(source2);
        assertThat(source1.hashCode()).isEqualTo(source2.hashCode());
    }

    @Test
    void testTrustedSourceToString() {
        // Given
        trustedSource.setId(1L);
        trustedSource.setName("The Verge");
        trustedSource.setDomain("theverge.com");

        // When
        String toString = trustedSource.toString();

        // Then
        assertThat(toString).contains("The Verge");
        assertThat(toString).contains("theverge.com");
        assertThat(toString).contains("1");
    }

    @Test
    void testTrustedSourceWithNullValues() {
        // Given/When
        trustedSource.setName(null);
        trustedSource.setDomain(null);
        trustedSource.setDescription(null);

        // Then
        assertThat(trustedSource.getName()).isNull();
        assertThat(trustedSource.getDomain()).isNull();
        assertThat(trustedSource.getDescription()).isNull();
    }

    @Test
    void testTrustedSourceDomainValidation() {
        // Given
        String validDomain = "example.com";
        String invalidDomain = "not-a-domain";

        // When
        trustedSource.setDomain(validDomain);

        // Then
        assertThat(trustedSource.getDomain()).isEqualTo(validDomain);
        
        // Note: In a real scenario, you might want to add domain validation
        // This test just ensures the setter works correctly
    }
}