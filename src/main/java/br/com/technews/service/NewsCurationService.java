package br.com.technews.service;

import br.com.technews.entity.CollectedNews;
import br.com.technews.entity.NewsArticle;
import br.com.technews.service.NewsArticleService;
import br.com.technews.repository.CollectedNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsCurationService {

    private final CollectedNewsRepository collectedNewsRepository;
    private final NewsArticleService newsArticleService;

    // Palavras-chave que indicam conteúdo de qualidade em tecnologia
    private static final Set<String> TECH_KEYWORDS = Set.of(
        "inteligência artificial", "machine learning", "blockchain", "criptomoeda",
        "desenvolvimento", "programação", "software", "aplicativo", "startup",
        "inovação", "tecnologia", "digital", "dados", "segurança", "cloud",
        "api", "framework", "javascript", "python", "java", "react", "angular",
        "mobile", "web", "frontend", "backend", "devops", "kubernetes", "docker"
    );

    // Palavras que indicam conteúdo de baixa qualidade
    private static final Set<String> SPAM_KEYWORDS = Set.of(
        "clique aqui", "ganhe dinheiro", "oferta imperdível", "promoção",
        "desconto", "compre agora", "urgente", "último dia"
    );

    // Padrões de títulos de baixa qualidade
    private static final Pattern CLICKBAIT_PATTERN = Pattern.compile(
        "(?i).*(você não vai acreditar|incrível|surpreendente|chocante|" +
        "não vai acreditar|vai te surpreender|impressionante).*"
    );

    @Transactional
    public void processAllPendingNews() {
        log.info("Iniciando processamento de notícias pendentes");
        
        List<CollectedNews> pendingNews = collectedNewsRepository
            .findByStatus(CollectedNews.NewsStatus.PENDING);
        
        log.info("Encontradas {} notícias pendentes para processamento", pendingNews.size());
        
        int approved = 0;
        int rejected = 0;
        
        for (CollectedNews news : pendingNews) {
            try {
                if (shouldApproveNews(news)) {
                    approveNews(news);
                    approved++;
                } else {
                    rejectNews(news);
                    rejected++;
                }
            } catch (Exception e) {
                log.error("Erro ao processar notícia ID {}: {}", news.getId(), e.getMessage());
            }
        }
        
        log.info("Processamento concluído: {} aprovadas, {} rejeitadas", approved, rejected);
    }

    @Transactional
    public void approveNews(CollectedNews news) {
        news.setStatus(CollectedNews.NewsStatus.APPROVED);
        news.setProcessedAt(LocalDateTime.now());
        collectedNewsRepository.save(news);
        log.debug("Notícia aprovada: {}", news.getTitle());
    }

    @Transactional
    public void rejectNews(CollectedNews news) {
        news.setStatus(CollectedNews.NewsStatus.REJECTED);
        news.setProcessedAt(LocalDateTime.now());
        collectedNewsRepository.save(news);
        log.debug("Notícia rejeitada: {}", news.getTitle());
    }

    @Transactional
    public int publishApprovedCollectedNews(int limit) {
        List<CollectedNews> approved = collectedNewsRepository.findTopQualityNewsByStatusAndDate(
            CollectedNews.NewsStatus.APPROVED,
            LocalDateTime.now().minusDays(7),
            PageRequest.of(0, limit)
        );

        int publishedCount = 0;
        for (CollectedNews cn : approved) {
            try {
                // Pula se já existe artigo com a mesma URL
                if (newsArticleService.existsByUrl(cn.getOriginalUrl())) {
                    markCollectedAsPublished(cn);
                    continue;
                }

                NewsArticle article = new NewsArticle();
                // Conteúdo completo (TEXT)
                article.setContent(cn.getContent());
                // Título com limite 200
                article.setTitle(truncateWithEllipsis(cn.getTitle(), 200));
                // Usa conteúdo como resumo truncado se disponível
                String content = cn.getContent();
                if (content != null) {
                    // Resumo com limite 500
                    article.setSummary(truncateWithEllipsis(content, 500));
                }
                // URL e imagem com limites de coluna
                article.setUrl(truncatePlain(cn.getOriginalUrl(), 500));
                article.setImageUrl(truncatePlain(cn.getImageUrl(), 500));
                article.setSourceDomain(extractDomain(cn.getOriginalUrl()));
                if (cn.getCategory() != null) {
                    article.setCategoryEntity(cn.getCategory());
                    article.setCategory(truncatePlain(cn.getCategory().getName(), 50));
                }
                article.setCreatedAt(LocalDateTime.now());
                article.setPublishedAt(cn.getPublishedAt() != null ? cn.getPublishedAt() : LocalDateTime.now());

                // Cria e publica usando serviço existente
                NewsArticle saved = newsArticleService.create(article);
                newsArticleService.publish(saved.getId());

                // Marca coletada como publicada
                markCollectedAsPublished(cn);
                publishedCount++;
            } catch (Exception e) {
                log.error("Erro ao publicar notícia coletada ID {}: {}", cn.getId(), e.getMessage());
            }
        }

        log.info("Publicação de coletadas concluída: {} artigos publicados", publishedCount);
        return publishedCount;
    }

    // Utilidades de truncamento
    private String truncateWithEllipsis(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        int slice = Math.max(0, maxLen - 3);
        return s.substring(0, slice) + "...";
    }

    private String truncatePlain(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private void markCollectedAsPublished(CollectedNews cn) {
        cn.setStatus(CollectedNews.NewsStatus.PUBLISHED);
        cn.setProcessedAt(LocalDateTime.now());
        collectedNewsRepository.save(cn);
    }

    private String extractDomain(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host == null) return null;
            // Remove www.
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean shouldApproveNews(CollectedNews news) {
        double score = calculateDetailedQualityScore(news);
        
        // Atualiza o score na entidade
        news.setQualityScore(score);
        
        // Aprova se score >= 6.0
        return score >= 6.0;
    }

    private double calculateDetailedQualityScore(CollectedNews news) {
        double score = 5.0; // Score base
        
        String title = news.getTitle() != null ? news.getTitle().toLowerCase() : "";
        String content = news.getContent() != null ? news.getContent().toLowerCase() : "";
        String fullText = title + " " + content;
        
        // 1. Análise do título
        score += analyzeTitleQuality(news.getTitle());
        
        // 2. Análise do conteúdo
        score += analyzeContentQuality(news.getContent());
        
        // 3. Relevância tecnológica
        score += analyzeTechRelevance(fullText);
        
        // 4. Detecção de spam/clickbait
        score -= detectSpamContent(fullText);
        
        // 5. Recência da notícia
        score += analyzeRecency(news);
        
        // 6. Qualidade da fonte
        score += analyzeSourceQuality(news);
        
        return Math.max(0.0, Math.min(10.0, score));
    }

    private double analyzeTitleQuality(String title) {
        if (title == null || title.trim().isEmpty()) {
            return -2.0;
        }
        
        double score = 0.0;
        
        // Tamanho adequado do título
        int length = title.length();
        if (length >= 20 && length <= 100) {
            score += 1.0;
        } else if (length < 10 || length > 150) {
            score -= 1.0;
        }
        
        // Não é apenas maiúsculas
        if (!title.equals(title.toUpperCase())) {
            score += 0.5;
        } else {
            score -= 1.0;
        }
        
        // Detecção de clickbait
        if (CLICKBAIT_PATTERN.matcher(title).matches()) {
            score -= 2.0;
        }
        
        // Presença de números (pode indicar listas/tutoriais)
        if (title.matches(".*\\d+.*")) {
            score += 0.3;
        }
        
        return score;
    }

    private double analyzeContentQuality(String content) {
        if (content == null || content.trim().isEmpty()) {
            return -1.0;
        }
        
        double score = 0.0;
        int length = content.length();
        
        // Conteúdo com tamanho adequado
        if (length >= 200) {
            score += 1.0;
        }
        if (length >= 500) {
            score += 1.0;
        }
        if (length >= 1000) {
            score += 0.5;
        }
        
        // Muito curto é ruim
        if (length < 50) {
            score -= 2.0;
        }
        
        // Contagem de frases (indica estrutura)
        long sentences = content.chars().filter(ch -> ch == '.' || ch == '!' || ch == '?').count();
        if (sentences >= 3) {
            score += 0.5;
        }
        
        return score;
    }

    private double analyzeTechRelevance(String text) {
        double score = 0.0;
        
        // Conta palavras-chave tecnológicas
        long techKeywordCount = TECH_KEYWORDS.stream()
            .mapToLong(keyword -> countOccurrences(text, keyword))
            .sum();
        
        if (techKeywordCount >= 3) {
            score += 2.0;
        } else if (techKeywordCount >= 1) {
            score += 1.0;
        } else {
            score -= 1.0; // Penaliza se não tem relevância tech
        }
        
        return score;
    }

    private double detectSpamContent(String text) {
        double penalty = 0.0;
        
        // Detecta palavras de spam
        long spamCount = SPAM_KEYWORDS.stream()
            .mapToLong(keyword -> countOccurrences(text, keyword))
            .sum();
        
        penalty += spamCount * 1.5;
        
        // Excesso de pontuação
        long exclamations = text.chars().filter(ch -> ch == '!').count();
        if (exclamations > 3) {
            penalty += 1.0;
        }
        
        // Excesso de maiúsculas
        long upperCaseCount = text.chars().filter(Character::isUpperCase).count();
        double upperCaseRatio = (double) upperCaseCount / text.length();
        if (upperCaseRatio > 0.3) {
            penalty += 2.0;
        }
        
        return penalty;
    }

    private double analyzeRecency(CollectedNews news) {
        if (news.getPublishedAt() == null) {
            return 0.0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime publishedAt = news.getPublishedAt();
        
        // Notícias mais recentes têm score maior
        if (publishedAt.isAfter(now.minusHours(24))) {
            return 1.0; // Últimas 24h
        } else if (publishedAt.isAfter(now.minusDays(3))) {
            return 0.5; // Últimos 3 dias
        } else if (publishedAt.isAfter(now.minusDays(7))) {
            return 0.2; // Última semana
        } else if (publishedAt.isBefore(now.minusDays(30))) {
            return -1.0; // Muito antiga
        }
        
        return 0.0;
    }

    private double analyzeSourceQuality(CollectedNews news) {
        // Por enquanto, score neutro
        // Pode ser expandido para avaliar a reputação da fonte
        return 0.0;
    }

    private long countOccurrences(String text, String keyword) {
        if (text == null || keyword == null) {
            return 0;
        }
        
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        int count = 0;
        int index = 0;
        
        while ((index = lowerText.indexOf(lowerKeyword, index)) != -1) {
            count++;
            index += lowerKeyword.length();
        }
        
        return count;
    }

    public List<CollectedNews> getTopQualityNews(int limit) {
        return collectedNewsRepository.findTopQualityNewsByStatusAndDate(
            CollectedNews.NewsStatus.APPROVED,
            LocalDateTime.now().minusDays(7),
            PageRequest.of(0, limit)
        );
    }

    public List<CollectedNews> getRecentApprovedNews(int days) {
        return collectedNewsRepository.findRecentApprovedNews(
            LocalDateTime.now().minusDays(days)
        );
    }

    @Transactional
    public void cleanupOldNews() {
        log.info("Iniciando limpeza de notícias antigas");
        
        // Remove notícias rejeitadas com mais de 30 dias
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        
        List<CollectedNews> oldRejectedNews = collectedNewsRepository
            .findByStatus(CollectedNews.NewsStatus.REJECTED)
            .stream()
            .filter(news -> news.getCreatedAt().isBefore(cutoffDate))
            .toList();
        
        if (!oldRejectedNews.isEmpty()) {
            collectedNewsRepository.deleteAll(oldRejectedNews);
            log.info("Removidas {} notícias rejeitadas antigas", oldRejectedNews.size());
        }
        
        log.info("Limpeza de notícias antigas concluída");
    }
}