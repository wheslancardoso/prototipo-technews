package br.com.technews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TechnewsApplication.class)
@ActiveProfiles("test")
class TechnewsApplicationTests {

	@Test
	void contextLoads() {
		// Este teste verifica se o contexto da aplicação carrega corretamente
	}

}