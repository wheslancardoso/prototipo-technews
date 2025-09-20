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
    
    @Column(name = "manage_token", unique = true)
    private String manageToken;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "reactivated_at")
    private LocalDateTime reactivatedAt;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "subscription_frequency", nullable = false)
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
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
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
    
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public LocalDateTime getVerificationTokenExpiresAt() {
        return verificationTokenExpiresAt;
    }

    public void setVerificationTokenExpiresAt(LocalDateTime verificationTokenExpiresAt) {
        this.verificationTokenExpiresAt = verificationTokenExpiresAt;
    }

    public SubscriptionFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(SubscriptionFrequency frequency) {
        this.frequency = frequency;
    }

    public String getUnsubscribeToken() {
        return unsubscribeToken;
    }

    public void setUnsubscribeToken(String unsubscribeToken) {
        this.unsubscribeToken = unsubscribeToken;
    }

    public Set<Category> getSubscribedCategories() {
        return subscribedCategories;
    }

    public void setSubscribedCategories(Set<Category> subscribedCategories) {
        this.subscribedCategories = subscribedCategories;
    }
    
    // Getters e Setters adicionais necessários
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public LocalDateTime getLastEmailSentAt() {
        return lastEmailSentAt;
    }

    public void setLastEmailSentAt(LocalDateTime lastEmailSentAt) {
        this.lastEmailSentAt = lastEmailSentAt;
    }

    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }

    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    public LocalDateTime getReactivatedAt() {
        return reactivatedAt;
    }
    
    public void setReactivatedAt(LocalDateTime reactivatedAt) {
        this.reactivatedAt = reactivatedAt;
    }
    
    public Integer getEmailCount() {
        return emailCount;
    }
    
    public void setEmailCount(Integer emailCount) {
        this.emailCount = emailCount;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getUnsubscribedAt() {
        return unsubscribedAt;
    }
    
    public void setUnsubscribedAt(LocalDateTime unsubscribedAt) {
        this.unsubscribedAt = unsubscribedAt;
    }
    
    public String getManageToken() {
        return manageToken;
    }
    
    public void setManageToken(String manageToken) {
        this.manageToken = manageToken;
    }
    
    public String getSubscriptionIp() {
        return subscriptionIp;
    }
    
    public void setSubscriptionIp(String subscriptionIp) {
        this.subscriptionIp = subscriptionIp;
    }
    
    public String getSubscriptionUserAgent() {
        return subscriptionUserAgent;
    }
    
    public void setSubscriptionUserAgent(String subscriptionUserAgent) {
        this.subscriptionUserAgent = subscriptionUserAgent;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}