package com.technews.controller;

import br.com.technews.service.NewsArticleService;
import br.com.technews.service.CategoryService;
import com.technews.service.TagService;
import com.technews.service.CommentService;
import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Category;
import com.technews.entity.Tag;
import com.technews.entity.CommentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Slf4j
public class StatsController {
    
    private final NewsArticleService newsArticleService;
    private final TagService tagService;
    private final CommentService commentService;
    
    /**
     * Estatísticas gerais do sistema
     */
    @GetMapping("/general")
    public ResponseEntity<Map<String, Object>> getGeneralStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Total de artigos publicados
            List<NewsArticle> publishedArticles = newsArticleService.findPublishedArticles();
            stats.put("totalPublishedArticles", publishedArticles.size());
            
            // Total de categorias distintas
            List<String> categories = newsArticleService.getDistinctCategories();
            stats.put("totalCategories", categories.size());
            
            // Total de tags ativas
            List<Tag> activeTags = tagService.findAllActiveTags();
            stats.put("totalActiveTags", activeTags.size());
            
            // Total de comentários aprovados
            Long approvedComments = commentService.getCommentCountByStatus(CommentStatus.APPROVED);
            stats.put("totalApprovedComments", approvedComments);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas gerais", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao carregar estatísticas"));
        }
    }
    
    /**
     * Artigos mais recentes (últimos 10)
     */
    @GetMapping("/recent-articles")
    public ResponseEntity<List<NewsArticle>> getRecentArticles() {
        try {
            List<NewsArticle> recentArticles = newsArticleService.findRecentArticles(10);
            return ResponseEntity.ok(recentArticles);
            
        } catch (Exception e) {
            log.error("Erro ao buscar artigos recentes", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Categorias mais populares (com mais artigos)
     */
    @GetMapping("/popular-categories")
    public ResponseEntity<Map<String, Object>> getPopularCategories() {
        try {
            List<NewsArticle> publishedArticles = newsArticleService.findPublishedArticles();
            
            // Conta artigos por categoria
            Map<String, Long> categoryCount = publishedArticles.stream()
                    .filter(article -> article.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            NewsArticle::getCategory,
                            Collectors.counting()
                    ));
            
            // Ordena por quantidade (decrescente) e pega os top 10
            List<Map<String, Object>> topCategories = categoryCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> {
                        Map<String, Object> categoryMap = new HashMap<>();
                        categoryMap.put("category", entry.getKey());
                        categoryMap.put("count", entry.getValue());
                        return categoryMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("categories", topCategories);
            response.put("totalCategories", categoryCount.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao buscar categorias populares", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao carregar categorias populares"));
        }
    }
    
    /**
     * Tags mais populares (com mais artigos)
     */
    @GetMapping("/popular-tags")
    public ResponseEntity<List<Tag>> getPopularTags(@RequestParam(defaultValue = "10") int limit) {
        try {
            List<Tag> popularTags = tagService.getMostPopularTags(limit);
            return ResponseEntity.ok(popularTags);
            
        } catch (Exception e) {
            log.error("Erro ao buscar tags populares", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Autores mais ativos (com mais artigos)
     */
    @GetMapping("/active-authors")
    public ResponseEntity<Map<String, Object>> getActiveAuthors() {
        try {
            List<NewsArticle> publishedArticles = newsArticleService.findPublishedArticles();
            
            // Conta artigos por autor
            Map<String, Long> authorCount = publishedArticles.stream()
                    .filter(article -> article.getAuthor() != null && !article.getAuthor().trim().isEmpty())
                    .collect(Collectors.groupingBy(
                            NewsArticle::getAuthor,
                            Collectors.counting()
                    ));
            
            // Ordena por quantidade (decrescente) e pega os top 10
            List<Map<String, Object>> topAuthors = authorCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> {
                        Map<String, Object> authorMap = new HashMap<>();
                        authorMap.put("author", entry.getKey());
                        authorMap.put("count", entry.getValue());
                        return authorMap;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("authors", topAuthors);
            response.put("totalAuthors", authorCount.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erro ao buscar autores ativos", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao carregar autores ativos"));
        }
    }
    
    /**
     * Estatísticas de comentários
     */
    @GetMapping("/comments")
    public ResponseEntity<Map<String, Object>> getCommentStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // Total de comentários por status
            stats.put("approved", commentService.getCommentCountByStatus(CommentStatus.APPROVED));
            stats.put("pending", commentService.getCommentCountByStatus(CommentStatus.PENDING));
            stats.put("rejected", commentService.getCommentCountByStatus(CommentStatus.REJECTED));
            stats.put("spam", commentService.getCommentCountByStatus(CommentStatus.SPAM));
            
            // Total geral
            Long totalComments = commentService.getCommentCountByStatus(CommentStatus.APPROVED) +
                               commentService.getCommentCountByStatus(CommentStatus.PENDING) +
                               commentService.getCommentCountByStatus(CommentStatus.REJECTED);
            stats.put("total", totalComments);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Erro ao buscar estatísticas de comentários", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao carregar estatísticas de comentários"));
        }
    }
    
    /**
     * Resumo completo das estatísticas
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getStatsSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            
            // Estatísticas gerais
            List<NewsArticle> publishedArticles = newsArticleService.findPublishedArticles();
            summary.put("totalArticles", publishedArticles.size());
            summary.put("totalCategories", newsArticleService.getDistinctCategories().size());
            summary.put("totalTags", tagService.findAllActiveTags().size());
            summary.put("totalComments", commentService.getCommentCountByStatus(CommentStatus.APPROVED));
            
            // Top 5 categorias
            Map<String, Long> categoryCount = publishedArticles.stream()
                    .filter(article -> article.getCategory() != null)
                    .collect(Collectors.groupingBy(
                            NewsArticle::getCategory,
                            Collectors.counting()
                    ));
            
            List<Map<String, Object>> topCategories = categoryCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> {
                        Map<String, Object> categoryMap = new HashMap<>();
                        categoryMap.put("name", entry.getKey());
                        categoryMap.put("count", entry.getValue());
                        return categoryMap;
                    })
                    .collect(Collectors.toList());
            
            summary.put("topCategories", topCategories);
            
            // Top 5 tags
            List<Tag> topTags = tagService.getMostPopularTags(5);
            summary.put("topTags", topTags);
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            log.error("Erro ao buscar resumo das estatísticas", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erro ao carregar resumo das estatísticas"));
        }
    }
    
    @GetMapping("/stats")
    public String statsPage() {
        return "stats";
    }
}