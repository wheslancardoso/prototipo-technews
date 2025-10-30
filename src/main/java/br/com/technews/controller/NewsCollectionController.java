package br.com.technews.controller;

import br.com.technews.entity.NewsArticle;
import br.com.technews.service.NewsScrapingService;
import br.com.technews.service.NewsCollectionService;
import br.com.technews.service.NewsSchedulerService;
import br.com.technews.service.NewsCurationService;
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
public class NewsCollectionController {

    private final NewsScrapingService newsScrapingService;
    private final NewsCollectionService newsCollectionService;
    private final NewsSchedulerService newsSchedulerService;
    private final NewsCurationService newsCurationService;

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
            long before = newsCollectionService.getTotalCollectedNews();
            // Coleta via fontes confiáveis (RSS) ignorando intervalos, para ação manual
            newsCollectionService.collectNewsFromAllSourcesForced();
            // Processa itens pendentes para aprovar/rejeitar imediatamente após a coleta
            newsCurationService.processAllPendingNews();
            long after = newsCollectionService.getTotalCollectedNews();
            int collected = (int) Math.max(0, after - before);

            response.put("success", true);
            response.put("message", "Coleta e curadoria realizadas com sucesso!");
            response.put("articlesCollected", collected);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro durante a coleta: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/publish-approved")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> publishApproved(@RequestParam(defaultValue = "20") int limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            int published = newsCurationService.publishApprovedCollectedNews(limit);
            response.put("success", true);
            response.put("message", "Publicação concluída com sucesso!");
            response.put("articlesPublished", published);
            response.put("limit", limit);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao publicar aprovadas: " + e.getMessage());
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