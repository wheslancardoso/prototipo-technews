package br.com.technews.entity;

import br.com.technews.entity.NewsArticle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String name;
    
    @Column(length = 7)
    private String color; // Cor hexadecimal para exibição
    
    @Column(length = 200)
    private String description;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<NewsArticle> articles = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Métodos utilitários
    public String getDisplayName() {
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
    
    public int getArticleCount() {
        return articles != null ? articles.size() : 0;
    }
    
    @Override
    public String toString() {
        return "Tag{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}