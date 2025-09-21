package br.com.technews;

import br.com.technews.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = br.com.technews.TechnewsApplication.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class SimpleIntegrationTest {

    @Test
    void contextLoads() {
        // Este teste verifica se o contexto Spring Boot carrega corretamente
    }
}