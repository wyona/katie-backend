package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Permissions {

    private String[] userIDs;
    private String[] groupIDs;

    private boolean isPublic;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Permissions() {
    }

    /**
     *
     */
    public Permissions(String[] userIDs, String[] groupIDs) {
        this.userIDs = userIDs;
        this.groupIDs = groupIDs;
    }

    /**
     * @param isPublic True when public
     */
    public Permissions(boolean isPublic) {
        this.isPublic = isPublic;
    }

    /**
     * @return true when public and false otherwise
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     *
     */
    public String[] getUserIDs() {
        return userIDs;
    }

    /**
     * @param id User ID
     * @return true when user is authorized and false otherwise
     */
    public boolean isUserAuthorized(String id) {
        if (userIDs != null) {
            for (int i = 0; i < userIDs.length; i++) {
                if (id.equals(userIDs[i])) {
                    log.info("User '" + userIDs[i] + "' has sufficient permissions.");
                    return true;
                }
            }
        }
        log.info("TODO: Check whether user is member of a group which might be authorized ...");
        return false;
    }

    /**
     *
     */
    public String[] getGroupIDs() {
        return groupIDs;
    }
}
