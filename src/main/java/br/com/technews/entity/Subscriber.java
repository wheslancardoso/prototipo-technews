package br.com.technews.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "subscribers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscriber {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email deve ter um formato válido")
    @Size(max = 255, message = "Email deve ter no máximo 255 caracteres")
    @Column(nullable = false, unique = true)
    private String email;
    
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Column(name = "full_name")
    private String fullName;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    
    @Builder.Default
    @Column(nullable = false)
    private Boolean emailVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;
    
    @Column(name = "unsubscribe_token", unique = true)
    private String unsubscribeToken;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private SubscriptionFrequency frequency = SubscriptionFrequency.WEEKLY;
    
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "subscriber_categories",
        joinColumns = @JoinColumn(name = "subscriber_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private Set<Category> subscribedCategories = new HashSet<>();
    
    @Column(name = "last_email_sent_at")
    private LocalDateTime lastEmailSentAt;
    
    @Builder.Default
    @Column(name = "email_count", nullable = false)
    private Integer emailCount = 0;
    
    @Column(name = "subscribed_at")
    @CreationTimestamp
    private LocalDateTime subscribedAt;
    
    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;
    
    @Size(max = 45, message = "IP deve ter no máximo 45 caracteres")
    @Column(name = "subscription_ip")
    private String subscriptionIp;
    
    @Size(max = 500, message = "User Agent deve ter no máximo 500 caracteres")
    @Column(name = "subscription_user_agent")
    private String subscriptionUserAgent;
    
    // Enum para frequência de envio
    public enum SubscriptionFrequency {
        DAILY("Diário"),
        WEEKLY("Semanal"),
        MONTHLY("Mensal");
        
        private final String displayName;
        
        SubscriptionFrequency(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Construtor personalizado
    public Subscriber(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
        this.active = true;
        this.emailVerified = false;
        this.frequency = SubscriptionFrequency.WEEKLY;
        this.subscribedCategories = new HashSet<>();
        this.emailCount = 0;
    }
    
    // Métodos utilitários
    public boolean isActive() {
        return active != null && active;
    }
    
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }
    
    public void activate() {
        this.active = true;
        this.unsubscribedAt = null;
    }
    
    public void deactivate() {
        this.active = false;
        this.unsubscribedAt = LocalDateTime.now();
    }
    
    public void verifyEmail() {
        this.emailVerified = true;
        this.verificationToken = null;
        this.verificationTokenExpiresAt = null;
    }
    
    public void incrementEmailCount() {
        this.emailCount = (this.emailCount == null ? 0 : this.emailCount) + 1;
        this.lastEmailSentAt = LocalDateTime.now();
    }
    
    public boolean isVerificationTokenExpired() {
        return verificationTokenExpiresAt != null && 
               verificationTokenExpiresAt.isBefore(LocalDateTime.now());
    }
    
    public void addCategory(Category category) {
        if (subscribedCategories == null) {
            subscribedCategories = new HashSet<>();
        }
        subscribedCategories.add(category);
    }
    
    public void removeCategory(Category category) {
        if (subscribedCategories != null) {
            subscribedCategories.remove(category);
        }
    }
    
    public boolean isSubscribedToCategory(Category category) {
        return subscribedCategories != null && subscribedCategories.contains(category);
    }
    
    public boolean hasSubscribedCategories() {
        return subscribedCategories != null && !subscribedCategories.isEmpty();
    }
    
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return email.substring(0, email.indexOf('@'));
    }
    
    @Override
    public String toString() {
        return "Subscriber{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", active=" + active +
                ", emailVerified=" + emailVerified +
                ", frequency=" + frequency +
                ", subscribedAt=" + subscribedAt +
                '}';
    }
}