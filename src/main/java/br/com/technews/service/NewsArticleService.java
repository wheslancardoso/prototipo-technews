package br.com.technews.service;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.repository.NewsArticleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NewsArticleService {

    @Autowired
    private NewsArticleRepository newsArticleRepository;

    public List<NewsArticle> findAll() {
        return newsArticleRepository.findAll();
    }

    public Page<NewsArticle> findAll(Pageable pageable) {
        return newsArticleRepository.findAll(pageable);
    }

    public Optional<NewsArticle> findById(Long id) {
        return newsArticleRepository.findById(id);
    }

    public List<NewsArticle> findPublishedArticles() {
        return newsArticleRepository.findByPublishedTrueOrderByPublishedAtDesc();
    }

    public Page<NewsArticle> findPublishedArticles(Pageable pageable) {
        return newsArticleRepository.findByPublishedTrueOrderByPublishedAtDesc(pageable);
    }

    public List<NewsArticle> findByCategory(String category) {
        return newsArticleRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    public List<NewsArticle> findByStatus(ArticleStatus status) {
        return newsArticleRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    public NewsArticle save(NewsArticle article) {
        return newsArticleRepository.save(article);
    }

    public NewsArticle create(NewsArticle article) {
        article.setId(null); // Garantir que é um novo artigo
        article.setStatus(ArticleStatus.PENDENTE_REVISAO);
        article.setPublished(false);
        return newsArticleRepository.save(article);
    }

    public NewsArticle update(Long id, NewsArticle articleDetails) {
        Optional<NewsArticle> optionalArticle = newsArticleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            NewsArticle article = optionalArticle.get();
            article.setTitle(articleDetails.getTitle());
            article.setContent(articleDetails.getContent());
            article.setSummary(articleDetails.getSummary());
            article.setAuthor(articleDetails.getAuthor());
            article.setCategory(articleDetails.getCategory());
            article.setUrl(articleDetails.getUrl());
            article.setImageUrl(articleDetails.getImageUrl());
            article.setSourceDomain(articleDetails.getSourceDomain());
            return newsArticleRepository.save(article);
        }
        throw new RuntimeException("Artigo não encontrado com ID: " + id);
    }

    public NewsArticle publish(Long id) {
        Optional<NewsArticle> optionalArticle = newsArticleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            NewsArticle article = optionalArticle.get();
            article.setPublished(true);
            article.setPublishedAt(LocalDateTime.now());
            article.setStatus(ArticleStatus.PUBLICADO);
            return newsArticleRepository.save(article);
        }
        throw new RuntimeException("Artigo não encontrado com ID: " + id);
    }

    public NewsArticle unpublish(Long id) {
        Optional<NewsArticle> optionalArticle = newsArticleRepository.findById(id);
        if (optionalArticle.isPresent()) {
            NewsArticle article = optionalArticle.get();
            article.setPublished(false);
            article.setStatus(ArticleStatus.PENDENTE_REVISAO);
            return newsArticleRepository.save(article);
        }
        throw new RuntimeException("Artigo não encontrado com ID: " + id);
    }

    public void deleteById(Long id) {
        if (newsArticleRepository.existsById(id)) {
            newsArticleRepository.deleteById(id);
        } else {
            throw new RuntimeException("Artigo não encontrado com ID: " + id);
        }
    }

    public long countAll() {
        return newsArticleRepository.count();
    }

    public long countPublished() {
        return newsArticleRepository.countByPublishedTrue();
    }

    public long countByStatus(ArticleStatus status) {
        return newsArticleRepository.countByStatus(status);
    }

    // Métodos adicionais para integração com GNews API

    /**
     * Verifica se já existe um artigo com a URL especificada
     */
    public boolean existsByUrl(String url) {
        return newsArticleRepository.existsByUrl(url);
    }

    /**
     * Remove artigos não publicados mais antigos que o número de dias especificado
     */
    public int deleteOldUnpublishedArticles(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        return newsArticleRepository.deleteByPublishedFalseAndCreatedAtBefore(cutoffDate);
    }

    /**
     * Busca artigos por fonte
     */
    public List<NewsArticle> findBySource(String source) {
        return newsArticleRepository.findBySourceDomain(source);
    }

    /**
     * Busca artigos criados após uma data específica
     */
    public List<NewsArticle> findByCreatedAtAfter(LocalDateTime date) {
        return newsArticleRepository.findByCreatedAtAfter(date);
    }

    /**
     * Busca artigos por título contendo texto (case insensitive)
     */
    public List<NewsArticle> findByTitleContaining(String title) {
        return newsArticleRepository.findByTitleContainingIgnoreCase(title);
    }

    /**
     * Busca artigos recentes (limitado)
     */
    public List<NewsArticle> findRecentArticles(int limit) {
        return newsArticleRepository.findTop10ByOrderByPublishedAtDesc()
                .stream()
                .limit(limit)
                .toList();
    }

    public Page<NewsArticle> searchPublishedArticles(String searchTerm, Pageable pageable) {
        return newsArticleRepository.findByPublishedTrueAndTitleContainingIgnoreCaseOrContentContainingIgnoreCaseOrderByPublishedAtDesc(
            searchTerm, pageable);
    }

    public Page<NewsArticle> findPublishedArticlesByCategory(String category, Pageable pageable) {
        return newsArticleRepository.findByPublishedTrueAndCategoryOrderByPublishedAtDesc(category, pageable);
    }
}