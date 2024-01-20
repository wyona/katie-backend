package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
public class MatchReplyToEmails {

    private List<String> emails;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MatchReplyToEmails() {
        emails = new ArrayList<String>();
    }

    /**
     *
     */
    public void addEmail(String email) {
        emails.add(email);
    }

    /**
     *
     */
    public String[] getEmails() {
        return emails.toArray(new String[0]);
    }
}
