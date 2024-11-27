package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyona.katie.models.*;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.util.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import technology.semi.weaviate.client.Config;
import technology.semi.weaviate.client.WeaviateClient;
import technology.semi.weaviate.client.v1.data.model.WeaviateObject;
import technology.semi.weaviate.client.base.Result;
import technology.semi.weaviate.client.v1.graphql.query.argument.NearImageArgument;
import technology.semi.weaviate.client.v1.graphql.model.GraphQLResponse;
import technology.semi.weaviate.client.v1.graphql.query.argument.WhereArgument;
import technology.semi.weaviate.client.v1.graphql.query.argument.WhereOperator;
import technology.semi.weaviate.client.v1.graphql.query.fields.Field;
import technology.semi.weaviate.client.v1.graphql.query.fields.Fields;

// TODO: Integrate with IAM, e.g. https://www.keycloak.org/
/**
 *
 */
@Slf4j
@Component
public class IAMService {

    @Value("${mail.subject.tag}")
    private String mailSubjectTag;

    @Value("${iam.data_path}")
    private String iamDataPath;

    @Value("${new.context.mail.body.host}")
    private String defaultHostname;

    @Value("${my.katie.host}")
    private String myKatieHostname;

    @Value("${iam.forgot.password.token.expire.seconds}")
    private Long tokenValidInSeconds;

    @Value("${self.registration.approval.required}")
    private Boolean selfRegistrationApprovalRequired;

    @Value("${self.registration.approval.max.time.since.creation}")
    private Long maxTimeSinceAccountCreation;

    @Value("${self_registration_requests.data_path}")
    private String selfRegistrationRequestsDataPath;

    private static final String USER_PROFILE_IMAGE_NAME = "selfie.jpg";

    private static final String PROFILE_PICTURE_CLASS_NAME = "ProfilePicture";

    private AuthenticationService authService;
    private XMLService xmlService;
    private UsersXMLFileService usersXMLFileService;
    private MailerService mailerService;
    private JwtService jwtService;

    private final static String SUBJECT_APPROVE_SELF_REGISTRATION = "approve-self-registration";
    private final static String TOKEN = "token";
    private final static String USER_ID = "user_id";

    @Autowired
    public IAMService(AuthenticationService authService, XMLService xmlService, UsersXMLFileService usersXMLFileService, MailerService mailerService, JwtService jwtService) {
        this.authService = authService;
        this.xmlService = xmlService;
        this.usersXMLFileService = usersXMLFileService;
        this.mailerService = mailerService;
        this.jwtService = jwtService;
    }

    /**
     *
     */
    public long getMaxTimeSinceAccountCreation() {
        return maxTimeSinceAccountCreation;
    }

    /**
     *
     */
    protected String getIAMDataPath() {
        return iamDataPath;
    }

