package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SelfRegistrationInformation {

    private String email;
    private String firstName;
    private String lastName;
    private String language;

    private String linkedInUrl;
    private String howDidYouLearnAboutKatie;
    private String howDoYouWantToUseKatie;
    private String whatAreYourExpectations;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SelfRegistrationInformation() {
    }

    /**
     * @param linkedInUrl LinkedIn URL, e.g. "https://www.linkedin.com/in/michaelwechner/"
     * @param howDidYouLearnAboutKatie Answer to the question "How did you learn about Katie?"
     */
    public SelfRegistrationInformation(String linkedInUrl, String howDidYouLearnAboutKatie, String howDoYouWantToUseKatie, String whatAreYourExpectations) {
        this.linkedInUrl = linkedInUrl;
        this.howDidYouLearnAboutKatie = howDidYouLearnAboutKatie;
        this.howDoYouWantToUseKatie = howDoYouWantToUseKatie;
        this.whatAreYourExpectations = whatAreYourExpectations;
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
    public String getEmail() {
        return email;
    }

    /**
     *
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     *
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     *
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     *
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param language Language of user, e.g. "de" or "en"
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * @return language of user, e.g. "de" or "en"
     */
    public String getLanguage() {
        return language;
    }

    /**
     *
     */
    public void setLinkedInUrl(String linkedInUrl) {
        this.linkedInUrl = linkedInUrl;
    }

    /**
     *
     */
    public String getLinkedInUrl() {
        return linkedInUrl;
    }

    /**
     *
     */
    public void setHowDidYouLearnAboutKatie(String howDidYouLearnAboutKatie) {
        this.howDidYouLearnAboutKatie = howDidYouLearnAboutKatie;
    }

    /**
     *
     */
    public String getHowDidYouLearnAboutKatie() {
        return howDidYouLearnAboutKatie;
    }

    /**
     *
     */
    public void setHowDoYouWantToUseKatie(String howDoYouWantToUseKatie) {
        this.howDoYouWantToUseKatie = howDoYouWantToUseKatie;
    }

    /**
     *
     */
    public String getHowDoYouWantToUseKatie() {
        return howDoYouWantToUseKatie;
    }

    /**
     *
     */
    public void setWhatAreYourExpectations(String whatAreYourExpectations) {
        this.whatAreYourExpectations = whatAreYourExpectations;
    }

    /**
     *
     */
    public String getWhatAreYourExpectations() {
        return whatAreYourExpectations;
    }
}
