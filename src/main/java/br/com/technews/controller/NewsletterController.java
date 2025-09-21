package br.com.technews.controller;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private static final Logger log = LoggerFactory.getLogger(NewsletterController.class);

    private final SubscriberService subscriberService;
    private final CategoryService categoryService;

    /**
     * Página de inscrição na newsletter
     */
    @GetMapping("/subscribe")
    public String showSubscribePage(Model model,
                                  @RequestParam(required = false) String success,
                                  @RequestParam(required = false) String error) {
        List<Category> categories = categoryService.findAllActive();
        model.addAttribute("categories", categories);
        model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
        
        // Adicionar estatísticas para exibir na página
        try {
            var stats = subscriberService.getStats();
            model.addAttribute("stats", stats);
        } catch (Exception e) {
            log.warn("Erro ao carregar estatísticas: {}", e.getMessage());
        }
        
        // Adicionar flags para success/error
        if (success != null) {
            model.addAttribute("success", true);
        }
        if (error != null) {
            model.addAttribute("error", true);
        }
        
        return "newsletter/newsletter-subscription";
    }

    /**
     * Processa inscrição na newsletter
     */
    @PostMapping("/subscribe")
    public String processSubscription(@RequestParam String email,
                                    @RequestParam String fullName,
                                    @RequestParam(required = false) Subscriber.SubscriptionFrequency frequency,
                                    @RequestParam(required = false) Set<Long> categoryIds,
                                    RedirectAttributes redirectAttributes) {
        try {
            subscriberService.subscribe(email, fullName, frequency, categoryIds);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Inscrição realizada com sucesso! Verifique seu email para confirmar a inscrição.");
            
            log.info("Nova inscrição processada: {}", email);
            return "redirect:/newsletter/subscribe?success";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("email", email);
            redirectAttributes.addFlashAttribute("fullName", fullName);
            redirectAttributes.addFlashAttribute("frequency", frequency);
            redirectAttributes.addFlashAttribute("categoryIds", categoryIds);
            
            return "redirect:/newsletter/subscribe?error";
        } catch (Exception e) {
            log.error("Erro ao processar inscrição para {}: {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ocorreu um erro interno. Tente novamente mais tarde.");
            
            return "redirect:/newsletter/subscribe?error";
        }
    }

    /**
     * Verifica email do assinante
     */
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam String token, Model model) {
        try {
            boolean verified = subscriberService.verifyEmail(token);
            
            if (verified) {
                model.addAttribute("successMessage", 
                    "Email verificado com sucesso! Você receberá nossa newsletter conforme sua frequência escolhida.");
                model.addAttribute("verified", true);
                
                log.info("Email verificado com sucesso para token: {}", token);
            } else {
                model.addAttribute("errorMessage", 
                    "Token de verificação inválido ou expirado. Solicite uma nova verificação.");
                model.addAttribute("verified", false);
            }
            
        } catch (Exception e) {
            log.error("Erro ao verificar email com token {}: {}", token, e.getMessage());
            model.addAttribute("errorMessage", 
                "Ocorreu um erro ao verificar seu email. Tente novamente mais tarde.");
            model.addAttribute("verified", false);
        }
        
        return "newsletter/verify";
    }

    /**
     * Página de cancelamento de inscrição
     */
    @GetMapping("/unsubscribe")
    public String showUnsubscribePage(@RequestParam String token, Model model) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberService.findByManageToken(token);
            
            if (subscriberOpt.isPresent()) {
                Subscriber subscriber = subscriberOpt.get();
                model.addAttribute("subscriber", subscriber);
                model.addAttribute("token", token);
                model.addAttribute("validToken", true);
            } else {
                model.addAttribute("validToken", false);
                model.addAttribute("errorMessage", "Token inválido ou expirado.");
            }
            
        } catch (Exception e) {
            log.error("Erro ao carregar página de cancelamento para token {}: {}", token, e.getMessage());
            model.addAttribute("validToken", false);
            model.addAttribute("errorMessage", "Ocorreu um erro. Tente novamente mais tarde.");
        }
        
        return "newsletter/unsubscribe";
    }

    /**
     * Processa cancelamento de inscrição
     */
    @PostMapping("/unsubscribe")
    public String processUnsubscription(@RequestParam String token, 
                                      RedirectAttributes redirectAttributes) {
        try {
            boolean unsubscribed = subscriberService.unsubscribe(token);
            
            if (unsubscribed) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Inscrição cancelada com sucesso. Você receberá um email de confirmação.");
                
                log.info("Inscrição cancelada com sucesso para token: {}", token);
                return "redirect:/newsletter/unsubscribe?token=" + token + "&success";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Token inválido ou inscrição já cancelada.");
                
                return "redirect:/newsletter/unsubscribe?token=" + token + "&error";
            }
            
        } catch (Exception e) {
            log.error("Erro ao cancelar inscrição para token {}: {}", token, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ocorreu um erro ao cancelar a inscrição. Tente novamente mais tarde.");
            
            return "redirect:/newsletter/unsubscribe?token=" + token + "&error";
        }
    }

    /**
     * Página de gerenciamento de preferências
     */
    @GetMapping("/manage")
    public String showManagePage(@RequestParam String token, Model model) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberService.findByManageToken(token);
            
            if (subscriberOpt.isPresent()) {
                Subscriber subscriber = subscriberOpt.get();
                List<Category> allCategories = categoryService.findAllActive();
                
                model.addAttribute("subscriber", subscriber);
                model.addAttribute("categories", allCategories);
                model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
                model.addAttribute("token", token);
                model.addAttribute("validToken", true);
            } else {
                model.addAttribute("validToken", false);
                model.addAttribute("errorMessage", "Token inválido ou expirado.");
            }
            
        } catch (Exception e) {
            log.error("Erro ao carregar página de gerenciamento para token {}: {}", token, e.getMessage());
            model.addAttribute("validToken", false);
            model.addAttribute("errorMessage", "Ocorreu um erro. Tente novamente mais tarde.");
        }
        
        return "newsletter/manage";
    }

    /**
     * Atualiza preferências do assinante
     */
    @PostMapping("/manage")
    public String updatePreferences(@RequestParam String token,
                                  @RequestParam(required = false) String fullName,
                                  @RequestParam(required = false) Subscriber.SubscriptionFrequency frequency,
                                  @RequestParam(required = false) Set<Long> categoryIds,
                                  RedirectAttributes redirectAttributes) {
        try {
            subscriberService.updatePreferences(token, frequency, categoryIds, fullName);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Preferências atualizadas com sucesso!");
            
            log.info("Preferências atualizadas para token: {}", token);
            return "redirect:/newsletter/manage?token=" + token + "&success";
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/newsletter/manage?token=" + token + "&error";
            
        } catch (Exception e) {
            log.error("Erro ao atualizar preferências para token {}: {}", token, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ocorreu um erro ao atualizar suas preferências. Tente novamente mais tarde.");
            
            return "redirect:/newsletter/manage?token=" + token + "&error";
        }
    }

    /**
     * API para verificar se email já está inscrito
     */
    @GetMapping("/api/check-email")
    @ResponseBody
    public ResponseEntity<CheckEmailResponse> checkEmail(@RequestParam String email) {
        try {
            Optional<Subscriber> subscriber = subscriberService.findByEmail(email);
            
            if (subscriber.isPresent()) {
                Subscriber sub = subscriber.get();
                return ResponseEntity.ok(new CheckEmailResponse(
                    true, 
                    sub.isActive(), 
                    sub.isEmailVerified(),
                    sub.isActive() ? "Este email já está inscrito na newsletter." : 
                                   "Este email estava inscrito mas foi cancelado."
                ));
            } else {
                return ResponseEntity.ok(new CheckEmailResponse(false, false, false, null));
            }
            
        } catch (Exception e) {
            log.error("Erro ao verificar email {}: {}", email, e.getMessage());
            return ResponseEntity.ok(new CheckEmailResponse(false, false, false, 
                "Erro ao verificar email."));
        }
    }

    /**
     * Página de reativação de inscrição
     */
    @GetMapping("/reactivate")
    public String showReactivatePage() {
        return "newsletter/reactivate";
    }

    /**
     * Processa reativação de inscrição
     */
    @PostMapping("/reactivate")
    public String processReactivation(@RequestParam String email,
                                    RedirectAttributes redirectAttributes) {
        try {
            boolean reactivated = subscriberService.reactivateSubscription(email);
            
            if (reactivated) {
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Inscrição reativada com sucesso! Você voltará a receber nossa newsletter.");
                
                log.info("Inscrição reativada para: {}", email);
                return "redirect:/newsletter/reactivate?success";
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", 
                    "Email não encontrado ou não foi cancelado anteriormente.");
                
                return "redirect:/newsletter/reactivate?error";
            }
            
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/newsletter/reactivate?error";
            
        } catch (Exception e) {
            log.error("Erro ao reativar inscrição para {}: {}", email, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Ocorreu um erro ao reativar a inscrição. Tente novamente mais tarde.");
            
            return "redirect:/newsletter/reactivate?error";
        }
    }

    /**
     * Página de sucesso genérica
     */
    @GetMapping("/success")
    public String showSuccessPage(@RequestParam(required = false) String message, Model model) {
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "newsletter/success";
    }

    /**
     * Página de arquivo de newsletters anteriores
     */
    @GetMapping("/archive")
    public String listNewsletters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model) {
        
        try {
            // Buscar assinantes que receberam newsletters (têm lastEmailSentAt preenchido)
            // Isso simula um histórico de newsletters enviadas
            List<Subscriber> subscribersWithEmails = subscriberService.getAllSubscribers()
                .stream()
                .filter(s -> s.getLastEmailSentAt() != null)
                .sorted((s1, s2) -> s2.getLastEmailSentAt().compareTo(s1.getLastEmailSentAt()))
                .collect(Collectors.toList());

            // Criar uma lista simulada de newsletters baseada nas datas de envio
            List<NewsletterInfo> newsletters = subscribersWithEmails.stream()
                .map(Subscriber::getLastEmailSentAt)
                .distinct()
                .sorted((d1, d2) -> d2.compareTo(d1))
                .limit(50) // Limitar a 50 newsletters mais recentes
                .map(this::createNewsletterInfo)
                .collect(Collectors.toList());

            // Implementar paginação manual
            int start = page * size;
            int end = Math.min(start + size, newsletters.size());
            List<NewsletterInfo> paginatedNewsletters = newsletters.subList(start, end);
            
            int totalPages = (int) Math.ceil((double) newsletters.size() / size);

            model.addAttribute("newsletters", paginatedNewsletters);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", totalPages);
            model.addAttribute("totalElements", newsletters.size());
            model.addAttribute("hasNext", page < totalPages - 1);
            model.addAttribute("hasPrevious", page > 0);

        } catch (Exception e) {
            // Em caso de erro, criar lista vazia
            model.addAttribute("newsletters", List.of());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalElements", 0);
            model.addAttribute("hasNext", false);
            model.addAttribute("hasPrevious", false);
            model.addAttribute("error", "Erro ao carregar newsletters: " + e.getMessage());
        }

        return "newsletters/archive";
    }

    private NewsletterInfo createNewsletterInfo(LocalDateTime sentDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("'Semana de' dd/MM/yyyy");
        
        String title = "TechNews - " + sentDate.format(weekFormatter);
        String description = "Newsletter semanal com as principais notícias de tecnologia, " +
                           "inovação e startups da semana.";
        
        return new NewsletterInfo(
            title,
            description,
            sentDate,
            sentDate.format(formatter),
            generateNewsletterUrl(sentDate)
        );
    }

    private String generateNewsletterUrl(LocalDateTime sentDate) {
        // Por enquanto, retorna uma URL placeholder
        // Futuramente pode ser implementado um sistema de arquivo de newsletters
        return "#newsletter-" + sentDate.toLocalDate().toString();
    }

    /**
     * Classe para resposta da verificação de email
     */
    public static class CheckEmailResponse {
        private final boolean exists;
        private final boolean active;
        private final boolean verified;
        private final String message;

        public CheckEmailResponse(boolean exists, boolean active, boolean verified, String message) {
            this.exists = exists;
            this.active = active;
            this.verified = verified;
            this.message = message;
        }

        // Getters
        public boolean isExists() { return exists; }
        public boolean isActive() { return active; }
        public boolean isVerified() { return verified; }
        public String getMessage() { return message; }
    }

    /**
     * Classe interna para representar informações da newsletter
     */
    public static class NewsletterInfo {
        private String title;
        private String description;
        private LocalDateTime sentDate;
        private String formattedDate;
        private String url;

        public NewsletterInfo(String title, String description, LocalDateTime sentDate, 
                            String formattedDate, String url) {
            this.title = title;
            this.description = description;
            this.sentDate = sentDate;
            this.formattedDate = formattedDate;
            this.url = url;
        }

        // Getters
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDateTime getSentDate() { return sentDate; }
        public String getFormattedDate() { return formattedDate; }
        public String getUrl() { return url; }
    }
}