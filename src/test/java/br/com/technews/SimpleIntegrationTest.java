package br.com.technews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = br.com.technews.TechnewsApplication.class)
@ActiveProfiles("test")
public class SimpleIntegrationTest {

    @Test
    void contextLoads() {
        // Este teste verifica se o contexto Spring Boot carrega corretamente
    }
}