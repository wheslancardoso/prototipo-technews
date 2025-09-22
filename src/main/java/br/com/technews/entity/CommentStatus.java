package br.com.technews.entity;

/**
 * Enum para representar os diferentes status de um comentário
 */
public enum CommentStatus {
    PENDING,    // Comentário pendente de aprovação
    APPROVED,   // Comentário aprovado
    REJECTED,   // Comentário rejeitado
    SPAM        // Comentário marcado como spam
}