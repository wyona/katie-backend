package com.wyona.katie.models.slack;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackViewStateValues {

    private SlackNodeDomainId domainId;
    private SlackNodeChannelId channelId;
    private SlackNodeEmail email;
    private SlackNodeBetteranswer betterAnswer;
    private SlackNodeRelevanturl relevanturl;

    public static final String BLOCK_ID_CHANNEL_ID = "channel_id";
    public static final String BLOCK_ID_DOMAIN_ID = "domain_id";
    public static final String BLOCK_ID_EMAIL = "email";
    public static final String BLOCK_ID_BETTER_ANSWER = "betteranswer";
    public static final String BLOCK_ID_RELEVANT_URL = "relevanturl";

    /**
     *
     */
    public SlackViewStateValues() {
    }

    /**
     *
     */
    public void setDomain_id(SlackNodeDomainId domainId) {
        this.domainId = domainId;
    }

    /**
     *
     */
    public SlackNodeDomainId getDomain_id() {
        return domainId;
    }

    /**
     *
     */
    public void setEmail(SlackNodeEmail email) {
        this.email = email;
    }

    /**
     *
     */
    public SlackNodeEmail getEmail() {
        return email;
    }

    /**
     *
     */
    public void setBetteranswer(SlackNodeBetteranswer betteranswer) {
        this.betterAnswer = betteranswer;
    }

    /**
     *
     */
    public SlackNodeBetteranswer getBetteranswwer() {
        return betterAnswer;
    }

    /**
     *
     */
    public void setRelevanturl(SlackNodeRelevanturl relevanturl) {
        this.relevanturl = relevanturl;
    }

    /**
     *
     */
    public SlackNodeRelevanturl getRelevanturl() {
        return relevanturl;
    }

    /**
     *
     */
    public void setChannel_id(SlackNodeChannelId channelId) {
        this.channelId = channelId;
    }

    /**
     *
     */
    public SlackNodeChannelId getChannel_id() {
        return channelId;
    }
}
