package br.com.technews.controller;

import br.com.technews.entity.Comment;
import br.com.technews.service.CommentService;
import br.com.technews.entity.NewsArticle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@CrossOrigin(origins = "*")
public class CommentController {
    
    @Autowired
    private CommentService commentService;
    
    @GetMapping("/article/{articleId}")
    public ResponseEntity<Map<String, Object>> getCommentsByArticle(@PathVariable Long articleId) {
        try {
            List<Comment> comments = commentService.getApprovedCommentsByArticle(articleId);
            Long totalComments = commentService.countApprovedCommentsByArticle(articleId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", comments);
            response.put("totalComments", totalComments);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao carregar comentários: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/replies/{parentId}")
    public ResponseEntity<Map<String, Object>> getRepliesByParent(@PathVariable Long parentId) {
        try {
            List<Comment> replies = commentService.getApprovedRepliesByParent(parentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("replies", replies);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao carregar respostas: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createComment(
            @Valid @RequestBody CommentRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            Comment comment = commentService.createComment(
                request.getArticleId(),
                request.getAuthorName(),
                request.getAuthorEmail(),
                request.getAuthorWebsite(),
                request.getContent(),
                ipAddress,
                userAgent,
                request.getParentId()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comment", comment);
            response.put("message", comment.getApproved() ? 
                "Comentário publicado com sucesso!" : 
                "Comentário enviado para moderação. Será publicado após aprovação.");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro interno do servidor. Tente novamente.");
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> getPendingComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Comment> pendingComments = commentService.getPendingComments(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comments", pendingComments.getContent());
            response.put("totalElements", pendingComments.getTotalElements());
            response.put("totalPages", pendingComments.getTotalPages());
            response.put("currentPage", pendingComments.getNumber());
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao carregar comentários pendentes: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @PutMapping("/{commentId}/approve")
    public ResponseEntity<Map<String, Object>> approveComment(@PathVariable Long commentId) {
        try {
            Comment comment = commentService.approveComment(commentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("comment", comment);
            response.put("message", "Comentário aprovado com sucesso!");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao aprovar comentário: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Comentário removido com sucesso!");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao remover comentário: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCommentStats() {
        try {
            Long totalComments = commentService.getTotalApprovedComments();
            Long pendingComments = commentService.countPendingComments();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalComments", totalComments);
            response.put("pendingComments", pendingComments);
            response.put("success", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erro ao carregar estatísticas: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    // DTO for comment requests
    public static class CommentRequest {
        @NotNull(message = "ID do artigo é obrigatório")
        private Long articleId;
        
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
        private String authorName;
        
        @NotBlank(message = "Email é obrigatório")
        @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
        private String authorEmail;
        
        @Size(max = 200, message = "Website deve ter no máximo 200 caracteres")
        private String authorWebsite;
        
        @NotBlank(message = "Comentário é obrigatório")
        @Size(max = 2000, message = "Comentário deve ter no máximo 2000 caracteres")
        private String content;
        
        private Long parentId;
        
        // Getters and Setters
        public Long getArticleId() { return articleId; }
        public void setArticleId(Long articleId) { this.articleId = articleId; }
        
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        
        public String getAuthorEmail() { return authorEmail; }
        public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
        
        public String getAuthorWebsite() { return authorWebsite; }
        public void setAuthorWebsite(String authorWebsite) { this.authorWebsite = authorWebsite; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }
    }
}