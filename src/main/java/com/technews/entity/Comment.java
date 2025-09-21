package com.technews.entity;

import br.com.technews.entity.NewsArticle;
import com.technews.entity.CommentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Column(nullable = false, length = 100)
    private String authorName;
    
    @NotBlank(message = "Email é obrigatório")
    @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
    @Column(nullable = false, length = 150)
    private String authorEmail;
    
    @Size(max = 200, message = "Website deve ter no máximo 200 caracteres")
    @Column(length = 200)
    private String authorWebsite;
    
    @NotBlank(message = "Comentário é obrigatório")
    @Size(max = 2000, message = "Comentário deve ter no máximo 2000 caracteres")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private NewsArticle article;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> replies = new ArrayList<>();
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private Boolean approved = false;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommentStatus status = CommentStatus.PENDING;
    
    @Column(length = 45)
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    // Constructors
    public Comment() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Comment(String authorName, String authorEmail, String content, NewsArticle article) {
        this();
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.content = content;
        this.article = article;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getAuthorEmail() {
        return authorEmail;
    }
    
    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }
    
    public String getAuthorWebsite() {
        return authorWebsite;
    }
    
    public void setAuthorWebsite(String authorWebsite) {
        this.authorWebsite = authorWebsite;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public NewsArticle getArticle() {
        return article;
    }
    
    public void setArticle(NewsArticle article) {
        this.article = article;
    }
    
    public Comment getParent() {
        return parent;
    }
    
    public void setParent(Comment parent) {
        this.parent = parent;
    }
    
    public List<Comment> getReplies() {
        return replies;
    }
    
    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }
    
    public Comment getParentComment() {
        return parent;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getApproved() {
        return approved;
    }
    
    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public CommentStatus getStatus() {
        return status;
    }
    
    public void setStatus(CommentStatus status) {
        this.status = status;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    // Helper methods
    public boolean isReply() {
        return parent != null;
    }
    
    public String getGravatarUrl() {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(authorEmail.toLowerCase().getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "https://www.gravatar.com/avatar/" + hexString.toString() + "?d=identicon&s=50";
        } catch (java.security.NoSuchAlgorithmException e) {
            return "https://www.gravatar.com/avatar/default?d=identicon&s=50";
        }
    }
    
    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", authorName='" + authorName + '\'' +
                ", content='" + content.substring(0, Math.min(content.length(), 50)) + "...'" +
                ", createdAt=" + createdAt +
                ", approved=" + approved +
                '}';
    }
}