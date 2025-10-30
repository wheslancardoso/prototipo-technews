package br.com.technews.service;

import br.com.technews.entity.Article;
import br.com.technews.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;

    public Article create(Article article) {
        validate(article);
        if (!StringUtils.hasText(article.getSlug())) {
            article.setSlug(generateSlug(article.getTitle()));
        }
        if (articleRepository.existsBySlug(article.getSlug())) {
            throw new IllegalArgumentException("Slug já existe");
        }
        article.setCreatedAt(LocalDateTime.now());
        return articleRepository.save(article);
    }

    public Article update(Long id, Article updates) {
        Article existing = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artigo não encontrado"));

        if (StringUtils.hasText(updates.getTitle())) existing.setTitle(updates.getTitle());
        if (StringUtils.hasText(updates.getSummary())) existing.setSummary(updates.getSummary());
        if (StringUtils.hasText(updates.getContent())) existing.setContent(updates.getContent());
        if (StringUtils.hasText(updates.getSourceUrl())) existing.setSourceUrl(updates.getSourceUrl());
        if (StringUtils.hasText(updates.getCategory())) existing.setCategory(updates.getCategory());
        if (StringUtils.hasText(updates.getAuthor())) existing.setAuthor(updates.getAuthor());

        if (StringUtils.hasText(updates.getSlug()) && !updates.getSlug().equals(existing.getSlug())) {
            if (articleRepository.existsBySlug(updates.getSlug())) {
                throw new IllegalArgumentException("Slug já existe");
            }
            existing.setSlug(updates.getSlug());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return articleRepository.save(existing);
    }

    public void delete(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new IllegalArgumentException("Artigo não encontrado");
        }
        articleRepository.deleteById(id);
    }

    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    public Optional<Article> findBySlug(String slug) {
        return articleRepository.findBySlug(slug);
    }

    public Page<Article> list(Pageable pageable) {
        return articleRepository.findAll(pageable);
    }

    public Article publish(Long id, boolean publish) {
        Article existing = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Artigo não encontrado"));
        existing.setPublished(publish);
        existing.setPublishedAt(publish ? LocalDateTime.now() : null);
        existing.setUpdatedAt(LocalDateTime.now());
        return articleRepository.save(existing);
    }

    private void validate(Article article) {
        if (!StringUtils.hasText(article.getTitle())) {
            throw new IllegalArgumentException("Título é obrigatório");
        }
        if (!StringUtils.hasText(article.getContent())) {
            throw new IllegalArgumentException("Conteúdo é obrigatório");
        }
    }

    private String generateSlug(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\u00E0-\u00FC\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
    }
}