package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class User {

    private String id;
    private String username;
    private String password;
    private String passwordEncoding;
    private Role systemRole;
    private JWT jwtToken;
    private String email;
    private String firstName;
    private String lastName;
    private String language;
    private String myKatieId;
    private boolean locked = false;
    private boolean approved = false;

    // INFO: Values depend on domain
    private boolean isExpert;
    private boolean isModerator;
    private RoleDomain domainRole;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public User() {
    }

    /**
     * @param id User id, which cannot be changed once the id is created
     * @param password Password, whereas can be plaintext or encrypted  according to password encoding
     * @param locked True when account should is locked and false when account is activated
     * @param approved True when account got approved by administrator and false otherwise
     */
    public User(String id, String username, String password, String passwordEncoding, Role systemRole, JWT jwtToken, String email, String firstName, String lastName, boolean isExpert, boolean isModerator, RoleDomain domainRole, String language, boolean locked, boolean approved) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.passwordEncoding = passwordEncoding;
        this.systemRole = systemRole;
        this.jwtToken = jwtToken;
        this.email = email;
        
        this.firstName = firstName;
        this.lastName = lastName;
        this.language = language;
        this.myKatieId = null;
        this.locked = locked;
        this.approved = approved;

        this.isExpert = isExpert;
        this.isModerator = isModerator;
        this.domainRole = domainRole;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     *
     */
    public String getFirstname() {
        return firstName;
    }

    /**
     *
     */
    public void setFirstname(String firstName) {
        this.firstName = firstName;
    }

    /**
     *
     */
    public String getLastname() {
        return lastName;
    }

    /**
     *
     */
    public void setLastname(String lastName) {
        this.lastName = lastName;
    }

    /**
     *
     */
    public String getPassword() {
        return password;
    }

    /**
     *
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return password encoding, e.g. "plaintext" (no encoding) or "bcrypt"
     */
    public String getPasswordEncoding() {
        return passwordEncoding;
    }

    /**
     *
     */
    public void setPasswordEncoding(String passwordEncoding) {
        this.passwordEncoding = passwordEncoding;
    }

    /**
     * Get system role
     */
    public Role getRole() {
        return systemRole;
    }

    /**
     * Set system role
     */
    public void setRole(String role) {
        this.systemRole = Role.valueOf(role);
    }

    /**
     * Get domain role, which depends on domain
     */
    public RoleDomain getDomainRole() {
        return domainRole;
    }

    /**
     * Set domain role, which depends on domain
     */
    public void setDomainRole(RoleDomain domainRole) {
        this.domainRole = domainRole;
    }

    /**
     *
     */
    public JWT getJwtToken() {
        return jwtToken;
    }

    /**
     *
     */
    public void setJwtToken(JWT jwtToken) {
        this.jwtToken = jwtToken;
    }

    /**
     * @param isExpert True when user is an expert for a particular domain or multiple domains, false otherwise
     */
    public void setIsExpert(boolean isExpert) {
        this.isExpert = isExpert;
    }

    /**
     * @return true when user is an expert for a particular domain or multiple domains, false otherwise
     */
    public boolean getIsExpert() {
        return isExpert;
    }

    /**
     * @param isModerator True when user is a moderator for a particular domain or multiple domains, false otherwise
     */
    public void setIsModerator(boolean isModerator) {
        this.isModerator = isModerator;
    }

    /**
     * @return true when user is a moderator for a particular domain or multiple domains, false otherwise
     */
    public boolean getIsModerator() {
        return isModerator;
    }

    /**
     * @return two-letter language code, e.g. "de" or "en"
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @param language Two-letter language code, e.g. "de" or "en"
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @param id MyKatie domain Id
     */
    public void setMyKatieId(String id) {
        this.myKatieId = id;
    }

    /**
     * @return MyKatie domain Id
     */
    public String getMyKatieId() {
        return myKatieId;
    }

    /**
     * @return true when user is locked / disabled or false otherwise
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     *
     */
    public void unlock() {
        this.locked = false;
    }

    /**
     * @return true when account was approved by administrator
     */
    public boolean isApproved() {
        return approved;
    }

    /**
     * Approve user account
     */
    public void approve() {
        this.approved = true;
    }
}
