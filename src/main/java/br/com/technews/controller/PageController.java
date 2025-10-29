package br.com.technews.controller;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.NewsArticle;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.NewsArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final SubscriberService subscriberService;
    
    private final NewsArticleService newsArticleService;
    
    @GetMapping("/")
    public String home(Model model) {
        try {
            // Estatísticas de assinantes
            List<Subscriber> subscribers = subscriberService.getAllSubscribers();
            model.addAttribute("subscriberCount", subscribers.size());
            
            // Artigos em destaque (últimos 6 publicados)
            List<NewsArticle> featuredArticles = newsArticleService.findPublishedArticles(
                PageRequest.of(0, 6, Sort.by("publishedAt").descending())
            ).getContent();
            model.addAttribute("featuredArticles", featuredArticles);
            
            // Estatísticas gerais
            model.addAttribute("totalArticles", newsArticleService.countPublished());
            model.addAttribute("categories", newsArticleService.getDistinctCategories());
            
        } catch (Exception e) {
            // Se houver erro, define valores padrão
            model.addAttribute("subscriberCount", 0);
            model.addAttribute("featuredArticles", List.of());
            model.addAttribute("totalArticles", 0);
            model.addAttribute("categories", List.of());
        }
        return "index";
    }

    // ADMIN DESATIVADO
    // @GetMapping("/admin")
    // public String adminDashboard() {
    //     return "redirect:/admin/articles";
    // }
    
    // LOGIN DESATIVADO
    // @GetMapping("/login")
    // public String login() {
    //     return "login";
    // }
    
    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String nome, 
                          @RequestParam String email, 
                          RedirectAttributes redirectAttributes) {
        try {
            subscriberService.subscribe(email, nome, null, null);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Obrigado, " + nome + "! Sua inscrição foi realizada com sucesso.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro interno. Tente novamente mais tarde.");
        }
        
        return "redirect:/";
    }
}
