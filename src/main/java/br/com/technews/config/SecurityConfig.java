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
                // Recursos estáticos primeiro
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                // Endpoints públicos da API - DEVE VIR ANTES das páginas web
                .requestMatchers("/api/newsletter/subscribe", "/api/newsletter/status/**", 
                               "/api/newsletter/unsubscribe/**", "/api/newsletter/reactivate",
                               "/api/newsletter/verify", "/api/newsletter/preferences/**",
                               "/api/news/**").permitAll()
                // Endpoints administrativos da API - requerem autenticação ADMIN
                .requestMatchers("/api/newsletter/subscribers", "/api/newsletter/stats", 
                               "/api/newsletter/send", "/api/newsletter/templates/**",
                               "/api/newsletter/schedule/**").hasRole("ADMIN")
                // Páginas públicas
                .requestMatchers("/", "/subscribe", "/unsubscribe/**", "/verify/**").permitAll()
                // Newsletter páginas públicas
                .requestMatchers("/newsletter/**").permitAll()
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
            )
            .headers(headers -> headers.frameOptions().disable());
        
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123")) // Gera hash dinamicamente
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}