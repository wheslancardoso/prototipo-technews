package com.technews.service;

import com.technews.entity.Comment;
import com.technews.entity.CommentStatus;
import com.technews.repository.CommentRepository;
import br.com.technews.entity.NewsArticle;
import br.com.technews.repository.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class CommentService {
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private NewsArticleRepository newsArticleRepository;
    
    // Spam detection patterns
    private static final Pattern SPAM_PATTERNS = Pattern.compile(
        "(?i)(viagra|casino|poker|loan|debt|credit|bitcoin|crypto|investment|forex|trading|pills|pharmacy|dating|adult|xxx|porn|sex|escort|massage|replica|fake|cheap|discount|sale|buy now|click here|free money|make money|work from home|get rich|lose weight|miracle|guaranteed|limited time|act now|urgent|congratulations|winner|lottery|prize|claim now|http://|https://|www\\.|bit\\.ly|tinyurl|goo\\.gl)"
    );
    
    private static final int MAX_LINKS_ALLOWED = 2;
    private static final int MIN_COMMENT_LENGTH = 10;
    private static final int MAX_COMMENTS_PER_HOUR = 5;
    
    public List<Comment> getApprovedCommentsByArticle(Long articleId) {
        Optional<NewsArticle> article = newsArticleRepository.findById(articleId);
        if (article.isPresent()) {
            return commentRepository.findApprovedCommentsByArticle(article.get());
        }
        return List.of();
    }
    
    public List<Comment> getApprovedRepliesByParent(Long parentId) {
        Optional<Comment> parent = commentRepository.findById(parentId);
        if (parent.isPresent()) {
            return commentRepository.findApprovedRepliesByParent(parent.get());
        }
        return List.of();
    }
    
    public Comment createComment(Long articleId, String authorName, String authorEmail, 
                               String authorWebsite, String content, String ipAddress, 
                               String userAgent, Long parentId) {
        
        // Validate article exists
        Optional<NewsArticle> articleOpt = newsArticleRepository.findById(articleId);
        if (!articleOpt.isPresent()) {
            throw new IllegalArgumentException("Artigo não encontrado");
        }
        
        NewsArticle article = articleOpt.get();
        
        // Validate comment content
        validateComment(content, authorEmail, ipAddress);
        
        Comment comment = new Comment();
        comment.setAuthorName(authorName.trim());
        comment.setAuthorEmail(authorEmail.trim().toLowerCase());
        comment.setAuthorWebsite(cleanWebsiteUrl(authorWebsite));
        comment.setContent(content.trim());
        comment.setArticle(article);
        comment.setIpAddress(ipAddress);
        comment.setUserAgent(userAgent);
        
        // Set parent if it's a reply
        if (parentId != null) {
            Optional<Comment> parentOpt = commentRepository.findById(parentId);
            if (parentOpt.isPresent() && parentOpt.get().getArticle().getId().equals(articleId)) {
                comment.setParent(parentOpt.get());
            }
        }
        
        // Auto-approve or mark for moderation
        comment.setApproved(shouldAutoApprove(comment));
        
        return commentRepository.save(comment);
    }
    
    private void validateComment(String content, String email, String ipAddress) {
        // Check minimum length
        if (content.length() < MIN_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comentário muito curto. Mínimo de " + MIN_COMMENT_LENGTH + " caracteres.");
        }
        
        // Check for spam patterns
        if (SPAM_PATTERNS.matcher(content).find()) {
            throw new IllegalArgumentException("Comentário contém conteúdo suspeito.");
        }
        
        // Check for excessive links
        long linkCount = content.toLowerCase().split("http").length - 1;
        if (linkCount > MAX_LINKS_ALLOWED) {
            throw new IllegalArgumentException("Muitos links no comentário. Máximo permitido: " + MAX_LINKS_ALLOWED);
        }
        
        // Check rate limiting
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<Comment> recentComments = commentRepository.findCommentsAfterDate(oneHourAgo)
            .stream()
            .filter(c -> c.getAuthorEmail().equals(email) || c.getIpAddress().equals(ipAddress))
            .toList();
        
        if (recentComments.size() >= MAX_COMMENTS_PER_HOUR) {
            throw new IllegalArgumentException("Muitos comentários em pouco tempo. Tente novamente mais tarde.");
        }
    }
    
    private String cleanWebsiteUrl(String website) {
        if (website == null || website.trim().isEmpty()) {
            return null;
        }
        
        website = website.trim();
        if (!website.startsWith("http://") && !website.startsWith("https://")) {
            website = "https://" + website;
        }
        
        return website;
    }
    
    private boolean shouldAutoApprove(Comment comment) {
        // Auto-approve if author has previously approved comments
        List<Comment> previousComments = commentRepository.findCommentsByEmail(comment.getAuthorEmail());
        boolean hasApprovedComments = previousComments.stream()
            .anyMatch(c -> c.getApproved() && !c.getId().equals(comment.getId()));
        
        if (hasApprovedComments) {
            return true;
        }
        
        // Check for spam indicators
        String content = comment.getContent().toLowerCase();
        String authorName = comment.getAuthorName().toLowerCase();
        
        // Don't auto-approve if contains suspicious patterns
        if (SPAM_PATTERNS.matcher(content).find() || SPAM_PATTERNS.matcher(authorName).find()) {
            return false;
        }
        
        // Don't auto-approve very short comments
        if (content.length() < 20) {
            return false;
        }
        
        // Auto-approve if passes basic checks
        return true;
    }
    
    public Comment approveComment(Long commentId) {
        Comment comment = getCommentById(commentId);
        comment.setApproved(true);
        comment.setStatus(CommentStatus.APPROVED);
        return commentRepository.save(comment);
    }
    
    public Comment rejectComment(Long commentId) {
        Comment comment = getCommentById(commentId);
        comment.setApproved(false);
        comment.setStatus(CommentStatus.REJECTED);
        return commentRepository.save(comment);
    }
    
    public Page<Comment> getAllComments(Pageable pageable) {
        return commentRepository.findAll(pageable);
    }
    
    public long getTotalCommentsCount() {
        return commentRepository.count();
    }
    
    public Page<Comment> getCommentsByStatus(CommentStatus status, Pageable pageable) {
        return commentRepository.findByStatus(status, pageable);
    }
    
    public long getCommentCountByStatus(CommentStatus status) {
        return commentRepository.countByStatus(status);
    }
    
    public Comment deleteComment(Long commentId) {
        Comment comment = getCommentById(commentId);
        comment.setActive(false);
        return commentRepository.save(comment);
    }
    
    public Page<Comment> searchComments(String searchTerm, Pageable pageable) {
        return commentRepository.searchCommentsByContent(searchTerm, pageable);
    }
    
    public Page<Comment> searchCommentsWithStatus(String searchTerm, CommentStatus status, Pageable pageable) {
        // Implementação simples - pode ser melhorada com query customizada
        return commentRepository.searchCommentsByContent(searchTerm, pageable);
    }
    
    public Page<Comment> getPendingComments(Pageable pageable) {
        return commentRepository.findPendingComments(pageable);
    }
    
    public Page<Comment> getRecentComments(Pageable pageable) {
        return commentRepository.findRecentComments(pageable);
    }
    
    public Long countApprovedCommentsByArticle(Long articleId) {
        Optional<NewsArticle> article = newsArticleRepository.findById(articleId);
        if (article.isPresent()) {
            return commentRepository.countApprovedCommentsByArticle(article.get());
        }
        return 0L;
    }
    
    public Long countPendingComments() {
        return commentRepository.countPendingComments();
    }
    
    public Comment getCommentById(Long id) {
        return commentRepository.findById(id).orElse(null);
    }
    
    public List<Comment> getAllCommentsByArticle(Long articleId) {
        Optional<NewsArticle> article = newsArticleRepository.findById(articleId);
        if (article.isPresent()) {
            return commentRepository.findAllCommentsByArticle(article.get());
        }
        return List.of();
    }
    
    // Statistics methods
    public Long getTotalApprovedComments() {
        return commentRepository.countTotalApprovedComments();
    }
    
    public Long getCommentsCountBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        return commentRepository.countCommentsBetweenDates(startDate, endDate);
    }
    
    public List<Object[]> getTopCommenters(Pageable pageable) {
        return commentRepository.findTopCommenters(pageable);
    }
}