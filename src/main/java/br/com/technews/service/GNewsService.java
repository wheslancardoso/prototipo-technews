package br.com.technews.service;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Category;
import br.com.technews.entity.TrustedSource;
import br.com.technews.repository.CategoryRepository;
import br.com.technews.repository.TrustedSourceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GNewsService {

    private static final Logger logger = LoggerFactory.getLogger(GNewsService.class);

    @Value("${gnews.api.key}")
    private String apiKey;

    @Value("${gnews.api.base-url}")
    private String baseUrl;

    @Value("${gnews.api.max-articles}")
    private int maxArticles;

    @Value("${gnews.api.language}")
    private String language;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;
    private final TrustedSourceRepository trustedSourceRepository;

    public GNewsService(CategoryRepository categoryRepository, 
                       TrustedSourceRepository trustedSourceRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.categoryRepository = categoryRepository;
        this.trustedSourceRepository = trustedSourceRepository;
    }

    /**
     * Busca notícias por palavra-chave usando a API GNews
     */
    public List<NewsArticle> searchNews(String query) {
        try {
            String url = String.format("%s/search?q=%s&lang=%s&max=%d&apikey=%s",
                    baseUrl, query, language, maxArticles, apiKey);

            logger.info("Buscando notícias na GNews API: {}", query);
            
            String response = restTemplate.getForObject(url, String.class);
            return parseNewsResponse(response);
            
        } catch (RestClientException e) {
            logger.error("Erro ao buscar notícias na API GNews: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Busca top headlines por categoria
     */
    public List<NewsArticle> getTopHeadlines(String category) {
        try {
            String url = String.format("%s/top-headlines?category=%s&lang=%s&max=%d&apikey=%s",
                    baseUrl, category, language, maxArticles, apiKey);

            logger.info("Buscando top headlines da categoria: {}", category);
            
            String response = restTemplate.getForObject(url, String.class);
            return parseNewsResponse(response);
            
        } catch (RestClientException e) {
            logger.error("Erro ao buscar top headlines na API GNews: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Busca notícias de tecnologia (categoria padrão)
     */
    public List<NewsArticle> getTechNews() {
        return getTopHeadlines("technology");
    }

    /**
     * Busca notícias usando múltiplas palavras-chave relacionadas à tecnologia
     */
    public List<NewsArticle> getTechNewsWithKeywords() {
        List<String> techKeywords = List.of(
            "technology", "software", "programming", "artificial intelligence",
            "machine learning", "cybersecurity", "blockchain", "cloud computing"
        );

        List<NewsArticle> allNews = new ArrayList<>();
        
        for (String keyword : techKeywords) {
            List<NewsArticle> keywordNews = searchNews(keyword);
            allNews.addAll(keywordNews);
            
            // Pequena pausa para evitar rate limiting
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return removeDuplicates(allNews);
    }

    /**
     * Filtra notícias por fontes confiáveis
     */
    public List<NewsArticle> filterByTrustedSources(List<NewsArticle> articles) {
        List<TrustedSource> trustedSources = trustedSourceRepository.findByActiveTrue();
        
        if (trustedSources.isEmpty()) {
            logger.warn("Nenhuma fonte confiável configurada. Retornando todas as notícias.");
            return articles;
        }

        List<String> trustedDomains = trustedSources.stream()
                .map(TrustedSource::getDomainName)
                .toList();

        return articles.stream()
                .filter(article -> {
                    String url = article.getUrl();
                    if (url == null) return false;
                    
                    return trustedDomains.stream()
                            .anyMatch(domain -> url.contains(domain));
                })
                .toList();
    }

    /**
     * Parse da resposta JSON da API GNews
     */
    private List<NewsArticle> parseNewsResponse(String jsonResponse) {
        List<NewsArticle> articles = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode articlesNode = root.get("articles");
            
            if (articlesNode != null && articlesNode.isArray()) {
                for (JsonNode articleNode : articlesNode) {
                    NewsArticle article = parseArticle(articleNode);
                    if (article != null) {
                        articles.add(article);
                    }
                }
            }
            
            logger.info("Parsed {} articles from GNews response", articles.size());
            
        } catch (Exception e) {
            logger.error("Erro ao fazer parse da resposta da API GNews: {}", e.getMessage());
        }
        
        return articles;
    }

    /**
     * Parse de um artigo individual
     */
    private NewsArticle parseArticle(JsonNode articleNode) {
        try {
            NewsArticle article = new NewsArticle();
            
            article.setTitle(getTextValue(articleNode, "title"));
            article.setContent(getTextValue(articleNode, "description"));
            article.setUrl(getTextValue(articleNode, "url"));
            article.setImageUrl(getTextValue(articleNode, "image"));
            
            // Parse da data de publicação
            String publishedAt = getTextValue(articleNode, "publishedAt");
            if (publishedAt != null) {
                try {
                    LocalDateTime publishDate = LocalDateTime.parse(publishedAt, 
                            DateTimeFormatter.ISO_DATE_TIME);
                    article.setPublishedAt(publishDate);
                } catch (Exception e) {
                    logger.warn("Erro ao fazer parse da data: {}", publishedAt);
                    article.setPublishedAt(LocalDateTime.now());
                }
            } else {
                article.setPublishedAt(LocalDateTime.now());
            }

            // Parse da fonte
            JsonNode sourceNode = articleNode.get("source");
            if (sourceNode != null) {
                String sourceName = getTextValue(sourceNode, "name");
                article.setSourceDomain(sourceName);
            }

            // Define categoria padrão
            Optional<Category> techCategory = categoryRepository.findByNameIgnoreCase("Technology");
            if (techCategory.isPresent()) {
                article.setCategoryEntity(techCategory.get());
                article.setCategory(techCategory.get().getName());
            } else {
                article.setCategory("Technology");
            }

            // Definir como não publicado inicialmente (precisa de aprovação)
            article.setPublished(false);
            article.setCreatedAt(LocalDateTime.now());
            article.setUpdatedAt(LocalDateTime.now());

            return article;
            
        } catch (Exception e) {
            logger.error("Erro ao fazer parse do artigo: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Utilitário para extrair valores de texto do JSON
     */
    private String getTextValue(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return (fieldNode != null && !fieldNode.isNull()) ? fieldNode.asText() : null;
    }

    /**
     * Remove artigos duplicados baseado na URL
     */
    private List<NewsArticle> removeDuplicates(List<NewsArticle> articles) {
        return articles.stream()
                .filter(article -> article.getUrl() != null)
                .collect(java.util.stream.Collectors.toMap(
                    NewsArticle::getUrl,
                    article -> article,
                    (existing, replacement) -> existing
                ))
                .values()
                .stream()
                .toList();
    }
}