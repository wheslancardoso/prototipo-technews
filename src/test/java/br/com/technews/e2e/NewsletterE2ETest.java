package br.com.technews.e2e;

import br.com.technews.entity.Subscriber;
import br.com.technews.entity.Category;
import br.com.technews.entity.NewsArticle;
import br.com.technews.entity.ArticleStatus;
import br.com.technews.repository.SubscriberRepository;
import br.com.technews.repository.CategoryRepository;
import br.com.technews.repository.NewsArticleRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/**
 * Testes End-to-End (E2E) para simular jornadas completas do usuário
 * Testa cenários reais de uso do sistema de newsletter
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class NewsletterE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private NewsArticleRepository articleRepository;

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

        // Criar categorias
        techCategory = createCategory("Technology", "technology", "Tech news and updates");
        businessCategory = createCategory("Business", "business", "Business news and insights");

        // Mock do EmailService
        when(emailService.sendVerificationEmail(any(Subscriber.class)))
                .thenReturn(CompletableFuture.completedFuture(true));
        when(emailService.sendWelcomeEmail(any(Subscriber.class)))
                .thenReturn(CompletableFuture.completedFuture(true));
    }

    @Test
    void shouldCompleteFullUserJourneyFromSubscriptionToUnsubscribe() throws Exception {
        // CENÁRIO: Usuário se inscreve, verifica email, atualiza preferências e cancela inscrição
        
        // 1. INSCRIÇÃO INICIAL
        String subscribeRequest = """
            {
                "email": "journey@example.com",
                "nome": "Journey User",
                "frequencia": "WEEKLY"
            }
            """;

        MvcResult subscribeResult = mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.verificationRequired").value(true))
                .andReturn();

        // Verificar que subscriber foi criado
        Optional<Subscriber> subscriberOpt = subscriberRepository.findByEmail("journey@example.com");
        assertThat(subscriberOpt).isPresent();
        Subscriber subscriber = subscriberOpt.get();
        assertThat(subscriber.getActive()).isTrue();
        assertThat(subscriber.isEmailVerified()).isFalse();

        // 2. VERIFICAÇÃO DE STATUS ANTES DA CONFIRMAÇÃO
        mockMvc.perform(get("/api/newsletter/status/journey@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.frequency").value("WEEKLY"));

        // 3. SIMULAÇÃO DE VERIFICAÇÃO DE EMAIL
        subscriber.setEmailVerified(true);
        subscriber.setVerificationToken(null);
        subscriberRepository.save(subscriber);

        // 4. VERIFICAÇÃO DE STATUS APÓS CONFIRMAÇÃO
        mockMvc.perform(get("/api/newsletter/status/journey@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        // 5. ATUALIZAÇÃO DE PREFERÊNCIAS
        String preferencesRequest = """
            {
                "frequencia": "DAILY",
                "categorias": [%d, %d]
            }
            """.formatted(techCategory.getId(), businessCategory.getId());

        mockMvc.perform(put("/api/newsletter/preferences/journey@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferencesRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verificar se preferências foram atualizadas
        subscriber = subscriberRepository.findByEmail("journey@example.com").get();
        assertThat(subscriber.getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.DAILY);
        assertThat(subscriber.getSubscribedCategories()).hasSize(2);

        // 6. CANCELAMENTO DE INSCRIÇÃO
        mockMvc.perform(delete("/api/newsletter/unsubscribe/journey@example.com")
                .with(csrf())
                .param("token", subscriber.getManageToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verificar que subscriber foi desativado
        subscriber = subscriberRepository.findByEmail("journey@example.com").get();
        assertThat(subscriber.getActive()).isFalse();

        // 7. TENTATIVA DE REATIVAÇÃO
        mockMvc.perform(post("/api/newsletter/reactivate")
                .with(csrf())
                .param("email", "journey@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verificar reativação
        subscriber = subscriberRepository.findByEmail("journey@example.com").get();
        assertThat(subscriber.getActive()).isTrue();
    }

    @Test
    void shouldHandleMultipleUsersWithDifferentPreferences() throws Exception {
        // CENÁRIO: Múltiplos usuários com diferentes preferências
        
        // 1. USUÁRIO TECH ENTHUSIAST
        String techUserRequest = """
            {
                "email": "tech@example.com",
                "nome": "Tech Enthusiast",
                "frequencia": "DAILY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(techUserRequest))
                .andExpect(status().isCreated());

        // Configurar preferências para tecnologia
        String techPreferences = """
            {
                "frequencia": "DAILY",
                "categorias": [%d]
            }
            """.formatted(techCategory.getId());

        mockMvc.perform(put("/api/newsletter/preferences/tech@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(techPreferences))
                .andExpect(status().isOk());

        // 2. USUÁRIO BUSINESS FOCUSED
        String businessUserRequest = """
            {
                "email": "business@example.com",
                "nome": "Business Professional",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(businessUserRequest))
                .andExpect(status().isCreated());

        // Configurar preferências para negócios
        String businessPreferences = """
            {
                "frequencia": "WEEKLY",
                "categorias": [%d]
            }
            """.formatted(businessCategory.getId());

        mockMvc.perform(put("/api/newsletter/preferences/business@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(businessPreferences))
                .andExpect(status().isOk());

        // 3. VERIFICAR QUE AMBOS OS USUÁRIOS EXISTEM COM PREFERÊNCIAS CORRETAS
        List<Subscriber> allSubscribers = subscriberRepository.findAll();
        assertThat(allSubscribers).hasSize(2);

        Subscriber techUser = subscriberRepository.findByEmail("tech@example.com").get();
        assertThat(techUser.getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.DAILY);
        assertThat(techUser.getSubscribedCategories()).contains(techCategory);

        Subscriber businessUser = subscriberRepository.findByEmail("business@example.com").get();
        assertThat(businessUser.getFrequency()).isEqualTo(Subscriber.SubscriptionFrequency.WEEKLY);
        assertThat(businessUser.getSubscribedCategories()).contains(businessCategory);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCompleteAdminWorkflowFromContentToNewsletter() throws Exception {
        // CENÁRIO: Admin cria conteúdo e envia newsletter
        
        // 1. CRIAR SUBSCRIBERS PARA TESTE
        Subscriber subscriber1 = createTestSubscriber("admin1@example.com", "Admin User 1");
        Subscriber subscriber2 = createTestSubscriber("admin2@example.com", "Admin User 2");
        subscriberRepository.saveAll(List.of(subscriber1, subscriber2));

        // 2. CRIAR ARTIGOS DE CONTEÚDO
        NewsArticle techArticle = createTestArticle("Breaking Tech News", techCategory);
        NewsArticle businessArticle = createTestArticle("Market Update", businessCategory);
        articleRepository.saveAll(List.of(techArticle, businessArticle));

        // 3. VERIFICAR ESTATÍSTICAS ANTES DO ENVIO
        mockMvc.perform(get("/api/newsletter/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.totalSubscribers").value(2))
                .andExpect(jsonPath("$.stats.activeSubscribers").value(2));

        // 4. LISTAR SUBSCRIBERS
        mockMvc.perform(get("/api/newsletter/subscribers")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.totalElements").value(2));

        // 5. ENVIAR NEWSLETTER (pode falhar por configuração, mas deve ser autorizado)
        mockMvc.perform(post("/api/newsletter/send")
                .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Deve ser autorizado (não 401 ou 403)
                    assertThat(status).isNotIn(401, 403);
                });
    }

    @Test
    void shouldHandleErrorScenariosGracefully() throws Exception {
        // CENÁRIO: Tratamento de erros e casos extremos
        
        // 1. TENTATIVA DE INSCRIÇÃO COM EMAIL INVÁLIDO
        String invalidEmailRequest = """
            {
                "email": "invalid-email",
                "nome": "Invalid User",
                "frequencia": "WEEKLY"
            }
            """;

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEmailRequest))
                .andExpect(status().isBadRequest());

        // 2. TENTATIVA DE DUPLA INSCRIÇÃO
        String validRequest = """
            {
                "email": "duplicate@example.com",
                "nome": "Duplicate User",
                "frequencia": "WEEKLY"
            }
            """;

        // Primeira inscrição
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
                .andExpect(status().isCreated());

        // Segunda inscrição (deve falhar)
        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest))
                .andExpect(status().isBadRequest());

        // 3. TENTATIVA DE CANCELAR INSCRIÇÃO INEXISTENTE
        mockMvc.perform(delete("/api/newsletter/unsubscribe/nonexistent@example.com")
                .with(csrf())
                .param("token", "invalid-token"))
                .andExpect(status().isBadRequest());

        // 4. VERIFICAÇÃO DE STATUS DE EMAIL INEXISTENTE
        mockMvc.perform(get("/api/newsletter/status/nonexistent@example.com"))
                .andExpect(status().isNotFound());

        // 5. ATUALIZAÇÃO DE PREFERÊNCIAS DE USUÁRIO INEXISTENTE
        String preferencesRequest = """
            {
                "frequencia": "DAILY",
                "categorias": []
            }
            """;

        mockMvc.perform(put("/api/newsletter/preferences/nonexistent@example.com")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(preferencesRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldMaintainDataConsistencyThroughoutUserJourney() throws Exception {
        // CENÁRIO: Verificar consistência de dados durante toda a jornada
        
        String email = "consistency@example.com";
        
        // 1. INSCRIÇÃO
        String subscribeRequest = """
            {
                "email": "%s",
                "nome": "Consistency User",
                "frequencia": "WEEKLY"
            }
            """.formatted(email);

        mockMvc.perform(post("/api/newsletter/subscribe")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(subscribeRequest))
                .andExpect(status().isCreated());

        // Verificar dados iniciais
        Subscriber subscriber = subscriberRepository.findByEmail(email).get();
        assertThat(subscriber.getSubscribedAt()).isNotNull();
        assertThat(subscriber.getManageToken()).isNotNull();
        assertThat(subscriber.getVerificationToken()).isNotNull();

        // 2. MÚLTIPLAS ATUALIZAÇÕES DE PREFERÊNCIAS
        for (int i = 0; i < 3; i++) {
            String frequency = i % 2 == 0 ? "DAILY" : "WEEKLY";
            String preferencesRequest = """
                {
                    "frequencia": "%s",
                    "categorias": [%d]
                }
                """.formatted(frequency, techCategory.getId());

            mockMvc.perform(put("/api/newsletter/preferences/" + email)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(preferencesRequest))
                    .andExpect(status().isOk());

            // Verificar que dados permanecem consistentes
            subscriber = subscriberRepository.findByEmail(email).get();
            assertThat(subscriber.getSubscribedAt()).isNotNull();
            assertThat(subscriber.getManageToken()).isNotNull();
            assertThat(subscriber.getEmail()).isEqualTo(email);
        }

        // 3. CANCELAMENTO E REATIVAÇÃO
        mockMvc.perform(delete("/api/newsletter/unsubscribe/" + email)
                .with(csrf())
                .param("token", subscriber.getManageToken()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/newsletter/reactivate")
                .with(csrf())
                .param("email", email))
                .andExpect(status().isOk());

        // Verificar que dados essenciais permanecem
        subscriber = subscriberRepository.findByEmail(email).get();
        assertThat(subscriber.getSubscribedAt()).isNotNull();
        assertThat(subscriber.getManageToken()).isNotNull();
        assertThat(subscriber.getEmail()).isEqualTo(email);
        assertThat(subscriber.getActive()).isTrue();
    }

    // Métodos auxiliares
    private Category createCategory(String name, String slug, String description) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        return categoryRepository.save(category);
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