package br.com.technews.scheduler;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Subscriber;
import br.com.technews.service.EmailService;
import br.com.technews.service.GNewsService;
import br.com.technews.service.NewsArticleService;
import br.com.technews.service.SubscriberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class NewsScheduler {

    private static final Logger logger = LoggerFactory.getLogger(NewsScheduler.class);

    private final GNewsService gNewsService;
    private final NewsArticleService newsArticleService;
    private final EmailService emailService;
    private final SubscriberService subscriberService;
    private final TemplateEngine templateEngine;

    public NewsScheduler(GNewsService gNewsService, 
                        NewsArticleService newsArticleService,
                        EmailService emailService,
                        SubscriberService subscriberService,
                        TemplateEngine templateEngine) {
        this.gNewsService = gNewsService;
        this.newsArticleService = newsArticleService;
        this.emailService = emailService;
        this.subscriberService = subscriberService;
        this.templateEngine = templateEngine;
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

    @Scheduled(cron = "0 0 9 * * ?") // Executa diariamente às 9:00
    public void sendDailyNewsletter() {
        logger.info("Iniciando envio automático da newsletter diária...");
        
        try {
            // Buscar artigos recentes (últimas 24 horas)
            List<NewsArticle> recentArticles = newsArticleService.findRecentArticles(1);
            
            if (recentArticles.isEmpty()) {
                logger.warn("Nenhum artigo recente encontrado para envio da newsletter");
                return;
            }
            
            // Gerar conteúdo HTML da newsletter
            String htmlContent = generateNewsletterHtml(recentArticles);
            
            // Obter lista de assinantes ativos
            List<Subscriber> activeSubscribers = subscriberService.findActiveSubscribers();
            
            if (activeSubscribers.isEmpty()) {
                logger.info("Nenhum assinante ativo encontrado para envio da newsletter");
                return;
            }
            
            logger.info("Enviando newsletter para {} assinantes ativos", activeSubscribers.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            // Enviar newsletter para cada assinante
            for (Subscriber subscriber : activeSubscribers) {
                try {
                    emailService.sendNewsletter(
                        subscriber.getEmail(), 
                        "Sua TechNews Diária - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
                        htmlContent
                    );
                    successCount++;
                    
                    // Pequena pausa para evitar sobrecarga do servidor de email
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    logger.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
                    errorCount++;
                }
            }
            
            logger.info("Envio da newsletter concluído - Sucessos: {}, Erros: {}", successCount, errorCount);
            
        } catch (Exception e) {
            logger.error("Erro crítico durante o envio automático da newsletter: {}", e.getMessage(), e);
        }
    }

    private String generateNewsletterHtml(List<NewsArticle> articles) {
        try {
            Context context = new Context();
            context.setVariable("articles", articles);
            context.setVariable("date", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("totalArticles", articles.size());
            
            return templateEngine.process("email/newsletter-template", context);
            
        } catch (Exception e) {
            logger.error("Erro ao gerar HTML da newsletter: {}", e.getMessage(), e);
            
            // Fallback: gerar HTML simples
            StringBuilder html = new StringBuilder();
            html.append("<html><body>");
            html.append("<h1>TechNews Diária - ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</h1>");
            html.append("<p>Aqui estão as principais notícias de tecnologia de hoje:</p>");
            
            for (NewsArticle article : articles) {
                html.append("<div style='margin-bottom: 20px; border-bottom: 1px solid #ccc; padding-bottom: 15px;'>");
                html.append("<h3><a href='").append(article.getUrl()).append("'>").append(article.getTitle()).append("</a></h3>");
                if (article.getSummary() != null && !article.getSummary().isEmpty()) {
                    html.append("<p>").append(article.getSummary()).append("</p>");
                } else if (article.getContent() != null && !article.getContent().isEmpty()) {
                    // Usar os primeiros 200 caracteres do conteúdo como descrição
                    String shortContent = article.getContent().length() > 200 
                        ? article.getContent().substring(0, 200) + "..." 
                        : article.getContent();
                    html.append("<p>").append(shortContent).append("</p>");
                }
                html.append("<small>Fonte: ").append(article.getSourceDomain() != null ? article.getSourceDomain() : "N/A").append("</small>");
                html.append("</div>");
            }
            
            html.append("</body></html>");
            return html.toString();
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
}