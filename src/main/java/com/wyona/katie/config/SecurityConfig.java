package com.wyona.katie.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.SessionManagementFilter;

import static org.springframework.http.HttpMethod.*;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final String ADMIN_ROLE = "ADMIN";
    //private static final String USER_ROLE = "USER";

    @Bean
    CorsFilter corsFilter() {
        return new CorsFilter();
    }

    /**
     * @return filter adding a Content Security Policy with dynamically generated nonce
     */
    @Bean
    CSPNonceFilter cspNonceFilter() {
        return new CSPNonceFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configure / set access permissions ...");

        http.addFilterBefore(corsFilter(), SessionManagementFilter.class); // https://stackoverflow.com/questions/40286549/spring-boot-security-cors
        http.addFilterBefore(cspNonceFilter(), SessionManagementFilter.class);

        // TODO: Reconsider enabling CSRF, whereas see https://portswigger.net/web-security/csrf
        // Disable CSRF for Swagger etc. because otherwise one cannot use protected interfaces via Swagger even when signed in via Swagger, whereas see https://stackoverflow.com/questions/38004035/could-not-verify-the-provided-csrf-token-because-your-session-was-not-found-in-s
        http.csrf(csrf -> csrf.disable());

        // Disable X-Frame-Options (https://developer.mozilla.org/de/docs/Web/HTTP/Headers/X-Frame-Options) such that iframe requeests are not blocked
        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        // Authorization configuration, also see for example https://www.baeldung.com/spring-security-expressions
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(POST, "/tmp-test/push-notification/topic").hasRole(ADMIN_ROLE)
                        .requestMatchers(POST, "/tmp-test/push-notification/token").hasRole(ADMIN_ROLE)
                        .requestMatchers(POST, "/tmp-test/push-notification/data").hasRole(ADMIN_ROLE)
                        .requestMatchers(GET, "/tmp-test/push-notification/sample").hasRole(ADMIN_ROLE)
                .anyRequest().permitAll()
        );

        // https://www.baeldung.com/spring-security-logout
        http.logout(logout -> logout
                .deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
        );

        return http.build();
    }
}
