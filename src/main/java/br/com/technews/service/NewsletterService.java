package br.com.technews.service;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Subscriber;
import br.com.technews.entity.CollectedNews;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
            log.error("Erro ao enviar newsletter diária", e);
            throw new RuntimeException("Falha no envio da newsletter", e);
        }
    }

    /**
     * Envia newsletter personalizada para um assinante específico
     */
    public void sendNewsletterToSubscriber(Subscriber subscriber, 
                                         List<NewsArticle> featuredArticles,
                                         List<NewsArticle> latestArticles,
                                         List<NewsArticle> popularArticles,
                                         Map<String, Object> stats) {
        try {
            String subject = generateNewsletterSubject(featuredArticles);
            
            // Prepara contexto do template
            Context context = new Context();
            context.setVariable("subscriber", subscriber);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("newsletter", Map.of(
                "subject", subject,
                "createdAt", LocalDateTime.now()
            ));
            context.setVariable("featuredArticles", featuredArticles);
            context.setVariable("latestArticles", latestArticles);
            context.setVariable("popularArticles", popularArticles);
            context.setVariable("stats", stats);

            String htmlContent = templateEngine.process("newsletter/newsletter-email", context);

            // Enviar email usando o método correto do EmailService
            CompletableFuture<Boolean> result = emailService.sendNewsletterToSubscriber(subscriber, 
                Stream.concat(
                    Stream.concat(featuredArticles.stream(), latestArticles.stream()),
                    popularArticles.stream()
                ).collect(Collectors.toList()));
            
            if (result.get()) {
                // Atualizar última data de envio
                subscriber.setLastEmailSentAt(LocalDateTime.now());
                subscriberService.save(subscriber);
            }
            
            log.debug("Newsletter enviada para: {}", subscriber.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
            throw new RuntimeException("Falha no envio da newsletter para " + subscriber.getEmail(), e);
        }
    }

    /**
     * Envia newsletter de boas-vindas para novo assinante
     */
    public void sendWelcomeNewsletter(Subscriber subscriber) {
        try {
            log.info("Enviando newsletter de boas-vindas para: {}", subscriber.getEmail());
            
            // Busca artigos populares para o novo assinante
            List<NewsArticle> popularArticles = newsArticleService.findPopularArticles(5);
            List<NewsArticle> recentArticles = newsArticleService.findLatestPublishedArticles(3);
            
            Context context = new Context();
            context.setVariable("subscriberName", subscriber.getName());
            context.setVariable("subscriberEmail", subscriber.getEmail());
            context.setVariable("featuredArticles", popularArticles);
            context.setVariable("latestArticles", recentArticles);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("isWelcome", true);
            
            String unsubscribeToken = generateUnsubscribeToken(subscriber);
            String preferencesToken = generatePreferencesToken(subscriber);
            context.setVariable("unsubscribeToken", unsubscribeToken);
            context.setVariable("preferencesToken", preferencesToken);

            String htmlContent = templateEngine.process("newsletter/welcome-template", context);
            String subject = "Bem-vindo ao TechNews! 🚀";
            
            // Usar o método correto do EmailService
            CompletableFuture<Boolean> result = emailService.sendNewsletterToSubscriber(subscriber, List.of());
            
            if (result.get()) {
                subscriber.setLastEmailSentAt(LocalDateTime.now());
                subscriberService.save(subscriber);
                log.info("Newsletter de boas-vindas enviada para: {}", subscriber.getEmail());
            } else {
                log.error("Falha ao enviar newsletter de boas-vindas para: {}", subscriber.getEmail());
            }
            
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter de boas-vindas para {}: {}", subscriber.getEmail(), e.getMessage());
            throw new RuntimeException("Falha no envio da newsletter de boas-vindas", e);
        }
    }

    /**
     * Gera estatísticas para incluir na newsletter
     */
    private Map<String, Object> generateNewsletterStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Estatísticas dos últimos 7 dias
            LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
            
            long totalArticles = newsArticleService.countArticlesPublishedSince(weekAgo);
            long totalViews = newsArticleService.getTotalViewsLastWeek();
            long totalSubscribers = subscriberService.countActiveSubscribers();
            
            stats.put("totalArticles", totalArticles);
            stats.put("totalViews", formatNumber(totalViews));
            stats.put("totalSubscribers", totalSubscribers);
            
        } catch (Exception e) {
            log.warn("Erro ao gerar estatísticas da newsletter: {}", e.getMessage());
            // Valores padrão em caso de erro
            stats.put("totalArticles", 0L);
            stats.put("totalViews", "0");
            stats.put("totalSubscribers", 0L);
        }
        
        return stats;
    }

    /**
     * Gera assunto personalizado baseado nos artigos em destaque
     */
    private String generateNewsletterSubject(List<NewsArticle> featuredArticles) {
        if (featuredArticles == null || featuredArticles.isEmpty()) {
            return "TechNews - Newsletter Diária 📰";
        }
        
        // Usa o primeiro artigo em destaque para personalizar o assunto
        NewsArticle topArticle = featuredArticles.get(0);
        String category = topArticle.getCategory();
        
        return String.format("TechNews - %s e mais novidades tech 🚀", 
                           category != null ? category : "Últimas notícias");
    }

    /**
     * Gera token seguro para cancelamento de inscrição
     */
    private String generateUnsubscribeToken(Subscriber subscriber) {
        // Implementação simples - em produção, usar JWT ou similar
        return UUID.randomUUID().toString() + "-" + subscriber.getId();
    }

    /**
     * Gera token seguro para gerenciar preferências
     */
    private String generatePreferencesToken(Subscriber subscriber) {
        // Implementação simples - em produção, usar JWT ou similar
        return UUID.randomUUID().toString() + "-pref-" + subscriber.getId();
    }

    /**
     * Formata números grandes para exibição (ex: 1.2K, 5.3M)
     */
    private String formatNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1000000) {
            return String.format("%.1fK", number / 1000.0);
        } else {
            return String.format("%.1fM", number / 1000000.0);
        }
    }

    /**
     * Envia newsletter de teste para um email específico
     */
    public void sendTestNewsletter(String testEmail) {
        try {
            log.info("Enviando newsletter de teste para: {}", testEmail);
            
            // Cria assinante temporário para teste
            Subscriber testSubscriber = new Subscriber();
            testSubscriber.setEmail(testEmail);
            testSubscriber.setName("Teste");
            
            List<NewsArticle> featuredArticles = newsArticleService.findFeaturedArticles(3);
            List<NewsArticle> latestArticles = newsArticleService.findLatestPublishedArticles(2);
            List<NewsArticle> popularArticles = newsArticleService.findPopularArticles(2);
            Map<String, Object> stats = generateNewsletterStats();
            
            sendNewsletterToSubscriber(testSubscriber, featuredArticles, latestArticles, popularArticles, stats);
            
            log.info("Newsletter de teste enviada com sucesso para: {}", testEmail);
            
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter de teste para {}: {}", testEmail, e.getMessage());
            throw new RuntimeException("Falha no envio da newsletter de teste", e);
        }
    }

    /**
     * Agenda envio automático da newsletter diária
     */
    public void scheduleWeeklyNewsletter() {
        // Este método será chamado pelo scheduler
        try {
            sendWeeklyNewsletter();
        } catch (Exception e) {
            log.error("Erro no envio automático da newsletter diária", e);
        }
    }

    /**
     * Gera e envia newsletter automática com notícias coletadas
     */
    @Transactional
    public void generateAndSendAutomaticNewsletter() {
        try {
            log.info("Iniciando geração automática de newsletter com notícias coletadas");
            
            List<Subscriber> activeSubscribers = subscriberService.findActiveSubscribers();
            if (activeSubscribers.isEmpty()) {
                log.info("Nenhum assinante ativo encontrado para newsletter automática");
                return;
            }

            // Busca notícias coletadas de alta qualidade
            List<CollectedNews> topCollectedNews = newsCurationService.getTopQualityNews(10);
            List<CollectedNews> recentCollectedNews = newsCurationService.getRecentApprovedNews(3);
            
            if (topCollectedNews.isEmpty() && recentCollectedNews.isEmpty()) {
                log.info("Nenhuma notícia coletada de qualidade encontrada para newsletter");
                return;
            }

            // Combina com artigos tradicionais se disponíveis
            List<NewsArticle> featuredArticles = newsArticleService.findFeaturedArticles(3);
            List<NewsArticle> latestArticles = newsArticleService.findLatestPublishedArticles(5);

            // Gera estatísticas incluindo notícias coletadas
            Map<String, Object> stats = generateAutomaticNewsletterStats();

            int successCount = 0;
            int errorCount = 0;

            for (Subscriber subscriber : activeSubscribers) {
                try {
                    sendAutomaticNewsletterToSubscriber(subscriber, topCollectedNews, 
                        recentCollectedNews, featuredArticles, latestArticles, stats);
                    successCount++;
                    
                    // Pequena pausa para evitar sobrecarga
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("Erro ao enviar newsletter automática para {}: {}", 
                        subscriber.getEmail(), e.getMessage());
                    errorCount++;
                }
            }

            // Marca notícias como publicadas
            markCollectedNewsAsPublished(topCollectedNews);
            markCollectedNewsAsPublished(recentCollectedNews);

            log.info("Newsletter automática enviada - Sucessos: {}, Erros: {}", successCount, errorCount);
            
        } catch (Exception e) {
            log.error("Erro ao gerar newsletter automática", e);
            throw new RuntimeException("Falha na geração da newsletter automática", e);
        }
    }

    private void sendAutomaticNewsletterToSubscriber(Subscriber subscriber,
                                                   List<CollectedNews> topNews,
                                                   List<CollectedNews> recentNews,
                                                   List<NewsArticle> featuredArticles,
                                                   List<NewsArticle> latestArticles,
                                                   Map<String, Object> stats) {
        try {
            String subject = generateAutomaticNewsletterSubject(topNews, featuredArticles);
            
            Context context = new Context();
            context.setVariable("subscriber", subscriber);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("newsletter", Map.of(
                "subject", subject,
                "createdAt", LocalDateTime.now(),
                "type", "automatic"
            ));
            context.setVariable("topCollectedNews", topNews);
            context.setVariable("recentCollectedNews", recentNews);
            context.setVariable("featuredArticles", featuredArticles);
            context.setVariable("latestArticles", latestArticles);
            context.setVariable("stats", stats);

            String htmlContent = templateEngine.process("newsletter/automatic-newsletter-email", context);

            // Enviar email
            emailService.sendHtmlEmail(subscriber.getEmail(), subject, htmlContent);
            
            // Atualizar última data de envio
            subscriber.setLastEmailSentAt(LocalDateTime.now());
            subscriberService.save(subscriber);
            
            log.debug("Newsletter automática enviada para: {}", subscriber.getEmail());
            
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter automática para {}: {}", 
                subscriber.getEmail(), e.getMessage());
            throw new RuntimeException("Falha no envio da newsletter automática para " + 
                subscriber.getEmail(), e);
        }
    }

    private String generateAutomaticNewsletterSubject(List<CollectedNews> topNews, 
                                                    List<NewsArticle> featuredArticles) {
        String baseSubject = "TechNews - Últimas Notícias de Tecnologia";
        
        if (!topNews.isEmpty()) {
            String firstTitle = topNews.get(0).getTitle();
            if (firstTitle.length() > 50) {
                firstTitle = firstTitle.substring(0, 47) + "...";
            }
            return baseSubject + " - " + firstTitle;
        } else if (!featuredArticles.isEmpty()) {
            return baseSubject + " - " + featuredArticles.get(0).getTitle();
        }
        
        return baseSubject + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private Map<String, Object> generateAutomaticNewsletterStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Estatísticas tradicionais
            long totalArticles = newsArticleService.countAll();
            long publishedArticles = newsArticleService.countPublished();
            
            // Estatísticas de notícias coletadas
            long totalCollected = newsCurationService.getTopQualityNews(1000).size();
            long approvedCollected = newsCurationService.getRecentApprovedNews(30).size();
            
            stats.put("totalArticles", totalArticles);
            stats.put("publishedArticles", publishedArticles);
            stats.put("totalCollectedNews", totalCollected);
            stats.put("approvedCollectedNews", approvedCollected);
            stats.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            
        } catch (Exception e) {
            log.warn("Erro ao gerar estatísticas da newsletter automática", e);
            stats.put("error", "Estatísticas indisponíveis");
        }
        
        return stats;
    }

    private void markCollectedNewsAsPublished(List<CollectedNews> newsList) {
        // Esta funcionalidade será implementada quando necessário
        // Por enquanto, apenas logamos
        log.debug("Marcando {} notícias coletadas como publicadas", newsList.size());
    }
}