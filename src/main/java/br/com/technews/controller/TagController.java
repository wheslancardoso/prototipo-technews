package br.com.technews.controller;

import br.com.technews.entity.Tag;
import br.com.technews.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@Slf4j
public class TagController {
    
    private final TagService tagService;
    
    /**
     * Lista todas as tags ativas
     */
    @GetMapping
    public ResponseEntity<List<Tag>> getAllTags() {
        List<Tag> tags = tagService.findAllActiveTags();
        return ResponseEntity.ok(tags);
    }
    
    /**
     * Busca tag por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tag> getTagById(@PathVariable Long id) {
        Optional<Tag> tag = tagService.findById(id);
        return tag.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Busca tags por texto
     */
    @GetMapping("/search")
    public ResponseEntity<List<Tag>> searchTags(@RequestParam String q) {
        List<Tag> tags = tagService.searchTags(q);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * Busca tags mais populares
     */
    @GetMapping("/popular")
    public ResponseEntity<List<Tag>> getPopularTags(@RequestParam(defaultValue = "10") int limit) {
        List<Tag> tags = tagService.getMostPopularTags(limit);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * Busca tags usadas em artigos publicados
     */
    @GetMapping("/published")
    public ResponseEntity<List<Tag>> getTagsFromPublishedArticles() {
        List<Tag> tags = tagService.getTagsUsedInPublishedArticles();
        return ResponseEntity.ok(tags);
    }
    
    /**
     * Cria nova tag
     */
    @PostMapping
    public ResponseEntity<?> createTag(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String color = request.get("color");
            String description = request.get("description");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nome da tag é obrigatório"));
            }
            
            Tag tag = tagService.createTag(name, color, description);
            return ResponseEntity.status(HttpStatus.CREATED).body(tag);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao criar tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Atualiza tag existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTag(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String color = request.get("color");
            String description = request.get("description");
            
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Nome da tag é obrigatório"));
            }
            
            Tag tag = tagService.updateTag(id, name, color, description);
            return ResponseEntity.ok(tag);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao atualizar tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Ativa/desativa tag
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleTagStatus(@PathVariable Long id) {
        try {
            Tag tag = tagService.toggleTagStatus(id);
            return ResponseEntity.ok(tag);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao alterar status da tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Deleta tag (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTag(@PathVariable Long id) {
        try {
            tagService.deleteTag(id);
            return ResponseEntity.ok(Map.of("message", "Tag removida com sucesso"));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erro ao remover tag", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro interno do servidor"));
        }
    }
    
    /**
     * Conta artigos por tag
     */
    @GetMapping("/{id}/articles/count")
    public ResponseEntity<Map<String, Long>> countArticlesByTag(@PathVariable Long id) {
        Long count = tagService.countArticlesByTag(id);
        return ResponseEntity.ok(Map.of("count", count));
    }
}