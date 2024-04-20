package com.wyona.katie.config;

import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Bean;

import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

import static org.springframework.http.HttpMethod.*;

import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.security.authentication.AuthenticationManager;

import com.wyona.katie.services.IAMService;
//import com.wyona.katie.security.RestAuthenticationEntryPoint;

/**
 * See https://www.baeldung.com/spring-security-expressions
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";

    @Autowired
    private IAMService iamService;

    @Bean
    CorsFilter corsFilter() {
        CorsFilter filter = new CorsFilter();
        return filter;
    }

    /**
     * @return filter adding a Content Security Policy with dynamically generated nonce
     */
    @Bean
    CSPNonceFilter cspNonceFilter() {
        CSPNonceFilter filter = new CSPNonceFilter();
        return filter;
    }

    /**
     *
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.info("Configure / set access permissions ...");

        http.addFilterBefore(corsFilter(), SessionManagementFilter.class); // https://stackoverflow.com/questions/40286549/spring-boot-security-cors
        http.addFilterBefore(cspNonceFilter(), SessionManagementFilter.class);

        // TODO: Reconsider enabling CSRF, whereas see https://portswigger.net/web-security/csrf
        http.csrf().disable(); // INFO: Disabled, because otherwise one cannot use protected interfaces via Swagger even when signed in via Swagger, whereas see https://stackoverflow.com/questions/38004035/could-not-verify-the-provided-csrf-token-because-your-session-was-not-found-in-s

        // INFO: Disable X-Frame-Options (https://developer.mozilla.org/de/docs/Web/HTTP/Headers/X-Frame-Options) such that iframe requeests are not blocked
        http.headers().frameOptions().disable();

        //http.authorizeRequests().anyRequest().permitAll();

        http.authorizeRequests()
                //.antMatchers("/h2-console/**").permitAll()
                .antMatchers(PUT, "/api/v2/faq/qna").authenticated()
                .antMatchers(GET, "/api/v1/submitQuestionToExpert").hasAnyRole(USER_ROLE, ADMIN_ROLE)
                .antMatchers(GET, "/api/v1/communication/process-emails").hasRole(ADMIN_ROLE)
                .antMatchers(PUT, "/api/v1/configuration/domain/*/message-body-hostname").authenticated()
                .antMatchers(GET, "/api/v1/summary/current").hasRole(ADMIN_ROLE)
                .antMatchers(GET, "/api/v1/question/resubmitted/*").hasAnyRole(USER_ROLE, ADMIN_ROLE)
                .antMatchers(PUT, "/api/v1/question/resubmitted/*").hasAnyRole(USER_ROLE, ADMIN_ROLE)
                .antMatchers(DELETE, "/api/v1/question/resubmitted/*").hasAnyRole(USER_ROLE, ADMIN_ROLE)
                .antMatchers(GET, "/api/v1/question/resubmitted/*/sendAnswer").hasAnyRole(USER_ROLE, ADMIN_ROLE)
                .antMatchers(GET, "/api/v1/question/resubmitted/*/train").hasAnyRole(USER_ROLE, ADMIN_ROLE)
                .antMatchers(POST, "/tmp-test/push-notification/topic").hasRole(ADMIN_ROLE)
                .antMatchers(POST, "/tmp-test/push-notification/token").hasRole(ADMIN_ROLE)
                .antMatchers(POST, "/tmp-test/push-notification/data").hasRole(ADMIN_ROLE)
                .antMatchers(GET, "/tmp-test/push-notification/sample").hasRole(ADMIN_ROLE)
                //.antMatchers(GET, "/**").permitAll()
                //.antMatchers(PUT, "/**").permitAll()
                //.antMatchers(POST, "/**").permitAll()
                //.antMatchers(PATCH, "/**").permitAll() // TODO: Check permissions
                //.antMatchers(DELETE, "/**").permitAll() // TODO: Check permissions
                //.anyRequest().permitAll() // TODO: Replace everything else by this line
                .and()
                .logout() // https://www.baeldung.com/spring-security-logout
                    .deleteCookies("JSESSIONID")
                    .invalidateHttpSession(true);
    }

    /**
     *
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {

        auth.authenticationProvider(new CustomAuthenticationProvider(iamService));

/*
        log.info("Use in memory authentication provider ...");
        User[] users = iamService.getUsers(true, true);
        for (int i = 0; i < users.length; i++) {
            auth.inMemoryAuthentication().withUser(users[i].getUsername()).password("{noop}" + users[i].getPlaintextPassword()).roles(users[i].getRole());
        }
*/
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        log.info("Set authentication manager ...");
        return super.authenticationManagerBean();
    }
}
