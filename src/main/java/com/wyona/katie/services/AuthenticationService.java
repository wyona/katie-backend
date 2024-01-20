package com.wyona.katie.services;

import com.wyona.katie.models.User;

import com.wyona.katie.models.UserDetails;
import com.wyona.katie.models.User;
import com.wyona.katie.models.UserDetails;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.security.Principal;

import io.jsonwebtoken.*;

import org.springframework.core.io.ClassPathResource;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import java.nio.charset.StandardCharsets;
import software.pando.crypto.nacl.Crypto;

@Slf4j
@Component
public class AuthenticationService {

    @Value("${jwt.issuer}")
    private String issuer;

    @Autowired
    XMLService xmlService;
    @Autowired
    UsersXMLFileService usersXMLFileService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    public static final String JWT_CLAIM_DOMAIN_ID = "did";

    @Autowired
    public AuthenticationService() {
    }

    /**
     * Generate a secure random password
     * @param length Length of password
     */
    public String generatePassword(int length) {
        String symbols = "-/.^&*_!@%=+>)";
        String cap_letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String small_letters = "abcdefghijklmnopqrstuvwxyz";
        String numbers = "0123456789";

        String finalString = cap_letters + small_letters + numbers + symbols;

        Random random = new Random();

        char[] password = new char[length];

        for (int i = 0; i < length; i++) {
            password[i] = finalString.charAt(random.nextInt(finalString.length()));
        }

        return "" + password;
    }

    /**
     * Login using credentials
     * @param username Username, e.g. "michael.wechner@wyona.com"
     * @param password Plain text password
     */
    public void login(HttpServletRequest request, String username, String password) throws ServletException {
        username = username.toLowerCase();

        log.info("Try to login user '" + username + "' ...");

        // INFO: See https://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#login(java.lang.String,%20java.lang.String)
        request.login(username, password);


        // TODO: Consider to replace login() by directly calling authenticationManager.authenticate() and using spring boot session in order to set SameSite=strict (also see RememberMeService.java)
        // https://www.baeldung.com/manually-set-user-authentication-spring-security https://www.marcobehler.com/guides/spring-security
        //log.info("Try to login user '" + username + "' using AuthenticationManager ...");
        //Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        //SecurityContext sc = SecurityContextHolder.getContext();
        //sc.setAuthentication(authentication);
        //HttpSession session = request.getSession(true);
        // HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        //session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, sc);
    }

    /**
     * Login using credentials
     * @param username Username, e.g. "michael.wechner@wyona.com"
     * @param password Plain text password
     */
    public void login(String username, String password) throws ServletException {
        log.info("Try to login user '" + username + "' using AuthenticationManager ...");
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SecurityContext sc = SecurityContextHolder.getContext();
        sc.setAuthentication(authentication);
    }

    /**
     * Try to sign in user using JWT token
     * @return user details when login was successful, return null otherwise
     */
    public UserDetails tryJWTLogin(HttpServletRequest request) throws Exception {
        String jwtToken = jwtService.getJWT(request);
        if (jwtToken != null) {
            if (jwtService.isJWTValid(jwtToken, null)) {
                String username = jwtService.getJWTSubject(jwtToken);
                log.info("JWT is valid and contains username '" + username + "'.");

                if (isUserLocked(username)) {
                    throw new Exception("User '" + username + "' is locked!");
                }

                if (!hasValidJWT(username, jwtToken)) { // INFO: Check necessary, in order to have the possibility to invalidate JWT on server
                    throw new Exception("JWT is valid, but user '" + username + "' has no such valid JWT!");
                }

                if (!userIsSignedInBySession(request)) {
                    login(request, username, null);
                    log.info("User '" + username + "' was authenticated successfully using JWT.");
                } else {
                    log.info("User '" + username + "' has already a valid session.");
                }

                UserDetails userDetails = getUserDetails(request.getUserPrincipal());
                return userDetails;
            } else {
                throw new Exception("JWT not valid");
            }
        } else {
            log.info("No JWT token provided.");
        }
        return null;
    }

    /**
     * Check whether user account is locked
     * @return true when user is locked and false otherwise
     */
    private boolean isUserLocked(String username) {
        User user = getUserByUsername(username, false, false);
        if (user != null) {
            return user.isLocked();
        } else {
            log.error("No such user with username '" + username + "'!");
            return true;
        }
    }

