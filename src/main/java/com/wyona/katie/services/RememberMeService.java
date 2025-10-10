package com.wyona.katie.services;

import com.wyona.katie.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class in order to support auto login
 */
@Slf4j
@Component
public class RememberMeService {

    @Autowired
    DataRepositoryService dataRepoService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private IAMService iamService;

    private static final String EMAIL_COOKIE_NAME = "KATIEEMAIL";
    private static final String AUTO_LOGIN_COOKIE_NAME = "KATIEAUTOLOGIN";
    private static final String SEP = "___";
    
    private static final String EXPIRES_FORMAT = "yyyyMMdd.HH.mm.ss";
    private static final SimpleDateFormat expiresSdf = new SimpleDateFormat(EXPIRES_FORMAT);
    
/*
With the following two parameters you can define after what time a token expires.
However: this expiry date is only verified and updated if the user starts a new session.
Means: if the session timeout is 4h and you configure here 30min, the cookie token won't be replaced within the session.
*/
    // TODO: Make configurable
    private static final int TOKEN_EXPIRY_UNIT = Calendar.DAY_OF_MONTH;
    //private static final int TOKEN_EXPIRY_UNIT = Calendar.MINUTE;
    private static final int TOKEN_EXPIRY_AMOUNT = 30;

    /**
     * Get cookie path based on context
     */
    private String getPath(HttpServletRequest request) {
        String path = "/";
        if (request.getContextPath().length() > 0) {
            path = request.getContextPath();
        }
        return path;
    }

    /**
     * Remember email address provided by user (without being signed in). One can retrieve it again using getEmail(HttpServletRequest)
     * @param domainId Domain Id for which user provided email address
     */
    public void rememberEmail(String email, HttpServletRequest request, HttpServletResponse response, String domainId) {
        log.info("Remember email '" + email + "' ...");
        Cookie cookie = getEmailCookie(request);
        if (cookie != null) {
            log.info("E-Mail of user: " + cookie.getValue());
            if (!email.equals(cookie.getValue())) {
                log.info("Email contained by cookie '" + cookie.getValue() + "' does not match with submitted email '" + email + "', therefore update cookie ...");
                setCookie(request, response, EMAIL_COOKIE_NAME, email);
                shareEmailWithThirdParty(email, domainId);
            } else {
                // INFO: Cookie to remember email already exists
            }
        } else {
            log.info("Set cookie to remember email ...");
            setCookie(request, response, EMAIL_COOKIE_NAME, email);
            shareEmailWithThirdParty(email, domainId);
        }
    }

    /**
     * Save email, e.g. for marketing campaigns
     */
    private void shareEmailWithThirdParty(String email, String domainId) {
        log.info("TODO: Share email with third party, e.g. MailChimp");
        // TODO: https://mailchimp.com/developer/marketing/api/list-members/add-member-to-list/
    }