    /**
     *
     */
    // INFO: We call initSuperadmin() inside ContextService, such that we do not have to care about in which order the PostConstruct(s) are executed
    //@javax.annotation.PostConstruct
    void initSuperadmin() {
        log.info("Check whether IAM already exists ...");
        if (!usersXMLFileService.existsUsersConfig()) {
            log.info("Users configuration does not exist yet, therefore create it now ...");
            User sysadmin = null;
            try {
                usersXMLFileService.createUsersConfig();
                String username = "superadmin"; // TODO: Make configurable
                String email = "contact@wyona.com"; // TODO: Make configurable
                String password = "Katie1234%"; // TODO: Make configurable
                String language = "en"; // TODO: Make configurable
                String firstName = "Super"; // TODO: Make configurable
                String lastName = "Admin"; // TODO: Make configurable
                sysadmin = createUser(new Username(username), email, Role.ADMIN, password, true, firstName, lastName, language, false, true);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Send email containing link to reset password
     * @param username Username, e.g. "superadmin" or "michael.wechner@wyona.com"
     * @param host Host of frontend, e.g. "www.ukatie.com" or "my.ukatie.com"
     */
    public void sendForgotPasswordEmail(Username username, HostFrontend host) {

        if (!usernameExists(username)) {
            log.warn("No such user '" + username + "'!");
            // INFO: For security reasons do not tell client that user does not exist

            // TODO: Check whether user exists with email
            //if (emailExists(username)) {}

            return;
        }

        User user = getUserByUsername(username, false, false);
        String email = user.getEmail();

        if (email != null) {
            log.info("Send forgot password email to '" + username + "' ...");
            try {
                JWTPayload payload = new JWTPayload();
                payload.setIss("Katie");
                payload.setSub(username.getUsername());
                String token = jwtService.generateJWT(payload, tokenValidInSeconds, null);

                mailerService.send(email, null, mailSubjectTag + " Reset your password", getResetPasswordEmailBody(token, tokenValidInSeconds, host), true);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.warn("No email for provided username '" + username + "'!");
        }
    }

    /**
     *
     */
    private String getResetPasswordEmailBody(String token, long tokenValidInSeconds, HostFrontend host) {

        String resetPasswordLink = defaultHostname + "/#/reset-password";
        if (host == HostFrontend.MY_KATIE) {
            resetPasswordLink = myKatieHostname + "/reset";
        }
        resetPasswordLink = resetPasswordLink + "?token=" + token;

        // TODO: Use template
        String body = "<html><body><h1>Reset your password</h1><p>You told us you forgot your password. If you really did, click here to choose a new one: <a href=\"" + resetPasswordLink + "\">Choose a new Password</a> (Please note that this link works only for the next " + (tokenValidInSeconds / 60) + " minutes)</p></body></html>";
        return body;
    }

    /**
     * Reset password of user
     * @param passwordResetToken Object containing new password and signed token containing username
     * @throws Exception when signed token expired or not valid or when password not strong enough
     */
    public void resetPassword(PasswordResetToken passwordResetToken) throws Exception {
        String jwtToken = passwordResetToken.getResetToken();

        log.info("Verify reset password token ...");

        JWT jwt = jwtService.convert(jwtToken, false);

        if (jwt.getIsExpired()) {
            String msg = "Security token is expired: " + new Date(jwt.getPayload().getExp()) + " (Current time: " + new Date() + ")!";
            log.warn(msg);
            throw new Exception(msg);
        }

        if (!jwtService.isJWTValid(jwtToken, null)) {
            String msg = "Reset password token is not valid!";
            log.warn(msg);
            throw new Exception(msg);
        }

        log.info("Token is valid.");
        String username = jwtService.getJWTSubject(jwtToken);
        User user = getUserByUsername(new Username(username), false, false);

        log.info("Update password of user '" + user.getId() + "' ...");

        // INFO: Throws exception when password is not strong enough
        if (passwordAcceptable(passwordResetToken.getPassword())) {
            log.info("Password is strong enough");
        }
        user.setPasswordEncoding("bcrypt");
        user.setPassword(encryptPassword(passwordResetToken.getPassword(), "bcrypt"));

        usersXMLFileService.updateUser(user);
    }

    /**
     * Check whether password is acceptable
     * @param password Password
     * @return true when password is strong enough
     */
    private boolean passwordAcceptable(String password) throws Exception {
        if (password.length() < 8) { // INFO: See https://en.wikipedia.org/wiki/Password_strength
            //log.debug("Submitted password: " + password);
            log.warn("Minimum password length of 8 is required!");
            throw new Exception("Minimum password length of 8 is required!");
        }
        return true;
    }

    /**
     * Create user account
     * @param username Username, e.g. "michaelwechner" or email address "michael.wechner@wyona.com"
     * @param email Email of user, e.g. "michael.wechner@wyona.com"
     * @param role Role of user, e.g. USER or ADMIN
     * @param password Plain text password
     * @param sendAccountInfoToUser When true, then send username and plaintext password to user by email
     * @param firstName First name of user, e.g. "Michael"
     * @param lastName  Last name of user, e.g. "Wechner"
     * @param language Language of user, e.g. "de" or "en"
     * @param locked True when account should be locked / disabled and requires approval to be unlocked / enabled
     * @param approved True when account was approved by administrator and false otherwise
     */
    public User createUser(Username username, String email, Role role, String password, boolean sendAccountInfoToUser, String firstName, String lastName, String language, boolean locked, boolean approved) throws Exception {

        log.info("Try to add user '" + username + "' persistently ...");
        if (usernameExists(username)) {
            log.warn("User '" + username + "' already exists!");
            throw new Exception("User '" + username + "' already exists!");
        } else {
            String userId = UUID.randomUUID().toString();

            String passwordEncoding = null;
            String encryptedPassword = password;
            if (password != null) {
                if (isPasswordValid(password)) {
                    if (passwordAcceptable(password)) {
                        log.info("Password is strong enough");
                    }
                    passwordEncoding = "bcrypt";
                    encryptedPassword = encryptPassword(password, passwordEncoding);
                } else {
                    throw new Exception("Password provided, but not valid format!");
                }
            }

            if (!isLanguageValid(language)) {
                throw new Exception("Provided Language '" + language + "' is not valid!");
            }

            Date created = new Date();
            User user = new User(userId, username.getUsername(), encryptedPassword, passwordEncoding, role, null, email, firstName, lastName, false, false, null, language, locked, approved, created);
            usersXMLFileService.addUser(user);

            if (sendAccountInfoToUser) {
                String subject = mailSubjectTag + " Your new Katie account, welcome!";
                sendAccountInfoByEmail(subject, user, password);
            }

            return user;
        }
    }

    /**
     * Send account credentials to new user
     * @param user New user
     * @param plaintextPassword Plain text password of user
     */
    private void sendAccountInfoByEmail(String subject, User user, String plaintextPassword) throws Exception {
        mailerService.send(user.getEmail(), null, subject, getWelcomeToKatieBody(user, plaintextPassword), true);
    }

    /**
     *
     */
    private String getWelcomeToKatieBody(User user, String plaintextPassword) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(null, defaultHostname);

        tmplArgs.add("username", user.getUsername());
        if (plaintextPassword != null) {
            tmplArgs.add("password", plaintextPassword);
        }

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("account_credentials_", Language.valueOf(user.getLanguage()), null);
        template.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * Check whether language is valid
     * @param language Two-letter code language, e.g. "en" or "de"
     * @return true when language is valid and false otherwise
     */
    private boolean isLanguageValid(String language) {
        log.info("Check whether language '" + language + "' is valid!");
        if (language == null) {
            log.error("No language set!");
            return false;
        }
        if (language.length() != 2) {
            log.error("Language must be a two-letter code!");
            return false;
        }

        try {
            Language.valueOf(language);
        } catch(Exception e) {
            log.error("No such two-letter code '" + language + "' supported!");
            return false;
        }

        return true;
    }

    /**
     * Delete user
     * @param id User Id
     */
    public void deleteUser(String id) throws Exception {
        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null && signedInUser.getRole() == Role.ADMIN) {
            log.info("User '" + signedInUser.getId() + "' has role " + Role.ADMIN + " and therefore can delete all users.");
            //User user = getUserById(id, false, false);
            log.info("Remove user '" + id + "' ...");
            usersXMLFileService.removeUser(id);
            File userDir = new File(getIAMDataPath(), id);
            if (userDir.isDirectory()) {
                log.info("Delete user directory: " + userDir.getAbsolutePath());
                FileUtils.deleteDirectory(userDir);
            }
        } else {
            throw new AccessDeniedException("User is either not signed in or has not role " + Role.ADMIN + "!");
        }
    }

    /**
     * Update user profile
     * @param id User Id
     * @param user Updated user profile
     * @return updated user
     */
    public User updateUser(String id, User user) throws Exception {
        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null && signedInUser.getRole() == Role.ADMIN) {
            log.info("User '" + signedInUser.getId() + "' has role " + Role.ADMIN + " and therefore can update all users.");
        } else {
            if (signedInUser != null) {
                if (signedInUser.getId().equals(id)) {
                    log.info("User '" + signedInUser.getId() + "' can update itself.");
                } else {
                    throw new AccessDeniedException("Id of signed in user '" + signedInUser.getId() + "' does not match with requested id '" + id + "'!");
                }
            } else {
                throw new AccessDeniedException("User not signed in!");
            }
        }

        log.info("Update user '" + id + "' persistently ...");

        User uUser = getUserById(id, false);
        if (uUser == null) {
            log.warn("User '" + id + "' does not exist!");
            throw new Exception("User '" + id + "' does not exist!");
        }

        if (user.getPassword() != null && user.getPassword().length() > 0) {
            if (signedInUser.getRole() == Role.ADMIN) {
                log.info("Update password of user '" + id + "'.");
                if (passwordAcceptable(user.getPassword())) {
                    log.info("Password is strong enough");
                }
                uUser.setPasswordEncoding("bcrypt");
                uUser.setPassword(encryptPassword(user.getPassword(), "bcrypt"));
            } else {
                throw new AccessDeniedException("User '" + signedInUser.getId() + "' cannot update password without providing current password, because user has not role " + Role.ADMIN + "!");
            }
        }

        uUser.setFirstname(user.getFirstname());
        uUser.setLastname(user.getLastname());
        uUser.setEmail(user.getEmail());
        uUser.setLanguage(user.getLanguage());

        usersXMLFileService.updateUser(uUser);

        return uUser;
    }

    /**
     * @param password Plain text password
     */
    private boolean isPasswordValid(String password) {
        // TODO: Check upper and lower case, special character, numbers, etc. whereas see for example https://app.hubspot.com
        return true;
    }

    /**
     * Encrypt password
     * @param password Plain text password
     * @param encoding Password encoding, e.g. "bcrypt"
     * @return encrypted password
     */
    private String encryptPassword(String password, String encoding) throws Exception {
        if (encoding.equals("plaintext")) {
            return password;
        } else if (encoding.equals("bcrypt")) {
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            return encoder.encode(password);
        } else {
            log.error("No such encoding algorithm '" + encoding + "' implemented!");
            throw new Exception("No such encoding algorithm '" + encoding + "' implemented!");
        }
    }

    /**
     * Add JWT to user
     */
    public void addJWT(Username username, String jwtToken) throws Exception {
        log.info("Add JWT to user '" + username + "' ...");
        usersXMLFileService.appendJWT(username.getUsername(), jwtToken);
    }

    /**
     * Get user by user id
     * @param id User id, e.g. "71aa8dc6-0f19-4787-bd91-08fe1e863473"
     * @param includingJWT Include JWT when true and null otherwise
     * @return user if user exists and null otherwise
     */
    public User getUserById(String id, boolean includingJWT) throws Exception {
        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null && signedInUser.getRole() == Role.ADMIN) {
            return usersXMLFileService.getIAMUserById(id, false, includingJWT);
        } else {
            if (signedInUser != null) {
                if (signedInUser.getId().equals(id)) {
                    log.info("Get profile of signed in user '" + id + "' ...");
                    return usersXMLFileService.getIAMUserById(id, false, includingJWT);
                } else {
                    throw new AccessDeniedException("Id of signed in user '" + signedInUser.getId() + "' does not match with requested id '" + id + "'!");
                }
            } else {
                throw new AccessDeniedException("User not signed in!");
            }
        }
    }

    /**
     * Get user by user id without authorization check
     * @param id User id, e.g. "71aa8dc6-0f19-4787-bd91-08fe1e863473"
     * @return user if user exists and null otherwise
     */
    public User getUserByIdWithoutAuthCheck(String id) throws Exception {
        log.warn("Get user data without checking authorization.");
        return usersXMLFileService.getIAMUserById(id, false, false);
    }

    /**
     * @param id Katie user Id, e.g. "9cfc7e09-fe62-4ae4-81b6-1605424d6f87"
     * @return true when user with provided Id already exists and false otherwise
     */
    public boolean userIdExists(String id) throws Exception {
        if (getUserByIdWithoutAuthCheck(id) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true when user with provided username already exists and false otherwise
     */
    public boolean usernameExists(Username username) {
        if (getUserByUsername(username, false, false) != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @param id User Id
     * @return true when user has selfie attached, otherwise false
     */
    public boolean hasSelfie(String id) {
        File selfieFile = new File(iamDataPath, id + "/" + USER_PROFILE_IMAGE_NAME);
        if (selfieFile.exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get profile picture as base64 (Decode base64 for example at https://base64.guru/converter/decode/image/jpg)
     * @param id User Id
     * @return profile picture as Base64
     */
    public String getSelfieAsBase64(String id) throws Exception {
        File selfieFile = new File(iamDataPath, id + "/" + USER_PROFILE_IMAGE_NAME);
        byte[] fileContent = Files.readAllBytes(selfieFile.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    /**
     * Get profile picture as binary
     * @param id User Id
     * @return profile picture as binary
     */
    public byte[] getSelfieAsBinary(String id) throws Exception {
        File selfieFile = new File(iamDataPath, id + "/" + USER_PROFILE_IMAGE_NAME);
        return Files.readAllBytes(selfieFile.toPath());
    }

    /**
     * Save profile picture
     * @param in Profile picture as binary input stream
     */
    public void saveSelfieAsBinary(String id, java.io.InputStream in) throws Exception {
        File selfieFile = new File(iamDataPath, id + "/" + USER_PROFILE_IMAGE_NAME);
        log.info("Try to save profile picture at: " + selfieFile.getAbsolutePath());

        if (!selfieFile.getParentFile().isDirectory()) {
            selfieFile.getParentFile().mkdir();
        }

        FileUtils.copyInputStreamToFile(in, selfieFile);
        in.close();

        indexProfilePictureAsVector(id);
    }

    /**
     * User is self-registering
     */
    public void selfRegister(SelfRegistrationInformation infos) throws Exception {
        validateInput("E-mail", infos.getEmail(), true,5, 50);
        validateInput("First name", infos.getFirstName(), true, 1, 50);
        validateInput("Last name", infos.getLastName(), true, 1, 50);

        Language lang = Language.valueOf(infos.getLanguage());

        validateInput("LinkedIn URL", infos.getLinkedInUrl(), false, 0, 200);
        validateInput("Feedback re how did you learn about Katie", infos.getHowDidYouLearnAboutKatie(), false, 0, 200);
        validateInput("Feedback re what would you like to use Katie for", infos.getHowDoYouWantToUseKatie(), false, 0, 200);
        validateInput("Feedback re what are your expectations regarding Katie", infos.getWhatAreYourExpectations(), false, 0, 200);

        String token = UUID.randomUUID().toString();
        saveSelfRegistrationInfoTemporarily(infos, token);

        sendConfirmationLinkEmailToUser(infos.getEmail(), infos.getFirstName(), infos.getLastName(), lang, token);
    }

    /**
     * Get spam score of input values
     * @return spam score, 0 means very unlikely to be spam, 1 means very likely to be spam
     */
    private float getSpamScore(String linkedInUrl) {
        if (!linkedInUrl.isEmpty()) {
            linkedInUrl = linkedInUrl.trim();
            if (!linkedInUrl.startsWith("https://www.linkedin.com")) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Approve self-registration
     * @param tokenAdmin JWT token
     */
    public void approveSelfRegistration(String tokenAdmin) throws Exception {
        User signedInUser = authService.getUser(false, false);
        if (!(signedInUser != null && signedInUser.getRole() == Role.ADMIN)) {
            throw new AccessDeniedException("User is either not signed in or has not role " + Role.ADMIN + "!");
        }
        if (jwtService.isJWTValid(tokenAdmin, null)) {
            String subject = jwtService.getJWTSubject(tokenAdmin);
            if (!subject.equals(SUBJECT_APPROVE_SELF_REGISTRATION)) {
                throw new Exception("Subject '" + subject + "' does not match with '" + SUBJECT_APPROVE_SELF_REGISTRATION + "'!");
            }
            String token = jwtService.getJWTClaimValue(tokenAdmin, TOKEN);
            String userId = jwtService.getJWTClaimValue(tokenAdmin, USER_ID);

            if (existsSelfRegistrationInfo(token)) {
                SelfRegistrationInformation infos = getSelfRegistrationInfo(token);
                User user = approveUser(userId);
                deleteSelfRegistrationInformation(token);

                String mailSubject = mailSubjectTag + " Your registration got approved, welcome to Katie!"; // TODO: language
                sendAccountInfoByEmail(mailSubject, user, null);
            } else {
                throw new Exception("Registration already got approved / rejected.");
            }
        } else {
            log.error("JWT not valid!");
            throw new Exception("JWT not valid!");
        }
    }

    /**
     * Unlock / enable user account
     */
    private User unlockUser(String userId) throws Exception {
        User user = getUserById(userId, false);
        user.unlock();
        usersXMLFileService.updateUser(user);
        return user;
    }

    /**
     * Approve user account
     */
    private User approveUser(String userId) throws Exception {
        User user = getUserById(userId, false);
        user.approve();
        usersXMLFileService.updateUser(user);
        return user;
    }

    /**
     * Reject self-registration
     * @param tokenAdmin JWT token
     */
    public void rejectSelfRegistration(String tokenAdmin) throws Exception {
        User signedInUser = authService.getUser(false, false);
        if (!(signedInUser != null && signedInUser.getRole() == Role.ADMIN)) {
            throw new AccessDeniedException("User is either not signed in or has not role " + Role.ADMIN + "!");
        }
        if (jwtService.isJWTValid(tokenAdmin, null)) {
            String subject = jwtService.getJWTSubject(tokenAdmin);
            if (!subject.equals(SUBJECT_APPROVE_SELF_REGISTRATION)) {
                throw new Exception("Subject '" + subject + "' does not match with '" + SUBJECT_APPROVE_SELF_REGISTRATION + "'!");
            }
            String token = jwtService.getJWTClaimValue(tokenAdmin, TOKEN);
            String userId = jwtService.getJWTClaimValue(tokenAdmin, USER_ID);

            if (existsSelfRegistrationInfo(token)) {
                SelfRegistrationInformation infos = getSelfRegistrationInfo(token);
                deleteUser(userId);
                deleteSelfRegistrationInformation(token);

                sendRejectionEmailToUser(infos.getEmail(), infos.getFirstName(), infos.getLastName(), Language.valueOf(infos.getLanguage()));
            } else {
                throw new Exception("Registration already got approved / rejected.");
            }
        } else {
            log.error("JWT not valid!");
            throw new Exception("JWT not valid!");
        }
    }

    /**
     * User confirmed email address and chose a password, therefore we create an actual user account
     * @param token Token associated with self-registration of user
     * @param password Password chosen by user
     * @return user object associated with newly created user account
     */
    public User selfRegisterEmailConfirmation(String token, String password) throws Exception {
        // INFO: Similar to resetPassword();
        if (token != null) {

            // TODO: If email confirmation took more than 72 hours, then abort registration
            SelfRegistrationInformation infos = getSelfRegistrationInfo(token);

            float spamScore = getSpamScore(infos.getLinkedInUrl());

            boolean userAccountLocked = false;

            boolean userAccountApproved = true;
            if (selfRegistrationApprovalRequired) {
                userAccountApproved = false;
            }

            log.info("Create account for username '" + infos.getEmail() + "' ...");
            User user = createUser(new Username(infos.getEmail()), infos.getEmail(), Role.USER, password, false, infos.getFirstName(), infos.getLastName(), infos.getLanguage(), userAccountLocked, userAccountApproved);

            if (selfRegistrationApprovalRequired) {
                notifyAdministratorReNewUser(infos, spamScore, token, user);
            } else {
                StringBuilder body = new StringBuilder("User confirmed email: " + infos.getEmail());
                body.append("\n\n");
                body.append("User profile: " + defaultHostname + "/#/iam/user/" + user.getId() + "/edit");

                mailerService.notifyAdministrator("User confirmed email and new user account created", body.toString(), null, false);

                // TODO: Create personal domain
            }

            return user;
        } else {
            log.error("No token provided!");
            throw new Exception("No token provided!");
        }
    }

    /**
     *
     */
    private void saveSelfRegistrationInfoTemporarily(SelfRegistrationInformation infos, String token) throws Exception {
        File selfRegistrationRequestDir = new File(selfRegistrationRequestsDataPath);
        if (!selfRegistrationRequestDir.isDirectory()) {
            selfRegistrationRequestDir.mkdirs();
        }

        // INFO: Save information temporarily at "volume/registrations-requests"
        ObjectMapper objectMapper = new ObjectMapper();
        File selfRegistrationRequestFile = new File(selfRegistrationRequestsDataPath, token + ".json");
        BufferedWriter writer = new BufferedWriter(new FileWriter(selfRegistrationRequestFile));
        objectMapper.writeValue(writer, infos);
        writer.close();
    }

    /**
     * @return true when temporary self-registration infos exist
     */
    private boolean existsSelfRegistrationInfo(String token) {
        return new File(selfRegistrationRequestsDataPath, token + ".json").isFile();
    }

    /**
     *
     */
    private SelfRegistrationInformation getSelfRegistrationInfo(String token) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        File selfRegistrationRequestFile = new File(selfRegistrationRequestsDataPath, token + ".json");
        BufferedReader reader = new BufferedReader(new FileReader(selfRegistrationRequestFile));
        SelfRegistrationInformation infos = objectMapper.readValue(reader, SelfRegistrationInformation.class);
        reader.close();

        return infos;
    }

    /**
     *
     */
    private void deleteSelfRegistrationInformation(String token) {
        File selfRegistrationRequestFile = new File(selfRegistrationRequestsDataPath, token + ".json");
        selfRegistrationRequestFile.delete();
    }

    /**
     * @param spamScore Spam score
     * @param token Token associated with self-registration
     * @param user Newly created user
     */
    private void notifyAdministratorReNewUser(SelfRegistrationInformation infos, float spamScore, String token, User user) throws Exception {
        String subject = "New User self-registered";

        StringBuilder body = new StringBuilder();
        body.append("The following user just self-registered:");
        body.append("\n\n");
        body.append("E-Mail: " + infos.getEmail());
        body.append("\n");
        body.append("Name: " + infos.getFirstName() + " " + infos.getLastName());
        body.append("\n\n");
        body.append("LinkedIn: " + infos.getLinkedInUrl());
        body.append("\n");
        body.append("How did you learn about Katie: " + infos.getHowDidYouLearnAboutKatie());
        body.append("\n");
        body.append("What would you like to use Katie for? : " + infos.getHowDoYouWantToUseKatie());
        body.append("\n");
        body.append("What are your expectations regarding Katie? : " + infos.getWhatAreYourExpectations());

        body.append("\n\n");
        body.append("Spam Score (0: very unlikely to be spam, 1: very likely to be spam): " + spamScore);

        if (selfRegistrationApprovalRequired) {
            body.append("\n\n");
            body.append("Check whether this email address is blacklisted: https://cleantalk.org/blacklists/" + infos.getEmail());
            body.append("\n\n");
            body.append("If not blacklisted, then please APPROVE the self-registration by clicking on the following link:");
            body.append("\n\n");
            String tokenAdmin = generateRegistrationToken(user, token, 172800); // INFO: 48h valid
            body.append(defaultHostname + "/api/v1/self-register-approve?token=" + tokenAdmin);
            body.append("\n\n");
            body.append("If blacklisted, then please REJECT the self-registration by clicking on the following link:");
            body.append("\n\n");
            body.append(defaultHostname + "/api/v1/self-register-reject?token=" + tokenAdmin);
        } else {
            body.append("\n\n");
            body.append("NOTE: Required self-registration approval by administrator is disabled.");
        }

        try {
            mailerService.notifyAdministrator(subject, body.toString(), null, false);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            mailerService.notifyAdministrator(subject, "Self-registration for user '" + infos.getEmail() + "' failed!", null, false);
        }
    }

    /**
     * Send email to user that self-registration got rejected
     */
    private void sendRejectionEmailToUser(String email, String firstName, String lastName, Language lang) throws Exception {
        String subject = mailSubjectTag + " Your self-registration request got rejected"; // TODO: language
        String body = "Your self-registration request got rejected. Please contact us at contact@wyona.com to get registered otherwise.";
        //String body = getRejectionMessage(email, firstName, lastName, lang);
        mailerService.send(email, null, subject, body, true);

    }

    /**
     * Send email to user containing confirmation link to complete self-registration
     */
    private void sendConfirmationLinkEmailToUser(String email, String firstName, String lastName, Language lang, String token) throws Exception {
        String subject = mailSubjectTag + " Please confirm your e-mail address"; // TODO: language

        String body = getConfirmationMessage(email, firstName, lastName, lang, token);

        mailerService.send(email, null, subject, body, true);

    }

    /**
     * Confirmation message to user in order to complete self-registration
     * @param lang Language of user, e.g. "en" or "de"
     */
    private String getConfirmationMessage(String email, String firstName, String lastName, Language lang, String token) throws Exception {
        TemplateArguments tmplArgs = new TemplateArguments(null, defaultHostname);

        tmplArgs.add("firstname", firstName);
        tmplArgs.add("lastname", lastName);

        tmplArgs.add("token", token);

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("confirm_self_registration_", lang, null);
        template.process(tmplArgs.getArgs(), writer);
        return writer.toString();
    }

    /**
     * @param user Newly created user
     * @param token Token associated with self-registration
     */
    private String generateRegistrationToken(User user, String token, int expirationInSeconds) throws Exception {
        JWTPayload payload = new JWTPayload();
        payload.setSub(SUBJECT_APPROVE_SELF_REGISTRATION);
        payload.setIss("Katie");
        HashMap<String, String> privateClaims = new HashMap<String, String>();
        privateClaims.put(TOKEN, token);
        privateClaims.put(USER_ID, user.getId());
        payload.setPrivateClaims(privateClaims);
        return jwtService.generateJWT(payload, expirationInSeconds, null);
    }

    /**
     * Check max length, etc.
     * @param key Input key, e.g. "First name" or "E-Mail"
     * @param value Input value, e.g. "Michael" or "michi@wyona.com"
     * @param required When set to true, then value is required
     */
    private void validateInput(String key, String value, boolean required, int minLength, int maxLength) throws Exception {
        if (required && value == null) {
            throw new Exception(key + " is required!");
        }

        if (value != null && value.length() < minLength) {
            throw new Exception(key + " is not allowed to be shorter than " + minLength + " characters!");
        }

        if (value != null && value.length() > maxLength) {
            throw new Exception(key + " is not allowed to be longer than " + maxLength + " characters!");
        }
    }

    /**
     * Search for similar profile pictures based on a given picture
     * @param data Given picture as base64
     * @return array of user IDs
     */
    public String[] searchForSimilarProfilePictures(String data) {
        log.info("Search for similar profile pictures ...");

        Config config = new Config("http", "localhost:8080"); // TODO: Get from application properties
        WeaviateClient client = new WeaviateClient(config);

        Field imageField = Field.builder().name("image").build();
        Field userIdField = Field.builder().name("userId").build();
        Fields fields = Fields.builder().fields(new Field[]{ userIdField }).build();
        //Fields fields = Fields.builder().fields(new Field[]{ userIdField, imageField }).build();

        NearImageArgument nearImage = client.graphQL().arguments().nearImageArgBuilder()
                .image(data)
                .build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName(PROFILE_PICTURE_CLASS_NAME)
                .withFields(fields)
                .withNearImage(nearImage)
                .run();

        java.util.List<String> ids = new ArrayList<String>();

        if (result.hasErrors()) {
            log.error("" + result.getError().getMessages());
        } else {
            log.info("Search similar profile picture result: " + result.getResult().getData());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode dataNode = mapper.valueToTree(result.getResult().getData());
            log.info("Answers: " + dataNode);
            JsonNode imageNodes = dataNode.get("Get").get(PROFILE_PICTURE_CLASS_NAME);
            if (imageNodes.isArray()) {
                for (JsonNode pictureNode: imageNodes) {
                    ids.add(pictureNode.get("userId").asText());
                }
            }
        }

        return ids.toArray(new String[0]);
    }

    /**
     * Index profile picture as vector using Weaviate
     * @param id User Id
     */
    private void indexProfilePictureAsVector(String id) throws Exception {

        String[] uuids = getObjectIdOfProfilePicture(id);
        if (uuids != null) {
            for (String uuid : uuids) {
                log.info("Delete profile picture '" + uuid + "' from vector index ...");
                deleteObjectFromVectorIndex(uuid); // TODO: Instead "delete" and "add" use "update"
            }
        }

        log.info("Index profile picture of user '" + id + "' as vector ...");

        Config config = new Config("http", "localhost:8080"); // TODO: Get from application properties
        WeaviateClient client = new WeaviateClient(config);

        java.util.Map<String, Object> properties = new java.util.HashMap<>();
        properties.put("userId", id);
        properties.put("image", getSelfieAsBase64(id));

        Result<WeaviateObject> result = client.data().creator()
                .withClassName(PROFILE_PICTURE_CLASS_NAME)
                .withID(UUID.randomUUID().toString())  // INFO: Weaviate requires a UUID and Katie user IDs are not always compliant with the UUID standard
                .withProperties(properties)
                .run();

        if (result.hasErrors()) {
            log.error("" + result.getError().getMessages());
        } else {
            log.info("Add profile picture result: " + result.getResult());
        }
    }

    /**
     * @param userId User Id
     * @return all object UUIDs which are associated with the given user Id
     */
    private String[] getObjectIdOfProfilePicture(String userId) {
        Config config = new Config("http", "localhost:8080"); // TODO: Get from application properties
        WeaviateClient client = new WeaviateClient(config);

        String[] path = {"userId"};
        WhereArgument whereArgument = WhereArgument.builder().
                operator(WhereOperator.Equal).
                valueString(userId).
                path(path).
                build();

        Field idField = Field.builder().name("id").build();
        Field[] subFields = new Field[1];
        subFields[0] = idField;
        Field additonalField = Field.builder().name("_additional").fields(subFields).build();
        Field userIdField = Field.builder().name("userId").build();
        Fields fields = Fields.builder().fields(new Field[]{ userIdField, additonalField }).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName(PROFILE_PICTURE_CLASS_NAME)
                .withWhere(whereArgument)
                .withFields(fields)
                .run();

        if (result.hasErrors()) {
            log.error("" + result.getError().getMessages());
            return null;
        } else {
            log.info("Get object id result: " + result.getResult().getData());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode dataNode = mapper.valueToTree(result.getResult().getData());
            log.info("Answers: " + dataNode);
            JsonNode nodes = dataNode.get("Get").get(PROFILE_PICTURE_CLASS_NAME);
            if (nodes.isArray()) {
                List<String> uuids = new ArrayList<String>();
                for (JsonNode node: nodes) {
                    uuids.add(node.get("_additional").get("id").asText());
                }
                return uuids.toArray(new String[0]);
            } else {
                log.info("No profile picture objects associated with user Id '" + userId + "'.");
                return null;
            }
        }
    }

    /**
     * Delete object from vector index of Weaviate
     * @param id Object Id
     */
    private void deleteObjectFromVectorIndex(String id) throws Exception {
        log.info("Delete object '" + id + "' from vector index ...");

        Config config = new Config("http", "localhost:8080"); // TODO: Get from application properties
        WeaviateClient client = new WeaviateClient(config);

        Result<Boolean> result = client.data().deleter()
                .withID(id)
                .run();

        if (result.hasErrors()) {
            log.error("" + result.getError().getMessages());
        } else {
            log.info("Delete object result: " + result.getResult());
        }
    }

    /**
     * Get user by username
     * @param username Username, e.g. "michael.wechner@wyona.com"
     * @param includingPassword When true, then returned user object will also contain password
     * @return user if user exists and null otherwise
     */
    public User getUserByUsername(Username username, boolean includingPassword, boolean includingJWT) {
        try {
            return usersXMLFileService.getIAMUserByUsername(username.getUsername(), includingPassword, includingJWT);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        log.warn("No such user with username '" + username + "'.");
        return null;
    }

    /**
     * Get user which submitted a particular resubmitted question
     * @param question Resubmitted question containing user information
     * @return user if user exists and null otherwise
     */
    public User getUser(ResubmittedQuestion question) {

        String userId = question.getQuestionerUserId();
        if (userId != null) {
            try {
                return usersXMLFileService.getIAMUserById(userId, false, false);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }

        String email = question.getEmail();
        if (email != null) {
            log.info("Email not used to get user, because there could be multiple accounts using the same email address '" + email + "'.");
        }

        String fcmToken = question.getFCMToken();
        if (fcmToken != null) {
            log.info("TODO: Match FCM token with JWT in order to get associated User.");
        }

        log.info("User which asked question not available, maybe because user not registered yet.");

        return null;
    }

    /**
     * Get all administrators
     */
    public User[] getAdministrators() throws Exception {
        User[] users = usersXMLFileService.getAllIAMUsers(false, false);
        List<User> admins = new ArrayList<User>();
        for (User user: users) {
            if (user.getRole() == Role.ADMIN) {
                admins.add(user);
            }
        }
        return admins.toArray(new User[0]);
    }

    /**
     * Get all users
     * @return all users
     */
    public User[] getUsers() throws Exception {
        log.info("Check whether user is signed in and has role " + Role.ADMIN);
        User signedInUser = authService.getUser(false, false);
        if (!(signedInUser != null && signedInUser.getRole() == Role.ADMIN)) {
            log.warn("Signed in user has not role " + Role.ADMIN + ", therefore permission denied.");
            throw new AccessDeniedException("Signed in user has not role " + Role.ADMIN + "!");
        }

        User[] users = usersXMLFileService.getAllIAMUsers(false, true);
        for (User user: users) {
            JWT jwt = user.getJwtToken();
            if (jwt  != null) {
                log.info("User '" + user.getUsername() + "' has a JWT.");
                user.setJwtToken(jwtService.convert(jwt.getToken(), true));
            } else {
                log.debug("User '" + user.getId() + "' has no token.");
            }
        }
        return users;
    }

    /**
     * @return true when user is authorized to get answer and false otherwise
     */
    public boolean isAuthorized(PermissionStatus permissionStatus) {
        if (permissionStatus == PermissionStatus.IS_PUBLIC || permissionStatus == PermissionStatus.USER_AUTHORIZED_TO_READ_ANSWER || permissionStatus == PermissionStatus.MEMBER_AUTHORIZED_TO_READ_ANSWER) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check whether a user requesting a particular answer is authorized to access answer, respectively get permission status
     * @param answer Particular answer which user is requesting to access
     * @param username Username of user requesting to access answer, whereas username can be null when not signed in
     * @return permission status
     */
    public PermissionStatus getPermissionStatus(Answer answer, String username) {
        if (username != null) {
            log.info("Check whether user '" + username + "' is authorized to access QnA '" + answer.getUuid() + "' ...");
        } else {
            log.info("Check whether anonymous user is authorized to access QnA '" + answer.getUuid() + "' ...");
        }

        if (answer.isPublic()) {
            log.info("QnA '" + answer.getUuid() + "' is public, therefore user is authorized to access QnA.");
            return PermissionStatus.IS_PUBLIC;
        } else {
            log.info("QnA '" + answer.getUuid() + "' is not public, therefore let's check permission status ...");
            if (username == null) {
                log.warn("User not signed in, therefore permission denied");
                return PermissionStatus.PERMISSION_DENIED;
            } else {
                // TODO: Consider doing the authentication check here, instead before calling this method
                log.info("User '" + username + "' is considered authenticated, when username is not null.");
                Permissions permissions = answer.getPermissions();
                User user = getUserByUsername(new Username(username), false, false);
                if (permissions != null) {
                    if (user != null) {
                        String userId = user.getId();
                        log.info("Check whether authenticated user with username '" + username + "' and Id '" + userId + "' has permission to access answer '" + answer.getUuid() + "' ...");
                        if (!permissions.isUserAuthorized(userId)) {
                            log.info("NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER");
                            return PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER;
                        } else {
                            log.info("USER_AUTHORIZED_TO_READ_ANSWER");
                            return PermissionStatus.USER_AUTHORIZED_TO_READ_ANSWER;
                        }
                    } else {
                        log.warn("No user with username '" + username + "'!");
                        return PermissionStatus.PERMISSION_DENIED;
                    }
                } else {
                   log.info("No permissions set for answer '" + answer.getDomainid() + " / " + answer.getUuid() + "', therefore every authenticated user can access this answer.");
                   try {
                       String domainId = answer.getDomainid();
                       if (xmlService.isUserMemberOfDomain(user.getId(), domainId)) {
                           log.info("User '" + username + "' is member of domain '" + domainId + "'.");
                           return PermissionStatus.MEMBER_AUTHORIZED_TO_READ_ANSWER;
                       } else {
                           log.info("User '" + username + "' is NOT member of domain '" + domainId + "'.");
                           return PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER;
                       }
                   } catch (Exception e) {
                       log.error(e.getMessage(), e);
                       return PermissionStatus.PERMISSION_DENIED;
                   }
                }
            }
        }
    }

    /**
     * Check whether provided password is correct
     * @param plaintext Plaintext password
     * @param user User for which password is provided
     * @return true when passwords match and false otherwise
     */
    public boolean matches(String plaintext, User user) {
        return matches(user.getPassword(), plaintext, user.getPasswordEncoding());
    }

    /**
     * Encode plaintext password with provided encoding algorithm and check whether it matches with encoded password
     * @param encoded Encoded password
     * @param plaintext Plaintext password
     * @param encoding Encoding algorithm, e.g. "plaintext" (no encoding) or "bcrypt"
     * @return true when passwords match and false otherwise
     */
    private boolean matches(String encoded, String plaintext, String encoding) {
        if (encoding.equals("plaintext")) {
            return plaintext.equals(encoded);
        } else if (encoding.equals("bcrypt")) {
            PasswordEncoder encoder = new BCryptPasswordEncoder();
            //log.debug("Plaintext: " + plaintext +  ", Encoded: " + encoder.encode(plaintext));
            return encoder.matches(plaintext, encoded);
        } else {
            log.error("No such encoding algorithm '" + encoding + "' implemented!");
            return false;
        }
    }
}
