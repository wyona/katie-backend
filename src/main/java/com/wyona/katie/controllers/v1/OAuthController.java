package com.wyona.katie.controllers.v1;

import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.JwtService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * OAuth controller
 */
@Slf4j
@Tag(name = "OAuth Controller v1", description = "Endpoints for OAuth")
@RestController
public class OAuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationService authenticationService;

    private String TODO_CODE = "TODO_CODE";

    /**
     * TODO
     */
    @GetMapping(value = "/authorize")
    @Operation(summary="TODO")
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
            String scope = "mcp";
            String once = "todo_once";
            String googleOAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + clientId + "&response_type=" + responseType + "&scope=" + scope + "&redirect_uri=" + redirectUri + "&state=" + state + "&nonce=" + once;
            redirectUri = googleOAuthUrl;
        } else {
            log.info("Already authenticated successfully.");
            redirectUri = redirectUri + "?code=" + TODO_CODE;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", redirectUri);
        return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
    }

    /**
     * TODO
     */
    @PostMapping(value = "/token",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary="TODO")
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

        log.info("Verify code: " + code);
        if (!code.equals(TODO_CODE)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        long seconds = 3600;
        StringBuilder body = new StringBuilder("{");
        body.append("\"token_type\":\"bearer\"");
        String username = authenticationService.getUsername();
        log.info("Generate access token for user '" + username + "' ...");
        String domainId = "todo_domain_id";
        String token = jwtService.generateJWT(username, domainId, seconds, null);
        body.append(",\"access_token\":\"" + token + "\"");
        body.append("}");

        return new ResponseEntity<>(body.toString(), HttpStatus.OK);
    }
}
