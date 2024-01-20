package com.wyona.katie.models;

import java.util.HashMap;

/**
 *
 */
public class Error {

    private String message;
    private String code;
    private HashMap<String, String> properties;

    /**
     * @param message Human readable error message
     */
    public Error(String message, String code) {
        this.message = message;
        this.code = code;
        this.properties = null;
    }

    /**
     * @param message Human readable error message
     * @param properties Additional properties
     */
    public Error(String message, String code, HashMap<String, String> properties) {
        this.message = message;
        this.code = code;
        this.properties = properties;
    }

    /**
     *
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     */
    public String getCode() {
        return code;
    }

    /**
     *
     */
    public HashMap<String, String> getProperties() {
        return properties;
    }
}
