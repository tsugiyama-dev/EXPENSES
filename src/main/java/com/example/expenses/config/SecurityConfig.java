package com.example.expenses.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomSuccessHandler successHandler;
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception  {
		
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST,"/expenses/*/approve", "/expenses/*/reject")
				.hasRole("APPROVER")
				.requestMatchers(
						"/static/**",
						"/login",
						"/health",
						"/swagger-ui",
						"/v3/api-docs/**",
						"/register/**",
						"/expenses/**"
						).permitAll()
				.anyRequest().authenticated()
				)
		.formLogin(
				f -> f
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.successHandler(successHandler))
//				.defaultSuccessUrl("/expenses", true))
		.logout(
				logout -> logout 
				.logoutSuccessUrl("/login")
				.logoutUrl("/logout")) 
//		.csrf(t -> t.disable())
		.httpBasic(Customizer.withDefaults());
		
		return http.build();
	}
	
	@Bean
	PasswordEncoder passwordEncorder() {
		return new BCryptPasswordEncoder();
	}
}
