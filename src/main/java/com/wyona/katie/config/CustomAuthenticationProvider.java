package com.wyona.katie.config;

import com.wyona.katie.models.Username;
import com.wyona.katie.services.UsersXMLFileService;
import com.wyona.katie.models.User;
import com.wyona.katie.models.Username;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import com.wyona.katie.models.User;
import com.wyona.katie.services.IAMService;
import com.wyona.katie.services.XMLService;

/**
 *
 */
@Slf4j
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private IAMService iamService;

    /**
     *
     */
    public CustomAuthenticationProvider(IAMService iamService) {
        this.iamService = iamService;
    }

    /**
     * @see org.springframework.security.authentication.AuthenticationProvider#authenticate(Authentication)
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        log.info("Try to authenticate: " + authentication);

        log.info("Username: " + authentication.getName());
        // INFO: Do not log password
        //log.info("Password: " + authentication.getCredentials());
        log.info("Authorities: " + authentication.getAuthorities());
        log.info("Details: " + authentication.getDetails());
        log.info("Is authenticated: " + authentication.isAuthenticated());

        User user = null;
        try {
            user = iamService.getUserByUsername(new Username(authentication.getName()), true, true);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        if (user == null) {
            log.warn("No such user '" + authentication.getName() + "'!");
            throw new BadCredentialsException("Bad credentials!");
        }

        if (user.isLocked()) {
            log.warn("User '" + authentication.getName() + "' is locked!");
            throw new AccountStatusException("User '" + authentication.getName() + "' is locked!") {
                @Override
                public String getMessage() {
                    return super.getMessage();
                }
            };
        }

        // TODO: Check whether user is approved resp. when user account was created more than 48 hours ago and account is not approved, then throw Exception
        if (!user.isApproved()) {
            log.warn("User '" + authentication.getName() + "' is not approved!");
            throw new AccountStatusException("User '" + authentication.getName() + "' is not approved!") {
                @Override
                public String getMessage() {
                    return super.getMessage();
                }
            };
        }

        Object password = authentication.getCredentials();
        if (password != null) {
            if (!iamService.matches(password.toString(), user)) {
                log.warn("Password does not match!");
                throw new BadCredentialsException("Bad credentials!");
            } else {
                log.info("Password is correct.");
            }
        } else {
            // IMPORTANT TODO: Do not allow null password, whereas see com/wyona/katie/controllers/AuthenticationController.java#getUsermame()
            log.info("No password, assuming remember me cookie is valid or JWT is valid or Slack signature or ...");
        }

        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<GrantedAuthority>();
        // INFO: https://docs.spring.io/spring-security/site/docs/4.2.1.RELEASE/reference/htmlsingle/#appendix-faq-role-prefix
        String role = "ROLE_" + user.getRole();
        log.info("Grant authority: " + role);
        authorities.add(new SimpleGrantedAuthority(role));

        return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(), password, authorities);
    }

    /**
     *
     */
    @Override
    public boolean supports(Class<?> authentication) {
        log.info("Supports authentication object: " + authentication);
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
