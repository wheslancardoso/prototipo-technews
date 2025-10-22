package br.com.technews.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "newsletters")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Newsletter {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "newsletter_date", nullable = false, unique = true)
    private LocalDate newsletterDate;
    
    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;
    
    @Column(name = "published", nullable = false)
    private Boolean published = false;
    
    @Column(name = "views", nullable = false)
    private Long views = 0L;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
    // Relacionamento com artigos
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "newsletter_articles",
        joinColumns = @JoinColumn(name = "newsletter_id"),
        inverseJoinColumns = @JoinColumn(name = "article_id")
    )
    private Set<NewsArticle> articles = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Gerar slug baseado na data se não foi definido
        if (this.slug == null && this.newsletterDate != null) {
            this.slug = generateSlugFromDate(this.newsletterDate);
        }
        
        // Gerar título baseado na data se não foi definido
        if (this.title == null && this.newsletterDate != null) {
            this.title = generateTitleFromDate(this.newsletterDate);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        
        // Atualizar publishedAt quando publicado
        if (this.published && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }
    
    // Métodos auxiliares
    private String generateSlugFromDate(LocalDate date) {
        return String.format("%02d-%02d-%04d", 
            date.getDayOfMonth(), 
            date.getMonthValue(), 
            date.getYear());
    }
    
    private String generateTitleFromDate(LocalDate date) {
        return String.format("TechNews - Edição de %02d/%02d/%04d", 
            date.getDayOfMonth(), 
            date.getMonthValue(), 
            date.getYear());
    }
    
    // Métodos de conveniência
    public void addArticle(NewsArticle article) {
        this.articles.add(article);
    }
    
    public void removeArticle(NewsArticle article) {
        this.articles.remove(article);
    }
    
    public int getArticleCount() {
        return this.articles != null ? this.articles.size() : 0;
    }
    
    public void incrementViews() {
        this.views = this.views != null ? this.views + 1 : 1L;
    }
    
    // Construtor personalizado
    public Newsletter(LocalDate newsletterDate) {
        this.newsletterDate = newsletterDate;
        this.slug = generateSlugFromDate(newsletterDate);
        this.title = generateTitleFromDate(newsletterDate);
        this.published = false;
        this.views = 0L;
    }
}