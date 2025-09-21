package br.com.technews.service;

import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
import br.com.technews.repository.CategoryRepository;
import br.com.technews.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsScrapingService {

    private final NewsArticleRepository newsArticleRepository;
    private final CategoryRepository categoryRepository;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    @Transactional
    public List<NewsArticle> scrapeNews() {
        List<NewsArticle> allNews = new ArrayList<>();
        
        try {
            // Scraping do TechCrunch
            allNews.addAll(scrapeTechCrunch());
            
            // Scraping do Ars Technica
            allNews.addAll(scrapeArsTechnica());
            
            // Scraping do The Verge
            allNews.addAll(scrapeTheVerge());
            
            log.info("Total de {} notícias coletadas", allNews.size());
            
        } catch (Exception e) {
            log.error("Erro durante o scraping de notícias", e);
        }
        
        return allNews;
    }

    private List<NewsArticle> scrapeTechCrunch() {
        List<NewsArticle> articles = new ArrayList<>();
        
        try {
            log.info("Iniciando scraping do TechCrunch...");
            Document doc = Jsoup.connect("https://techcrunch.com/")
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            Elements articleElements = doc.select("article.post-block");
            
            for (Element element : articleElements.subList(0, Math.min(5, articleElements.size()))) {
                try {
                    String title = element.select("h2.post-block__title a").text();
                    String url = element.select("h2.post-block__title a").attr("href");
                    String summary = element.select(".post-block__content").text();
                    
                    if (!title.isEmpty() && !url.isEmpty()) {
                        // Verificar se já existe
                        if (!newsArticleRepository.existsByUrl(url)) {
                            NewsArticle article = createNewsArticle(title, summary, url, "TechCrunch", "Tecnologia");
                            articles.add(article);
                            newsArticleRepository.save(article);
                            log.info("Artigo salvo: {}", title);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Erro ao processar artigo do TechCrunch", e);
                }
            }
            
        } catch (IOException e) {
            log.error("Erro ao conectar com TechCrunch", e);
        }
        
        return articles;
    }

    private List<NewsArticle> scrapeArsTechnica() {
        List<NewsArticle> articles = new ArrayList<>();
        
        try {
            log.info("Iniciando scraping do Ars Technica...");
            Document doc = Jsoup.connect("https://arstechnica.com/")
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            Elements articleElements = doc.select("article");
            
            for (Element element : articleElements.subList(0, Math.min(5, articleElements.size()))) {
                try {
                    String title = element.select("h2 a, h1 a").text();
                    String url = element.select("h2 a, h1 a").attr("href");
                    String summary = element.select("p.excerpt").text();
                    
                    if (!title.isEmpty() && !url.isEmpty()) {
                        if (!url.startsWith("http")) {
                            url = "https://arstechnica.com" + url;
                        }
                        
                        if (!newsArticleRepository.existsByUrl(url)) {
                            NewsArticle article = createNewsArticle(title, summary, url, "Ars Technica", "Tecnologia");
                            articles.add(article);
                            newsArticleRepository.save(article);
                            log.info("Artigo salvo: {}", title);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Erro ao processar artigo do Ars Technica", e);
                }
            }
            
        } catch (IOException e) {
            log.error("Erro ao conectar com Ars Technica", e);
        }
        
        return articles;
    }

    private List<NewsArticle> scrapeTheVerge() {
        List<NewsArticle> articles = new ArrayList<>();
        
        try {
            log.info("Iniciando scraping do The Verge...");
            Document doc = Jsoup.connect("https://www.theverge.com/tech")
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            Elements articleElements = doc.select("article, .c-entry-box--compact");
            
            for (Element element : articleElements.subList(0, Math.min(5, articleElements.size()))) {
                try {
                    String title = element.select("h2 a, .c-entry-box--compact__title a").text();
                    String url = element.select("h2 a, .c-entry-box--compact__title a").attr("href");
                    String summary = element.select(".c-entry-summary p, .c-entry-box--compact__body").text();
                    
                    if (!title.isEmpty() && !url.isEmpty()) {
                        if (!url.startsWith("http")) {
                            url = "https://www.theverge.com" + url;
                        }
                        
                        if (!newsArticleRepository.existsByUrl(url)) {
                            NewsArticle article = createNewsArticle(title, summary, url, "The Verge", "Tecnologia");
                            articles.add(article);
                            newsArticleRepository.save(article);
                            log.info("Artigo salvo: {}", title);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Erro ao processar artigo do The Verge", e);
                }
            }
            
        } catch (IOException e) {
            log.error("Erro ao conectar com The Verge", e);
        }
        
        return articles;
    }

    private NewsArticle createNewsArticle(String title, String summary, String url, String source, String categoryName) {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setSummary(summary.length() > 500 ? summary.substring(0, 500) + "..." : summary);
        article.setUrl(url);
        article.setSource(source);
        article.setPublishedAt(LocalDateTime.now());
        article.setCreatedAt(LocalDateTime.now());
        
        // Buscar ou criar categoria
        Optional<Category> categoryOpt = categoryRepository.findByName(categoryName);
        if (categoryOpt.isPresent()) {
            article.setCategoryEntity(categoryOpt.get());
            article.setCategory(categoryOpt.get().getName());
        } else {
            Category category = new Category();
            category.setName(categoryName);
            category.setDescription("Categoria " + categoryName);
            category.setCreatedAt(LocalDateTime.now());
            categoryRepository.save(category);
            article.setCategoryEntity(category);
            article.setCategory(category.getName());
        }
        
        return article;
    }

    public long getArticleCount() {
        return newsArticleRepository.count();
    }

    public List<NewsArticle> getRecentArticles(int limit) {
        return newsArticleRepository.findTop10ByOrderByPublishedAtDesc()
                .stream()
                .limit(limit)
                .toList();
    }
}