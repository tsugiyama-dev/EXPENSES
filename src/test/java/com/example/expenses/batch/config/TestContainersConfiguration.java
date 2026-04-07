package com.example.expenses.batch.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
@TestConfiguration(proxyBeanMethods = false)
public class TestContainersConfiguration {

	@Bean
	@ServiceConnection
	static MySQLContainer<?> mysqlContainer() {
		return new MySQLContainer<>("mysql:8.0");
//				.withDatabaseName("testdb")
//				.withUsername("test")
//				.withPassword("test");
	}
}
