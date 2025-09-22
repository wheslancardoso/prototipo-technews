package br.com.technews.controller;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import br.com.technews.entity.Newsletter;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.CategoryService;
import br.com.technews.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
    private final NewsletterService newsletterService;

    /**
     * Página de inscrição na newsletter
     */
    @GetMapping("/subscribe")
    public String showSubscribePage(Model model,
                                  @RequestParam(value = "success", required = false) String success,
                                  @RequestParam(value = "error", required = false) String error) {
        
        // Buscar todas as categorias disponíveis
        List<Category> categories = categoryService.findAll(PageRequest.of(0, 100)).getContent();
        model.addAttribute("categories", categories);
        
        // Adicionar mensagens de feedback
        if (success != null) {
            model.addAttribute("successMessage", "Inscrição realizada com sucesso!");
        }
        if (error != null) {
            model.addAttribute("errorMessage", "Erro ao realizar inscrição. Tente novamente.");
        }
        
        return "newsletter/subscribe";
    }

    /**
     * Processar inscrição na newsletter
     */
    @PostMapping("/subscribe")
    public String processSubscription(@RequestParam String email,
                                    @RequestParam String name,
                                    @RequestParam(required = false) List<Long> categoryIds,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Verificar se o email já está cadastrado
            if (subscriberService.isEmailSubscribed(email)) {
                redirectAttributes.addAttribute("error", "email_exists");
                return "redirect:/newsletter/subscribe";
            }

            // Criar novo subscriber
            Subscriber subscriber = new Subscriber();
            subscriber.setEmail(email);
            subscriber.setFullName(name);
            subscriber.setActive(true);

            // Adicionar categorias selecionadas
            if (categoryIds != null && !categoryIds.isEmpty()) {
                Set<Category> selectedCategories = categoryIds.stream()
                    .map(categoryService::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet());
                
                // Usar o método addCategory para cada categoria
                selectedCategories.forEach(subscriber::addCategory);
            }

            subscriberService.save(subscriber);
            
            redirectAttributes.addAttribute("success", "true");
            return "redirect:/newsletter/subscribe";
            
        } catch (Exception e) {
            log.error("Erro ao processar inscrição: ", e);
            redirectAttributes.addAttribute("error", "processing_error");
            return "redirect:/newsletter/subscribe";
        }
    }

    /**
     * Página de cancelamento de inscrição
     */
    @GetMapping("/unsubscribe")
    public String showUnsubscribePage(@RequestParam(required = false) String token,
                                    @RequestParam(value = "success", required = false) String success,
                                    Model model) {
        
        if (token != null) {
            model.addAttribute("token", token);
        }
        
        if (success != null) {
            model.addAttribute("successMessage", "Inscrição cancelada com sucesso!");
        }
        
        return "newsletter/unsubscribe";
    }

    /**
     * Processar cancelamento de inscrição
     */
    @PostMapping("/unsubscribe")
    public String processUnsubscription(@RequestParam String email,
                                      RedirectAttributes redirectAttributes) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberService.findByEmail(email);
            
            if (subscriberOpt.isPresent()) {
                Subscriber subscriber = subscriberOpt.get();
                subscriber.setActive(false);
                subscriberService.save(subscriber);
                
                redirectAttributes.addAttribute("success", "true");
            } else {
                redirectAttributes.addAttribute("error", "email_not_found");
            }
            
            return "redirect:/newsletter/unsubscribe";
            
        } catch (Exception e) {
            log.error("Erro ao cancelar inscrição: ", e);
            redirectAttributes.addAttribute("error", "processing_error");
            return "redirect:/newsletter/unsubscribe";
        }
    }

    /**
     * API para verificar se email já está cadastrado
     */
    @GetMapping("/api/check-email")
    @ResponseBody
    public ResponseEntity<Boolean> checkEmailExists(@RequestParam String email) {
        boolean exists = subscriberService.isEmailSubscribed(email);
        return ResponseEntity.ok(exists);
    }

    /**
     * Página principal das newsletters
     */
    @GetMapping
    public String showNewsletterHome(Model model) {
        // Buscar newsletter mais recente
        Optional<Newsletter> latestNewsletter = newsletterService.findLatestPublished();
        
        if (latestNewsletter.isPresent()) {
            model.addAttribute("latestNewsletter", latestNewsletter.get());
        }
        
        // Buscar estatísticas
        long totalSubscribers = subscriberService.countActiveSubscribers();
        long totalNewsletters = newsletterService.countPublished();
        
        model.addAttribute("totalSubscribers", totalSubscribers);
        model.addAttribute("totalNewsletters", totalNewsletters);
        
        return "newsletter/home";
    }

    /**
     * Exibir newsletter específica
     */
    @GetMapping("/{slug}")
    public String showNewsletter(@PathVariable String slug, Model model) {
        Optional<Newsletter> newsletterOpt = newsletterService.findBySlug(slug);
        
        if (newsletterOpt.isEmpty()) {
            return "redirect:/newsletter";
        }
        
        Newsletter newsletter = newsletterOpt.get();
        
        // Verificar se está publicada
        if (!newsletter.getPublished()) {
            return "redirect:/newsletter";
        }
        
        model.addAttribute("newsletter", newsletter);
        
        return "newsletter/view";
    }

    /**
     * Exibir newsletter por data
     */
    @GetMapping("/data/{date}")
    public String showNewsletterByDate(@PathVariable String date, Model model) {
        try {
            LocalDate newsletterDate = parseDate(date);
            Optional<Newsletter> newsletterOpt = newsletterService.findByDate(newsletterDate);
            
            if (newsletterOpt.isEmpty()) {
                model.addAttribute("errorMessage", "Nenhuma newsletter encontrada para a data: " + date);
                return "newsletter/not-found";
            }
            
            Newsletter newsletter = newsletterOpt.get();
            
            // Verificar se está publicada
            if (!newsletter.getPublished()) {
                model.addAttribute("errorMessage", "Newsletter não está disponível.");
                return "newsletter/not-found";
            }
            
            model.addAttribute("newsletter", newsletter);
            
            return "newsletter/view";
            
        } catch (DateTimeParseException e) {
            model.addAttribute("errorMessage", "Formato de data inválido: " + date);
            return "newsletter/not-found";
        }
    }

    /**
     * Página de arquivo de newsletters
     */
    @GetMapping("/arquivo")
    public String showNewsletterArchive(Model model,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Newsletter> newsletters = newsletterService.findAllPublished(pageable);
            
            model.addAttribute("newsletters", newsletters);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", newsletters.getTotalPages());
            model.addAttribute("totalElements", newsletters.getTotalElements());
            
            return "newsletter/archive";
            
        } catch (Exception e) {
            log.error("Erro ao carregar arquivo de newsletters: ", e);
            model.addAttribute("errorMessage", "Erro ao carregar newsletters.");
            return "newsletter/archive";
        }
    }

    /**
     * API para buscar newsletters por período
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<Page<Newsletter>> searchNewsletters(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Newsletter> newsletters;
            
            if (startDate != null && endDate != null) {
                LocalDate start = parseDate(startDate);
                LocalDate end = parseDate(endDate);
                newsletters = newsletterService.findByDateRange(start, end, pageable);
            } else {
                newsletters = newsletterService.findAllPublished(pageable);
            }
            
            return ResponseEntity.ok(newsletters);
            
        } catch (Exception e) {
            log.error("Erro ao buscar newsletters: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gerar newsletter para uma data específica
     */
    @PostMapping("/api/generate/{date}")
    @ResponseBody
    public ResponseEntity<String> generateNewsletterForDate(@PathVariable String date) {
        try {
            LocalDate newsletterDate = parseDate(date);
            
            // Verificar se já existe newsletter para esta data
            if (newsletterService.existsForDate(newsletterDate)) {
                return ResponseEntity.badRequest().body("Já existe newsletter para esta data");
            }
            
            // Gerar newsletter
            Newsletter newsletter = newsletterService.generateDailyNewsletter(newsletterDate);
            
            if (newsletter != null) {
                return ResponseEntity.ok("Newsletter gerada com sucesso para " + date);
            } else {
                return ResponseEntity.badRequest().body("Não foi possível gerar newsletter para esta data");
            }
            
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Formato de data inválido: " + date);
        } catch (Exception e) {
            log.error("Erro ao gerar newsletter: ", e);
            return ResponseEntity.internalServerError().body("Erro interno do servidor");
        }
    }

    /**
     * Método auxiliar para fazer parse de datas em diferentes formatos
     */
    private LocalDate parseDate(String dateStr) throws DateTimeParseException {
        List<DateTimeFormatter> formatters = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        );
        
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (DateTimeParseException ignored) {
                // Tentar próximo formato
            }
        }
        
        throw new DateTimeParseException("Formato de data não suportado: " + dateStr, dateStr, 0);
    }
}