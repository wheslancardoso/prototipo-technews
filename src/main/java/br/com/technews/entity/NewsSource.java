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
@Table(name = "news_sources")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "last_fetch_at")
    private LocalDateTime lastFetchAt;

    @Column(name = "fetch_interval_minutes")
    @Builder.Default
    private Integer fetchIntervalMinutes = 60; // Default: 1 hora

    @Column(name = "max_articles_per_fetch")
    @Builder.Default
    private Integer maxArticlesPerFetch = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SourceType {
        RSS_FEED,
        API,
        WEB_SCRAPING
    }

    public boolean shouldFetch() {
        if (!active || lastFetchAt == null) {
            return active;
        }
        return lastFetchAt.plusMinutes(fetchIntervalMinutes).isBefore(LocalDateTime.now());
    }
}