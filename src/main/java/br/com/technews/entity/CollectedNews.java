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

    // Getters e Setters manuais
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }



    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public NewsSource getSource() {
        return source;
    }

    public void setSource(NewsSource source) {
        this.source = source;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public NewsStatus getStatus() {
        return status;
    }

    public void setStatus(NewsStatus status) {
        this.status = status;
    }

    public Double getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder pattern manual
    public static CollectedNewsBuilder builder() {
        return new CollectedNewsBuilder();
    }

    public static class CollectedNewsBuilder {
        private String title;
        private String content;
        private String originalUrl;
        private String imageUrl;
        private LocalDateTime publishedAt;
        private NewsSource source;
        private Category category;
        private String contentHash;
        private NewsStatus status = NewsStatus.PENDING;
        private Double qualityScore = 0.0;
        private LocalDateTime processedAt;

        public CollectedNewsBuilder title(String title) {
            this.title = title;
            return this;
        }

        public CollectedNewsBuilder content(String content) {
            this.content = content;
            return this;
        }

        public CollectedNewsBuilder originalUrl(String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        public CollectedNewsBuilder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public CollectedNewsBuilder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public CollectedNewsBuilder source(NewsSource source) {
            this.source = source;
            return this;
        }

        public CollectedNewsBuilder category(Category category) {
            this.category = category;
            return this;
        }

        public CollectedNewsBuilder contentHash(String contentHash) {
            this.contentHash = contentHash;
            return this;
        }

        public CollectedNewsBuilder status(NewsStatus status) {
            this.status = status;
            return this;
        }

        public CollectedNewsBuilder qualityScore(Double qualityScore) {
            this.qualityScore = qualityScore;
            return this;
        }

        public CollectedNewsBuilder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public CollectedNews build() {
            CollectedNews collectedNews = new CollectedNews();
            collectedNews.setTitle(this.title);
            collectedNews.setContent(this.content);
            collectedNews.setOriginalUrl(this.originalUrl);
            collectedNews.setImageUrl(this.imageUrl);
            collectedNews.setPublishedAt(this.publishedAt);
            collectedNews.setSource(this.source);
            collectedNews.setCategory(this.category);
            collectedNews.setContentHash(this.contentHash);
            collectedNews.setStatus(this.status);
            collectedNews.setQualityScore(this.qualityScore);
            collectedNews.setProcessedAt(this.processedAt);
            collectedNews.setCreatedAt(LocalDateTime.now());
            collectedNews.setUpdatedAt(LocalDateTime.now());
            return collectedNews;
        }
    }

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
        return qualityScore != null && qualityScore >= 0.7;
    }
}