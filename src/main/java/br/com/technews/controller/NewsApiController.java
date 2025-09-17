package br.com.technews.controller;

import br.com.technews.entity.NewsArticle;
import br.com.technews.service.GNewsService;
import br.com.technews.service.NewsArticleService;
import br.com.technews.scheduler.NewsScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsApiController {

    private final GNewsService gNewsService;
    private final NewsArticleService newsArticleService;
    private final NewsScheduler newsScheduler;

    public NewsApiController(GNewsService gNewsService, 
                           NewsArticleService newsArticleService,
                           NewsScheduler newsScheduler) {
        this.gNewsService = gNewsService;
        this.newsArticleService = newsArticleService;
        this.newsScheduler = newsScheduler;
    }

    /**
     * Endpoint para buscar notícias manualmente da API GNews
     */
    @GetMapping("/fetch")
    public ResponseEntity<Map<String, Object>> fetchNews(@RequestParam(required = false) String query) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NewsArticle> articles;
            
            if (query != null && !query.trim().isEmpty()) {
                articles = gNewsService.searchNews(query);
            } else {
                articles = gNewsService.getTechNews();
            }
            
            // Filtra por fontes confiáveis
            List<NewsArticle> filteredArticles = gNewsService.filterByTrustedSources(articles);
            
            // Salva apenas artigos novos
            int savedCount = 0;
            for (NewsArticle article : filteredArticles) {
                if (!newsArticleService.existsByUrl(article.getUrl())) {
                    newsArticleService.save(article);
                    savedCount++;
                }
            }
            
            response.put("success", true);
            response.put("message", "Busca concluída com sucesso");
            response.put("totalFound", filteredArticles.size());
            response.put("newArticlesSaved", savedCount);
            response.put("query", query != null ? query : "technology");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar notícias: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para forçar execução do scheduler
     */
    @PostMapping("/force-update")
    public ResponseEntity<Map<String, Object>> forceUpdate() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            newsScheduler.forceNewsUpdate();
            
            response.put("success", true);
            response.put("message", "Atualização forçada executada com sucesso");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao executar atualização: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para obter estatísticas da importação de notícias
     */
    @GetMapping("/import-stats")
    public ResponseEntity<Map<String, Object>> getImportStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long totalArticles = newsArticleService.countAll();
            long publishedArticles = newsArticleService.countPublished();
            long pendingArticles = totalArticles - publishedArticles;
            
            response.put("success", true);
            response.put("totalArticles", totalArticles);
            response.put("publishedArticles", publishedArticles);
            response.put("pendingArticles", pendingArticles);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao obter estatísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para testar conectividade com a API GNews
     */
    @GetMapping("/test-connection")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Tenta buscar apenas 1 artigo para testar
            List<NewsArticle> testArticles = gNewsService.searchNews("test");
            
            response.put("success", true);
            response.put("message", "Conexão com GNews API funcionando");
            response.put("testResultsCount", testArticles.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro na conexão com GNews API: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint para buscar notícias por categoria específica
     */
    @GetMapping("/fetch-by-category")
    public ResponseEntity<Map<String, Object>> fetchByCategory(@RequestParam String category) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NewsArticle> articles = gNewsService.getTopHeadlines(category);
            List<NewsArticle> filteredArticles = gNewsService.filterByTrustedSources(articles);
            
            int savedCount = 0;
            for (NewsArticle article : filteredArticles) {
                if (!newsArticleService.existsByUrl(article.getUrl())) {
                    newsArticleService.save(article);
                    savedCount++;
                }
            }
            
            response.put("success", true);
            response.put("message", "Busca por categoria concluída");
            response.put("category", category);
            response.put("totalFound", filteredArticles.size());
            response.put("newArticlesSaved", savedCount);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar por categoria: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}