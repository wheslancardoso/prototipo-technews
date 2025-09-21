package br.com.technews.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "collected_news", 
       indexes = {
           @Index(name = "idx_collected_news_url", columnList = "original_url"),
           @Index(name = "idx_collected_news_hash", columnList = "content_hash"),
           @Index(name = "idx_collected_news_published", columnList = "published_at"),
           @Index(name = "idx_collected_news_status", columnList = "status")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedNews {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "original_url", nullable = false, unique = true, length = 1000)
    private String originalUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private NewsSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "content_hash", nullable = false, unique = true)
    private String contentHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NewsStatus status = NewsStatus.PENDING;

    @Column(name = "quality_score")
    @Builder.Default
    private Double qualityScore = 0.0;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum NewsStatus {
        PENDING,      // Coletada, aguardando processamento
        APPROVED,     // Aprovada para newsletter
        REJECTED,     // Rejeitada (baixa qualidade, duplicata, etc.)
        PUBLISHED     // Já incluída em newsletter
    }

    public boolean isRecentNews() {
        if (publishedAt == null) {
            return false;
        }
        return publishedAt.isAfter(LocalDateTime.now().minusDays(7));
    }

    public boolean isHighQuality() {
        return qualityScore != null && qualityScore >= 7.0;
    }
}