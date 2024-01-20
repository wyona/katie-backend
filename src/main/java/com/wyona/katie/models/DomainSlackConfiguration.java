package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class DomainSlackConfiguration {

    private boolean buttonSendToExpertEnabled;
    private boolean buttonImproveAnswerEnabled;
    private boolean buttonLoginKatieEnabled;
    private boolean buttonAnswerQuestionEnabled;

    private String subdomain;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public DomainSlackConfiguration() {
        this.buttonSendToExpertEnabled = true;
        this.buttonImproveAnswerEnabled = true;
        this.buttonLoginKatieEnabled = true;
        this.buttonAnswerQuestionEnabled = true;

        this.subdomain = null;
    }

    /**
     * @param buttonSendToExpertEnabled True when button send question to expert is enabled, false otherwise
     * @param buttonImproveAnswerEnabled True when button to improve/correct answer is enabled, false otherwise
     * @param buttonLoginKatieEnabled True when button to log into Katie is enabled, false otherwise
     * @param buttonAnswerQuestionEnabled True when button to answer question is enabled, false otherwise
     * @param subdomain Subdomain of Slack team, e.g. "wyona-katie" (https://wyona-katie.slack.com)
     */
    public DomainSlackConfiguration(boolean buttonSendToExpertEnabled, boolean buttonImproveAnswerEnabled, boolean buttonLoginKatieEnabled, boolean buttonAnswerQuestionEnabled, String subdomain) {
        this.buttonSendToExpertEnabled = buttonSendToExpertEnabled;
        this.buttonImproveAnswerEnabled = buttonImproveAnswerEnabled;
        this.buttonLoginKatieEnabled = buttonLoginKatieEnabled;
        this.buttonAnswerQuestionEnabled = buttonAnswerQuestionEnabled;

        this.subdomain = subdomain;
    }

    /**
     * @return true when button send question to expert is enabled, false otherwise
     */
    public boolean getButtonSendToExpertEnabled() {
        return buttonSendToExpertEnabled;
    }

    /**
     * @return true when button to improve/correct answer is enabled, false otherwise
     */
    public boolean getButtonImproveAnswerEnabled() {
        return buttonImproveAnswerEnabled;
    }

    /**
     * @return true when button to log into Katie is enabled, false otherwise
     */
    public boolean getButtonLoginKatieEnabled() {
        return buttonLoginKatieEnabled;
    }

    /**
     *
     */
    public boolean getButtonAnswerQuestionEnabled() {
        return buttonAnswerQuestionEnabled;
    }

    /**
     * @return subdomain of Slack team, e.g. "wyona-katie" (https://wyona-katie.slack.com)
     */
    public String getSubdomain() {
        return subdomain;
    }
}
