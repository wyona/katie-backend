package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiImplicitParam;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

import org.springframework.security.core.Authentication;

/**
 * Authentication controller to handle login, logout, etc.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private RememberMeService rememberMeService;

    @Autowired
    private ContextService domainService;

    @Autowired
    private HttpServletRequest request;

    /**
     * Create generic JWT token (only Administrators)
     */
    @RequestMapping(value = "/token/generic", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Create generic JWT token (only Administrators)")
    public ResponseEntity<?> generateGenericJWT(@RequestBody JWTPayload payload,
        @ApiParam(name = "seconds", value = "Token validity in seconds, e.g. 3600 (60 minutes)",required = true)
        @RequestParam(value = "seconds", required = true) long seconds,
        HttpServletRequest request,
        HttpServletResponse response) {

        if (!domainService.isAdmin()) {
            return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
        }

        try {
            // INFO: See https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/
            StringBuilder body = new StringBuilder("{");
            body.append("\"token_type\":\"bearer\"");

            body.append(",\"access_token\":\"" + jwtService.generateJWT(payload, seconds, null) + "\"");

            //body.append(",\"refresh_token\":\"TODO\",");
            body.append("}");

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "JWT_GENERATION_FAILED"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a JWT token for a particular Katie user which exists inside the IAM of Katie
     */
    @RequestMapping(value = "/token/katie", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Create a JWT token for a particular Katie user which exists inside the IAM of Katie")
    public ResponseEntity<?> generateJWT(
        @ApiParam(name = "username", value = "Username (e.g. 'louise@wyona.com' or 'lawrence')",required = true)
        @RequestParam(value = "username", required = true) String username,
        @ApiParam(name = "seconds", value = "Token validity in seconds, e.g. 3600 (60 minutes)",required = true)
        @RequestParam(value = "seconds", required = true) long seconds,
        @ApiParam(name = "addProfile", value = "When true, then user profile information is added to the token, like for example date of birth or selfie as Base64",required = true)
        @RequestParam(value = "addProfile", required = true) boolean addProfile,
        @ApiParam(name = "linkAccount", value = "When true, then add JWT to user account",required = true)
        @RequestParam(value = "linkAccount", required = true) boolean linkWithUserAccount,
        HttpServletRequest request,
        HttpServletResponse response) {

        if (!domainService.isAdmin()) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        try {
            User user = iamService.getUserByUsername(new com.wyona.katie.models.Username(username), false, false);
            String selfie = null;
            if (addProfile && iamService.hasSelfie(user.getId())) {
                selfie = iamService.getSelfieAsBase64(user.getId()); // INFO: Selfie image of user
            }
            String jwt = jwtService.generateJWT(user, seconds, addProfile, selfie);
            if (linkWithUserAccount) {
                iamService.addJWT(new com.wyona.katie.models.Username(user.getUsername()), jwt);
            }

            // INFO: See https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/
            StringBuilder body = new StringBuilder("{");
            body.append("\"token_type\":\"bearer\"");

            body.append(",\"access_token\":\"" + jwt + "\"");

            //body.append(",\"refresh_token\":\"TODO\",");
            body.append("}");

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "JWT_GENERATION_FAILED"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Create a JWT token for my own Katie user which exists inside the IAM of Katie
     */
    @RequestMapping(value = "/token/myself", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary="Create a JWT token for my own Katie user which exists inside the IAM of Katie")
    public ResponseEntity<?> generateJWTForMyself(
            @ApiParam(name = "seconds", value = "Token validity in seconds, e.g. 3600 (60 minutes)",required = true)
            @RequestParam(value = "seconds", required = true) long seconds,
            @ApiParam(name = "addProfile", value = "When true, then user profile information is added to the token, like for example date of birth or selfie as Base64", required = true, defaultValue = "false")
            @RequestParam(value = "addProfile", required = true) boolean addProfile,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (!authService.userIsSignedInBySession(request)) {
            return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        }

        try {
            String username = authService.getUsername();
            User user = iamService.getUserByUsername(new com.wyona.katie.models.Username(username), false, false);
            String selfie = null;
            if (addProfile && iamService.hasSelfie(user.getId())) {
                selfie = iamService.getSelfieAsBase64(user.getId()); // INFO: Selfie image of user
            }
            String jwt = jwtService.generateJWT(user, seconds, addProfile, selfie);
            if (true) {
                iamService.addJWT(new com.wyona.katie.models.Username(user.getUsername()), jwt);
            }

            // INFO: See https://auth0.com/blog/refresh-tokens-what-are-they-and-when-to-use-them/
            StringBuilder body = new StringBuilder("{");
            body.append("\"token_type\":\"bearer\"");

            body.append(",\"access_token\":\"" + jwt + "\"");

            //body.append(",\"refresh_token\":\"TODO\",");
            body.append("}");

            return new ResponseEntity<>(body.toString(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "JWT_GENERATION_FAILED"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get public key (in PEM format) to validate JWT token
     */
    @RequestMapping(value = "/token-public-key", method = RequestMethod.GET, produces = "text/plain")
    @ApiOperation(value="Get public key (in PEM format) to validate JWT token")
    public ResponseEntity<?> getJWTPublicKey(
        HttpServletRequest request,
        HttpServletResponse response) {

        try {
            return new ResponseEntity<>(jwtService.getPublicKeyAsPem(), HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Login with username/password or JWT token
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value= "Login with username/password or JWT token")
    @ApiImplicitParams({
    @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                      required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> doLogin(@RequestBody(required = false) Credentials credentials,
        @ApiParam(name = "rememberMe", value = "True when user wants to stay logged in and false otherwise",required = false)
        @RequestParam(value = "rememberMe", required = false) boolean rememberMe,
        HttpServletRequest request,
        HttpServletResponse response) {

        String jwtToken = jwtService.getJWT(request);
        if (jwtToken == null) {
            if (credentials.getEmail() == null || credentials.getPassword() == null) {
                log.info("Either username or password is null!");
                return new ResponseEntity<>(new Error("Bad credentials", "AUTHENTICATION_FAILED"), HttpStatus.BAD_REQUEST);
            }
        } else {
            if (credentials.getEmail() != null || credentials.getPassword() != null) {
                log.info("Username and password should not be set when token is set!");
                return new ResponseEntity<>(new Error("Bad credentials", "AUTHENTICATION_FAILED"), HttpStatus.BAD_REQUEST);
            }
        }

        // INFO: Check whether user is already signed in
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null && ((Authentication)userPrincipal).isAuthenticated()) {
            UserDetails userDetails = authService.getUserDetails(userPrincipal);
            log.info("User is already authenticated as '" + userDetails.getUsername() + "'.");

            String providedUsername = null;
            if (credentials.getEmail() != null) {
                providedUsername = credentials.getEmail();
            } else if (jwtToken != null && jwtService.isJWTValid(jwtToken, null)) {
                providedUsername = jwtService.getJWTSubject(jwtToken);
            } else {
                log.info("Either username or valid token should be provided!");
                return new ResponseEntity<>(new Error("Bad credentials", "AUTHENTICATION_FAILED"), HttpStatus.BAD_REQUEST);
            }

            if (!userDetails.getUsername().equals(providedUsername)) { // TODO/TBD: Only logout, when new credentials are valid for new user
                log.info("Provided username '" + providedUsername + "' is not the same as signed in user '" + userDetails.getUsername() + "', therefore logout user ...");
                try {
                    logout(response);
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.info("Provided username '" + providedUsername + "' is the same as signed in user '" + userDetails.getUsername() + "'.");
                return new ResponseEntity<>(userDetails, HttpStatus.OK);
            }
        }

        try {
            UserDetails userDetails = authService.tryJWTLogin(request);
            if (userDetails != null) {
                return new ResponseEntity<>(userDetails, HttpStatus.OK);
            }
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "AUTHENTICATION_FAILED"), HttpStatus.BAD_REQUEST);
        }

        // INFO: Sign in user using username and password
        try {
            log.info("Try to authenticate user '" + credentials.getEmail() + "' using password ...");

            authService.login(request, credentials.getEmail(), credentials.getPassword());
            String username = authService.getUsername();
            log.info("User '" + username + "' signed in successfully.");
            userPrincipal = request.getUserPrincipal();
            UserDetails userDetails = authService.getUserDetails(userPrincipal);
            log.info("User '" + userDetails.getUsername() + "' authenticated successfully.");

            if (rememberMe) {
                // INFO: See https://github.com/wyona/yanel/blob/master/src/webapp/src/java/org/wyona/yanel/servlet/security/impl/AutoLogin.java
                log.info("Keep user '" + userDetails.getUsername() + "' logged in by setting persistent cookie.");
                rememberMeService.enableAutoLogin(userDetails.getUsername(), request, response);
            }

            return new ResponseEntity<>(userDetails, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "AUTHENTICATION_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Impersonate a user
     */
    @RequestMapping(value = "/impersonate", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value= "Impersonate a user")
    public ResponseEntity<?> impersonate(
            @ApiParam(name = "username", value = "Username of impersonated user",required = true)
            @RequestParam(value = "username", required = true) String username,
            HttpServletRequest request) {

        if (authService.userIsSignedInBySession(request)) {
            User signedInUser = authService.getUser(false, false);
            if (!signedInUser.getRole().equals(Role.ADMIN)) {
                log.warn("User does not have role " + Role.ADMIN);
                return new ResponseEntity<>(new Error("Impersonation failed", "IMPERSONATION_FAILED"), HttpStatus.BAD_REQUEST);
            } else {
                log.info("User '" + signedInUser.getUsername() + "' has role " + Role.ADMIN);
                if (iamService.usernameExists(new com.wyona.katie.models.Username(username))) {
                    try {
                        authService.login(username, null);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        return new ResponseEntity<>(new Error(e.getMessage(), "IMPERSONATION_FAILED"), HttpStatus.BAD_REQUEST);
                    }
                    return new ResponseEntity<>(HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Error("No such user '" + username + "'", "NO_SUCH_USERNAME"), HttpStatus.BAD_REQUEST);
                }
            }
        } else {
            return new ResponseEntity<>(new Error("Not signed in", "NOT_SIGNED_IN"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get username of signed in user
     */
    @RequestMapping(value = "/username", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get username of signed in user")
    public ResponseEntity<?> getUsername(HttpServletResponse response) {
        User signedInUser = rememberMeService.tryAutoLogin(request, response);
        if (signedInUser != null) {
            return new ResponseEntity<>(new Username(signedInUser.getUsername()), HttpStatus.OK);
        } else {
            String msg = "User is not signed in, therefore username is not available";
            return new ResponseEntity<>(new Error(msg, "USER_NOT_AUTHENTICATED_YET"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Get email of user (either user is signed in or from persistent cookie)
     */
    @RequestMapping(value = "/email", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get email of user")
    public ResponseEntity<?> getEmail(HttpServletRequest request, HttpServletResponse response) {
        User signedInUser = rememberMeService.tryAutoLogin(request, response);
        if (signedInUser != null) {
            return new ResponseEntity<>("{\"email\":\"" + signedInUser.getEmail() + "\", \"username\":\"" + signedInUser.getUsername() + "\"}", HttpStatus.OK);
        } else {
            String email = rememberMeService.getEmail(request);
            if (email != null) {
                return new ResponseEntity<>("{\"email\":\"" + email + "\", \"username\":" + null + "}", HttpStatus.OK);
            }
            String msg = "Email of user not available";
            return new ResponseEntity<>(new Error(msg, "USER_EMAIL_NOT_AVAILABLE"), HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Get user information (username, role, email, etc.) of signed in user
     */
    @RequestMapping(value = "/user", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get user information of signed in user")
    public ResponseEntity<?> getUser(HttpServletResponse response) {
        User signedInUser = rememberMeService.tryAutoLogin(request, response);
        if (signedInUser != null) {
            try {
                String domainId = domainService.getPersonalDomainId(signedInUser.getId());
                signedInUser.setMyKatieId(domainId);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
            return new ResponseEntity<>(signedInUser, HttpStatus.OK);
        } else {
            String msg = "User is not authenticated yet, therefore user information is not available";
            return new ResponseEntity<>(new Error(msg, "USER_NOT_AUTHENTICATED_YET"), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Logout
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Logout")
    public ResponseEntity<?> doLogout(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean isSignedIn = authService.userIsSignedInBySession(request);
            if (!isSignedIn) {
                log.info("User is not signed in by session!");
            }

            String username = logout(response);

            LogoutResponse logoutResponse = new LogoutResponse();
            if (username != null) {
                logoutResponse.setMessage("User '" + username + "' logged out successfully.");
            } else {
                logoutResponse.setMessage("User is not signed in, so cannot be logged out.");
            }
            log.info(logoutResponse.toString());

            return new ResponseEntity<>(logoutResponse, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "AUTHENTICATION_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @return username of logged out user
     */
    private String logout(HttpServletResponse response) throws ServletException {
        Principal userPrincipal = request.getUserPrincipal();
        String username = null;
        if (userPrincipal != null) {
            username = userPrincipal.getName();
        }
        request.logout();
        rememberMeService.disableAutoLogin(request, response);

        return username;
    }
}

/**
 *
 */
class Username {

    private String username;

    /**
     *
     */
    public Username() {
    }

    /**
     *
     */
    public Username(String username) {
        this.username = username;
    }

    /**
     *
     */
    public String getUsername() {
        return username;
    }
}
