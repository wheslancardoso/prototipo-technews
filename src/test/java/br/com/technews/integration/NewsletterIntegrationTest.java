package br.com.technews.integration;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.repository.SubscriberRepository;
import br.com.technews.repository.CategoryRepository;
import br.com.technews.repository.NewsArticleRepository;
import br.com.technews.service.SubscriberService;
import br.com.technews.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Testes de integração para o sistema de newsletter
 * Testa fluxos completos entre camadas (Controller -> Service -> Repository)
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class NewsletterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private NewsArticleRepository articleRepository;

    @Autowired
    private SubscriberService subscriberService;

    @MockBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private Category techCategory;
    private Category businessCategory;

    @BeforeEach
    void setUp() {
        // Limpar dados de teste
        subscriberRepository.deleteAll();
        articleRepository.deleteAll();
        categoryRepository.deleteAll();

        // Criar categorias de teste
        techCategory = new Category();
        techCategory.setName("Technology");
        techCategory.setSlug("technology");
        techCategory.setDescription("Tech news and updates");
        techCategory = categoryRepository.save(techCategory);

        businessCategory = new Category();
        businessCategory.setName("Business");
        businessCategory.setSlug("business");
        businessCategory.setDescription("Business news and insights");
        businessCategory = categoryRepository.save(businessCategory);

        // Mock do EmailService
        when(emailService.sendVerificationEmail(any(Subscriber.class))).thenReturn(CompletableFuture.completedFuture(true));
        when(emailService.sendWelcomeEmail(any(Subscriber.class))).thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    void shouldCompleteFullSubscriptionFlow() throws Exception {
        // 1. Inscrever novo usuário
        String subscribeRequest = """
            {
                "email": "test@example.com",
                "nome": "Test User",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscrição realizada com sucesso. Verifique seu email para confirmar."))
                .andExpect(jsonPath("$.verificationRequired").value(true));

        // 2. Verificar se subscriber foi criado no banco
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmail("test@example.com");
        assertThat(subscriberOpt).isPresent();
        
        Subscriber subscriber = subscriberOpt.get();
        assertThat(subscriber.getFullName()).isEqualTo("Test User");
        assertThat(subscriber.getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.WEEKLY);
        assertThat(subscriber.getActive()).isTrue();
        assertThat(subscriber.isEmailVerified()).isFalse();

        // 3. Verificar status da inscrição
        mockMvc.perform(get("/api/newsletter/status/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"));

        // 4. Simular verificação de email
        subscriber.setEmailVerified(true);
        subscriber.setVerificationToken(null);
        subscriberRepository.save(subscriber);

        // 5. Verificar status após verificação
        mockMvc.perform(get("/api/newsletter/status/test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void shouldHandleSubscriptionPreferencesFlow() throws Exception {
        // 1. Criar subscriber
        Subscriber subscriber = createTestSubscriber("preferences@example.com", "Preferences User");
        subscriber = subscriberRepository.save(subscriber);

        // 2. Atualizar preferências
        String preferencesRequest = """
            {
                "frequencia": "DAILY",
                "categorias": [%d, %d]
            }
            """.formatted(techCategory.getId(), businessCategory.getId());

        mockMvc.perform(put("/api/newsletter/preferences/preferences@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferencesRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Preferências atualizadas com sucesso"));

        // 3. Verificar se preferências foram salvas
        Optional<Subscriber> updatedSubscriber = subscriberRepository.findByEmail("preferences@example.com");
        assertThat(updatedSubscriber).isPresent();
        assertThat(updatedSubscriber.get().getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.DAILY);
        assertThat(updatedSubscriber.get().getSubscribedCategories()).hasSize(2);
    }

    @Test
    void shouldHandleUnsubscribeFlow() throws Exception {
        // 1. Criar subscriber ativo
        Subscriber subscriber = createTestSubscriber("unsubscribe@example.com", "Unsubscribe User");
        subscriber.setManageToken("manage-token-123");
        subscriber = subscriberRepository.save(subscriber);

        // 2. Cancelar inscrição
        mockMvc.perform(delete("/api/newsletter/unsubscribe/unsubscribe@example.com")
                .with(csrf())
                .param("token", "manage-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscrição cancelada com sucesso"));

        // 3. Verificar se subscriber foi desativado
        Optional<Subscriber> unsubscribedUser = subscriberRepository.findByEmail("unsubscribe@example.com");
        assertThat(unsubscribedUser).isPresent();
        assertThat(unsubscribedUser.get().getActive()).isFalse();

        // 4. Verificar status após cancelamento
        mockMvc.perform(get("/api/newsletter/status/unsubscribe@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void shouldHandleReactivationFlow() throws Exception {
        // 1. Criar subscriber inativo
        Subscriber subscriber = createTestSubscriber("reactivate@example.com", "Reactivate User");
        subscriber.setActive(false);
        subscriber = subscriberRepository.save(subscriber);

        // 2. Reativar inscrição
        mockMvc.perform(post("/api/newsletter/reactivate")
                .with(csrf())
                .param("email", "reactivate@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscrição reativada com sucesso"));

        // 3. Verificar se subscriber foi reativado
        Optional<Subscriber> reactivatedUser = subscriberRepository.findByEmail("reactivate@example.com");
        assertThat(reactivatedUser).isPresent();
        assertThat(reactivatedUser.get().getActive()).isTrue();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleAdminSubscriberListingFlow() throws Exception {
        // 1. Criar múltiplos subscribers
        Subscriber subscriber1 = createTestSubscriber("admin1@example.com", "Admin User 1");
        Subscriber subscriber2 = createTestSubscriber("admin2@example.com", "Admin User 2");
        Subscriber subscriber3 = createTestSubscriber("admin3@example.com", "Admin User 3");
        subscriber3.setActive(false);

        subscriberRepository.saveAll(List.of(subscriber1, subscriber2, subscriber3));

        // 2. Listar todos os subscribers com paginação
        mockMvc.perform(get("/api/newsletter/subscribers")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.subscribers").isArray())
                .andExpect(jsonPath("$.totalElements").value(3));

        // 3. Filtrar apenas subscribers ativos
        mockMvc.perform(get("/api/newsletter/subscribers")
                .param("page", "0")
                .param("size", "10")
                .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleNewsletterStatisticsFlow() throws Exception {
        // 1. Criar subscribers com diferentes status
        Subscriber activeVerified = createTestSubscriber("active@example.com", "Active User");
        activeVerified.setEmailVerified(true);

        Subscriber activeUnverified = createTestSubscriber("unverified@example.com", "Unverified User");
        activeUnverified.setEmailVerified(false);

        Subscriber inactive = createTestSubscriber("inactive@example.com", "Inactive User");
        inactive.setActive(false);

        subscriberRepository.saveAll(List.of(activeVerified, activeUnverified, inactive));

        // 2. Obter estatísticas
        mockMvc.perform(get("/api/newsletter/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.totalSubscribers").value(3))
                .andExpect(jsonPath("$.stats.activeSubscribers").value(2))
                .andExpect(jsonPath("$.stats.verifiedSubscribers").value(1));
    }

    @Test
    void shouldHandleArticleAndCategoryIntegration() throws Exception {
        // 1. Criar artigos em diferentes categorias
        NewsArticle techArticle = createTestArticle("Tech News", techCategory);
        NewsArticle businessArticle = createTestArticle("Business Update", businessCategory);
        articleRepository.saveAll(List.of(techArticle, businessArticle));

        // 2. Criar subscriber interessado apenas em tecnologia
        Subscriber techSubscriber = createTestSubscriber("tech@example.com", "Tech Enthusiast");
        techSubscriber.getSubscribedCategories().add(techCategory);
        subscriberRepository.save(techSubscriber);

        // 3. Verificar se artigos são filtrados corretamente por categoria
        List<NewsArticle> techArticles = articleRepository.findByStatus(ArticleStatus.PUBLICADO);
        List<NewsArticle> filteredTechArticles = techArticles.stream()
                .filter(article -> article.getCategoryEntity() != null && 
                        article.getCategoryEntity().equals(techCategory))
                .toList();
        assertThat(filteredTechArticles).hasSize(1);
        assertThat(filteredTechArticles.get(0).getTitle()).isEqualTo("Tech News");

        // 4. Verificar se subscriber tem as categorias corretas
        Optional<Subscriber> savedSubscriber = subscriberRepository.findByEmail("tech@example.com");
        assertThat(savedSubscriber).isPresent();
        assertThat(savedSubscriber.get().getSubscribedCategories()).contains(techCategory);
    }

    private Subscriber createTestSubscriber(String email, String name) {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(email);
        subscriber.setFullName(name);
        subscriber.setActive(true);
        subscriber.setEmailVerified(true);
        subscriber.setFrequency(Subscriber.SubscriptionFrequency.WEEKLY);
        subscriber.setSubscribedAt(LocalDateTime.now());
        subscriber.setManageToken("token-" + email.hashCode());
        return subscriber;
    }

    private NewsArticle createTestArticle(String title, Category category) {
        NewsArticle article = new NewsArticle();
        article.setTitle(title);
        article.setContent("Test content for " + title);
        article.setSummary("Test summary");
        article.setCategoryEntity(category);
        article.setStatus(ArticleStatus.PUBLICADO);
        article.setPublishedAt(LocalDateTime.now());
        article.setCreatedAt(LocalDateTime.now());
        article.setUpdatedAt(LocalDateTime.now());
        return article;
    }
}