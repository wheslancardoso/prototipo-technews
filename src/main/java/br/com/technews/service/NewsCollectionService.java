package br.com.technews.service;

import br.com.technews.entity.CollectedNews;
import br.com.technews.entity.NewsSource;
import br.com.technews.repository.CollectedNewsRepository;
import br.com.technews.repository.NewsSourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NewsCollectionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewsCollectionService.class);

    private final NewsSourceRepository newsSourceRepository;
    private final CollectedNewsRepository collectedNewsRepository;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public void collectNewsFromAllSources() {
        log.info("Iniciando coleta de notícias de todas as fontes ativas");
        
        List<NewsSource> activeSources = newsSourceRepository.findByActiveTrue();
        log.info("Encontradas {} fontes ativas para coleta", activeSources.size());

        for (NewsSource source : activeSources) {
            if (source.shouldFetch()) {
                try {
                    collectNewsFromSource(source);
                    updateSourceLastFetch(source);
                } catch (Exception e) {
                    log.error("Erro ao coletar notícias da fonte {}: {}", source.getName(), e.getMessage(), e);
                }
            } else {
                log.debug("Fonte {} não precisa ser coletada ainda", source.getName());
            }
        }
        
        log.info("Coleta de notícias finalizada");
    }

    /**
     * Variante para ação manual: ignora o intervalo configurado e força coleta
     * de todas as fontes ativas.
     */
    @Transactional
    public void collectNewsFromAllSourcesForced() {
        log.info("Coleta manual: forçando coleta de todas as fontes ativas");

        List<NewsSource> activeSources = newsSourceRepository.findByActiveTrue();
        log.info("Encontradas {} fontes ativas para coleta (forçada)", activeSources.size());

        for (NewsSource source : activeSources) {
            try {
                collectNewsFromSource(source);
                updateSourceLastFetch(source);
            } catch (Exception e) {
                log.error("Erro ao coletar notícias da fonte {} (forçada): {}", source.getName(), e.getMessage(), e);
            }
        }

        log.info("Coleta manual finalizada");
    }

    @Transactional
    public void collectNewsFromSource(NewsSource source) {
        log.info("Coletando notícias da fonte: {} ({})", source.getName(), source.getUrl());

        try {
            switch (source.getType()) {
                case RSS_FEED -> collectFromRssFeed(source);
                case API -> collectFromApi(source);
                case WEB_SCRAPING -> collectFromWebScraping(source);
                default -> log.warn("Tipo de fonte não suportado: {}", source.getType());
            }
        } catch (Exception e) {
            log.error("Erro ao coletar da fonte {}: {}", source.getName(), e.getMessage(), e);
        }
    }

    private void collectFromRssFeed(NewsSource source) {
        try {
            log.debug("Coletando RSS feed: {}", source.getUrl());
            
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new URL(source.getUrl())));
            
            List<SyndEntry> entries = feed.getEntries();
            int collected = 0;
            int maxArticles = source.getMaxArticlesPerFetch();
            
            for (SyndEntry entry : entries) {
                if (collected >= maxArticles) {
                    break;
                }
                
                try {
                    if (processRssEntry(entry, source)) {
                        collected++;
                    }
                } catch (Exception e) {
                    log.warn("Erro ao processar entrada RSS: {}", e.getMessage());
                }
            }
            
            log.info("Coletadas {} notícias da fonte RSS {}", collected, source.getName());
            
        } catch (Exception e) {
            log.error("Erro ao processar RSS feed {}: {}", source.getUrl(), e.getMessage(), e);
        }
    }

    private boolean processRssEntry(SyndEntry entry, NewsSource source) {
        try {
            String url = entry.getLink();
            String title = entry.getTitle();
            String content = entry.getDescription() != null ? entry.getDescription().getValue() : "";
            
            // Verificar se já existe
            if (collectedNewsRepository.findByOriginalUrl(url).isPresent()) {
                log.debug("Notícia já existe: {}", url);
                return false;
            }
            
            // Gerar hash do conteúdo para deduplicação
            String contentHash = generateContentHash(title + content);
            if (collectedNewsRepository.existsByContentHash(contentHash)) {
                log.debug("Conteúdo duplicado detectado: {}", title);
                return false;
            }
            
            // Converter data de publicação
            LocalDateTime publishedAt = null;
            if (entry.getPublishedDate() != null) {
                publishedAt = entry.getPublishedDate().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            }
            
            // Extrair imagem se disponível
            String imageUrl = extractImageFromContent(content);
            
            // Criar notícia coletada
            CollectedNews news = CollectedNews.builder()
                .title(cleanText(title))
                .content(cleanText(content))
                .originalUrl(url)
                .imageUrl(imageUrl)
                .publishedAt(publishedAt != null ? publishedAt : LocalDateTime.now())
                .source(source)
                .category(source.getCategory())
                .contentHash(contentHash)
                .qualityScore(calculateQualityScore(title, content))
                .build();
            
            collectedNewsRepository.save(news);
            log.debug("Nova notícia coletada: {}", title);
            return true;
            
        } catch (Exception e) {
            log.error("Erro ao processar entrada RSS: {}", e.getMessage(), e);
            return false;
        }
    }

    private void collectFromApi(NewsSource source) {
        // Implementação para APIs específicas (NewsAPI, etc.)
        log.info("Coleta via API ainda não implementada para: {}", source.getName());
    }

    private void collectFromWebScraping(NewsSource source) {
        try {
            log.debug("Fazendo web scraping: {}", source.getUrl());
            
            Document doc = Jsoup.connect(source.getUrl())
                .userAgent("Mozilla/5.0 (compatible; TechNews Bot)")
                .timeout(10000)
                .get();
            
            // Implementação básica - pode ser customizada por fonte
            // Por enquanto, apenas logamos que a funcionalidade existe
            log.info("Web scraping básico executado para: {}", source.getName());
            
        } catch (Exception e) {
            log.error("Erro no web scraping de {}: {}", source.getUrl(), e.getMessage(), e);
        }
    }

    private String generateContentHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            log.error("Erro ao gerar hash do conteúdo", e);
            return String.valueOf(content.hashCode());
        }
    }

    private String extractImageFromContent(String content) {
        try {
            if (content == null || content.isEmpty()) {
                return null;
            }
            
            Document doc = Jsoup.parse(content);
            return doc.select("img").first() != null ? 
                doc.select("img").first().attr("src") : null;
                
        } catch (Exception e) {
            log.debug("Erro ao extrair imagem do conteúdo", e);
            return null;
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        
        // Remove HTML tags e limpa o texto
        return Jsoup.parse(text).text().trim();
    }

    private Double calculateQualityScore(String title, String content) {
        double score = 5.0; // Score base
        
        if (title != null) {
            // Título com tamanho adequado
            if (title.length() >= 20 && title.length() <= 100) {
                score += 1.0;
            }
            
            // Título não é apenas maiúsculas
            if (!title.equals(title.toUpperCase())) {
                score += 0.5;
            }
        }
        
        if (content != null) {
            // Conteúdo com tamanho mínimo
            if (content.length() >= 100) {
                score += 1.0;
            }
            
            // Conteúdo substancial
            if (content.length() >= 500) {
                score += 1.5;
            }
        }
        
        return Math.min(score, 10.0); // Máximo 10
    }

    @Transactional
    private void updateSourceLastFetch(NewsSource source) {
        source.setLastFetchAt(LocalDateTime.now());
        newsSourceRepository.save(source);
    }

    public long getTotalCollectedNews() {
        return collectedNewsRepository.count();
    }

    public long getPendingNewsCount() {
        return collectedNewsRepository.countByStatus(CollectedNews.NewsStatus.PENDING);
    }

    public long getApprovedNewsCount() {
        return collectedNewsRepository.countByStatus(CollectedNews.NewsStatus.APPROVED);
    }
}