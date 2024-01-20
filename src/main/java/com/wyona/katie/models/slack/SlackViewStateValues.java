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
