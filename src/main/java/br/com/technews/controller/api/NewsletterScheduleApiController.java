package br.com.technews.controller.api;

import br.com.technews.entity.NewsletterSchedule;
import br.com.technews.entity.Subscriber;
import br.com.technews.service.NewsletterScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * API REST para gerenciamento de agendamento de newsletters
 */
@RestController
@RequestMapping("/api/newsletter/schedule")
public class NewsletterScheduleApiController {

    private static final Logger log = LoggerFactory.getLogger(NewsletterScheduleApiController.class);

    @Autowired
    private NewsletterScheduleService scheduleService;

    /**
     * Lista todos os agendamentos
     */
    @GetMapping
    public ResponseEntity<Page<NewsletterSchedule>> getAllSchedules(Pageable pageable) {
        try {
            Page<NewsletterSchedule> schedules = scheduleService.findAll(pageable);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("Erro ao buscar agendamentos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Busca agendamento por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NewsletterSchedule> getScheduleById(@PathVariable Long id) {
        try {
            return scheduleService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Erro ao buscar agendamento ID={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista agendamentos por status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<NewsletterSchedule>> getSchedulesByStatus(
            @PathVariable NewsletterSchedule.ScheduleStatus status, 
            Pageable pageable) {
        try {
            Page<NewsletterSchedule> schedules = scheduleService.findByStatus(status, pageable);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("Erro ao buscar agendamentos por status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista agendamentos pendentes
     */
    @GetMapping("/pending")
    public ResponseEntity<List<NewsletterSchedule>> getPendingSchedules() {
        try {
            List<NewsletterSchedule> schedules = scheduleService.findPendingSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("Erro ao buscar agendamentos pendentes: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista agendamentos do dia
     */
    @GetMapping("/today")
    public ResponseEntity<List<NewsletterSchedule>> getTodaySchedules() {
        try {
            List<NewsletterSchedule> schedules = scheduleService.findTodaySchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("Erro ao buscar agendamentos do dia: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista agendamentos da semana
     */
    @GetMapping("/week")
    public ResponseEntity<List<NewsletterSchedule>> getWeekSchedules() {
        try {
            List<NewsletterSchedule> schedules = scheduleService.findWeekSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("Erro ao buscar agendamentos da semana: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lista agendamentos do mês
     */
    @GetMapping("/month")
    public ResponseEntity<List<NewsletterSchedule>> getMonthSchedules() {
        try {
            List<NewsletterSchedule> schedules = scheduleService.findMonthSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            log.error("Erro ao buscar agendamentos do mês: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cria novo agendamento
     */
    @PostMapping
    public ResponseEntity<NewsletterSchedule> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        try {
            NewsletterSchedule schedule = scheduleService.createSchedule(
                request.getSubject(),
                request.getTemplateKey(),
                request.getScheduledDate(),
                request.getCategoryIds(),
                request.getFrequencyFilter(),
                request.getActiveOnly(),
                request.getVerifiedOnly()
            );
            
            log.info("Agendamento criado via API: ID={}", schedule.getId());
            return ResponseEntity.ok(schedule);
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao criar agendamento: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao criar agendamento: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Atualiza agendamento
     */
    @PutMapping("/{id}")
    public ResponseEntity<NewsletterSchedule> updateSchedule(
            @PathVariable Long id, 
            @Valid @RequestBody UpdateScheduleRequest request) {
        try {
            NewsletterSchedule schedule = scheduleService.updateSchedule(
                id,
                request.getSubject(),
                request.getScheduledDate(),
                request.getCategoryIds()
            );
            
            log.info("Agendamento atualizado via API: ID={}", id);
            return ResponseEntity.ok(schedule);
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Estado inválido para atualização: ID={}, erro={}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao atualizar agendamento ID={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Cancela agendamento
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelSchedule(@PathVariable Long id) {
        try {
            scheduleService.cancelSchedule(id);
            log.info("Agendamento cancelado via API: ID={}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Estado inválido para cancelamento: ID={}, erro={}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao cancelar agendamento ID={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Processa agendamento imediatamente
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<Void> processSchedule(@PathVariable Long id) {
        try {
            NewsletterSchedule schedule = scheduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
            
            if (!schedule.isPending()) {
                return ResponseEntity.badRequest().build();
            }
            
            scheduleService.processSchedule(schedule);
            log.info("Processamento manual iniciado para agendamento ID={}", id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro ao processar agendamento ID={}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Obtém estatísticas de agendamentos
     */
    @GetMapping("/stats")
    public ResponseEntity<NewsletterScheduleService.ScheduleStats> getStats() {
        try {
            NewsletterScheduleService.ScheduleStats stats = scheduleService.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas de agendamentos: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Request para criar agendamento
     */
    public static class CreateScheduleRequest {
        @NotBlank(message = "Assunto é obrigatório")
        private String subject;

        private String templateKey = "default";

        @NotNull(message = "Data de agendamento é obrigatória")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledDate;

        private Set<Long> categoryIds;

        private Subscriber.SubscriptionFrequency frequencyFilter;

        private Boolean activeOnly = true;

        private Boolean verifiedOnly = true;

        // Getters e Setters
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getTemplateKey() { return templateKey; }
        public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }

        public LocalDateTime getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }

        public Set<Long> getCategoryIds() { return categoryIds; }
        public void setCategoryIds(Set<Long> categoryIds) { this.categoryIds = categoryIds; }

        public Subscriber.SubscriptionFrequency getFrequencyFilter() { return frequencyFilter; }
        public void setFrequencyFilter(Subscriber.SubscriptionFrequency frequencyFilter) { this.frequencyFilter = frequencyFilter; }

        public Boolean getActiveOnly() { return activeOnly; }
        public void setActiveOnly(Boolean activeOnly) { this.activeOnly = activeOnly; }

        public Boolean getVerifiedOnly() { return verifiedOnly; }
        public void setVerifiedOnly(Boolean verifiedOnly) { this.verifiedOnly = verifiedOnly; }
    }

    /**
     * Request para atualizar agendamento
     */
    public static class UpdateScheduleRequest {
        @NotBlank(message = "Assunto é obrigatório")
        private String subject;

        @NotNull(message = "Data de agendamento é obrigatória")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private LocalDateTime scheduledDate;

        private Set<Long> categoryIds;

        // Getters e Setters
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public LocalDateTime getScheduledDate() { return scheduledDate; }
        public void setScheduledDate(LocalDateTime scheduledDate) { this.scheduledDate = scheduledDate; }

        public Set<Long> getCategoryIds() { return categoryIds; }
        public void setCategoryIds(Set<Long> categoryIds) { this.categoryIds = categoryIds; }
    }
}