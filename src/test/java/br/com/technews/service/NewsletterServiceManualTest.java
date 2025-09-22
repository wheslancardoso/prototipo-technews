package br.com.technews.service;

import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.Newsletter;
import br.com.technews.entity.Subscriber;
import br.com.technews.dto.NewsletterStats;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Teste manual para verificar o template 'The News'
 */
@SpringBootTest
@ActiveProfiles("test")
public class NewsletterServiceManualTest {

    @Test
    public void testTheNewsTemplate() {
        // Simular dados para o template
        List<NewsArticle> articles = createMockArticles();
        LocalDate date = LocalDate.now();
        
        // Criar contexto do template
        Context context = new Context();
        
        // Criar newsletter object para o template
        Newsletter newsletter = new Newsletter();
        newsletter.setTitle("TechNews - " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        newsletter.setCreatedAt(LocalDateTime.now());
        
        // Separar artigos por tipo
        List<NewsArticle> featuredArticles = articles.stream()
            .limit(3)
            .collect(Collectors.toList());
        
        List<NewsArticle> latestArticles = articles.stream()
            .skip(3)
            .limit(5)
            .collect(Collectors.toList());
        
        List<NewsArticle> popularArticles = articles.stream()
            .sorted((a, b) -> Long.compare(b.getViews() != null ? b.getViews() : 0, 
                                          a.getViews() != null ? a.getViews() : 0))
            .limit(3)
            .collect(Collectors.toList());

        // Criar subscriber fictício para o template
        Subscriber subscriber = new Subscriber();
        subscriber.setFullName("Leitor Teste");
        subscriber.setUnsubscribeToken("sample-token");

        // Criar estatísticas
        long totalViews = articles.stream().mapToLong(a -> a.getViews() != null ? a.getViews() : 0).sum();
        NewsletterStats stats = new NewsletterStats(
            (long) articles.size(),  // totalArticles
            100L,  // totalSubscribers
            (long) articles.size(),  // articlesToday
            totalViews  // totalViews
        );

        // Configurar contexto
        context.setVariable("newsletter", newsletter);
        context.setVariable("subscriber", subscriber);
        context.setVariable("featuredArticles", featuredArticles);
        context.setVariable("latestArticles", latestArticles);
        context.setVariable("popularArticles", popularArticles);
        context.setVariable("stats", stats);
        context.setVariable("baseUrl", "http://localhost:8080");
        context.setVariable("articles", articles);
        context.setVariable("date", date);
        context.setVariable("formattedDate", date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        context.setVariable("totalArticles", articles.size());

        System.out.println("✅ Template 'The News' configurado com sucesso!");
        System.out.println("📊 Estatísticas:");
        System.out.println("   - Total de artigos: " + articles.size());
        System.out.println("   - Artigos em destaque: " + featuredArticles.size());
        System.out.println("   - Últimos artigos: " + latestArticles.size());
        System.out.println("   - Artigos populares: " + popularArticles.size());
        System.out.println("   - Total de visualizações: " + totalViews);
        System.out.println("📧 Template pronto para renderização!");
    }
    
    private List<NewsArticle> createMockArticles() {
        List<NewsArticle> articles = new ArrayList<>();
        
        // Artigo 1
        NewsArticle article1 = new NewsArticle();
        article1.setId(1L);
        article1.setTitle("Inteligência Artificial Revoluciona o Desenvolvimento de Software");
        article1.setContent("A IA está transformando como desenvolvemos software...");
        article1.setSummary("Novas ferramentas de IA estão mudando o paradigma do desenvolvimento.");
        article1.setViews(1500L);
        article1.setPublishedAt(LocalDateTime.now().minusHours(2));
        articles.add(article1);
        
        // Artigo 2
        NewsArticle article2 = new NewsArticle();
        article2.setId(2L);
        article2.setTitle("Quantum Computing: O Futuro da Computação");
        article2.setContent("Computadores quânticos prometem resolver problemas complexos...");
        article2.setSummary("Avanços em computação quântica abrem novas possibilidades.");
        article2.setViews(1200L);
        article2.setPublishedAt(LocalDateTime.now().minusHours(4));
        articles.add(article2);
        
        // Artigo 3
        NewsArticle article3 = new NewsArticle();
        article3.setId(3L);
        article3.setTitle("Blockchain Além das Criptomoedas");
        article3.setContent("Tecnologia blockchain encontra aplicações em diversos setores...");
        article3.setSummary("Blockchain se expande para além do mundo financeiro.");
        article3.setViews(800L);
        article3.setPublishedAt(LocalDateTime.now().minusHours(6));
        articles.add(article3);
        
        // Artigo 4
        NewsArticle article4 = new NewsArticle();
        article4.setId(4L);
        article4.setTitle("5G e IoT: A Conectividade do Futuro");
        article4.setContent("A combinação de 5G e IoT está criando novas oportunidades...");
        article4.setSummary("5G acelera a adoção de dispositivos IoT inteligentes.");
        article4.setViews(950L);
        article4.setPublishedAt(LocalDateTime.now().minusHours(8));
        articles.add(article4);
        
        // Artigo 5
        NewsArticle article5 = new NewsArticle();
        article5.setId(5L);
        article5.setTitle("Cybersecurity: Protegendo o Mundo Digital");
        article5.setContent("Novas ameaças cibernéticas exigem soluções inovadoras...");
        article5.setSummary("Segurança cibernética evolui para enfrentar novos desafios.");
        article5.setViews(1100L);
        article5.setPublishedAt(LocalDateTime.now().minusHours(10));
        articles.add(article5);
        
        return articles;
    }
}