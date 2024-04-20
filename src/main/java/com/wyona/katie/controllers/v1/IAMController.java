package com.wyona.katie.controllers.v1;

import com.wyona.katie.models.Error;
import com.wyona.katie.models.Username;
import com.wyona.katie.services.JwtService;
import com.wyona.katie.models.*;
import com.wyona.katie.services.ContextService;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import com.wyona.katie.services.AuthenticationService;
import com.wyona.katie.services.IAMService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.AccessDeniedException;
import java.util.Base64;

/**
 * Controller to manage users, etc.
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1") 
public class IAMController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ContextService contextService;

    /**
     * Self-registration
     */
    @RequestMapping(value = "/self-register", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary= "Self-registration")
    public ResponseEntity<?> selfRegister(
            @RequestBody(required = false) SelfRegistrationInformation infos,
            @Parameter(name = "h-captcha-response-token", description = "hCaptcha response token", required = false)
            @RequestParam(value = "h-captcha-response-token", required = false) String hCaptchaResponseToken,
            HttpServletRequest request) {

        if (!isHCaptchaTokenValid(hCaptchaResponseToken)) {
            log.warn("Self-registration failed, because hCaptcha response token is not valid!");
            return new ResponseEntity<>(new Error("Self-registration failed!", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }

        if (iamService.usernameExists(new com.wyona.katie.models.Username(infos.getEmail()))) {
            log.warn("Self-registration failed, because user '" + infos.getEmail() + "' already exists.");
            return new ResponseEntity<>(new Error("Self-registration failed!", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }

        log.info("Self-register user '" + infos.getEmail() + "' ...");
        try {
            iamService.selfRegister(infos);

            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error("Self-registration failed: " + e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Verify hCaptcha response token
     * @param token hCaptcha response token
     * @return true when token valid and false otherwise
     */
    private boolean isHCaptchaTokenValid(String token) {
        if (true) {
            return true;
        }
        if (token == null) {
            return false;
        }
        // https://docs.hcaptcha.com/?utm_medium=checkbox&utm_campaign=f00bbee7-34b1-4dac-8d9c-71561261093d#verify-the-user-response-server-side
        //curl https://api.hcaptcha.com/siteverify -X POST -d "response=CLIENT-RESPONSE&secret=YOUR-SECRET"
        return false;
    }

    /**
     * E-mail confirmation of user to complete self-registration account-setup
     */
    @RequestMapping(value = "/self-register-email-confirmation", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary= "E-mail confirmation of user to complete self-registration account-setup")
    public ResponseEntity<?> selfRegisterEmailConfirmation(
            @RequestBody PasswordResetToken passwordToken,
            HttpServletRequest request) {

        try {
            User newUser = iamService.selfRegisterEmailConfirmation(passwordToken.getResetToken(), passwordToken.getPassword());

            return new ResponseEntity<>("{\"locked\":\"" + newUser.isLocked() + "\"}", HttpStatus.CREATED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error("E-mail confirmation failed: " + e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to approve self-registration of user by administrator
     */
    @RequestMapping(value = "/self-register-approve", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Approve self-registration")
    public ResponseEntity<?> approveSelfRegistration(
            @Parameter(name = "token", description = "Token containing user information",required = true)
            @RequestParam(value = "token", required = true) String token,
            HttpServletRequest request) {

        try {
            iamService.approveSelfRegistration(token);
            return new ResponseEntity<>("{\"status\":\"approved\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to reject self-registration of user by administrator
     */
    @RequestMapping(value = "/self-register-reject", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Reject self-registration")
    public ResponseEntity<?> rejectSelfRegistration(
            @Parameter(name = "token", description = "Token containing user information",required = true)
            @RequestParam(value = "token", required = true) String token,
            HttpServletRequest request) {

        try {
            iamService.rejectSelfRegistration(token);
            return new ResponseEntity<>("{\"status\":\"rejected\"}", HttpStatus.OK);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get all users of all domains or all users of a particular domain
     */
    @RequestMapping(value = "/users", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get all users", description = "Get all users")
    public ResponseEntity<?> getUsers(
        @Parameter(name = "domainId", description = "Optional domain Id, for example 'df39dcaf-dd7e-4d04-bec4-ba60ee25834a', which represents a single realm containing its own users / members, etc.",required = false)
        @RequestParam(value = "domainId", required = false) String domainId,
        HttpServletRequest request) {

        // TEST: Uncomment lines below to test frotend spinner
        /*
        try {
            for (int i = 0; i < 1; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        try {
            if (domainId != null) {
                return new ResponseEntity<>(contextService.getMembers(domainId, true, null), HttpStatus.OK);
            } else {
                User[] users = iamService.getUsers();
                return new ResponseEntity<>(users, HttpStatus.OK);
            }
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get a particular user
     */
    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get a particular user", description = "Get a particular user")
    public ResponseEntity<?> getUser(
            @Parameter(name = "id", description = "User Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {
        try {
            User user = iamService.getUserById(id, true);
            JWT jwt = user.getJwtToken();
            if (jwt != null) {
                log.info("User '" + user.getUsername() + "' has a JWT");
                user.setJwtToken(jwtService.convert(jwt.getToken(), true));
            } else {
                log.debug("User '" + user.getId() + "' has no token.");
            }
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "FORBIDDEN"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Submit email or username when forgot password
     */
    @RequestMapping(value = "/user/forgot-password", method = RequestMethod.POST)
    @Operation(summary = "Submit email or username when forgot password")
    public ResponseEntity<?> submitUsername(
            @Parameter(name = "username", description = "Email or username",required = true)
            @RequestParam(value = "username", required = true) String username,
            @Parameter(name = "host-frontend", description = "Host of frontend",required = false)
            @RequestParam(value = "host-frontend", required = false) HostFrontend hostFrontend,
            HttpServletRequest request) {
        try {
            log.info("Host of frontend: " + hostFrontend);
            if (hostFrontend == null) {
                hostFrontend = HostFrontend.WWW_KATIE;
            }
            iamService.sendForgotPasswordEmail(new com.wyona.katie.models.Username(username), hostFrontend);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "SUBMIT_USERNAME_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Reset password
     */
    @RequestMapping(value = "/user/reset-password", method = RequestMethod.PUT)
    @Operation(summary = "Reset password")
    public ResponseEntity<?> resetPassword(
            @RequestBody PasswordResetToken passwordResetToken,
            HttpServletRequest request) {
        try {
            iamService.resetPassword(passwordResetToken);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "RESET_PASSWORD_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Add user
     */
    @RequestMapping(value = "/user", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Add user, whereas username, email, role (ADMIN, USER) and password are required. Username and email can be the same.")
    public ResponseEntity<?> addUser(
            @Parameter(name = "mykatie", description = "When not set or set to true, then a personal MyKatie domain will be created", required = false)
            @RequestParam(value = "mykatie", required = false) Boolean createMyKatie,
            @RequestBody User user,
            HttpServletRequest request) {
        try {
            if (!contextService.isAdmin()) {
                throw new AccessDeniedException("User is either not signed in or has not role ADMIN!");
            }
            User newUser = iamService.createUser(new Username(user.getUsername()), user.getEmail(), user.getRole(), user.getPassword(), true, user.getFirstname(), user.getLastname(), user.getLanguage(), false);
            log.info("Every new user gets a personal domain for free (MyKatie), for example to remember credentials, bookmarks, etc.");

            StringBuilder name = new StringBuilder("My Katie");
            if (newUser.getFirstname() != null || newUser.getLastname() != null) {
                name.append(" of");
                if (newUser.getFirstname() != null) {
                    name.append(" " + newUser.getFirstname());
                }
                if (newUser.getLastname() != null) {
                    name.append(" " + newUser.getLastname());
                }
            }

            boolean createPersonalDomain = true;
            if (createMyKatie != null && !createMyKatie.booleanValue()) {
                createPersonalDomain = false;
            }
            if (createPersonalDomain) {
                log.info("Create MyKatie for new user '" + newUser.getUsername() + "'.");
                Context domain = contextService.createDomain(true, name.toString(), "My Katie", false, newUser);
                contextService.setPersonalDomainId(newUser.getId(), domain.getId());
            } else {
                log.info("Do not create MyKatie for new user '" + newUser.getUsername() + "'.");
            }

            return new ResponseEntity<>(newUser, HttpStatus.OK);
        } catch (AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "ADD_USER_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update user
     */
    @RequestMapping(value = "/user/{id}", method = RequestMethod.PUT, produces = "application/json")
    @Operation(summary = "Update a particular user")
    public ResponseEntity<?> updateUser(
            @Parameter(name = "id", description = "User Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            @RequestBody User user,
            HttpServletRequest request) {
        try {
            log.info("User language: " + user.getLanguage());
            User updatedUser = iamService.updateUser(id, user);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "UPDATE_USER_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete user
     */
    @RequestMapping(value = "/user/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @Operation(summary = "Delete a particular user")
    public ResponseEntity<?> updateUser(
            @Parameter(name = "id", description = "User Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {
        try {
            String[] domainIds = contextService.removeUserFromDomains(id, true);
            iamService.deleteUser(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch(AccessDeniedException e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "DELETE_USER_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the profile picture of a particular user
     */
    @RequestMapping(value = "/user/{id}/profile-picture", method = RequestMethod.GET)
    @Operation(summary = "Get the profile picture of a particular user")
    public ResponseEntity<?> getProfilePicture(
            @Parameter(name = "id", description = "User Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {
        try {
            User signedInUser = authService.getUser(false, false);
            if (!(signedInUser.getId().equals(id) || signedInUser.getRole() == Role.ADMIN)) {
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }

            HttpHeaders responseHeaders = new HttpHeaders();
            if (iamService.hasSelfie(id)) {
                responseHeaders.add(HttpHeaders.CONTENT_TYPE, "image/jpeg");
                return new ResponseEntity<>(iamService.getSelfieAsBinary(id), responseHeaders, HttpStatus.OK);
            } else {
                log.warn("User '" + id + "' has no profile picture!");
                responseHeaders.add(HttpHeaders.CONTENT_TYPE, "application/json");
                return new ResponseEntity<>(new Error("No profile picture available", "NO_PROFILE_PICTURE"), responseHeaders, HttpStatus.NOT_FOUND);
            }
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "GET_PROFILE_PICTURE_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the profile picture as base64 of a particular user
     */
    @RequestMapping(value = "/user/{id}/profile-picture_base64", method = RequestMethod.GET, produces = "application/json")
    @Operation(summary = "Get the profile picture as base64 of a particular user")
    public ResponseEntity<?> getProfilePictureAsBase64(
            @Parameter(name = "id", description = "User Id",required = true)
            @PathVariable(value = "id", required = true) String id,
            HttpServletRequest request) {
        try {
            User signedInUser = authService.getUser(false, false);
            if (!(signedInUser.getId().equals(id) || signedInUser.getRole() == Role.ADMIN)) {
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }

            if (iamService.hasSelfie(id)) {
                return new ResponseEntity<>("{\"image\":\"" + iamService.getSelfieAsBase64(id) + "\"}", HttpStatus.OK);
            } else {
                log.warn("User '" + id + "' has no profile picture!");
                return new ResponseEntity<>(new Error("No profile picture available", "NO_PROFILE_PICTURE"), HttpStatus.NOT_FOUND);
            }
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "GET_PROFILE_PICTURE_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Update profile picture of a particular user
     */
    @RequestMapping(value = "/user/{id}/profile-picture", method = RequestMethod.PUT, produces = "application/json")
    @Operation(summary = "Update profile picture of a particular user")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataTypeClass = String.class, paramType = "header") })
    public ResponseEntity<?> updateProfilePicture(
            @Parameter(name = "id", description = "Katie user Id, e.g. '9cfc7e09-fe62-4ae4-81b6-1605424d6f87'",required = true)
            @PathVariable(value = "id", required = true) String id,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            authService.tryJWTLogin(request);

            User signedInUser = authService.getUser(false, false);

            if (signedInUser == null || !(signedInUser.getId().equals(id) || signedInUser.getRole() == Role.ADMIN)) {
                log.warn("Access denied.");
                return new ResponseEntity<>(new Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }

            if (!iamService.userIdExists(id)) {
                return new ResponseEntity<>(new Error("No such user Id '" + id + "'!", "NO_SUCH_USER_ID"), HttpStatus.BAD_REQUEST);
            }

            String mimeType = file.getContentType();
            long fileSize= file.getSize();
            log.info("Try to save picture with mime-type '" + mimeType + "' and file size " + fileSize + "B ...");
            if (!mimeType.equals("image/jpeg")) {
                return new ResponseEntity<>(new Error("Mime-type is '" + mimeType + "', but Katie only supports 'image/jpeg' as mime-type", "MIME_TYPE_NOT_SUPPORTED"), HttpStatus.BAD_REQUEST);
            }
            if (fileSize > 1048576) { // INFO: Image must be smaller than 1MB
                return new ResponseEntity<>(new Error("File size is " + fileSize + "B, but Katie only supports file size smaller 1MB", "FILE_SIZE_TOO_BIG"), HttpStatus.BAD_REQUEST);
            }

            iamService.saveSelfieAsBinary(id, file.getInputStream());

            if (iamService.hasSelfie(id)) {
                return new ResponseEntity<>("{\"image\":\"" + iamService.getSelfieAsBase64(id) + "\"}", HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new Error("No profile picture available", "NO_PROFILE_PICTURE"), HttpStatus.BAD_REQUEST);
            }
        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "UPDATE_PROFILE_PICTURE_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get username by providing a profile picture
     */
    @RequestMapping(value = "/user/forgot-username", method = RequestMethod.POST, produces = "application/json")
    @Operation(summary = "Get username by providing a profile picture")
    public ResponseEntity<?> getUsernameByProfilePicture(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        try {
            String mimeType = file.getContentType();
            long fileSize= file.getSize();
            if (!mimeType.equals("image/jpeg")) {
                return new ResponseEntity<>(new Error("Mime-type is '" + mimeType + "', but Katie only supports 'image/jpeg' as mime-type", "MIME_TYPE_NOT_SUPPORTED"), HttpStatus.BAD_REQUEST);
            }
            if (fileSize > 1048576) { // INFO: Image must be smaller than 1MB
                return new ResponseEntity<>(new Error("File size is " + fileSize + "B, but Katie only supports file size smaller 1MB", "FILE_SIZE_TOO_BIG"), HttpStatus.BAD_REQUEST);
            }

            log.info("Search for similar images for given image with mime-type '" + mimeType + "' and file size " + fileSize + "B ...");
            String data = Base64.getEncoder().encodeToString(file.getBytes());
            String[] userIDs = iamService.searchForSimilarProfilePictures(data);
            if (userIDs !=  null && userIDs.length > 0) {
                StringBuilder ids = new StringBuilder();
                for (String id : userIDs) {
                    ids.append(id + " ");
                }
                log.info("User IDs associated with similar profile pictures: " + ids);
            } else {
                log.info("No similar profile pictures found");
            }

            return new ResponseEntity<>("{}", HttpStatus.OK);
            // INFO: For security reasons do not show user IDs, but just send an email to user which matched most likely
            //return new ResponseEntity<>(userIDs, HttpStatus.OK);
            //return new ResponseEntity<>("{\"image\":\"" + data + "\"}", HttpStatus.OK);

        } catch(Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage(), "GET_PROFILE_PICTURE_FAILED"), HttpStatus.BAD_REQUEST);
        }
    }
}
