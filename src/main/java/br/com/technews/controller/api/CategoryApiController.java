package br.com.technews.controller.api;

import br.com.technews.entity.Category;
import br.com.technews.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * API REST para integração externa com categorias
 * Fornece endpoints públicos para acesso às categorias ativas
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*", maxAge = 3600)
@RequiredArgsConstructor
public class CategoryApiController {

    private final CategoryService categoryService;

    /**
     * Lista todas as categorias ativas
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            List<Category> allActiveCategories = categoryService.findAllActive();
            // Implementar paginação manual para categorias ativas
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), allActiveCategories.size());
            List<Category> pageContent = allActiveCategories.subList(start, end);
            Page<Category> categories = new PageImpl<>(pageContent, pageable, allActiveCategories.size());
            
            response.put("success", true);
            response.put("categories", categories.getContent());
            response.put("currentPage", page);
            response.put("totalPages", categories.getTotalPages());
            response.put("totalElements", categories.getTotalElements());
            response.put("hasNext", categories.hasNext());
            response.put("hasPrevious", categories.hasPrevious());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar categorias: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Lista todas as categorias ativas (sem paginação)
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getAllCategoriesSimple() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Category> categories = categoryService.findAllActive();
            
            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar categorias: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Busca categoria por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCategoryById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Category> category = categoryService.findById(id);
            
            if (category.isPresent() && category.get().getActive()) {
                response.put("success", true);
                response.put("category", category.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Categoria não encontrada ou inativa");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar categoria: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Busca categoria por slug
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<Map<String, Object>> getCategoryBySlug(@PathVariable String slug) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Category> category = categoryService.findBySlug(slug);
            
            if (category.isPresent() && category.get().getActive()) {
                response.put("success", true);
                response.put("category", category.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Categoria não encontrada ou inativa");
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar categoria: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Pesquisa categorias por termo
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCategories(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Category> categories = categoryService.search(query);
            
            response.put("success", true);
            response.put("categories", categories);
            response.put("query", query);
            response.put("count", categories.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao pesquisar categorias: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtém categorias com artigos publicados
     */
    @GetMapping("/with-articles")
    public ResponseEntity<Map<String, Object>> getCategoriesWithArticles() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Category> categories = categoryService.findCategoriesWithPublishedArticles();
            
            response.put("success", true);
            response.put("categories", categories);
            response.put("count", categories.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao buscar categorias com artigos: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Obtém estatísticas das categorias
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCategoryStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            long totalCategories = categoryService.count();
            long activeCategories = categoryService.countActive();
            
            response.put("success", true);
            response.put("totalCategories", totalCategories);
            response.put("activeCategories", activeCategories);
            response.put("inactiveCategories", totalCategories - activeCategories);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Erro ao obter estatísticas: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}