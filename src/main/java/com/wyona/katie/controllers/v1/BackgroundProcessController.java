package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.BackgroundProcess;
import com.wyona.katie.models.Role;
import com.wyona.katie.models.User;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.IAMService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

/**
 * Controller to generate and send reports
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/bg-process")
public class BackgroundProcessController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    /**
     * REST interface to get status of a particular background process (running or completed)
     */
    @RequestMapping(value = "/{process-id}/status", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get status of a particular background process (running or completed)")
    @Parameter(
            name = "Authorization",
            description = "Bearer JWT",
            required = false,
            in = ParameterIn.HEADER,
            schema = @Schema(type = "string")
    )
    public ResponseEntity<?> getStatusOfRunningProcess(
        @Parameter(name = "process-id", description = "Process Id",required = true)
        @PathVariable(value = "process-id", required = true) String processId,
        HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            User signedInUser = iamService.getUser(false, false);
            if (signedInUser == null) {
                log.warn("User is not signed in!");
                return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }

            BackgroundProcess process = backgroundProcessService.getStatus(processId);
            if (!(process.getUserId().equals(signedInUser.getId()) || signedInUser.getRole().equals(Role.ADMIN))) {
                log.warn("User is not authorized to view process status!");
                return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }

            return new ResponseEntity<>(process, HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }
}
