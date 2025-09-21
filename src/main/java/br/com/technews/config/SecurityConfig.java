package br.com.technews.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/subscribe", "/unsubscribe/**", "/verify/**", 
                               "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Endpoints públicos da API
                .requestMatchers("/api/newsletter/subscribe", "/api/newsletter/status/**", 
                               "/api/newsletter/unsubscribe/**", "/api/newsletter/reactivate",
                               "/api/newsletter/verify", "/api/news/**").permitAll()
                // Endpoints administrativos da API - requerem autenticação ADMIN
                .requestMatchers("/api/newsletter/subscribers", "/api/newsletter/stats", 
                               "/api/newsletter/send", "/api/newsletter/templates/**",
                               "/api/newsletter/schedule/**").hasRole("ADMIN")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/newsletter/subscribe", "/api/newsletter/unsubscribe/**")
            )
            .headers(headers -> headers.frameOptions().disable());
        
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password("$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM1JiOV7M0OKu9o4.jTW") // admin123
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}