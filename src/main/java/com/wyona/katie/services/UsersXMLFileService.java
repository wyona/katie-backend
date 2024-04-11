package com.wyona.katie.services;

import com.wyona.katie.models.JWT;
import com.wyona.katie.models.Role;
import com.wyona.katie.models.User;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.io.*;

@Slf4j
@Component
public class UsersXMLFileService {

    @Value("${iam.data_path}")
    private String iamDataPath;

    @Autowired
    private XMLService xmlService;

    private static final String KATIE_NAMESPACE_1_0_0 = "http://www.wyona.com/askkatie/1.0.0";

    private static final String IAM_USERS_TAG = "users";
    private static final String IAM_USER_TAG = "user";
    private static final String IAM_USER_UUID_ATTR = "uuid";
    private static final String IAM_USER_LOCKED_ATTR = "locked";
    private static final String IAM_USERNAME_TAG = "username";
    private static final String IAM_PASSWORD_TAG = "password";
    private static final String IAM_PASSWORD_ENCODING_ATTR = "encoding";
    private static final String IAM_ROLE_TAG = "role";
    private static final String IAM_JWT_TAG = "jwt";
    private static final String IAM_EMAIL_TAG = "email";
    private static final String IAM_FIRST_NAME_TAG = "firstname";
    private static final String IAM_LAST_NAME_TAG = "lastname";
    private static final String IAM_LANGUAGE_TAG = "language";

    /**
     * Get XML containing all users
     */
    private File getUsersXMLFile() {
        File usersXMLFile = new File(iamDataPath, "users.xml");
        log.debug("Users XML file: " + usersXMLFile);
        return usersXMLFile;
    }

    /**
     * Get XML file containing user data
     * @param id User Id, e.g. "71aa8dc6-0f19-4787-bd91-08fe1e863473"
     */
    private File getUserXMLFile(String id) {
        File userXMLFile = new File(iamDataPath, id + "/user.xml");
        log.debug("User XML file: " + userXMLFile);
        return userXMLFile;
    }

