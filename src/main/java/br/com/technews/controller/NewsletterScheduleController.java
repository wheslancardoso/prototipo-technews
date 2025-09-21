package br.com.technews.controller;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsletterSchedule;
import br.com.technews.entity.Subscriber;
import br.com.technews.service.CategoryService;
import br.com.technews.service.NewsletterScheduleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller para gerenciamento de agendamento de newsletters
 */
@Controller
@RequestMapping("/admin/newsletter/schedule")
@RequiredArgsConstructor
public class NewsletterScheduleController {

    private static final Logger log = LoggerFactory.getLogger(NewsletterScheduleController.class);

    private final NewsletterScheduleService scheduleService;
    private final CategoryService categoryService;

    /**
     * Lista agendamentos
     */
    @GetMapping
    public String listSchedules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "scheduledDate") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @RequestParam(required = false) NewsletterSchedule.ScheduleStatus status,
            Model model) {
        
        try {
            Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
            
            Page<NewsletterSchedule> schedules;
            if (status != null) {
                schedules = scheduleService.findByStatus(status, pageable);
            } else {
                schedules = scheduleService.findAll(pageable);
            }
            
            model.addAttribute("schedules", schedules);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", schedules.getTotalPages());
            model.addAttribute("totalElements", schedules.getTotalElements());
            model.addAttribute("sort", sort);
            model.addAttribute("direction", direction);
            model.addAttribute("selectedStatus", status);
            model.addAttribute("statuses", NewsletterSchedule.ScheduleStatus.values());
            
            // Estatísticas
            NewsletterScheduleService.ScheduleStats stats = scheduleService.getStats();
            model.addAttribute("stats", stats);
            
            return "admin/newsletter/schedule/list";
        } catch (Exception e) {
            log.error("Erro ao listar agendamentos: {}", e.getMessage(), e);
            model.addAttribute("error", "Erro ao carregar agendamentos");
            return "admin/newsletter/schedule/list";
        }
    }

    /**
     * Exibe formulário para novo agendamento
     */
    @GetMapping("/new")
    public String newScheduleForm(Model model) {
        try {
            model.addAttribute("scheduleForm", new ScheduleForm());
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
            return "admin/newsletter/schedule/form";
        } catch (Exception e) {
            log.error("Erro ao carregar formulário de agendamento: {}", e.getMessage(), e);
            model.addAttribute("error", "Erro ao carregar formulário");
            return "redirect:/admin/newsletter/schedule";
        }
    }

    /**
     * Cria novo agendamento
     */
    @PostMapping("/new")
    public String createSchedule(
            @Valid @ModelAttribute("scheduleForm") ScheduleForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
            return "admin/newsletter/schedule/form";
        }
        
        try {
            NewsletterSchedule schedule = scheduleService.createSchedule(
                form.getSubject(),
                form.getTemplateKey(),
                form.getScheduledDate(),
                form.getCategoryIds(),
                form.getFrequencyFilter(),
                form.getActiveOnly(),
                form.getVerifiedOnly()
            );
            
            log.info("Agendamento criado: ID={}, assunto={}", schedule.getId(), schedule.getSubject());
            redirectAttributes.addFlashAttribute("success", "Agendamento criado com sucesso!");
            return "redirect:/admin/newsletter/schedule";
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao criar agendamento: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
            return "admin/newsletter/schedule/form";
        } catch (Exception e) {
            log.error("Erro ao criar agendamento: {}", e.getMessage(), e);
            model.addAttribute("error", "Erro interno do servidor");
            model.addAttribute("categories", categoryService.findAllActive());
            model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
            return "admin/newsletter/schedule/form";
        }
    }

    /**
     * Exibe detalhes do agendamento
     */
    @GetMapping("/{id}")
    public String viewSchedule(@PathVariable Long id, Model model) {
        try {
            NewsletterSchedule schedule = scheduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
            
            model.addAttribute("schedule", schedule);
            return "admin/newsletter/schedule/view";
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            return "redirect:/admin/newsletter/schedule";
        } catch (Exception e) {
            log.error("Erro ao visualizar agendamento ID={}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Erro ao carregar agendamento");
            return "redirect:/admin/newsletter/schedule";
        }
    }

    /**
     * Exibe formulário para editar agendamento
     */
    @GetMapping("/{id}/edit")
    public String editScheduleForm(@PathVariable Long id, Model model) {
        try {
            NewsletterSchedule schedule = scheduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
            
            if (!schedule.isPending()) {
                model.addAttribute("error", "Apenas agendamentos pendentes podem ser editados");
                return "redirect:/admin/newsletter/schedule/" + id;
            }
            
            ScheduleForm form = new ScheduleForm();
            form.setSubject(schedule.getSubject());
            form.setScheduledDate(schedule.getScheduledDate());
            // Converter categorias para IDs
            Set<Long> categoryIds = schedule.getCategories().stream()
                    .map(Category::getId)
                    .collect(Collectors.toSet());
            form.setCategoryIds(categoryIds);
            
            model.addAttribute("scheduleForm", form);
            model.addAttribute("schedule", schedule);
            model.addAttribute("categories", categoryService.findAllActive());
            return "admin/newsletter/schedule/edit";
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            return "redirect:/admin/newsletter/schedule";
        } catch (Exception e) {
            log.error("Erro ao carregar formulário de edição ID={}: {}", id, e.getMessage(), e);
            model.addAttribute("error", "Erro ao carregar formulário");
            return "redirect:/admin/newsletter/schedule";
        }
    }

    /**
     * Atualiza agendamento
     */
    @PostMapping("/{id}/edit")
    public String updateSchedule(
            @PathVariable Long id,
            @Valid @ModelAttribute("scheduleForm") ScheduleForm form,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            try {
                NewsletterSchedule schedule = scheduleService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
                model.addAttribute("schedule", schedule);
                model.addAttribute("categories", categoryService.findAllActive());
                return "admin/newsletter/schedule/edit";
            } catch (Exception e) {
                return "redirect:/admin/newsletter/schedule";
            }
        }
        
        try {
            NewsletterSchedule schedule = scheduleService.updateSchedule(
                id,
                form.getSubject(),
                form.getScheduledDate(),
                form.getCategoryIds()
            );
            
            log.info("Agendamento atualizado: ID={}", id);
            redirectAttributes.addFlashAttribute("success", "Agendamento atualizado com sucesso!");
            return "redirect:/admin/newsletter/schedule/" + id;
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            redirectAttributes.addFlashAttribute("error", "Agendamento não encontrado");
            return "redirect:/admin/newsletter/schedule";
        } catch (IllegalStateException e) {
            log.warn("Estado inválido para atualização: ID={}, erro={}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/newsletter/schedule/" + id;
        } catch (Exception e) {
            log.error("Erro ao atualizar agendamento ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erro interno do servidor");
            return "redirect:/admin/newsletter/schedule/" + id;
        }
    }

    /**
     * Cancela agendamento
     */
    @PostMapping("/{id}/cancel")
    public String cancelSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.cancelSchedule(id);
            log.info("Agendamento cancelado: ID={}", id);
            redirectAttributes.addFlashAttribute("success", "Agendamento cancelado com sucesso!");
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            redirectAttributes.addFlashAttribute("error", "Agendamento não encontrado");
        } catch (IllegalStateException e) {
            log.warn("Estado inválido para cancelamento: ID={}, erro={}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Erro ao cancelar agendamento ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erro interno do servidor");
        }
        
        return "redirect:/admin/newsletter/schedule";
    }

    /**
     * Processa agendamento imediatamente
     */
    @PostMapping("/{id}/process")
    public String processSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            NewsletterSchedule schedule = scheduleService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Agendamento não encontrado"));
            
            if (!schedule.isPending()) {
                redirectAttributes.addFlashAttribute("error", "Apenas agendamentos pendentes podem ser processados");
                return "redirect:/admin/newsletter/schedule/" + id;
            }
            
            scheduleService.processSchedule(schedule);
            log.info("Processamento manual iniciado para agendamento ID={}", id);
            redirectAttributes.addFlashAttribute("success", "Processamento iniciado com sucesso!");
        } catch (IllegalArgumentException e) {
            log.warn("Agendamento não encontrado: ID={}", id);
            redirectAttributes.addFlashAttribute("error", "Agendamento não encontrado");
        } catch (Exception e) {
            log.error("Erro ao processar agendamento ID={}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Erro interno do servidor");
        }
        
        return "redirect:/admin/newsletter/schedule/" + id;
    }

    /**
     * Form para agendamento
     */
    public static class ScheduleForm {
        @NotBlank(message = "Assunto é obrigatório")
        private String subject;

        private String templateKey = "default";

        @NotNull(message = "Data de agendamento é obrigatória")
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
}