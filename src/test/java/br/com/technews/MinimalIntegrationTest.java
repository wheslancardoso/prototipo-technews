package br.com.technews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = br.com.technews.TechnewsApplication.class, 
                webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class MinimalIntegrationTest {

    @Test
    void contextLoads() {
        // Teste mínimo para verificar se o contexto carrega
    }
}