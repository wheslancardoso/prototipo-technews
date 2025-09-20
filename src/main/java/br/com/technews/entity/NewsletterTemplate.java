package br.com.technews.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade para templates personalizáveis de newsletter
 */
@Entity
@Table(name = "newsletter_templates")
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

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String createdBy; // Usuário que criou o template

    // Construtores
    public NewsletterTemplate() {
        this.isActive = true;
        this.isDefault = false;
    }

    public NewsletterTemplate(String name, String templateKey) {
        this();
        this.name = name;
        this.templateKey = templateKey;
    }

    // Getters e Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getCssStyles() {
        return cssStyles;
    }

    public void setCssStyles(String cssStyles) {
        this.cssStyles = cssStyles;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
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

    // Método builder estático para substituir o Lombok
    public static NewsletterTemplate builder() {
        return new NewsletterTemplate();
    }

    public NewsletterTemplate name(String name) {
        this.name = name;
        return this;
    }

    public NewsletterTemplate description(String description) {
        this.description = description;
        return this;
    }

    public NewsletterTemplate templateKey(String templateKey) {
        this.templateKey = templateKey;
        return this;
    }

    public NewsletterTemplate htmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
        return this;
    }

    public NewsletterTemplate cssStyles(String cssStyles) {
        this.cssStyles = cssStyles;
        return this;
    }

    public NewsletterTemplate configuration(String configuration) {
        this.configuration = configuration;
        return this;
    }

    public NewsletterTemplate isActive(Boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public NewsletterTemplate isDefault(Boolean isDefault) {
        this.isDefault = isDefault;
        return this;
    }

    public NewsletterTemplate createdBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public NewsletterTemplate build() {
        return this;
    }

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