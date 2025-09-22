package br.com.technews.service;

import br.com.technews.dto.NewsletterStats;
import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Newsletter;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.CollectedNews;
import br.com.technews.repository.NewsletterRepository;
import br.com.technews.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private static final Logger log = LoggerFactory.getLogger(NewsletterService.class);

    private final NewsArticleService newsArticleService;
    private final SubscriberService subscriberService;
    private final EmailService emailService;
    private final TemplateEngine templateEngine;
    private final NewsCurationService newsCurationService;
    
    // Novos repositórios para funcionalidade de newsletter diária
    private final NewsletterRepository newsletterRepository;
    private final NewsArticleRepository newsArticleRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${newsletter.featured-articles-limit:5}")
    private int featuredArticlesLimit;

    @Value("${newsletter.latest-articles-limit:10}")
    private int latestArticlesLimit;

    /**
     * Envia newsletter para todos os assinantes ativos
     */
    public void sendWeeklyNewsletter() {
        try {
            log.info("Iniciando envio da newsletter diária");
            
            List<Subscriber> activeSubscribers = subscriberService.findActiveSubscribers();
            if (activeSubscribers.isEmpty()) {
                log.info("Nenhum assinante ativo encontrado");
                return;
            }

            // Busca artigos para a newsletter
            List<NewsArticle> featuredArticles = newsArticleService.findFeaturedArticles(featuredArticlesLimit);
            List<NewsArticle> latestArticles = newsArticleService.findLatestPublishedArticles(latestArticlesLimit);
            List<NewsArticle> popularArticles = newsArticleService.findPopularArticles(5);

            // Gera estatísticas
            Map<String, Object> stats = generateNewsletterStats();

            int successCount = 0;
            int errorCount = 0;

            for (Subscriber subscriber : activeSubscribers) {
                try {
                    sendNewsletterToSubscriber(subscriber, featuredArticles, latestArticles, popularArticles, stats);
                    successCount++;
                    
                    // Pequena pausa para evitar sobrecarga do servidor de email
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
                    errorCount++;
                }
            }

            log.info("Newsletter enviada - Sucessos: {}, Erros: {}", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Erro crítico no envio da newsletter: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia newsletter para um assinante específico
     */
    private void sendNewsletterToSubscriber(Subscriber subscriber, 
                                          List<NewsArticle> featuredArticles,
                                          List<NewsArticle> latestArticles, 
                                          List<NewsArticle> popularArticles,
                                          NewsletterStats stats) {
        
        try {
            // Combinar todos os artigos
            List<NewsArticle> allArticles = new ArrayList<>();
            allArticles.addAll(featuredArticles);
            allArticles.addAll(latestArticles);
            allArticles.addAll(popularArticles);
            
            // Remover duplicatas mantendo a ordem
            Set<Long> seenIds = new HashSet<>();
            List<NewsArticle> uniqueArticles = allArticles.stream()
                .filter(article -> seenIds.add(article.getId()))
                .collect(Collectors.toList());
            
            // Enviar newsletter
            CompletableFuture<Boolean> result = emailService.sendNewsletterToSubscriber(subscriber,
                uniqueArticles);
            
            // Aguardar resultado e atualizar contador
            if (result.get()) {
                subscriber.incrementEmailCount();
                subscriberRepository.save(subscriber);
                logger.info("Newsletter enviada com sucesso para: {}", subscriber.getEmail());
            } else {
                logger.warn("Falha ao enviar newsletter para: {}", subscriber.getEmail());
            }
            
        } catch (Exception e) {
            logger.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
        }
    }

    /**
     * Gera estatísticas para incluir na newsletter
     */
    private Map<String, Object> generateNewsletterStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            long totalArticles = newsArticleService.countPublishedArticles();
            long totalSubscribers = subscriberService.countActiveSubscribers();
            long articlesToday = newsArticleService.countArticlesPublishedToday();
            
            stats.put("totalArticles", formatNumber(totalArticles));
            stats.put("totalSubscribers", formatNumber(totalSubscribers));
            stats.put("articlesToday", formatNumber(articlesToday));
            
        } catch (Exception e) {
            log.warn("Erro ao gerar estatísticas da newsletter: {}", e.getMessage());
            stats.put("totalArticles", "N/A");
            stats.put("totalSubscribers", "N/A");
            stats.put("articlesToday", "N/A");
        }
        
        return stats;
    }

    /**
     * Formata números para exibição
     */
    private String formatNumber(long number) {
        if (number >= 1000000) {
            return new DecimalFormat("#.#M").format(number / 1000000.0);
        } else if (number >= 1000) {
            return new DecimalFormat("#.#K").format(number / 1000.0);
        } else {
            return String.valueOf(number);
        }
    }

    /**
     * Gera URL de cancelamento de inscrição
     */
    private String generateUnsubscribeUrl(Subscriber subscriber) {
        return baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getManageToken();
    }

    /**
     * Gera URL de gerenciamento de preferências
     */
    private String generateManageUrl(Subscriber subscriber) {
        return baseUrl + "/newsletter/manage?token=" + subscriber.getManageToken();
    }

    /**
     * Agenda envio automático da newsletter
     * Executa todos os dias às 8:00
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduledNewsletterSend() {
        log.info("Executando envio automático da newsletter");
        sendWeeklyNewsletter();
    }

    /**
     * Envia newsletter de teste para um assinante específico
     */
    @Transactional
    public CompletableFuture<Boolean> sendTestNewsletter(String email) {
        try {
            Optional<Subscriber> subscriberOpt = subscriberService.findByEmail(email);
            if (subscriberOpt.isEmpty()) {
                log.warn("Assinante não encontrado para email: {}", email);
                return CompletableFuture.completedFuture(false);
            }

            Subscriber subscriber = subscriberOpt.get();
            
            // Criar estatísticas de teste
            NewsletterStats stats = new NewsletterStats(
                newsArticleService.countPublishedArticles(),
                subscriberService.countActiveSubscribers(),
                newsArticleService.countArticlesPublishedToday(),
                0L // totalViews
            );
            
            return sendNewsletterToSubscriber(subscriber, stats);
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter de teste para {}: {}", email, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Busca newsletter por ID
     */
    public Optional<Newsletter> findById(Long id) {
        return newsletterRepository.findById(id);
    }

    /**
     * Busca newsletter por slug
     */
    public Optional<Newsletter> findBySlug(String slug) {
        return newsletterRepository.findBySlug(slug);
    }

    /**
     * Busca newsletter por data
     */
    public Optional<Newsletter> findByDate(LocalDate date) {
        return newsletterRepository.findByNewsletterDate(date);
    }

    /**
     * Busca todas as newsletters publicadas com paginação
     */
    public Page<Newsletter> findAllPublished(Pageable pageable) {
        return newsletterRepository.findByPublishedTrueOrderByNewsletterDateDesc(pageable);
    }

    /**
     * Busca newsletters por período
     */
    public Page<Newsletter> findByDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return newsletterRepository.findByNewsletterDateBetweenAndPublishedTrueOrderByNewsletterDateDesc(
            startDate, endDate, pageable);
    }

    /**
     * Busca a newsletter mais recente publicada
     */
    public Optional<Newsletter> findLatestPublished() {
        return newsletterRepository.findFirstByPublishedTrueOrderByNewsletterDateDesc();
    }

    /**
     * Conta newsletters publicadas
     */
    public long countPublished() {
        return newsletterRepository.countByPublishedTrue();
    }

    /**
     * Verifica se existe newsletter para uma data
     */
    public boolean existsForDate(LocalDate date) {
        return newsletterRepository.existsByNewsletterDate(date);
    }

    /**
     * Gera newsletter diária para uma data específica
     */
    @Transactional
    public Newsletter generateDailyNewsletter(LocalDate date) {
        try {
            log.info("Gerando newsletter para a data: {}", date);

            // Verificar se já existe newsletter para esta data
            if (existsForDate(date)) {
                log.warn("Já existe newsletter para a data: {}", date);
                return null;
            }

            // Buscar artigos publicados na data
            List<NewsArticle> articlesForDate = newsArticleRepository
                .findByPublishedDateBetweenAndStatus(
                    date.atStartOfDay(),
                    date.plusDays(1).atStartOfDay(),
                    ArticleStatus.PUBLICADO
                );

            if (articlesForDate.isEmpty()) {
                log.info("Nenhum artigo encontrado para a data: {}", date);
                return null;
            }

            // Criar nova newsletter
            Newsletter newsletter = new Newsletter();
            newsletter.setTitle("TechNews - " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            newsletter.setSlug(generateSlug(date));
            newsletter.setNewsletterDate(date);
            newsletter.setPublished(true);
            newsletter.setCreatedAt(LocalDateTime.now());
            newsletter.setViews(0L);

            // Gerar conteúdo HTML
            String htmlContent = generateNewsletterContent(articlesForDate, date);
            newsletter.setContent(htmlContent);

            // Salvar newsletter
            newsletter = newsletterRepository.save(newsletter);

            // Associar artigos à newsletter
            for (NewsArticle article : articlesForDate) {
                newsletter.addArticle(article);
            }
            
            // Salvar newsletter novamente com os artigos associados
            newsletter = newsletterRepository.save(newsletter);

            log.info("Newsletter gerada com sucesso para a data: {} com {} artigos", 
                date, articlesForDate.size());

            return newsletter;

        } catch (Exception e) {
            log.error("Erro ao gerar newsletter para a data {}: {}", date, e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar newsletter", e);
        }
    }

    /**
     * Gera slug único para a newsletter
     */
    private String generateSlug(LocalDate date) {
        return "newsletter-" + date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * Gera conteúdo HTML da newsletter
     */
    private String generateNewsletterContent(List<NewsArticle> articles, LocalDate date) {
        try {
            Context context = new Context();
            context.setVariable("articles", articles);
            context.setVariable("date", date);
            context.setVariable("formattedDate", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("totalArticles", articles.size());

            // Separar artigos por categoria se necessário
            Map<String, List<NewsArticle>> articlesByCategory = articles.stream()
                .collect(Collectors.groupingBy(article -> 
                    article.getCategory() != null ? article.getCategory() : "Geral"));
            
            context.setVariable("articlesByCategory", articlesByCategory);

            return templateEngine.process("newsletter/daily-newsletter", context);

        } catch (Exception e) {
            log.error("Erro ao gerar conteúdo HTML da newsletter: {}", e.getMessage());
            
            // Fallback: gerar conteúdo simples
            StringBuilder content = new StringBuilder();
            content.append("<h1>TechNews - ").append(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("</h1>");
            content.append("<p>").append(articles.size()).append(" artigos encontrados:</p>");
            content.append("<ul>");
            
            for (NewsArticle article : articles) {
                content.append("<li><a href=\"").append(baseUrl).append("/articles/").append(article.getSlug())
                       .append("\">").append(article.getTitle()).append("</a></li>");
            }
            
            content.append("</ul>");
            
            return content.toString();
        }
    }

    /**
     * Incrementa visualizações da newsletter
     */
    @Transactional
    public void incrementViews(String slug) {
        try {
            Optional<Newsletter> newsletterOpt = findBySlug(slug);
            if (newsletterOpt.isPresent()) {
                Newsletter newsletter = newsletterOpt.get();
                newsletter.setViews(newsletter.getViews() + 1);
                newsletterRepository.save(newsletter);
            }
        } catch (Exception e) {
            log.warn("Erro ao incrementar visualizações para newsletter {}: {}", slug, e.getMessage());
        }
    }

    /**
     * Busca newsletters publicadas ordenadas por data
     */
    public List<Newsletter> findPublishedNewsletters() {
        return newsletterRepository.findByPublishedTrueOrderByNewsletterDateDesc();
    }

    /**
     * Busca todas as newsletters publicadas (sem paginação)
     */
    public List<Newsletter> findAllPublished() {
        return newsletterRepository.findByPublishedTrueOrderByNewsletterDateDesc();
    }

    /**
     * Gera newsletter automaticamente todos os dias às 6:00
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void scheduledDailyNewsletterGeneration() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("Executando geração automática de newsletter para: {}", yesterday);
            
            Newsletter newsletter = generateDailyNewsletter(yesterday);
            
            if (newsletter != null) {
                log.info("Newsletter gerada automaticamente: {}", newsletter.getSlug());
            } else {
                log.info("Nenhuma newsletter gerada para: {}", yesterday);
            }
            
        } catch (Exception e) {
            log.error("Erro na geração automática de newsletter: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia newsletter diária para todos os assinantes
     */
    @Scheduled(cron = "0 30 8 * * ?")
    public void scheduledDailyNewsletterSend() {
        try {
            log.info("Executando envio automático da newsletter diária");
            
            // Buscar newsletter de ontem (que deve ter sido gerada às 6:00)
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Optional<Newsletter> newsletterOpt = findByDate(yesterday);
            
            if (newsletterOpt.isEmpty()) {
                log.warn("Nenhuma newsletter encontrada para envio da data: {}", yesterday);
                return;
            }
            
            Newsletter newsletter = newsletterOpt.get();
            
            if (!newsletter.getPublished()) {
                log.warn("Newsletter não está publicada para envio: {}", newsletter.getSlug());
                return;
            }
            
            // Enviar para todos os assinantes ativos
            sendNewsletterToAllSubscribers(newsletter);
            
        } catch (Exception e) {
            log.error("Erro no envio automático da newsletter diária: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia newsletter específica para todos os assinantes
     */
    private void sendNewsletterToAllSubscribers(Newsletter newsletter) {
        try {
            List<Subscriber> activeSubscribers = subscriberService.findActiveSubscribers();
            
            if (activeSubscribers.isEmpty()) {
                log.info("Nenhum assinante ativo encontrado para envio da newsletter");
                return;
            }
            
            log.info("Enviando newsletter '{}' para {} assinantes", 
                newsletter.getTitle(), activeSubscribers.size());
            
            int successCount = 0;
            int errorCount = 0;
            
            for (Subscriber subscriber : activeSubscribers) {
                try {
                    sendNewsletterEmailToSubscriber(subscriber, newsletter);
                    successCount++;
                    
                    // Pequena pausa para evitar sobrecarga
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
                    errorCount++;
                }
            }
            
            log.info("Newsletter '{}' enviada - Sucessos: {}, Erros: {}", 
                newsletter.getTitle(), successCount, errorCount);
                
        } catch (Exception e) {
            log.error("Erro crítico no envio da newsletter: {}", e.getMessage(), e);
        }
    }

    /**
     * Envia email da newsletter para um assinante específico
     */
    private void sendNewsletterEmailToSubscriber(Subscriber subscriber, Newsletter newsletter) {
        try {
            Context context = new Context();
            context.setVariable("subscriber", subscriber);
            context.setVariable("newsletter", newsletter);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("unsubscribeUrl", generateUnsubscribeUrl(subscriber));
            context.setVariable("manageUrl", generateManageUrl(subscriber));
            context.setVariable("newsletterUrl", baseUrl + "/newsletter/" + newsletter.getSlug());
            
            String htmlContent = templateEngine.process("email/newsletter-daily", context);
            String subject = newsletter.getTitle();
            
            emailService.sendHtmlEmail(subscriber.getEmail(), subject, htmlContent);
            
            // Atualizar data do último email enviado
            subscriber.setLastEmailSentAt(LocalDateTime.now());
            subscriberService.save(subscriber);
            
            log.debug("Newsletter enviada com sucesso para: {}", subscriber.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
            throw e;
        }
    }
}