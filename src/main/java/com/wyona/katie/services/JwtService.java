package com.wyona.katie.services;

import com.wyona.katie.models.JWT;
import com.wyona.katie.models.JWTPayload;
import com.wyona.katie.models.User;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.Principal;

import io.jsonwebtoken.*;

import org.springframework.core.io.ClassPathResource;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Component
public class JwtService {

    @Value("${jwt.issuer}")
    private String issuer;

    @Value("${jwt.allowed.clock.skew.seconds}")
    private Long allowedClockSkewSeconds;

    public static final String JWT_CLAIM_DOMAIN_ID = "did";
    public static final String JWT_CLAIM_SCOPE = "scope";
    public static final String JWT_CLAIM_ENDPOINT = "endpoint";

    public static final String SCOPE_READ_LABELS = "read:labels";
    public static final String SCOPE_CONNECT_WITH_DOMAIN = "connect-with-domain";
    public static final String GET_SENTENCE_SIMILARITY = "get-sentence-similarity";

    @Value("${config.data_path}")
    private String configDataPath;

    /**
     * Generate identity JWT token
     * @param user Katie user
     * @param seconds Token validity in seconds, e.g. 3600 (60 minutes)
     * @param addProfile When true, then user profile attributes are added, like for example date of birth or selfie as Base64
     * @param selfie Selfie image as Base64
     * @return identity JWT token
     */
    public String generateJWT(User user, long seconds, boolean addProfile, String selfie) throws Exception {
        PrivateKey privateKey = getPrivateKey();
        Date issuedAt = new Date();

        JwtBuilder builder = Jwts.builder()
                .setIssuer(issuer)
                .setSubject(user.getUsername()) // INFO: See https://tools.ietf.org/html/rfc7519#section-4.1.2
                .signWith(SignatureAlgorithm.RS256, privateKey) // INFO: Private key length (e.g. 512) has to be longer than signature length (e.g. 256)
                .setIssuedAt(issuedAt)
                .setExpiration(new Date(issuedAt.getTime() + seconds * 1000));

        if (addProfile) {
            builder.claim("dob", "2020.10.22"); // TODO: Date of birth

            if (selfie != null) {
                builder.claim("selfie", selfie);
            }
        }

        if (user.getFirstname() != null) {
            builder.claim("firstname", user.getFirstname());
        }
        if (user.getLastname() != null) {
            builder.claim("lastname", user.getLastname());
        }

        String jwt = builder.compact();

        return jwt;
    }

    /**
     * Generate a generic JWT token
     * @param payload  Payload containing Issuer (e.g. "AskKatie"), Subject (e.g. UUID "71aa8dc6-0f19-4787-bd91-08fe1e863473" or email "michael.wechner@wyona.com") and private claims (e.g. "firstname", "lastname", "email", ...)
     * @param seconds Token validity in seconds, e.g. 3600 (60 minutes)
     * @param privateKey Optional private key
     * @return JWT token
     */
    public String generateJWT(JWTPayload payload, long seconds, PrivateKey privateKey) throws Exception {
        if (privateKey == null) {
            log.info("Use default private key ...");
            privateKey = getPrivateKey();
        }
        Date issuedAt = new Date();

        String myIssuer = issuer; // INFO: Set globally configured issuer
        if (payload.getIss() != null) {
            myIssuer = payload.getIss();
        }

        JwtBuilder builder = Jwts.builder()
            .setIssuer(myIssuer)
            .setSubject(payload.getSub()) // INFO: See https://tools.ietf.org/html/rfc7519#section-4.1.2
            .signWith(SignatureAlgorithm.RS256, privateKey) // INFO: Private key length (e.g. 512) has to be longer than signature length (e.g. 256)
            .setIssuedAt(issuedAt)
            .setExpiration(new Date(issuedAt.getTime() + seconds * 1000));

        if (payload.getPrivateClaims() != null) {
            for (Map.Entry claim : payload.getPrivateClaims().entrySet()) {
                builder.claim((String) claim.getKey(), (String) claim.getValue());
            }
        } else {
            log.info("No private claims set.");
        }

        return builder.compact();
    }

