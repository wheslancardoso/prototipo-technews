package br.com.technews.controller;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Controller
@RequestMapping("/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final SubscriberService subscriberService;
    private final CategoryService categoryService;

    /**
     * Página de inscrição na newsletter
     */
    @GetMapping("/subscribe")
    public String showSubscribePage(Model model) {
        List<Category> categories = categoryService.findAllActive();
        model.addAttribute("categories", categories);
        model.addAttribute("frequencies", Subscriber.SubscriptionFrequency.values());
        return "newsletter/subscribe";
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
}