    /**
     * Get email from persistent cookie
     * @return email, e.g. "michi@wyona.com"
     */
    public String getEmail(HttpServletRequest request) {
        Cookie cookie = getEmailCookie(request);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    /**
     * Disable Auto Login feature (deletes cookie)
     */
    public void disableAutoLogin(HttpServletRequest request, HttpServletResponse response) {
        Cookie currentCookie = getAutoLoginCookie(request);
        if (currentCookie != null) {
            log.info("Remove auto login cookie '" + currentCookie.getValue() + "' ...");

// Set-Cookie: KATIEAUTOLOGIN=; Max-Age=0; Expires=Thu, 01-Jan-1970 00:00:10 GMT

            String setCookieValue = AUTO_LOGIN_COOKIE_NAME + "=; Max-Age=0; Path=" + getPath(request) + "; SameSite=strict";
            log.info("New 'Set-Cookie' value: " + setCookieValue);
            response.setHeader("Set-Cookie", setCookieValue);

/*
            Cookie newCookie = new Cookie(COOKIE_NAME, null);
            newCookie.setMaxAge(0); // INFO: A zero value tells the browser to delete the cookie immediately.
            cookie.setPath(getPath(request));
            response.addCookie(newCookie);
*/

            // TODO: Check whether current cookie still exists inside data repo and if so, then remove: dataRepoService.removeTODO()
        } else {
            log.warn("No auto login cookie to delete!");
        }
    }
 
    /**
     * Enable auto login by adding a cookie and setting a unique token persistently on the server side
     * @param username User for which auto login will be enabled
     * @param response HTTP response to which cookie will be attached
     */
    public void enableAutoLogin(String username, HttpServletRequest request, HttpServletResponse response) {
        try {
            // INFO: Set cookie
            String token = setNewAutoLoginCookie(username, request, response);

            // INFO: Save token
            saveToken(username, token);
        } catch (Exception e) {
            log.error("Could not enable Auto Login feature! Exception: " + e, e);
        }
    }

    /**
     *
     */
    /*
    public String getUsername(HttpServletRequest request) {
        return getUsername(getCookie(request));
    }
     */

    /**
     * This method checks whether the current user should get logged in automatically based on an existing auto login cookie
     * @param request HTTP request
     * @return true means that this user can be logged in automatically.
     */
    /*
    public boolean existsAutoLoginCookie(HttpServletRequest request) {
        try {
            Cookie cookie = getCookie(request);
            if (cookie != null) {
                return true;
            } else {
                //log.debug("No auto login cookie exists yet.");
                return false;
            }
        } catch (Exception e) {
            log.error("Can not find out whether to perform auto login or not! Exception message : " + e.getMessage(), e);
            return false;
        }
    }

     */

    /**
     * Check whether the current user should get logged in automatically based on the cookie information and the persisted token and if so, then do the auto login
     * @param request TODO
     * @param response TODO
     * @return user object when user is already signed in or has been signed in automatically and null otherwise
     */
    public User tryAutoLogin(HttpServletRequest request, HttpServletResponse response) {
        log.info("Try to auto login user ...");
        User signedInUser = iamService.getUser(false, false);
        if (signedInUser != null) {
            log.info("Auto login not necessary, because user '" + signedInUser.getUsername() + "' is already signed in.");
            return signedInUser;
        }

        try {
            log.info("Check whether remember-me cookie exists ...");
            Cookie cookie = getAutoLoginCookie(request);
            if (cookie != null) {
                log.info("Analyze Autologin cookie ...");
                String username = getUsername(cookie);
                // TODO: Verify username with IAMService
                String token = getToken(cookie);
                if (username != null && token != null) {
                    if (isTokenValid(username, token)) {
                        log.info("Autologin cookie '" + username + "' / '" + token + "' is valid.");
                        // IMPORTANT TODO: Replace null password by remember me token, whereas see com/wyona/katie/config/CustomAuthenticationProvider.java
                        // TODO: Consider calling directly authenticationManager.authenticate()
                        authService.login(request, username, null);
                        // TODO: Extend expiry date
/*
                                String newToken = setNewCookie(username, request, response);
                                saveToken(username, newToken);
                                log.debug("Token was expired and has been renewed now.");
*/
                        return iamService.getUser(false, false);
                    } else {
                        log.warn("Autologin cookie '" + username + "' / '" + token + "' is invalid!");
                        disableAutoLogin(request, response);
                        return null;
                    }
                } else {
                    log.warn("No username/token inside auto login cookie!");
                    return null;
                }
            } else {
                //log.debug("No auto login cookie exists yet.");
                return null;
            }
        } catch (Exception e) {
            log.error("Can not find out whether to perform auto login or not! Exception message: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check whether token is valid
     * @return true when token is valid and false otherwise
     */
    private boolean isTokenValid(String username, String token) {
        String expiryDate = dataRepoService.getExpiryDate(username, token);
        log.info("Check whether expiry date '" + expiryDate + "' is valid ...");
        if (expiryDate != null && !hasTokenExpired(expiryDate)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set new auto login cookie in response
     * @param username Name of user for which auto login will be enabled
     * @return token, e.g. "60e836c3-d780-443b-8b46-f7fae77560d9"
     */
    private String setNewAutoLoginCookie(String username, HttpServletRequest request, HttpServletResponse response) {
        if (username != null) {

            // Set-Cookie: JSESSIONID=; Max-Age=0; Expires=Thu, 01-Jan-1970 00:00:10 GMT; Path=/
            // Set-Cookie: KATIEAUTOLOGIN=60e836c3-d780-443b-8b46-f7fae77560d9___lawrence; Max-Age=2147483647; Expires=Fri, 16-Jul-2088 21:14:57 GMT

            String token = UUID.randomUUID().toString();

            setCookie(request, response, AUTO_LOGIN_COOKIE_NAME, token + SEP + username);

            return token;
        } else {
            log.error("No username provided!");
            return null;
        }
    }

    /**
     * Add cookie to response
     * @param name Cookie name, e.g. "KATIEEMAIL"
     * @param value Cookie name value, e.g. "michael.wechner@wyona.com"
     */
    private void setCookie(HttpServletRequest request, HttpServletResponse response, String name, String value) {
        String setCookieValue = name + "=" + value + "; Max-Age=" + Integer.MAX_VALUE + "; Path=" + getPath(request) + "; SameSite=strict";
        //log.info("Set-Cookie value: " + setCookieValue);
        response.setHeader("Set-Cookie", setCookieValue);

        /*
        Cookie cookie = new Cookie(COOKIE_NAME,token+SEP+username);
        cookie.setMaxAge(Integer.MAX_VALUE);
        cookie.setPath(getPath(request));
        response.addCookie(cookie);
        */
    }

    /**
     * Get specific auto login cookie
     */
    private Cookie getAutoLoginCookie(HttpServletRequest request) {
        return getCookie(request, AUTO_LOGIN_COOKIE_NAME);
    }

    /**
     * Get cookie containing email of user when not registered yet (or also not signed in)
     */
    private Cookie getEmailCookie(HttpServletRequest request) {
        return getCookie(request, EMAIL_COOKIE_NAME);
    }

    /**
     * Get specific cookie
     * @param request Http request maybe containing cookie
     * @param name Cookie name
     * @return cookie when it exists and null otherwise
     */
    private Cookie getCookie(HttpServletRequest request, String name) {
        try {
            log.info("Get cookies from request ...");
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : request.getCookies()) {
                    log.debug("Cookie: " + c.getName());
                    if (c.getName().equals(name)) {
                        log.info("Cookie with name '" + name + "' exists.");
                        return c;
                    }
                }
            }
            log.info("No cookie with name '" + name + "' exists.");
            return null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
 
    /**
     * Get username from cookie
     * @param cookie Auto login cookie, e.g. "9933cede-a15f-4ea1-8c77-f4dcaefdc499___michaelwechner@gmail.com"
     */
    private String getUsername(Cookie cookie) {
        String result = null;
        if (cookie != null) {
            try {
                result = cookie.getValue(); // For example 9933cede-a15f-4ea1-8c77-f4dcaefdc499___michaelwechner@gmail.com
                //log.info("Cookie value: " + result);
                result = result.substring(result.lastIndexOf(SEP) + SEP.length());
            } catch (Exception e) {
                log.error("Can not extract username from cookie with name '" + cookie.getName() + "' and value '" + cookie.getValue() + "'");
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * Get token from cookie
     */
    private String getToken(Cookie cookie) {
        String result = null;
        if (cookie != null) {
            try {
                result = cookie.getValue();
                result = result.substring(0, result.lastIndexOf(SEP));
            } catch (Exception e) {
                log.error("Can not extract token from cookie with name '"+cookie.getName()+"' and value '"+cookie.getValue()+"'");
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * Save auto login token persistently
     */
    private void saveToken(String username, String token) {
        // TODO: House cleaning; delete all tokens which have expired
        if (username != null && token != null) {
            String expiryDate = getExpiryString();
            log.info("Save token with expiry date: " + expiryDate);
            try {
                dataRepoService.addRememberMeToken(username, token, getExpiryString());
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
 
    /**
     * Get expiry date
     * @return for example "20200520.21.36.44"
     */
    private String getExpiryString() {
        Calendar cal = Calendar.getInstance();
        cal.add(TOKEN_EXPIRY_UNIT, TOKEN_EXPIRY_AMOUNT);
        return expiresSdf.format(cal.getTime());
    }
 
    /**
     * Check whether expiry date is not valid anymore
     * @return true when expiry date is not valid anymore and false when expiry date is still valid
     */
    private boolean hasTokenExpired(String expiryString) {
        try {
            Date expiryDate = expiresSdf.parse(expiryString);
            if (expiryDate.before(new Date())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return true;
        }
    }
}