    /**
     * @return true when users configuration xml file exists and false otherwise
     */
    protected boolean existsUsersConfig() {
        File usersXMLFile = getUsersXMLFile();
        if (usersXMLFile.isFile()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create XML file containing list of all users
     */
    protected void createUsersConfig() throws Exception {
        File usersXMLFile = getUsersXMLFile();
        usersXMLFile.getParentFile().mkdirs();
        Document doc = xmlService.createDocument(KATIE_NAMESPACE_1_0_0, IAM_USERS_TAG);
        xmlService.save(doc, usersXMLFile);
    }

    /**
     * Remove user
     * @param id User Id
     */
    protected void removeUser(String id) throws Exception {
        Document doc = xmlService.read(getUsersXMLFile());
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(IAM_USER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element) userNL.item(i);
            String userId = userEl.getAttribute(IAM_USER_UUID_ATTR);
            log.debug("User Id: " + userId);
            if (id.equals(userId)) {
                doc.getDocumentElement().removeChild(userEl);
                break;
            }
        }

        xmlService.save(doc, getUsersXMLFile());
    }

    /**
     * Add user to XML configuration file containing users
     */
    protected void addUser(User user) throws Exception {
        Document doc = xmlService.read(getUsersXMLFile());
        doc.getDocumentElement().normalize();

        Element userEl = doc.createElement(IAM_USER_TAG);
        doc.getDocumentElement().appendChild(userEl);

        if (user.getId() != null) {
            userEl.setAttribute(IAM_USER_UUID_ATTR, user.getId());
        } else {
            throw new Exception("No user Id provided!");
        }

        if (user.isLocked()) {
            userEl.setAttribute(IAM_USER_LOCKED_ATTR, "true");
        }

        Element usernameEl = xmlService.appendElement(userEl, IAM_USERNAME_TAG, user.getUsername());
        if (usernameEl == null) {
            throw new Exception("No username provided!");
        }

        Element roleEl = xmlService.appendElement(userEl, IAM_ROLE_TAG, user.getRole().toString());
        if (roleEl == null) {
            throw new Exception("No role provided!");
        }

        Element emailEl = xmlService.appendElement(userEl, IAM_EMAIL_TAG, user.getEmail());

        Element passwordEl = xmlService.appendElement(userEl, IAM_PASSWORD_TAG, user.getPassword());
        if (passwordEl != null) {
            passwordEl.setAttribute(IAM_PASSWORD_ENCODING_ATTR, user.getPasswordEncoding());
        }

        xmlService.appendElement(userEl, IAM_FIRST_NAME_TAG, user.getFirstname());
        xmlService.appendElement(userEl, IAM_LAST_NAME_TAG, user.getLastname());

        xmlService.appendElement(userEl, IAM_LANGUAGE_TAG, user.getLanguage());

        xmlService.save(doc, getUsersXMLFile());
    }

    /**
     * Upddate user inside XML configuration file containing users
     */
    protected void updateUser(User user) throws Exception {
        Document doc = xmlService.read(getUsersXMLFile());
        doc.getDocumentElement().normalize();

        Element userEl = getUserElementByUsername(doc, user.getUsername());
        log.info("Update user '" + user.getUsername() + "' ...");

        log.info("Set locked attribute of user '" + user.getId() + "' to '" + user.isLocked() + "'.");
        userEl.setAttribute(IAM_USER_LOCKED_ATTR, "" + user.isLocked());

        if (user.getPassword() != null && user.getPassword().length() > 0) {
            Element pwdEl = getChildByTagName(userEl, IAM_PASSWORD_TAG, true);
            pwdEl.setTextContent(user.getPassword());
            pwdEl.setAttribute(IAM_PASSWORD_ENCODING_ATTR, user.getPasswordEncoding());
        }

        log.info("Update first name: " + user.getFirstname());
        Element firstNameEl = getChildByTagName(userEl, IAM_FIRST_NAME_TAG, true);
        firstNameEl.setTextContent(user.getFirstname());

        log.info("Update last name: " + user.getLastname());
        Element lastNameEl = getChildByTagName(userEl, IAM_LAST_NAME_TAG, true);
        lastNameEl.setTextContent(user.getLastname());

        log.info("Update email: " + user.getEmail());
        Element emailEl = getChildByTagName(userEl, IAM_EMAIL_TAG, true);
        emailEl.setTextContent(user.getEmail());

        log.info("Update language: " + user.getLanguage());
        Element languageEl = getChildByTagName(userEl, IAM_LANGUAGE_TAG, true);
        languageEl.setTextContent(user.getLanguage());

        xmlService.save(doc, getUsersXMLFile());
    }

    /**
     * @param create If set to true, then create child element when child element does not exist yet
     */
    private Element getChildByTagName(Element parent, String name, boolean create) {
        NodeList nl = parent.getElementsByTagName(name);
        if (nl.getLength() == 1) {
            return (Element) nl.item(0);
        } else if (nl.getLength() > 1) {
            log.warn("There are more than one child elements with name '" + name + "'!");
            return (Element) nl.item(0);
        } else {
            log.debug("Parent element '" + parent.getTagName() + "' does not have child '" + name + "'.");
            if (create) {
                Element child = parent.getOwnerDocument().createElement(name);
                parent.appendChild(child);
                return child;
            } else {
                return null;
            }
        }
    }

    /**
     * Get user element by username
     */
    private Element getUserElementByUsername(Document doc, String username) {
        NodeList userNL = doc.getElementsByTagName(IAM_USER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element)userNL.item(i);
            String id = userEl.getAttribute(IAM_USER_UUID_ATTR);
            log.debug("User Id: " + id);

            NodeList usernameNL = userEl.getElementsByTagName(IAM_USERNAME_TAG);
            String uname = ((Element)usernameNL.item(0)).getTextContent();
            if (uname.equals(username)) {
                return userEl;
            }
        }
        log.error("No such user with username '" + username + "'!");
        return null;
    }

