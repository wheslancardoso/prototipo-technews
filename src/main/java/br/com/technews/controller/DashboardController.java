package br.com.technews.controller;

import br.com.technews.service.NewsArticleService;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.CategoryService;
import br.com.technews.service.TrustedSourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller para o dashboard administrativo principal
 */
@Controller
@RequestMapping("/admin")
public class DashboardController {

    @Autowired
    private NewsArticleService newsArticleService;
    
    @Autowired
    private SubscriberService subscriberService;
    
    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private TrustedSourceService trustedSourceService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Estatísticas gerais
            model.addAttribute("totalArticles", newsArticleService.countAll());
            model.addAttribute("publishedArticles", newsArticleService.countPublished());
            // Estatísticas de assinantes
        SubscriberService.SubscriberStats subscriberStats = subscriberService.getStats();
        model.addAttribute("totalSubscribers", subscriberStats.getTotalSubscribers());
        model.addAttribute("activeSubscribers", subscriberStats.getActiveSubscribers());
        model.addAttribute("totalCategories", categoryService.count());
        model.addAttribute("activeSources", trustedSourceService.countActive());
            
            // Conteúdo recente
            model.addAttribute("recentArticles", newsArticleService.findRecentArticles(5));
            model.addAttribute("recentSubscribers", subscriberService.findRecentSubscribers(5));
            
        } catch (Exception e) {
            // Em caso de erro, definir valores padrão
            model.addAttribute("error", "Erro ao carregar estatísticas: " + e.getMessage());
            model.addAttribute("totalArticles", 0L);
            model.addAttribute("publishedArticles", 0L);
            model.addAttribute("totalSubscribers", 0L);
            model.addAttribute("totalCategories", 0L);
            model.addAttribute("activeSources", 0L);
        }
        
        return "admin/dashboard";
    }
}