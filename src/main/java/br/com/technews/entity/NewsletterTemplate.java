package br.com.technews.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Entidade para templates personalizáveis de newsletter
 */
@Entity
@Table(name = "newsletter_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, length = 50)
    private String templateKey; // 'default', 'grid', 'mobile', 'custom'

    @Column(columnDefinition = "TEXT")
    private String htmlContent; // Para templates customizados

    @Column(columnDefinition = "TEXT")
    private String cssStyles; // Estilos CSS personalizados

    @Column(columnDefinition = "TEXT")
    private String configuration; // JSON com configurações do template

    @Builder.Default
    @Column(nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy; // Usuário que criou o template

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Enum para tipos de template predefinidos
     */
    public enum TemplateType {
        DEFAULT("default", "Template Padrão", "Layout clássico com artigos em lista"),
        GRID("grid", "Template Grid", "Layout em grade com cards"),
        MOBILE("mobile", "Template Mobile", "Otimizado para dispositivos móveis"),
        CUSTOM("custom", "Template Personalizado", "Template totalmente customizável");

        private final String key;
        private final String displayName;
        private final String description;

        TemplateType(String key, String displayName, String description) {
            this.key = key;
            this.displayName = displayName;
            this.description = description;
        }

        public String getKey() { return key; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public static TemplateType fromKey(String key) {
            for (TemplateType type : values()) {
                if (type.key.equals(key)) {
                    return type;
                }
            }
            return DEFAULT;
        }
    }
}