package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsletterSchedule;
import br.com.technews.entity.Subscriber;
import br.com.technews.repository.CategoryRepository;
import br.com.technews.repository.NewsletterScheduleRepository;
import br.com.technews.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Serviço para gerenciamento de agendamento de newsletters
 */
@Service
@Transactional
public class NewsletterScheduleService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterScheduleService.class);

    @Autowired
    private NewsletterScheduleRepository scheduleRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Autowired
    private SubscriberService subscriberService;
    
    @Autowired
    private EmailService emailService;

    /**
     * Cria um novo agendamento de newsletter
     */
    public NewsletterSchedule createSchedule(String subject, String templateKey, 
                                           LocalDateTime scheduledDate, Set<Long> categoryIds,
                                           Subscriber.SubscriptionFrequency frequencyFilter,
                                           Boolean activeOnly, Boolean verifiedOnly) {
        
        // Validações
        if (scheduledDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data de agendamento deve ser futura");
        }

        Set<Category> categories = null;
        if (categoryIds != null && !categoryIds.isEmpty()) {
            categories = categoryRepository.findAllById(categoryIds)
                .stream().collect(Collectors.toSet());
        }

        NewsletterSchedule schedule = NewsletterSchedule.builder()
            .subject(subject)
            .scheduledDate(scheduledDate)
            .categories(categories)
            .frequencyFilter(frequencyFilter)
            .build();

        // Definir activeOnly e verifiedOnly manualmente
        schedule.setActiveOnly(activeOnly != null ? activeOnly : true);
        schedule.setVerifiedOnly(verifiedOnly != null ? verifiedOnly : true);

        // Definir template padrão se não especificado
        if (templateKey == null || templateKey.isEmpty()) {
            schedule.setTemplateKey("default");
        } else {
            schedule.setTemplateKey(templateKey);
        }

        NewsletterSchedule saved = scheduleRepository.save(schedule);
        log.info("Agendamento de newsletter criado: ID={}, Data={}, Assunto={}", 
                saved.getId(), saved.getScheduledDate(), saved.getSubject());
        
        return saved;
    }

    /**
     * Busca agendamento por ID
     */
    @Transactional(readOnly = true)
    public Optional<NewsletterSchedule> findById(Long id) {
        return scheduleRepository.findById(id);
    }

    /**
     * Lista todos os agendamentos com paginação
     */
    @Transactional(readOnly = true)
    public Page<NewsletterSchedule> findAll(Pageable pageable) {
        return scheduleRepository.findRecentSchedules(pageable);
    }

    /**
     * Lista agendamentos por status
     */
    @Transactional(readOnly = true)
    public Page<NewsletterSchedule> findByStatus(NewsletterSchedule.ScheduleStatus status, Pageable pageable) {
        return scheduleRepository.findByStatusOrderByScheduledDateDesc(status, pageable);
    }

    /**
     * Lista agendamentos pendentes
     */
    @Transactional(readOnly = true)
    public List<NewsletterSchedule> findPendingSchedules() {
        return scheduleRepository.findByStatus(NewsletterSchedule.ScheduleStatus.PENDING);
    }

    /**
     * Lista agendamentos do dia
     */
    @Transactional(readOnly = true)
    public List<NewsletterSchedule> findTodaySchedules() {
        return scheduleRepository.findByScheduledDate(LocalDateTime.now());
    }

    /**
     * Lista agendamentos da semana
     */
    @Transactional(readOnly = true)
    public List<NewsletterSchedule> findWeekSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStart = now.truncatedTo(ChronoUnit.DAYS).minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        return scheduleRepository.findByWeek(weekStart, weekEnd);
    }

    /**
     * Lista agendamentos do mês
     */
    @Transactional(readOnly = true)
    public List<NewsletterSchedule> findMonthSchedules() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        return scheduleRepository.findByMonth(monthStart, monthEnd);
    }

    /**
     * Atualiza agendamento
     */
    public NewsletterSchedule updateSchedule(Long id, String subject, LocalDateTime scheduledDate,
                                           Set<Long> categoryIds) {
        NewsletterSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        if (!schedule.isPending()) {
            throw new IllegalStateException("Só é possível editar agendamentos pendentes");
        }

        if (scheduledDate.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data de agendamento deve ser futura");
        }

        schedule.setSubject(subject);
        schedule.setScheduledDate(scheduledDate);

        if (categoryIds != null) {
            Set<Category> categories = categoryRepository.findAllById(categoryIds)
                .stream().collect(Collectors.toSet());
            schedule.setCategories(categories);
        }

        NewsletterSchedule updated = scheduleRepository.save(schedule);
        log.info("Agendamento atualizado: ID={}", id);
        
        return updated;
    }

    /**
     * Cancela agendamento
     */
    public void cancelSchedule(Long id) {
        NewsletterSchedule schedule = scheduleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));

        if (!schedule.isPending()) {
            throw new IllegalStateException("Só é possível cancelar agendamentos pendentes");
        }

        schedule.cancel();
        scheduleRepository.save(schedule);
        log.info("Agendamento cancelado: ID={}", id);
    }

    /**
     * Processa agendamentos pendentes (executado automaticamente)
     */
    @Scheduled(fixedRate = 60000) // A cada minuto
    @Async
    public void processScheduledNewsletters() {
        List<NewsletterSchedule> pendingSchedules = scheduleRepository
            .findPendingSchedulesToSend(LocalDateTime.now());

        for (NewsletterSchedule schedule : pendingSchedules) {
            try {
                processSchedule(schedule);
            } catch (Exception e) {
                log.error("Erro ao processar agendamento ID={}: {}", schedule.getId(), e.getMessage(), e);
                schedule.markAsFailed("Erro interno: " + e.getMessage());
                scheduleRepository.save(schedule);
            }
        }
    }

    /**
     * Processa um agendamento específico
     */
    @Async
    public void processSchedule(NewsletterSchedule schedule) {
        log.info("Processando agendamento ID={}", schedule.getId());
        
        schedule.markAsProcessing();
        scheduleRepository.save(schedule);

        try {
            // Buscar assinantes baseado nos filtros
            List<Subscriber> recipients = getRecipientsForSchedule(schedule);
            
            if (recipients.isEmpty()) {
                schedule.markAsFailed("Nenhum destinatário encontrado com os filtros especificados");
                scheduleRepository.save(schedule);
                return;
            }

            // Enviar newsletter
            int successCount = 0;
            int errorCount = 0;

            for (Subscriber subscriber : recipients) {
                try {
                    // Enviar newsletter para cada assinante
                    CompletableFuture<Boolean> emailResult = emailService.sendNewsletterToSubscriber(subscriber, 
                        List.of()); // Lista vazia por enquanto, pode ser implementada depois
                    successCount++;
                } catch (Exception e) {
                    log.warn("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
                    errorCount++;
                }
            }

            schedule.markAsSent(recipients.size(), successCount, errorCount);
            scheduleRepository.save(schedule);
            
            log.info("Agendamento ID={} processado: {} enviados, {} sucessos, {} erros", 
                    schedule.getId(), recipients.size(), successCount, errorCount);

        } catch (Exception e) {
            log.error("Erro ao processar agendamento ID={}: {}", schedule.getId(), e.getMessage(), e);
            schedule.markAsFailed("Erro no processamento: " + e.getMessage());
            scheduleRepository.save(schedule);
        }
    }

    /**
     * Busca destinatários baseado nos filtros do agendamento
     */
    private List<Subscriber> getRecipientsForSchedule(NewsletterSchedule schedule) {
        // Implementar lógica de filtro baseada nos critérios do agendamento
        List<Subscriber> subscribers = subscriberService.findActiveSubscribers();
        
        // Filtrar por verificação de email se necessário
        if (schedule.getVerifiedOnly()) {
            subscribers = subscribers.stream()
                .filter(Subscriber::isEmailVerified)
                .collect(Collectors.toList());
        }

        // Filtrar por frequência se especificada
        if (schedule.getFrequencyFilter() != null) {
            subscribers = subscribers.stream()
                .filter(s -> s.getFrequency() == schedule.getFrequencyFilter())
                .collect(Collectors.toList());
        }

        // Filtrar por categorias se especificadas
        if (schedule.getCategories() != null && !schedule.getCategories().isEmpty()) {
            subscribers = subscribers.stream()
                .filter(s -> s.getSubscribedCategories().stream()
                    .anyMatch(cat -> schedule.getCategories().contains(cat)))
                .collect(Collectors.toList());
        }

        return subscribers;
    }

    /**
     * Obtém estatísticas de agendamentos
     */
    @Transactional(readOnly = true)
    public ScheduleStats getStats() {
        long total = scheduleRepository.count();
        long pending = scheduleRepository.countByStatus(NewsletterSchedule.ScheduleStatus.PENDING);
        long processing = scheduleRepository.countByStatus(NewsletterSchedule.ScheduleStatus.PROCESSING);
        long sent = scheduleRepository.countByStatus(NewsletterSchedule.ScheduleStatus.SENT);
        long failed = scheduleRepository.countByStatus(NewsletterSchedule.ScheduleStatus.FAILED);
        long cancelled = scheduleRepository.countByStatus(NewsletterSchedule.ScheduleStatus.CANCELLED);
        
        return new ScheduleStats(total, pending, processing, sent, failed, cancelled);
    }

    /**
     * Busca todos os agendamentos com paginação (método alternativo)
     */
    public Page<NewsletterSchedule> findAllPaginated(Pageable pageable) {
        return scheduleRepository.findAll(pageable);
    }

    /**
     * Busca agendamentos por status com paginação
     */
    public Page<NewsletterSchedule> findByStatusPaginated(NewsletterSchedule.ScheduleStatus status, Pageable pageable) {
        return scheduleRepository.findByStatus(status, pageable);
    }

    /**
     * Remove agendamentos antigos
     */
    @Scheduled(cron = "0 0 2 * * ?") // Todo dia às 2h
    public void cleanupOldSchedules() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(3);
        scheduleRepository.deleteOldFailedSchedules(cutoffDate);
        log.info("Limpeza de agendamentos antigos executada");
    }

    /**
     * Classe para estatísticas de agendamentos
     */
    public static class ScheduleStats {
        private final long total;
        private final long pending;
        private final long processing;
        private final long completed;
        private final long failed;
        private final long cancelled;

        public ScheduleStats(long total, long pending, long processing, long completed, long failed, long cancelled) {
            this.total = total;
            this.pending = pending;
            this.processing = processing;
            this.completed = completed;
            this.failed = failed;
            this.cancelled = cancelled;
        }

        public long getTotal() { return total; }
        public long getPending() { return pending; }
        public long getProcessing() { return processing; }
        public long getCompleted() { return completed; }
        public long getFailed() { return failed; }
        public long getCancelled() { return cancelled; }
    }
}