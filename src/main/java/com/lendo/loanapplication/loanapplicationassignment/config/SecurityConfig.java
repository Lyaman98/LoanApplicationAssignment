package com.lendo.loanapplication.loanapplicationassignment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Customers and lenders are separate roles, so a lender cannot accept offers on a customer's behalf.
 *
 * <p>HTTP Basic over two in-memory demo users was chosen for simplicity. The rules about who may do
 * what are the part that matters, and they stay the same if the mechanism becomes JWT later.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/offers/*/accept")
                        .hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications/*/offers")
                        .hasRole("LENDER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/applications")
                        .hasRole("CUSTOMER")
                        .anyRequest()
                        .authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /** Demo users, so the API can be tried end to end. . */
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("customer")
                        .password(passwordEncoder.encode("password"))
                        .roles("CUSTOMER")
                        .build(),
                User.withUsername("lender")
                        .password(passwordEncoder.encode("password"))
                        .roles("LENDER")
                        .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
