package br.com.technews.controller;

import br.com.technews.entity.NewsArticle;
import br.com.technews.service.NewsScrapingService;
import br.com.technews.service.NewsSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NewsCollectionController {

    private final NewsScrapingService newsScrapingService;
    private final NewsSchedulerService newsSchedulerService;

    @GetMapping
    public String newsManagement(Model model) {
        try {
            long totalArticles = newsScrapingService.getArticleCount();
            List<NewsArticle> recentArticles = newsScrapingService.getRecentArticles(10);
            
            model.addAttribute("totalArticles", totalArticles);
            model.addAttribute("recentArticles", recentArticles);
            
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao carregar dados: " + e.getMessage());
            model.addAttribute("totalArticles", 0);
            model.addAttribute("recentArticles", List.of());
        }
        
        return "admin/news/management";
    }

    @PostMapping("/collect")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> collectNews() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NewsArticle> articles = newsScrapingService.scrapeNews();
            
            response.put("success", true);
            response.put("message", "Coleta realizada com sucesso!");
            response.put("articlesCollected", articles.size());
            response.put("articles", articles.stream()
                    .map(article -> Map.of(
                            "title", article.getTitle(),
                            "source", article.getSource(),
                            "url", article.getUrl()
                    ))
                    .toList());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro durante a coleta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/send-newsletter")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendNewsletter() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            newsSchedulerService.executeManualNewsletter();
            
            response.put("success", true);
            response.put("message", "Newsletter enviada com sucesso!");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao enviar newsletter: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalArticles = newsScrapingService.getArticleCount();
            List<NewsArticle> recentArticles = newsScrapingService.getRecentArticles(5);
            
            stats.put("totalArticles", totalArticles);
            stats.put("recentCount", recentArticles.size());
            stats.put("lastUpdate", recentArticles.isEmpty() ? null : 
                    recentArticles.get(0).getCreatedAt());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            stats.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(stats);
        }
    }
}