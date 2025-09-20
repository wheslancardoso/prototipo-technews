package br.com.technews.controller;

import br.com.technews.service.NewsArticleService;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.CategoryService;
import br.com.technews.service.TrustedSourceService;
import br.com.technews.service.EmailService;
import br.com.technews.service.NewsletterTemplateService;
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
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private NewsletterTemplateService templateService;

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
    
    @GetMapping("/newsletter")
    public String newsletter(Model model) {
        try {
            // Estatísticas básicas
        SubscriberService.SubscriberStats stats = subscriberService.getStats();
        model.addAttribute("totalSubscribers", stats.getTotal());
        model.addAttribute("activeSubscribers", stats.getActive());
            model.addAttribute("totalCategories", categoryService.count());
            
            // Artigos recentes para newsletter
            model.addAttribute("recentArticles", newsArticleService.findRecentArticles(10));
            model.addAttribute("categories", categoryService.findAllActive());
            
            // Templates disponíveis
            model.addAttribute("templates", templateService.findAllActive());
            
            // Histórico de envios (simulado por enquanto)
            model.addAttribute("lastNewsletterSent", "Não enviado ainda");
            model.addAttribute("scheduledNewsletters", 0);
            
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar dados da newsletter: " + e.getMessage());
            model.addAttribute("totalSubscribers", 0L);
            model.addAttribute("activeSubscribers", 0L);
            model.addAttribute("totalCategories", 0L);
        }
        
        return "admin/newsletter";
    }
}