package br.com.technews.controller.api;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.service.NewsArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * API REST para integração externa com artigos
 * Fornece endpoints públicos para acesso aos artigos publicados
 */
@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ArticleApiController {

    @Autowired
    private NewsArticleService newsArticleService;

    /**
     * Lista todos os artigos publicados com paginação
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "publishedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<NewsArticle> articles = newsArticleService.findPublishedArticles(pageable);
            
            response.put("success", true);
            response.put("articles", articles.getContent());
            response.put("currentPage", page);
            response.put("totalPages", articles.getTotalPages());
            response.put("totalElements", articles.getTotalElements());
            response.put("hasNext", articles.hasNext());
            response.put("hasPrevious", articles.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar artigos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Busca artigo por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getArticleById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<NewsArticle> article = newsArticleService.findById(id);
            
            if (article.isPresent() && article.get().getPublished()) {
                response.put("success", true);
                response.put("article", article.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Artigo não encontrado ou não publicado");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar artigo: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Busca artigos por categoria
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Map<String, Object>> getArticlesByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
            Page<NewsArticle> articles = newsArticleService.findPublishedArticlesByCategory(category, pageable);
            
            response.put("success", true);
            response.put("articles", articles.getContent());
            response.put("category", category);
            response.put("currentPage", page);
            response.put("totalPages", articles.getTotalPages());
            response.put("totalElements", articles.getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar artigos por categoria: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Busca artigos por termo de pesquisa
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchArticles(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());
            Page<NewsArticle> articles = newsArticleService.searchArticlesWithFilters(
                query, null, null, null, null, pageable);
            
            response.put("success", true);
            response.put("articles", articles.getContent());
            response.put("query", query);
            response.put("currentPage", page);
            response.put("totalPages", articles.getTotalPages());
            response.put("totalElements", articles.getTotalElements());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao pesquisar artigos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtém artigos mais recentes
     */
    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecentArticles(
            @RequestParam(defaultValue = "10") int limit) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<NewsArticle> articles = newsArticleService.findRecentArticles(limit);
            
            response.put("success", true);
            response.put("articles", articles);
            response.put("count", articles.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar artigos recentes: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtém estatísticas dos artigos
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getArticleStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long totalArticles = newsArticleService.countAll();
            long publishedArticles = newsArticleService.countPublished();
            
            response.put("success", true);
            response.put("totalArticles", totalArticles);
            response.put("publishedArticles", publishedArticles);
            response.put("pendingArticles", totalArticles - publishedArticles);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao obter estatísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}