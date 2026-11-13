package com.senai.cti_insights;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Este teste sobe o Spring inteiro e por isso precisa do banco.
// Só roda quando DB_USER está definido; sem banco, o "mvn package" passa normalmente.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_USER", matches = ".+")
class CtiInsightsApplicationTests {

	@Test
	void contextLoads() {
	}

}
