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

    // Getters e Setters manuais
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SourceType getType() {
        return type;
    }

    public void setType(SourceType type) {
        this.type = type;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getLastFetchAt() {
        return lastFetchAt;
    }

    public void setLastFetchAt(LocalDateTime lastFetchAt) {
        this.lastFetchAt = lastFetchAt;
    }

    public Integer getFetchIntervalMinutes() {
        return fetchIntervalMinutes;
    }

    public void setFetchIntervalMinutes(Integer fetchIntervalMinutes) {
        this.fetchIntervalMinutes = fetchIntervalMinutes;
    }

    public Integer getMaxArticlesPerFetch() {
        return maxArticlesPerFetch;
    }

    public void setMaxArticlesPerFetch(Integer maxArticlesPerFetch) {
        this.maxArticlesPerFetch = maxArticlesPerFetch;
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
    public static NewsSourceBuilder builder() {
        return new NewsSourceBuilder();
    }

    public static class NewsSourceBuilder {
        private String name;
        private String url;
        private SourceType type;
        private Category category;
        private Boolean active = true;
        private LocalDateTime lastFetchAt;
        private Integer fetchIntervalMinutes = 60;
        private Integer maxArticlesPerFetch = 10;

        public NewsSourceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public NewsSourceBuilder url(String url) {
            this.url = url;
            return this;
        }

        public NewsSourceBuilder type(SourceType type) {
            this.type = type;
            return this;
        }

        public NewsSourceBuilder category(Category category) {
            this.category = category;
            return this;
        }

        public NewsSourceBuilder active(Boolean active) {
            this.active = active;
            return this;
        }

        public NewsSourceBuilder lastFetchAt(LocalDateTime lastFetchAt) {
            this.lastFetchAt = lastFetchAt;
            return this;
        }

        public NewsSourceBuilder fetchIntervalMinutes(Integer fetchIntervalMinutes) {
            this.fetchIntervalMinutes = fetchIntervalMinutes;
            return this;
        }

        public NewsSourceBuilder maxArticlesPerFetch(Integer maxArticlesPerFetch) {
            this.maxArticlesPerFetch = maxArticlesPerFetch;
            return this;
        }

        public NewsSource build() {
            NewsSource newsSource = new NewsSource();
            newsSource.setName(this.name);
            newsSource.setUrl(this.url);
            newsSource.setType(this.type);
            newsSource.setCategory(this.category);
            newsSource.setActive(this.active);
            newsSource.setLastFetchAt(this.lastFetchAt);
            newsSource.setFetchIntervalMinutes(this.fetchIntervalMinutes);
            newsSource.setMaxArticlesPerFetch(this.maxArticlesPerFetch);
            newsSource.setCreatedAt(LocalDateTime.now());
            newsSource.setUpdatedAt(LocalDateTime.now());
            return newsSource;
        }
    }

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