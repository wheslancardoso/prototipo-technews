package br.com.technews.service;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Category;
import br.com.technews.repository.SubscriberRepository;
import br.com.technews.repository.NewsArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final SubscriberRepository subscriberRepository;
    private final NewsArticleRepository articleRepository;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.name}")
    private String appName;

    /**
     * Envia email de verificação para novo assinante
     */
    @Async
    public CompletableFuture<Boolean> sendVerificationEmail(Subscriber subscriber) {
        try {
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            context.setVariable("subscriber", subscriber);
            context.setVariable("appName", appName);
            context.setVariable("verificationUrl", baseUrl + "/newsletter/verify?token=" + subscriber.getVerificationToken());
            context.setVariable("unsubscribeUrl", baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken());

            String htmlContent = templateEngine.process("email/verification", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(subscriber.getEmail());
            helper.setSubject("Confirme sua inscrição na newsletter - " + appName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            log.info("Email de verificação enviado para: {}", subscriber.getEmail());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Erro ao enviar email de verificação para {}: {}", subscriber.getEmail(), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Envia newsletter com artigos recentes
     */
    @Async
    @Transactional
    public CompletableFuture<Boolean> sendNewsletterToSubscriber(Subscriber subscriber, List<NewsArticle> articles) {
        try {
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            context.setVariable("subscriber", subscriber);
            context.setVariable("articles", articles);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("unsubscribeUrl", baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken());
            context.setVariable("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            String htmlContent = templateEngine.process("email/newsletter", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(subscriber.getEmail());
            helper.setSubject(generateNewsletterSubject(articles.size()));
            helper.setText(htmlContent, true);

            mailSender.send(message);

            // Atualiza estatísticas do assinante
            subscriber.incrementEmailCount();
            subscriber.setLastEmailSentAt(LocalDateTime.now());
            subscriberRepository.save(subscriber);

            log.info("Newsletter enviada para: {} com {} artigos", subscriber.getEmail(), articles.size());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Envia newsletter para todos os assinantes ativos e verificados
     */
    @Async
    @Transactional
    public CompletableFuture<Map<String, Integer>> sendNewsletterToAll() {
        try {
            List<Subscriber> subscribers = subscriberRepository.findActiveAndVerifiedSubscribers();
            List<NewsArticle> recentArticles = articleRepository.findRecentPublishedArticles(
                LocalDateTime.now().minusDays(7), 
                org.springframework.data.domain.PageRequest.of(0, 10)
            );

            if (recentArticles.isEmpty()) {
                log.info("Nenhum artigo recente encontrado para envio da newsletter");
                return CompletableFuture.completedFuture(Map.of("sent", 0, "failed", 0, "total", 0));
            }

            int sent = 0;
            int failed = 0;

            for (Subscriber subscriber : subscribers) {
                try {
                    // Filtra artigos por categorias do assinante se ele tiver preferências
                    List<NewsArticle> articlesToSend = filterArticlesBySubscriberPreferences(subscriber, recentArticles);
                    
                    if (!articlesToSend.isEmpty()) {
                        boolean success = sendNewsletterToSubscriber(subscriber, articlesToSend).get();
                        if (success) {
                            sent++;
                        } else {
                            failed++;
                        }
                        
                        // Pequena pausa entre envios para evitar spam
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
                    failed++;
                }
            }

            log.info("Newsletter enviada: {} sucessos, {} falhas, {} total", sent, failed, subscribers.size());
            return CompletableFuture.completedFuture(Map.of("sent", sent, "failed", failed, "total", subscribers.size()));

        } catch (Exception e) {
            log.error("Erro ao enviar newsletter para todos: {}", e.getMessage());
            return CompletableFuture.completedFuture(Map.of("sent", 0, "failed", 0, "total", 0));
        }
    }

    /**
     * Envia newsletter por frequência específica
     */
    @Async
    @Transactional
    public CompletableFuture<Map<String, Integer>> sendNewsletterByFrequency(Subscriber.SubscriptionFrequency frequency) {
        try {
            LocalDateTime cutoffDate = calculateCutoffDateByFrequency(frequency);
            List<Subscriber> subscribers = subscriberRepository.findDueForEmailByFrequency(frequency, cutoffDate);
            
            List<NewsArticle> recentArticles = articleRepository.findRecentPublishedArticles(
                cutoffDate, 
                org.springframework.data.domain.PageRequest.of(0, 15)
            );

            if (recentArticles.isEmpty()) {
                log.info("Nenhum artigo recente encontrado para frequência: {}", frequency);
                return CompletableFuture.completedFuture(Map.of("sent", 0, "failed", 0, "total", 0));
            }

            int sent = 0;
            int failed = 0;

            for (Subscriber subscriber : subscribers) {
                List<NewsArticle> articlesToSend = filterArticlesBySubscriberPreferences(subscriber, recentArticles);
                
                if (!articlesToSend.isEmpty()) {
                    boolean success = sendNewsletterToSubscriber(subscriber, articlesToSend).get();
                    if (success) {
                        sent++;
                    } else {
                        failed++;
                    }
                    
                    Thread.sleep(100);
                }
            }

            log.info("Newsletter por frequência {} enviada: {} sucessos, {} falhas, {} total", 
                    frequency, sent, failed, subscribers.size());
            return CompletableFuture.completedFuture(Map.of("sent", sent, "failed", failed, "total", subscribers.size()));

        } catch (Exception e) {
            log.error("Erro ao enviar newsletter por frequência {}: {}", frequency, e.getMessage());
            return CompletableFuture.completedFuture(Map.of("sent", 0, "failed", 0, "total", 0));
        }
    }

    /**
     * Envia email de confirmação de cancelamento
     */
    @Async
    public CompletableFuture<Boolean> sendUnsubscribeConfirmationEmail(Subscriber subscriber) {
        try {
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            context.setVariable("subscriber", subscriber);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("resubscribeUrl", baseUrl + "/newsletter/subscribe");

            String htmlContent = templateEngine.process("email/unsubscribe-confirmation", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(subscriber.getEmail());
            helper.setSubject("Cancelamento confirmado - " + appName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            log.info("Email de confirmação de cancelamento enviado para: {}", subscriber.getEmail());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Erro ao enviar email de confirmação de cancelamento para {}: {}", subscriber.getEmail(), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Envia email de boas-vindas após verificação
     */
    @Async
    public CompletableFuture<Boolean> sendWelcomeEmail(Subscriber subscriber) {
        try {
            Context context = new Context(Locale.forLanguageTag("pt-BR"));
            context.setVariable("subscriber", subscriber);
            context.setVariable("appName", appName);
            context.setVariable("baseUrl", baseUrl);
            context.setVariable("unsubscribeUrl", baseUrl + "/newsletter/unsubscribe?token=" + subscriber.getUnsubscribeToken());
            context.setVariable("manageUrl", baseUrl + "/newsletter/manage?token=" + subscriber.getUnsubscribeToken());

            String htmlContent = templateEngine.process("email/welcome", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(subscriber.getEmail());
            helper.setSubject("Bem-vindo à nossa newsletter! - " + appName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            log.info("Email de boas-vindas enviado para: {}", subscriber.getEmail());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Erro ao enviar email de boas-vindas para {}: {}", subscriber.getEmail(), e.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Filtra artigos baseado nas preferências do assinante
     */
    private List<NewsArticle> filterArticlesBySubscriberPreferences(Subscriber subscriber, List<NewsArticle> articles) {
        if (subscriber.getSubscribedCategories() == null || subscriber.getSubscribedCategories().isEmpty()) {
            return articles; // Se não tem preferências, recebe todos
        }

        return articles.stream()
                .filter(article -> article.getCategories().stream()
                        .anyMatch(category -> subscriber.getSubscribedCategories().contains(category)))
                .collect(Collectors.toList());
    }

    /**
     * Calcula data de corte baseada na frequência
     */
    private LocalDateTime calculateCutoffDateByFrequency(Subscriber.SubscriptionFrequency frequency) {
        LocalDateTime now = LocalDateTime.now();
        return switch (frequency) {
            case DAILY -> now.minusDays(1);
            case WEEKLY -> now.minusDays(7);
            case MONTHLY -> now.minusDays(30);
        };
    }

    /**
     * Gera assunto da newsletter baseado no número de artigos
     */
    private String generateNewsletterSubject(int articleCount) {
        if (articleCount == 1) {
            return String.format("%s - 1 novo artigo para você!", appName);
        } else {
            return String.format("%s - %d novos artigos para você!", appName, articleCount);
        }
    }

    /**
     * Limpa tokens de verificação expirados
     */
    @Transactional
    public void cleanupExpiredVerificationTokens() {
        List<Subscriber> expiredTokens = subscriberRepository.findExpiredVerificationTokens(LocalDateTime.now());
        
        for (Subscriber subscriber : expiredTokens) {
            if (!subscriber.isEmailVerified()) {
                // Remove assinantes não verificados com tokens expirados
                subscriberRepository.delete(subscriber);
                log.info("Removido assinante não verificado com token expirado: {}", subscriber.getEmail());
            } else {
                // Limpa apenas o token se já foi verificado
                subscriber.setVerificationToken(null);
                subscriber.setVerificationTokenExpiresAt(null);
                subscriberRepository.save(subscriber);
            }
        }
    }

    /**
     * Envia newsletter para assinantes por frequência e categorias específicas
     */
    @Async
    @Transactional
    public int sendNewsletterToSubscribers(Subscriber.SubscriptionFrequency frequency, 
                                         String categoryIds, 
                                         boolean testMode) {
        try {
            LocalDateTime cutoffDate = calculateCutoffDateByFrequency(frequency);
            List<Subscriber> subscribers;
            
            if (testMode) {
                // Em modo teste, pega apenas alguns assinantes
                subscribers = subscriberRepository.findActiveAndVerifiedSubscribers()
                    .stream()
                    .filter(s -> s.getFrequency() == frequency)
                    .limit(5)
                    .collect(Collectors.toList());
            } else {
                subscribers = subscriberRepository.findDueForEmailByFrequency(frequency, cutoffDate);
            }
            
            List<NewsArticle> recentArticles = articleRepository.findRecentPublishedArticles(
                cutoffDate, 
                org.springframework.data.domain.PageRequest.of(0, 15)
            );

            if (recentArticles.isEmpty()) {
                log.info("Nenhum artigo recente encontrado para frequência: {}", frequency);
                return 0;
            }

            int emailsSent = 0;
            for (Subscriber subscriber : subscribers) {
                List<NewsArticle> articlesToSend = filterArticlesBySubscriberPreferences(subscriber, recentArticles);
                
                if (!articlesToSend.isEmpty()) {
                    boolean success = sendNewsletterToSubscriber(subscriber, articlesToSend).get();
                    if (success) {
                        emailsSent++;
                    }
                    
                    // Pausa entre envios
                    Thread.sleep(100);
                }
            }

            log.info("Newsletter enviada para {} assinantes (frequência: {}, modo teste: {})", 
                    emailsSent, frequency, testMode);
            return emailsSent;

        } catch (Exception e) {
            log.error("Erro ao enviar newsletter: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Envia newsletter diária com artigos específicos
     */
    @Async
    @Transactional
    public CompletableFuture<Map<String, Integer>> sendDailyNewsletter(List<NewsArticle> articles) {
        log.info("Iniciando envio da newsletter diária com {} artigos", articles.size());
        
        Map<String, Integer> result = Map.of(
            "sent", 0,
            "failed", 0,
            "total", 0
        );
        
        try {
            List<Subscriber> activeSubscribers = subscriberRepository.findByActiveTrue();
            
            if (activeSubscribers.isEmpty()) {
                log.info("Nenhum assinante ativo encontrado");
                return CompletableFuture.completedFuture(result);
            }
            
            int sent = 0;
            int failed = 0;
            
            for (Subscriber subscriber : activeSubscribers) {
                try {
                    CompletableFuture<Boolean> future = sendNewsletterToSubscriber(subscriber, articles);
                    Boolean success = future.get();
                    if (success) {
                        sent++;
                        log.debug("Newsletter enviada para: {}", subscriber.getEmail());
                    } else {
                        failed++;
                    }
                } catch (Exception e) {
                    failed++;
                    log.error("Erro ao enviar newsletter para {}: {}", subscriber.getEmail(), e.getMessage());
                }
            }
            
            result = Map.of(
                "sent", sent,
                "failed", failed,
                "total", sent + failed
            );
            
            log.info("Newsletter diária enviada: {} sucessos, {} falhas", sent, failed);
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            log.error("Erro ao enviar newsletter diária: {}", e.getMessage());
            return CompletableFuture.completedFuture(result);
        }
    }



    /**
     * Testa configuração de email
     */
    public boolean testEmailConfiguration() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(fromEmail); // Envia para si mesmo
            helper.setSubject("Teste de configuração de email - " + appName);
            helper.setText("Este é um email de teste para verificar a configuração do sistema.", false);

            mailSender.send(message);
            log.info("Email de teste enviado com sucesso");
            return true;

        } catch (Exception e) {
            log.error("Erro ao enviar email de teste: {}", e.getMessage());
            return false;
        }
    }
}