    /**
     * Generate access JWT token
     * @param username Username, e.g. "michael.wechner"
     * @param domainId DomainId, e.g. "313b2155-0df0-439a-beb5-c666208ffc8d"
     * @param seconds Token validity in seconds, e.g. 60 (1 minute)
     * @param claims Additional claims
     * @return accessJWT token
     */
    public String generateJWT(String username, String domainId, long seconds, HashMap<String, String> claims) throws Exception {
        PrivateKey privateKey = getPrivateKey();
        Date issuedAt = new Date();

        JwtBuilder builder = Jwts.builder()
            .setIssuer(issuer)
            //.setSubject(username) // INFO: See https://tools.ietf.org/html/rfc7519#section-4.1.2
            .signWith(SignatureAlgorithm.RS256, privateKey) // INFO: Private key length (e.g. 512) has to be longer than signature length (e.g. 256)
            .setIssuedAt(issuedAt)
            .setExpiration(new Date(issuedAt.getTime() + seconds * 1000));

        if (username != null) {
            builder.setSubject(username); // INFO: See https://tools.ietf.org/html/rfc7519#section-4.1.2
        }

        builder.claim(JWT_CLAIM_DOMAIN_ID, domainId);

        if (claims != null && !claims.isEmpty()) {
            claims.forEach((key, value) -> {
                // TODO: Check for duplicated keys
                builder.claim(key, value);
            });
        }

        return builder.compact();
    }

