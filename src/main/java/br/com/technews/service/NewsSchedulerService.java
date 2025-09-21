package br.com.technews.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "technews.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class NewsSchedulerService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NewsSchedulerService.class);

    private final NewsScrapingService newsScrapingService;
    private final EmailService emailService;
    private final NewsCollectionService newsCollectionService;
    private final NewsletterService newsletterService;

    @Scheduled(cron = "0 0 */2 * * *")
    public void scheduleNewsCollection() {
        log.info("Iniciando coleta automatica de noticias as {}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        
        try {
            // Coleta via sistema novo (RSS/APIs)
            newsCollectionService.collectNewsFromAllSources();
            
            // Mantém coleta via scraping para compatibilidade
            var articles = newsScrapingService.scrapeNews();
            log.info("Coleta automatica concluida. {} novos artigos via scraping", articles.size());
            
            // Estatísticas do novo sistema
            long totalCollected = newsCollectionService.getTotalCollectedNews();
            long pending = newsCollectionService.getPendingNewsCount();
            log.info("Sistema RSS/API: {} total, {} pendentes", totalCollected, pending);
            
        } catch (Exception e) {
            log.error("Erro durante a coleta automatica de noticias", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * *") // Todos os dias às 8h
    public void scheduleDailyNewsletter() {
        log.info("Executando envio automático da newsletter diária");
        try {
            // Usa o novo método de newsletter automática com notícias coletadas
            newsletterService.generateAndSendAutomaticNewsletter();
            log.info("Newsletter diária enviada com sucesso");
        } catch (Exception e) {
            log.error("Erro no envio automático da newsletter diária", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * MON")
    public void scheduleCleanupOldArticles() {
        log.info("Iniciando limpeza de artigos antigos as {}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        
        try {
            log.info("Limpeza de artigos antigos concluida");
            
        } catch (Exception e) {
            log.error("Erro durante a limpeza de artigos antigos", e);
        }
    }

    public void executeManualCollection() {
        log.info("Executando coleta manual de noticias");
        scheduleNewsCollection();
    }

    public void executeManualNewsletter() {
        log.info("Executando envio manual de newsletter");
        scheduleDailyNewsletter();
    }
}