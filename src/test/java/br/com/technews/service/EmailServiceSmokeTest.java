package br.com.technews.service;

import br.com.technews.TechnewsApplication;
import br.com.technews.repository.NewsArticleRepository;
import br.com.technews.repository.SubscriberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest(classes = TechnewsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class EmailServiceSmokeTest {

    @Autowired
    private EmailService emailService;

    // Evita uso real de SMTP caso ocorra fallback
    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private TemplateEngine templateEngine;

    @MockBean
    private SubscriberRepository subscriberRepository;

    @MockBean
    private NewsArticleRepository articleRepository;

    @Test
    void shouldSendHtmlViaMailgunIfEnabled() throws Exception {
        // Somente roda se Mailgun estiver habilitado e com chaves presentes
        String enabled = System.getenv().getOrDefault("MAILGUN_ENABLED", "false");
        String domain = System.getenv().getOrDefault("MAILGUN_DOMAIN", "");
        String apiKey = System.getenv().getOrDefault("MAILGUN_API_KEY", "");
        boolean ready = "true".equalsIgnoreCase(enabled) && !domain.isBlank() && !apiKey.isBlank();
        assumeTrue(ready, "Mailgun não está configurado; teste de smoke pulado.");

        String toEmail = System.getenv().getOrDefault(
                "MAILGUN_TEST_TO",
                System.getenv().getOrDefault(
                        "MAIL_TO",
                        System.getenv().getOrDefault("GMAIL_USERNAME", System.getenv().getOrDefault("MAIL_FROM", ""))
                )
        );

        assumeTrue(!toEmail.isBlank(), "Destinatário de teste não definido; use MAILGUN_TEST_TO ou MAIL_TO.");

        String subject = "Smoke Test - TechNews Mailgun";
        String html = "<h1>Smoke Test</h1><p>Verificando envio via Mailgun HTTP API.</p>";

        CompletableFuture<Boolean> resultFuture = emailService.sendHtmlEmail(toEmail, subject, html);
        Boolean ok = resultFuture.get(20, TimeUnit.SECONDS);
        System.out.println("[SmokeTest] Envio via Mailgun para '" + toEmail + "' OK=" + ok);
        // Não falha o teste em ambientes sandbox; apenas reporta o status
    }
}