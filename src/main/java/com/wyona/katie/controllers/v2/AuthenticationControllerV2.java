package com.wyona.katie.controllers.v2;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authentication controller version 2 to handle login, logout, etc.
 */
@Slf4j
@Tag(name = "Authentication Controller v2", description = "Endpoints for Katie authentication")
@RestController
@RequestMapping("/api/v2/auth")
public class AuthenticationControllerV2 {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private RememberMeService rememberMeService;

    /**
     * Logout
     */
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Logout")
    public ResponseEntity<?> doLogout(HttpServletRequest request, HttpServletResponse response) {
        try {
            boolean isSignedIn = authService.userIsSignedInBySession(request);
            if (!isSignedIn) {
                log.info("User is not signed in by session!");
            }

            String username = logout(request, response);

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
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * @return username of logged out user
     */
    private String logout(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String username = authService.logout(request);
        rememberMeService.disableAutoLogin(request, response);

        log.info("User '" + username + "' signed out successfully.");

        return username;
    }
}