    /**
     *
     */
    protected void appendJWT(String username, String jwtToken) throws Exception {
        Document doc = xmlService.read(getUsersXMLFile());
        doc.getDocumentElement().normalize();

        Element userEl = getUserElementByUsername(doc, username);

        if (userEl != null) {
            // TODO: Replace JWT if one already exists (actually allow multiple valid tokens and remove all expired tokens)
            log.info("Add JWT to user '" + username + "' ...");

            NodeList jwtNL = userEl.getElementsByTagName(IAM_JWT_TAG);
            if (jwtNL.getLength() > 0) {
                ((Element)jwtNL.item(0)).setTextContent(jwtToken);
            } else {
                log.info("No '" + IAM_JWT_TAG + "' element yet, therefore create and append one ...");
                Element jwtEl = doc.createElement(IAM_JWT_TAG);
                userEl.appendChild(jwtEl);
                jwtEl.setTextContent(jwtToken);
            }

            xmlService.save(doc, getUsersXMLFile());
            return;
        }

        throw new Exception("No such user '" +  username + "'!");
    }

    /**
     * Get a user with a particular username managed by IAM system
     * @param username Username of user
     * @param includingPassword True when password should be returned as well
     * @param includingJWT True when JWT should be returned as well
     */
    public User getIAMUserByUsername(String username, boolean includingPassword, boolean includingJWT) throws Exception {
        return getIAMUser(username, includingPassword, includingJWT);
    }

    /**
     * Get a particular user managed by IAM system
     * @param value Value of element, e.g. username "michael.wechner@wyona.com"
     * @param includingPassword True when password should be returned as well
     * @param includingJWT True when JWT should be returned as well
     */
    private User getIAMUser(String value, boolean includingPassword, boolean includingJWT) throws Exception {

        // TODO: Only temporarily to test, whereas set to 'true' in order to test
        if (false) {
            if (value.equals("michael.wechner@wyona.com")) {
                User user = getIAMUserV2("TODO_1", includingPassword, includingJWT);
                if (user != null) {
                    return user;
                }
            }
        }

        String nameEl = IAM_USERNAME_TAG;
        log.debug("Get user from IAM where the element '" + nameEl + "' has the value '" + value + "' ...");

        log.info("Read users XML file ...");
        Document doc = xmlService.read(getUsersXMLFile());

        log.debug("Normalize document ...");
        doc.getDocumentElement().normalize();

        // TODO: Consider using getUserElementByUsername(...)
        //Element userElement = getUserElementByUsername(doc, value);

        log.debug("Get all elements with tag name '" + IAM_USER_TAG + "' ...");
        NodeList userNL = doc.getElementsByTagName(IAM_USER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            Element userEl = (Element)userNL.item(i);

            String elementValue = null;
            NodeList elementNL = userEl.getElementsByTagName(nameEl);
            if (elementNL.getLength() == 1) {
                elementValue = ((Element)elementNL.item(0)).getTextContent();
                if (value.equals(elementValue)) {
                    log.info("User '" + value + "' found.");
                    return parseUserElement(userEl, includingPassword, includingJWT);
                }
            }
        }

        log.warn("No user where element '" + nameEl + "' has value '" + value + "'!");
        return null;
    }

    /**
     * Get a particular user managed by IAM system
     * @param id User Id, e.g. "71aa8dc5-0f19-4787-bd72-08fe1e863475"
     * @param includingPassword True when password should be returned as well
     * @param includingJWT True when JWT should be returned as well
     */
    private User getIAMUserV2(String id, boolean includingPassword, boolean includingJWT) throws Exception {
        File userFile = getUserXMLFile(id);
        if (userFile.isFile()) {
            log.info("Read user XML file '" + userFile + "' ...");
            Document doc = xmlService.read(userFile);
            log.info("Parse user XML ...");
            return parseUserElement(doc.getDocumentElement(), includingPassword, includingJWT);
        } else {
            log.error("No such user file '" + userFile.getAbsolutePath() + "'!");
            return null;
        }
    }

    /**
     * Get a user with particular Id managed by IAM system
     * @param id User id
     * @param includingPassword True when password should be returned as well
     * @param includingJWT True when JWT should be returned as well
     */
    protected User getIAMUserById(String id, boolean includingPassword, boolean includingJWT) throws Exception {
        Document doc = xmlService.read(getUsersXMLFile());
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(IAM_USER_TAG);
        for (int i = 0; i < userNL.getLength(); i++) {
            User user = parseUserElement((Element)userNL.item(i), includingPassword, includingJWT);
            if (id.equals(user.getId())) {
                return user;
            }
        }

        log.warn("No such user with id '" + id + "'!");
        return null;
    }

