package br.com.technews.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Entidade para agendamento de newsletters
 */
@Entity
@Table(name = "newsletter_schedules")
public class NewsletterSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(name = "template_key")
    private String templateKey;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status;

    @Column(name = "recipient_count")
    private Integer recipientCount;

    @Column(name = "success_count")
    private Integer successCount;

    @Column(name = "error_count")
    private Integer errorCount;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "newsletter_schedule_categories",
        joinColumns = @JoinColumn(name = "schedule_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency_filter")
    private Subscriber.SubscriptionFrequency frequencyFilter;

    @Column(name = "active_only")
    private Boolean activeOnly;

    @Column(name = "verified_only")
    private Boolean verifiedOnly;

    /**
     * Status do agendamento
     */
    public enum ScheduleStatus {
        PENDING,    // Aguardando envio
        PROCESSING, // Sendo processado
        SENT,       // Enviado com sucesso
        FAILED,     // Falhou no envio
        CANCELLED   // Cancelado
    }

    // Constructors
    public NewsletterSchedule() {}

    public NewsletterSchedule(String subject, LocalDateTime scheduledDate) {
        this.subject = subject;
        this.scheduledDate = scheduledDate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public void setTemplateKey(String templateKey) {
        this.templateKey = templateKey;
    }

    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public Integer getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void setCategories(Set<Category> categories) {
        this.categories = categories;
    }

    public Subscriber.SubscriptionFrequency getFrequencyFilter() {
        return frequencyFilter;
    }

    public void setFrequencyFilter(Subscriber.SubscriptionFrequency frequencyFilter) {
        this.frequencyFilter = frequencyFilter;
    }

    public Boolean getActiveOnly() {
        return activeOnly;
    }

    public void setActiveOnly(Boolean activeOnly) {
        this.activeOnly = activeOnly;
    }

    public Boolean getVerifiedOnly() {
        return verifiedOnly;
    }

    public void setVerifiedOnly(Boolean verifiedOnly) {
        this.verifiedOnly = verifiedOnly;
    }

    // Método builder estático para substituir o Lombok
    public static NewsletterSchedule builder() {
        return new NewsletterSchedule();
    }

    public NewsletterSchedule subject(String subject) {
        this.subject = subject;
        return this;
    }

    public NewsletterSchedule scheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
        return this;
    }

    public NewsletterSchedule categories(Set<Category> categories) {
        this.categories = categories;
        return this;
    }

    public NewsletterSchedule frequencyFilter(Subscriber.SubscriptionFrequency frequencyFilter) {
        this.frequencyFilter = frequencyFilter;
        return this;
    }

    public NewsletterSchedule build() {
        return this;
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
        if (status == null) {
            status = ScheduleStatus.PENDING;
        }
        if (activeOnly == null) {
            activeOnly = true;
        }
        if (verifiedOnly == null) {
            verifiedOnly = true;
        }
    }

    /**
     * Verifica se o agendamento está pendente
     */
    public boolean isPending() {
        return status == ScheduleStatus.PENDING;
    }

    /**
     * Verifica se o agendamento foi enviado
     */
    public boolean isSent() {
        return status == ScheduleStatus.SENT;
    }

    /**
     * Verifica se o agendamento falhou
     */
    public boolean isFailed() {
        return status == ScheduleStatus.FAILED;
    }

    /**
     * Marca como processando
     */
    public void markAsProcessing() {
        this.status = ScheduleStatus.PROCESSING;
    }

    /**
     * Marca como enviado
     */
    public void markAsSent(int recipientCount, int successCount, int errorCount) {
        this.status = ScheduleStatus.SENT;
        this.sentDate = LocalDateTime.now();
        this.recipientCount = recipientCount;
        this.successCount = successCount;
        this.errorCount = errorCount;
    }

    /**
     * Marca como falhou
     */
    public void markAsFailed(String errorMessage) {
        this.status = ScheduleStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * Cancela o agendamento
     */
    public void cancel() {
        this.status = ScheduleStatus.CANCELLED;
    }
}