    /**
     * Get private key to sign JWT
     */
    private PrivateKey getPrivateKey() throws Exception {
        String privateKeyContent = getPrivateKeyAsPem();

        privateKeyContent = privateKeyContent.replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "");
        //log.debug("Private key: " + privateKeyContent);

        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getMimeDecoder().decode(privateKeyContent));

        PrivateKey privKey = kf.generatePrivate(keySpec);
        return privKey;
    }

    /**
     * Get public key in PEM format
     */
    public String getPublicKeyAsPem() throws Exception {
        // TODO: If private and public keys do not exist yet, then generate them, see for example https://docs.oracle.com/javase/tutorial/security/apisign/step2.html

        //return readString(new ClassPathResource("jwt/public_key.pem").getInputStream());

        File file = new File(configDataPath,"jwt/public_key.pem");
        InputStream in = new FileInputStream(file);
        String key = readString(in);
        in.close();
        return key;
    }

    /**
     *
     */
    public String getPrivateKeyAsPem() throws Exception {
        // TODO: If private and public keys do not exist yet, then generate them, see for example https://docs.oracle.com/javase/tutorial/security/apisign/step2.html

        //return readString(new ClassPathResource("jwt/private_key_pkcs8.pem").getInputStream());

        File file = new File(configDataPath,"jwt/private_key_pkcs8.pem");
        if (!file.isFile()) {
            log.error("No private key exists: " + file.getAbsolutePath());
            throw new Exception("No private key exists: " + file.getName());
        }

        InputStream in = new FileInputStream(file);
        String key = readString(in);
        in.close();
        return key;
    }

    /**
     * Get public key to validate JWT
     */
    private RSAPublicKey getPublicKey() throws Exception {
        String publicKeyContent = getPublicKeyAsPem();

        publicKeyContent = publicKeyContent.replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "");
        log.debug("Public key: " + publicKeyContent);

        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");

        X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getMimeDecoder().decode(publicKeyContent));
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(keySpecX509);

        return pubKey;
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
     * Get JWT object from encoded token string
     * @param blackOut When true, then black-out encoded token string
     */
    public JWT convert(String token, boolean blackOut) {
        JWT jwt = new JWT(token);

        if (blackOut) {
            // INFO: Do not show token itself (TODO: Or only part of the token?!)
            jwt.setToken(null);
        }

        boolean isExpired = true;
        long expiration = -1;
        long issuedAt = -1;
        String subject = null;
        String issuer = null;
        try {
            expiration = getClaimFromToken(token, Claims::getExpiration).getTime();
            isExpired = false;

            issuedAt = getJWTIssuedAt(token);
            subject = getJWTSubject(token);
            issuer = getJWTIssuer(token);
        } catch(ExpiredJwtException e) {
            isExpired = true;
            expiration = e.getClaims().getExpiration().getTime();
            issuedAt = e.getClaims().getIssuedAt().getTime();
            subject = e.getClaims().getSubject();
            issuer = e.getClaims().getIssuer();
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        jwt.setIsExpired(isExpired);
        jwt.getPayload().setExp(expiration);
        jwt.getPayload().setIat(issuedAt);
        jwt.getPayload().setSub(subject);
        jwt.getPayload().setIss(issuer);

        return jwt;
    }

    /**
     * Check whether JWT is valid
     * https://auth0.com/docs/tokens/access-tokens/validate-access-tokens
     * @param publicKey Optional custom public key, e.g. to validate token from an external provider, like for example Microsoft
     * @return true when token is valid and false otherwise
     */
    public boolean isJWTValid(String jwtToken, java.security.PublicKey publicKey) {
        log.info("Check signature and expiry date of JWT token ...");
        try {
            if (publicKey == null) {
                log.info("Use default public key ...");
                publicKey = getPublicKey();
            }

            Jwts.parser().setSigningKey(publicKey).setAllowedClockSkewSeconds(allowedClockSkewSeconds).parseClaimsJws(jwtToken);
            log.info("JWT is valid :-)");
            return true;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: " + e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token: " + e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid:" + e.getMessage());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * @param claim Claim Id, e.g. "kid"
     */
    public String getHeaderValue(String jwtToken, String claim) {
        log.info("Get header value for '" + claim + "' ...");
        String header = getChunk(jwtToken,0);
        log.info("JWT header: " + header);
        return parseChunk(header, claim);
    }

    /**
     * @param claim Claim Id, e.g. "aud"
     */
    public String getPayloadValue(String jwtToken, String claim) {
        log.info("Get payload value for '" + claim + "' ...");
        String payload = getChunk(jwtToken, 1);
        log.info("JWT payload: " + payload);
        return parseChunk(payload, claim);
    }

    /**
     *
     */
    private String getChunk(String jwtToken, int i) {
        String[] chunks = jwtToken.split("\\.");
        Base64.Decoder decoder = Base64.getUrlDecoder();
        return new String(decoder.decode(chunks[i]));
    }

    /**
     *
     */
    private String parseChunk(String chunk, String claim) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode headerNode = mapper.readTree(chunk);
            return headerNode.get(claim).asText();
        } catch(Exception e) {
            log.error(e.getMessage(),e);
            return null;
        }
    }

    /**
     * Get value of subject claim
     * @return subject value of JWT token or null when token is not valid
     */
    public String getJWTSubject(String jwtToken) {
        try {
            // TODO return getPayloadValue(jwtToken, "sub");
            return getClaimFromToken(jwtToken, Claims::getSubject);
        } catch(ExpiredJwtException e) {
            log.warn(e.getMessage());
            Date expiryDate = e.getClaims().getExpiration();
            log.warn("Token expired at " + expiryDate);
            return null;
        } catch(Exception e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get value of issuer claim
     * @return issuer value of JWT token or null when token is not valid
     */
    public String getJWTIssuer(String jwtToken) {
        log.info("Try to get issuer from token ...");
        try {
            // TODO return getPayloadValue(jwtToken, "iss");
            return getClaimFromToken(jwtToken, Claims::getIssuer);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get expiration time of token
     * @return expiration time of token in seconds Unix epoch or -1 when token is not valid
     */
    protected long getJWTExpirationTime(String jwtToken) {
        try {
            return getClaimFromToken(jwtToken, Claims::getExpiration).getTime();
        } catch(Exception e) {
            //log.error(e.getMessage(), e);
            log.warn(e.getMessage());
            return -1;
        }
    }

    /**
     * Get time when token was issued
     * @return time when token was issued at in seconds Unix epoch or -1 when token is not valid
     */
    protected long getJWTIssuedAt(String jwtToken) {
        try {
            return getClaimFromToken(jwtToken, Claims::getIssuedAt).getTime();
        } catch(Exception e) {
            //log.error(e.getMessage(), e);
            log.warn(e.getMessage());
            return -1;
        }
    }

    /**
     * @return array of scope(s), e.g. "read:labels"
     */
    public String[] getJWTScope(String jwtToken) {
        String value = getJWTClaimValue(jwtToken, JWT_CLAIM_SCOPE);
        if (value != null && value.length() > 0) {
            return value.split(" ");
        }
        return null;
    }

    /**
     * Get value of a particular claim
     * @param claim Custom claim name, e.g. "dob" (date of birth)
     * @return value of a particular claim of JWT token or null when token is not valid
     */
    public String getJWTClaimValue(String jwtToken, String claim) {
        // TODO return getPayloadValue(jwtToken, claim);
        try {
            Claims claims = getAllClaimsFromToken(jwtToken);

            return (String)claims.get(claim);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get a particular claim from JWT token
     */
    private <T> T getClaimFromToken(String jwtToken, java.util.function.Function<Claims, T> claimsResolver) throws Exception {
        final Claims claims = getAllClaimsFromToken(jwtToken);
        return claimsResolver.apply(claims);
    }

    /**
     * Get all claims from JWT token, e.g. "iss", "sub", "iat", "exp", ...
     */
    private Claims getAllClaimsFromToken(String jwtToken) throws Exception {
        return Jwts.parser()
                // TODO: Why is it necessary to set public key as signing key in order to read claims (see for example https://jwt.io/)?!
                .setSigningKey(getPublicKey())
                .parseClaimsJws(jwtToken)
                .getBody();
    }

    /**
     * Get JWT from Authorization request header
     */
    public String getJWT(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            if (authorizationHeader.indexOf("Bearer") >= 0) {
                return authorizationHeader.substring("Bearer".length()).trim();
            } else {
                log.warn("Authorization header does not contain prefix 'Bearer'.");
                return null;
            }
        } else {
            log.info("No Authorization header.");
            return null;
        }
    }
}