    /**
     * Get user data
     * @param userEl User XML element containing user information
     */
    private User parseUserElement(Element userEl, boolean includingPassword, boolean includingJWT) {
        String id = userEl.getAttribute(IAM_USER_UUID_ATTR);
        log.debug("Parse user element with Id: " + id);

        JWT jwt = null;
        if (includingJWT) {
            log.info("Check whether user with id '" + id + "' has a JWT ...");
            NodeList jwtNL = userEl.getElementsByTagName(IAM_JWT_TAG);
            if (jwtNL.getLength() == 1) {
                String _jwt = ((Element) jwtNL.item(0)).getTextContent();
                jwt = new JWT(_jwt);
            } else {
                log.info("User with id '" + id + "' does not have a JWT.");
            }
        }

        NodeList roleNL = userEl.getElementsByTagName(IAM_ROLE_TAG);
        Role role = getRole(((Element)roleNL.item(0)).getTextContent());

        String password = null;
        String passwordEncoding = null;
        if (includingPassword) {
            NodeList passwordNL = userEl.getElementsByTagName(IAM_PASSWORD_TAG);
            if (passwordNL.getLength() == 1) {
                Element passwordEl = (Element)passwordNL.item(0);
                password = passwordEl.getTextContent();
                passwordEncoding = passwordEl.getAttribute(IAM_PASSWORD_ENCODING_ATTR);
            }
        }

        String email = null;
        NodeList emailNL = userEl.getElementsByTagName(IAM_EMAIL_TAG);
        if (emailNL.getLength() == 1) {
            email = ((Element)emailNL.item(0)).getTextContent();
        }

        NodeList usernameNL = userEl.getElementsByTagName(IAM_USERNAME_TAG);
        String username = ((Element)usernameNL.item(0)).getTextContent();

        String lastName = null;
        NodeList lastnameNL = userEl.getElementsByTagName(IAM_LAST_NAME_TAG);
        if (lastnameNL.getLength() == 1) {
            lastName = ((Element)lastnameNL.item(0)).getTextContent();
        }

        String firstName = null;
        NodeList firstnameNL = userEl.getElementsByTagName(IAM_FIRST_NAME_TAG);
        if (firstnameNL.getLength() == 1) {
            firstName = ((Element)firstnameNL.item(0)).getTextContent();
        }

        String language = null;
        NodeList languageNL = userEl.getElementsByTagName(IAM_LANGUAGE_TAG);
        if (languageNL.getLength() == 1) {
            language = ((Element)languageNL.item(0)).getTextContent();
        } else {
            language ="en";
        }

        boolean locked = false;
        if (userEl.hasAttribute(IAM_USER_LOCKED_ATTR)) {
            locked = Boolean.parseBoolean(userEl.getAttribute(IAM_USER_LOCKED_ATTR));
        }

        User user = new User(id, username, password, passwordEncoding, role, jwt, email, firstName, lastName, false, false, null, language, locked);
        return user;
    }

    /**
     * Get all users managed by IAM system
     * @param includingPassword True when password should be returned as well
     * @param includingJWT True when JWT should be returned as well
     */
    protected User[] getAllIAMUsers(boolean includingPassword, boolean includingJWT) throws Exception {
        Document doc = xmlService.read(getUsersXMLFile());
        doc.getDocumentElement().normalize();

        NodeList userNL = doc.getElementsByTagName(IAM_USER_TAG);
        User[] users = new User[userNL.getLength()];
        for (int i = 0; i < users.length; i++) {
            Element userEl = (Element)userNL.item(i);
            users[i] = parseUserElement((Element)userNL.item(i), includingPassword, includingJWT);
        }

        return users;
    }

    /**
     *
     */
    private Role getRole(String role) {
        if (role.equals("ADMIN")) {
            return Role.ADMIN;
        } else if (role.equals("USER")) {
            return Role.USER;
        } else if (role.equals("BENCHMARK")) {
            return Role.BENCHMARK;
        } else {
            log.error("No such role '" + role + "' implemented!");
            return null;
        }
    }
}