    /**
     * Check whether JWT is valid
     * @param username User associated with JWT
     * @param jwtToken JWT token
     * @return true when user has valid JWT equals provided JWT, false otherwise
     */
    private boolean hasValidJWT(String username, String jwtToken) {
        log.info("Check whether user '" + username + "' has valid JWT ...");
        try {
            User user = getUserByUsername(username, false, true);
            if (user != null) {
                if (user.getJwtToken() != null && user.getJwtToken().getToken().equals(jwtToken)) {
                    return true;
                } else {
                    log.warn("User '" + username + "' exists, but has no matching JWT");
                    return false;
                }
            } else {
                log.warn("No such user '" + username + "'!");
                return false;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     *
     */
    public UserDetails getUserDetails(Principal userPrincipal) {
        Object[] grantedAuthorities = ((Authentication)userPrincipal).getAuthorities().toArray();
        String[] roles = new String[grantedAuthorities.length];
        for (int i = 0; i < grantedAuthorities.length; i++) {
            roles[i] = ((org.springframework.security.core.GrantedAuthority)grantedAuthorities[i]).getAuthority();
        }
        return new UserDetails(userPrincipal.getName(), roles);
    }

    /**
     * Get public key in PEM format
     */
    public String getPublicKeyAsPem() throws Exception {
        return readString(new ClassPathResource("jwt/public_key.pem").getInputStream());
    }

    /**
     * Convert InputStream to String
     * @param inputStream
     * @return
     * @throws Exception
     */
    private String readString(java.io.InputStream inputStream) throws Exception {

        java.io.ByteArrayOutputStream into = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; 0 < (n = inputStream.read(buf));) {
            into.write(buf, 0, n);
        }
        into.close();
        return new String(into.toByteArray());
    }

    /**
     * Check whether user is signed in by session
     * @return true when user is signed in by session and false otherwise
     */
    public boolean userIsSignedInBySession(HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get Id of signed in user
     * @return user Id when user is signed in and null otherwise
     */
    public String getUserId() {
        log.info("Get Id of signed in user ...");
        User user = getUser(false, false);
        if (user != null) {
            return user.getId();
        }
        log.info("User is not signed in.");
        return null;
    }

    /**
     * Get user object of signed in user or user associated with valid bearer token (JWT)
     * @return user when user has a valid session or request contains a valid bearer token (JWT), otherwise return null
     */
    public User getUser(boolean includingPassword, boolean includingJWT) {
        String username = getUsername();
        return getUserByUsername(username, includingPassword, includingJWT);
    }

    /**
     * Get user by username
     * @return user
     */
    private User getUserByUsername(String username, boolean includingPassword, boolean includingJWT) {
        log.debug("Get user object of '" + username + "' ...");
        if (username != null) {
            try {
                return usersXMLFileService.getIAMUserByUsername(username, includingPassword, includingJWT);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Get username of signed in user
     * @return username when user is signed in and null otherwise
     */
    public String getUsername() {
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = context.getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            log.info("User is not signed in.");
            return null;
        }
        String username = (String)authentication.getPrincipal();
        log.info("Username of signed in user: " + username);
        return username;
    }

    /**
     * https://discord.com/developers/docs/interactions/receiving-and-responding#security-and-authorization
     */
    public boolean isDiscordSignatureValid(String payload, String timestamp, String signature, String publicKey) {

        boolean isVerified = Crypto.signVerify(
                Crypto.signingPublicKey(fromHex(publicKey)),
                (timestamp + payload).getBytes(StandardCharsets.UTF_8),
                fromHex(signature));

        log.info("Signature with timestamp '" + timestamp + "' verified: " + isVerified);

        return isVerified;
    }

    /**
     * Check whether Slack signature is valid
     * https://api.slack.com/authentication/verifying-requests-from-slack
     * @param body Raw request body
     * @param signatureTimestamp X-Slack-Request-Timestamp
     * @param signature X-Slack-Signature
     * @param signingSecret Slack signing secret
     */
    public boolean isSignatureValid(String body, String signatureTimestamp, String signature, String signingSecret) {
        String basestring = "v0:" + signatureTimestamp + ":" + body;

        String hex = null;
        try {
            hex = hashMac(basestring, signingSecret);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        String mySignature = "v0=" + hex;
        log.info("... and compare my signature '" + mySignature + "' with X-Slack-Signature '" + signature + "' ...");
        if (mySignature.equals(signature)) {
            return true;
        } else {
            log.warn("Signatures do not match!");
            return false;
        }
    }

    /**
     * Encryption of a given text using the provided secretKey
     *
     * @param text
     * @param secretKey
     * @return the encoded string
     * @throws java.security.SignatureException
     */
    private static String hashMac(String text, String secretKey) throws java.security.SignatureException {
        String HASH_ALGORITHM = "HmacSHA256";

        try {
            Key sk = new SecretKeySpec(secretKey.getBytes(), HASH_ALGORITHM);
            Mac mac = Mac.getInstance(sk.getAlgorithm());
            mac.init(sk);
            final byte[] hmac = mac.doFinal(text.getBytes());
            return toHexString(hmac);
        } catch (java.security.NoSuchAlgorithmException e1) {
            // throw an exception or pick a different encryption method
            throw new java.security.SignatureException(
                    "error building signature, no such algorithm in device "
                            + HASH_ALGORITHM);
        } catch (java.security.InvalidKeyException e) {
            throw new SignatureException(
                    "error building signature, invalid key " + HASH_ALGORITHM);
        }
    }

    /**
     *
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return sb.toString();
    }

    /**
     *
     */
    private static byte[] fromHex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
