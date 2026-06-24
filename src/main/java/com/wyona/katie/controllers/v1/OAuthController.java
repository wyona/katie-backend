package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.OAuthRegisterBody;
import com.wyona.katie.models.User;
import com.wyona.katie.models.Username;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.IAMService;
import com.wyona.katie.services.JwtService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * OAuth controller
 */
@Slf4j
@Tag(name = "OAuth Controller v1", description = "Endpoints for OAuth")
@RestController
public class OAuthController {

    @Value("${new.context.mail.body.host}")
    private String defaultHostnameMailBody;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ContextService domainService;

    @Autowired
    private IAMService iamService;

    /**
     * Registers a new OAuth 2.0 client application dynamically and returns client credentials
     */
    @PostMapping(value = "/register",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Registers a new OAuth 2.0 client application dynamically and returns client credentials")
    public ResponseEntity<?> register(
            @RequestBody OAuthRegisterBody oAuthRegisterBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("Client name: " + oAuthRegisterBody.getClient_name());
        log.info("Redirect URIs: " + String.join(",", oAuthRegisterBody.getRedirect_uris()));
        log.info("Grant types: " + String.join(",", oAuthRegisterBody.getGrant_types()));
        log.info("Response types: " + String.join(",", oAuthRegisterBody.getResponse_types()));
        log.info("Token endpoint auth method: " + oAuthRegisterBody.getToken_endpoint_auth_method());

        // TODO: Make configurable
        String client_id = "1045897086839-7dhg0h1rbc9kdeklfdghtfj9r85p08dj";
        String client_secret = "3D138r5719ru3e1";

        StringBuilder body = new StringBuilder("{");
        body.append("\"client_id\":\"" + client_id + "\"");
        body.append(",\"client_secret\":\"" + client_secret + "\"");
        body.append(",\"redirect_uris\":[\"" + oAuthRegisterBody.getRedirect_uris()[0] + "\"]");
        body.append(",\"token_endpoint\":\"" + defaultHostnameMailBody + "/token" + "\"");
        body.append("}");

        return new ResponseEntity<>(body.toString(), HttpStatus.OK);
    }

    /**
     * Check whether user is authenticated / authorized
     */
    @GetMapping(value = "/authorize")
    @Operation(summary="Check whether user is authenticated / authorized. If not authenticated, then redirect to single-sign-on server.")
    public ResponseEntity<?> authorize(
        @Parameter(name = "response_type", description = "Response type, e.g., code", required = false)
        @RequestParam(value = "response_type", required = false) String responseType,
        @Parameter(name = "redirect_uri", description = "Redirect URI", required = false)
        @RequestParam(value = "redirect_uri", required = false) String redirectUri,
        @Parameter(name = "state", description = "State", required = false)
        @RequestParam(value = "state", required = false) String state,
        @Parameter(name = "client_id", description = "Client Id, e.g, 1045897086839-7dhg0h1rbc9kdeklfdghtfj9r85p08dj.apps.googleusercontent.com", required = false)
        @RequestParam(value = "client_id", required = false) String clientId,
        HttpServletRequest request,
        HttpServletResponse response) {

        log.info("Response type: " + responseType);
        log.info("Redirect URI: " + redirectUri);

        boolean authenticated = authenticationService.userIsSignedInBySession(request);
        if (!authenticated) {
            log.info("User is not authenticated yet.");

            // TODO: Make sign in server configurable, resp. use Katie itself
            String scope = "mcp";
            String once = "todo_once";
            String googleOAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + clientId + "&response_type=" + responseType + "&scope=" + scope + "&redirect_uri=" + redirectUri + "&state=" + state + "&nonce=" + once;
            redirectUri = googleOAuthUrl;
        } else {
            String username = authenticationService.getUsername();
            log.info("User '" + username + "' already authenticated successfully.");
            String code = username; // TODO: Replace hack
            redirectUri = redirectUri + "?code=" + code + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }

        // INFO: Sleep a little in order to prevent race condition
        try {
            for (int i = 0; i < 2; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUri);
        return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    /**
     * Get access token to access Katie MCP
     */
    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="Get access token to access Katie MCP")
    public ResponseEntity<?> getToken(
            @Parameter(name = "grant_type", description = "TODO", required = false)
            @RequestParam(value = "grant_type", required = false) String grantType,
            @Parameter(name = "redirect_urli", description = "Redirect URI", required = false)
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @Parameter(name = "code", description = "TODO", required = false)
            @RequestParam(value = "code", required = false) String code,
            @Parameter(name = "code_verifier", description = "TODO", required = false)
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @Parameter(name = "client_id", description = "Client Id, e.g, 1045897086839-7dhg0h1rbc9kdeklfdghtfj9r85p08dj.apps.googleusercontent.com", required = false)
            @RequestParam(value = "client_id", required = false) String clientId,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception  {

        String username = code; // TODO: getToken() is a back-channel request and therefore we have to get username otherwise
        log.info("Username: " + username);

        log.info("Generate access token for user '" + username + "' ...");
        User user = iamService.getUserByUsername(new Username(username), false, false);
        String[] domainIDs = domainService.getDomainIDsUserIsMemberOf(user);
        String domainId = domainIDs[0]; // TODO: Select domain Id ...
        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put(JwtService.JWT_CLAIM_ENDPOINT, "/mcp");
        claims.put(JwtService.JWT_CLAIM_SCOPE, "search");
        long seconds = 3600;
        String token = jwtService.generateJWT(username, domainId, seconds, claims);

        StringBuilder body = new StringBuilder("{");
        body.append("\"token_type\":\"Bearer\"");
        body.append(",\"access_token\":\"" + token + "\"");
        body.append(",\"expires_in\":" + seconds);
        body.append(",\"" + JwtService.JWT_CLAIM_SCOPE + "\":\"mcp\"");
        body.append("}");

        return new ResponseEntity<>(body.toString(), HttpStatus.OK);
    }
}
