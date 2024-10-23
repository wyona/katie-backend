package com.wyona.katie.models;

/**
 *
 */
public enum PromptMessageRoleLowerCase {

    // INFO: enum constants calling the enum constructor
    user("user"),
    system("system"),
    assistant("assistant"),
    tool("tool");

    private String role;

    /**
     * @param role Role, e.g. "user"
     */
    private PromptMessageRoleLowerCase(String role) {
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
     * @return role as enum, e.g. PromptMessageRole.user
     */
    public static PromptMessageRoleLowerCase fromString(String text) {
        for (PromptMessageRoleLowerCase role : PromptMessageRoleLowerCase.values()) {
            if (role.toString().equals(text)) {
                return role;
            }
        }
        return null;
    }
}
