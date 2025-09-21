package br.com.technews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = br.com.technews.TechnewsApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SimpleIntegrationTest {

    @Test
    public void contextLoads() {
        // Este teste verifica se o contexto Spring Boot carrega corretamente
        // Se chegou até aqui, o contexto foi carregado com sucesso
    }
}