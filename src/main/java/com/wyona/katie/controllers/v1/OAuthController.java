package com.wyona.katie.controllers.v1;

import lombok.extern.slf4j.Slf4j;

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

        String scope = "mcp";
        String once = "todo_once";

        // TODO: Make redirect base URL configuarble
        String redirectUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + clientId + "&response_type=" + responseType + "&scope=" + scope + "&redirect_uri=" + redirectUri + "&state=" + state + "&nonce=" + once;
        if (true) {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", redirectUrl);

            return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
        } else {
            return new ResponseEntity<>(HttpStatus.OK);
        }
    }
}
