package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.BackgroundProcess;
import com.wyona.katie.models.Role;
import com.wyona.katie.models.User;
import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.BackgroundProcessService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;

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
    private BackgroundProcessService backgroundProcessService;

    /**
     * REST interface to get a particular status of a running process
     */
    @RequestMapping(value = "/{process-id}/status", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get a particular status of a running process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> getStatusOfRunningProcess(
        @ApiParam(name = "process-id", value = "Process Id",required = true)
        @PathVariable(value = "process-id", required = true) String processId,
        HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            User signedInUser = authenticationService.getUser(false, false);
            if (signedInUser == null) {
                log.warn("User is not signed in!");
                return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }

            BackgroundProcess process = backgroundProcessService.getStatusOfRunningProcess(processId);
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

    /**
     * REST interface to get a particular status of a completed process
     */
    @RequestMapping(value = "/{process-id}/status-completed", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get a particular status of a completed process")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> getStatusOfCompletedProcess(
            @ApiParam(name = "process-id", value = "Process Id",required = true)
            @PathVariable(value = "process-id", required = true) String processId,
            HttpServletRequest request) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        try {
            User signedInUser = authenticationService.getUser(false, false);
            if (signedInUser == null) {
                log.warn("User is not signed in!");
                return new ResponseEntity<>(new Error("Access denied", "FORBIDDEN"), HttpStatus.FORBIDDEN);
            }

            BackgroundProcess process = backgroundProcessService.getStatusOfCompletedProcess(processId);
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