package br.com.technews.scheduler;

import br.com.technews.entity.NewsArticle;
import br.com.technews.service.GNewsService;
import br.com.technews.service.NewsArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NewsScheduler.class);

    private final GNewsService gNewsService;
    private final NewsArticleService newsArticleService;

    public NewsScheduler(GNewsService gNewsService, NewsArticleService newsArticleService) {
        this.gNewsService = gNewsService;
        this.newsArticleService = newsArticleService;
    }

    @Scheduled(cron = "0 0 */2 * * *")
    public void fetchTechNews() {
        logger.info("Iniciando busca automatica de noticias de tecnologia...");
        
        try {
            List<NewsArticle> techHeadlines = gNewsService.getTechNews();
            List<NewsArticle> keywordNews = gNewsService.getTechNewsWithKeywords();
            
            List<NewsArticle> allNews = combineAndDeduplicate(techHeadlines, keywordNews);
            List<NewsArticle> filteredNews = gNewsService.filterByTrustedSources(allNews);
            
            int savedCount = 0;
            for (NewsArticle article : filteredNews) {
                if (!newsArticleService.existsByUrl(article.getUrl())) {
                    newsArticleService.save(article);
                    savedCount++;
                }
            }
            
            logger.info("Busca automatica concluida. {} novas noticias salvas de {} encontradas.", 
                       savedCount, filteredNews.size());
            
        } catch (Exception e) {
            logger.error("Erro durante a busca automatica de noticias: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 30 */4 * * *")
    public void fetchSpecificKeywordNews() {
        logger.info("Iniciando busca de noticias por palavras-chave especificas...");
        
        List<String> specificKeywords = List.of(
            "Java Spring Boot",
            "React JavaScript",
            "Python Django",
            "Docker Kubernetes",
            "AWS Cloud",
            "DevOps CI/CD"
        );
        
        try {
            int totalSaved = 0;
            
            for (String keyword : specificKeywords) {
                List<NewsArticle> keywordArticles = gNewsService.searchNews(keyword);
                List<NewsArticle> filteredArticles = gNewsService.filterByTrustedSources(keywordArticles);
                
                for (NewsArticle article : filteredArticles) {
                    if (!newsArticleService.existsByUrl(article.getUrl())) {
                        newsArticleService.save(article);
                        totalSaved++;
                    }
                }
                
                Thread.sleep(200);
            }
            
            logger.info("Busca por palavras-chave especificas concluida. {} novas noticias salvas.", totalSaved);
            
        } catch (Exception e) {
            logger.error("Erro durante a busca por palavras-chave especificas: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldNews() {
        logger.info("Iniciando limpeza de noticias antigas...");
        
        try {
            int deletedCount = newsArticleService.deleteOldUnpublishedArticles(7);
            logger.info("Limpeza concluida. {} noticias antigas removidas.", deletedCount);
            
        } catch (Exception e) {
            logger.error("Erro durante a limpeza de noticias antigas: {}", e.getMessage(), e);
        }
    }

    public void forceNewsUpdate() {
        logger.info("Executando busca manual de noticias...");
        fetchTechNews();
    }

    private List<NewsArticle> combineAndDeduplicate(List<NewsArticle> list1, List<NewsArticle> list2) {
        List<NewsArticle> combined = new java.util.ArrayList<>(list1);
        combined.addAll(list2);
        
        return combined.stream()
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