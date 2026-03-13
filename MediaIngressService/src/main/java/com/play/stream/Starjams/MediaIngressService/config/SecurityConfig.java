package com.play.stream.Starjams.MediaIngressService.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Secures admin endpoints with HTTP Basic authentication.
 *
 * <p>All {@code /api/v1/admin/**} routes require the ADMIN role.
 * All other routes (stream start, watch, end, heartbeat) are open;
 * they rely on the {@code X-User-Id} header for user identity
 * following the existing platform pattern.
 *
 * <p>Credentials are set via:
 * <ul>
 *   <li>Environment: {@code ADMIN_USERNAME} / {@code ADMIN_PASSWORD}</li>
 *   <li>Config: {@code admin.username} / {@code admin.password}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${admin.username:admin}")
    private String adminUsername;

    @Value("${admin.password:changeme}")
    private String adminPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        @SuppressWarnings("deprecation")
        UserDetails admin = User.withDefaultPasswordEncoder()
            .username(adminUsername)
            .password(adminPassword)
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
