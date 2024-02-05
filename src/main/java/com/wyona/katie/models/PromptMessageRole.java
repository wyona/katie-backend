package com.wyona.katie.models;

/**
 *
 */
public enum PromptMessageRole {

    // INFO: enum constants calling the enum constructor
    USER("user"),
    SYSTEM("system"),
    ASSISTANT("assistant");

    private String role;

    /**
     * @param role Role, e.g. "user"
     */
    private PromptMessageRole(String role) {
        this.role = role;
    }

    /**
     * @return role, e.g. "user"
     */
    @Override
    public String toString() {
        return role;
    }

    /**
     * @param text Role as string, e.g. "user"
     * @return role as enum, e.g. PromptMessageRole.USER
     */
    public static PromptMessageRole fromString(String text) {
        for (PromptMessageRole role : PromptMessageRole.values()) {
            if (role.toString().equals(text)) {
                return role;
            }
        }
        return null;
    }
}
