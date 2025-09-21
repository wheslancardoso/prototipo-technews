package com.technews.repository;

import com.technews.entity.Comment;
import com.technews.entity.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // Find approved comments for an article (excluding replies)
    @Query("SELECT c FROM Comment c WHERE c.article = :article AND c.approved = true AND c.active = true AND c.parent IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findApprovedCommentsByArticle(@Param("article") NewsArticle article);
    
    // Find approved replies for a comment
    @Query("SELECT c FROM Comment c WHERE c.parent = :parent AND c.approved = true AND c.active = true ORDER BY c.createdAt ASC")
    List<Comment> findApprovedRepliesByParent(@Param("parent") Comment parent);
    
    // Find all comments for an article (for admin)
    @Query("SELECT c FROM Comment c WHERE c.article = :article AND c.active = true ORDER BY c.createdAt DESC")
    List<Comment> findAllCommentsByArticle(@Param("article") NewsArticle article);
    
    // Find pending comments (for moderation)
    @Query("SELECT c FROM Comment c WHERE c.approved = false AND c.active = true ORDER BY c.createdAt DESC")
    Page<Comment> findPendingComments(Pageable pageable);
    
    // Count approved comments for an article
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.article = :article AND c.approved = true AND c.active = true")
    Long countApprovedCommentsByArticle(@Param("article") NewsArticle article);
    
    // Count pending comments
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.approved = false AND c.active = true")
    Long countPendingComments();
    
    // Find recent comments (for admin dashboard)
    @Query("SELECT c FROM Comment c WHERE c.active = true ORDER BY c.createdAt DESC")
    Page<Comment> findRecentComments(Pageable pageable);
    
    // Find comments by email (for spam detection)
    @Query("SELECT c FROM Comment c WHERE c.authorEmail = :email AND c.active = true ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByEmail(@Param("email") String email);
    
    // Find comments by IP address (for spam detection)
    @Query("SELECT c FROM Comment c WHERE c.ipAddress = :ipAddress AND c.active = true ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByIpAddress(@Param("ipAddress") String ipAddress);
    
    // Find comments created after a specific date
    @Query("SELECT c FROM Comment c WHERE c.createdAt >= :date AND c.active = true ORDER BY c.createdAt DESC")
    List<Comment> findCommentsAfterDate(@Param("date") LocalDateTime date);
    
    // Search comments by content
    @Query("SELECT c FROM Comment c WHERE LOWER(c.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) AND c.active = true ORDER BY c.createdAt DESC")
    Page<Comment> searchCommentsByContent(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    // Find comments by author name
    @Query("SELECT c FROM Comment c WHERE LOWER(c.authorName) LIKE LOWER(CONCAT('%', :authorName, '%')) AND c.active = true ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByAuthorName(@Param("authorName") String authorName);
    
    // Statistics queries
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.approved = true AND c.active = true")
    Long countTotalApprovedComments();
    
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt >= :startDate AND c.createdAt <= :endDate AND c.active = true")
    Long countCommentsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // Find top commenters
    @Query("SELECT c.authorName, c.authorEmail, COUNT(c) as commentCount FROM Comment c WHERE c.approved = true AND c.active = true GROUP BY c.authorName, c.authorEmail ORDER BY commentCount DESC")
    List<Object[]> findTopCommenters(Pageable pageable);
}