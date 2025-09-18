package br.com.technews.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(NewsSchedulerService.class);
    private final NewsScrapingService newsScrapingService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 */2 * * *")
    public void scheduleNewsCollection() {
        log.info("Iniciando coleta automatica de noticias as {}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        
        try {
            var articles = newsScrapingService.scrapeNews();
            log.info("Coleta automatica concluida. {} novos artigos coletados", articles.size());
            
            if (!articles.isEmpty()) {
                log.info("Novos artigos disponiveis para newsletter");
            }
            
        } catch (Exception e) {
            log.error("Erro durante a coleta automatica de noticias", e);
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void scheduleDailyNewsletter() {
        log.info("Iniciando envio da newsletter diaria as {}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        
        try {
            var recentArticles = newsScrapingService.getRecentArticles(10);
            
            if (!recentArticles.isEmpty()) {
                emailService.sendDailyNewsletter(recentArticles);
                log.info("Newsletter diaria enviada com {} artigos", recentArticles.size());
            } else {
                log.info("Nenhum artigo recente encontrado para newsletter");
            }
            
        } catch (Exception e) {
            log.error("Erro durante o envio da newsletter diaria", e);
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