package com.example.technews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = br.com.technews.TechnewsApplication.class)
@ActiveProfiles("test")
class TechnewsApplicationTests {

	@Test
	void contextLoads() {
	}

}
