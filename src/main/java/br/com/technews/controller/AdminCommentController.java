package br.com.technews.controller;

import br.com.technews.entity.Comment;
import br.com.technews.entity.CommentStatus;
import br.com.technews.service.CommentService;
import br.com.technews.entity.NewsArticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/comments")
public class AdminCommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping
    public String commentsPage() {
        return "admin/comments";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<Page<CommentDTO>> getComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        // Parse sort parameter
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && "desc".equals(sortParts[1]) 
            ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        
        Page<Comment> comments;
        
        if (search != null && !search.trim().isEmpty()) {
            if (status != null && !status.trim().isEmpty()) {
                comments = commentService.searchCommentsWithStatus(search, 
                    CommentStatus.valueOf(status), pageable);
            } else {
                comments = commentService.searchComments(search, pageable);
            }
        } else if (status != null && !status.trim().isEmpty()) {
            comments = commentService.getCommentsByStatus(
                CommentStatus.valueOf(status), pageable);
        } else {
            comments = commentService.getAllComments(pageable);
        }

        Page<CommentDTO> commentDTOs = comments.map(this::convertToDTO);
        return ResponseEntity.ok(commentDTOs);
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Long>> getCommentStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", commentService.getTotalCommentsCount());
        stats.put("pending", commentService.getCommentCountByStatus(CommentStatus.PENDING));
        stats.put("approved", commentService.getCommentCountByStatus(CommentStatus.APPROVED));
        stats.put("rejected", commentService.getCommentCountByStatus(CommentStatus.REJECTED));
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<CommentDetailDTO> getCommentDetails(@PathVariable Long id) {
        Comment comment = commentService.getCommentById(id);
        if (comment == null) {
            return ResponseEntity.notFound().build();
        }
        
        CommentDetailDTO dto = convertToDetailDTO(comment);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/api/{id}/approve")
    @ResponseBody
    public ResponseEntity<String> approveComment(@PathVariable Long id) {
        try {
            commentService.approveComment(id);
            return ResponseEntity.ok("Comentário aprovado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao aprovar comentário: " + e.getMessage());
        }
    }

    @PostMapping("/api/{id}/reject")
    @ResponseBody
    public ResponseEntity<String> rejectComment(@PathVariable Long id) {
        try {
            commentService.rejectComment(id);
            return ResponseEntity.ok("Comentário rejeitado com sucesso");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao rejeitar comentário: " + e.getMessage());
        }
    }

    @PostMapping("/api/bulk-approve")
    @ResponseBody
    public ResponseEntity<String> bulkApproveComments(@RequestBody List<Long> commentIds) {
        try {
            int approved = 0;
            for (Long id : commentIds) {
                try {
                    commentService.approveComment(id);
                    approved++;
                } catch (Exception e) {
                    // Log error but continue with other comments
                    System.err.println("Erro ao aprovar comentário " + id + ": " + e.getMessage());
                }
            }
            return ResponseEntity.ok(approved + " comentário(s) aprovado(s) com sucesso");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro na aprovação em lote: " + e.getMessage());
        }
    }

    @PostMapping("/api/bulk-reject")
    @ResponseBody
    public ResponseEntity<String> bulkRejectComments(@RequestBody List<Long> commentIds) {
        try {
            int rejected = 0;
            for (Long id : commentIds) {
                try {
                    commentService.rejectComment(id);
                    rejected++;
                } catch (Exception e) {
                    // Log error but continue with other comments
                    System.err.println("Erro ao rejeitar comentário " + id + ": " + e.getMessage());
                }
            }
            return ResponseEntity.ok(rejected + " comentário(s) rejeitado(s) com sucesso");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro na rejeição em lote: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteComment(@PathVariable Long id) {
        try {
            commentService.deleteComment(id);
            return ResponseEntity.ok("Comentário excluído com sucesso");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao excluir comentário: " + e.getMessage());
        }
    }

    // DTO Classes
    public static class CommentDTO {
        private Long id;
        private String authorName;
        private String authorEmail;
        private String authorWebsite;
        private String content;
        private String status;
        private String createdAt;
        private String articleTitle;
        private Long articleId;
        private Long parentId;
        private boolean hasReplies;

        // Constructors
        public CommentDTO() {}

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }

        public String getAuthorEmail() { return authorEmail; }
        public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }

        public String getAuthorWebsite() { return authorWebsite; }
        public void setAuthorWebsite(String authorWebsite) { this.authorWebsite = authorWebsite; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getArticleTitle() { return articleTitle; }
        public void setArticleTitle(String articleTitle) { this.articleTitle = articleTitle; }

        public Long getArticleId() { return articleId; }
        public void setArticleId(Long articleId) { this.articleId = articleId; }

        public Long getParentId() { return parentId; }
        public void setParentId(Long parentId) { this.parentId = parentId; }

        public boolean isHasReplies() { return hasReplies; }
        public void setHasReplies(boolean hasReplies) { this.hasReplies = hasReplies; }
    }

    public static class CommentDetailDTO extends CommentDTO {
        private String articleUrl;
        private List<CommentDTO> replies;
        private CommentDTO parentComment;

        // Additional getters and setters
        public String getArticleUrl() { return articleUrl; }
        public void setArticleUrl(String articleUrl) { this.articleUrl = articleUrl; }

        public List<CommentDTO> getReplies() { return replies; }
        public void setReplies(List<CommentDTO> replies) { this.replies = replies; }

        public CommentDTO getParentComment() { return parentComment; }
        public void setParentComment(CommentDTO parentComment) { this.parentComment = parentComment; }
    }

    // Helper methods
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setAuthorName(comment.getAuthorName());
        dto.setAuthorEmail(comment.getAuthorEmail());
        dto.setAuthorWebsite(comment.getAuthorWebsite());
        dto.setContent(comment.getContent());
        dto.setStatus(comment.getStatus().name());
        dto.setCreatedAt(comment.getCreatedAt().toString());
        dto.setArticleTitle(comment.getArticle().getTitle());
        dto.setArticleId(comment.getArticle().getId());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setHasReplies(comment.getReplies() != null && !comment.getReplies().isEmpty());
        
        return dto;
    }

    private CommentDetailDTO convertToDetailDTO(Comment comment) {
        CommentDetailDTO dto = new CommentDetailDTO();
        dto.setId(comment.getId());
        dto.setAuthorName(comment.getAuthorName());
        dto.setAuthorEmail(comment.getAuthorEmail());
        dto.setAuthorWebsite(comment.getAuthorWebsite());
        dto.setContent(comment.getContent());
        dto.setStatus(comment.getStatus().name());
        dto.setCreatedAt(comment.getCreatedAt().toString());
        dto.setArticleTitle(comment.getArticle().getTitle());
        dto.setArticleId(comment.getArticle().getId());
        dto.setArticleUrl("/articles/" + comment.getArticle().getId());
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setHasReplies(comment.getReplies() != null && !comment.getReplies().isEmpty());
        
        // Set parent comment if exists
        if (comment.getParent() != null) {
            dto.setParentComment(convertToDTO(comment.getParent()));
        }
        
        // Set replies if exist
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            dto.setReplies(comment.getReplies().stream()
                .map(this::convertToDTO)
                .toList());
        }
        
        return dto;
    }
}