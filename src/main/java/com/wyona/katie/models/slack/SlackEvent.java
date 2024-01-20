package com.wyona.katie.models.slack;

//https://slack.dev/java-slack-sdk/guides/events-api
//import com.slack.api.model.event.MessageEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * See https://api.slack.com/types/event
 */
@Slf4j
public class SlackEvent {

    private String client_msg_id;
    private String bot_id;
    //TODO: private BotProfile bot_profile;
    private String type;
    private String subtype;
    private String channel;
    private String userId;
    private String teamId;
    private String text;
    private String ts;
    private String event_ts;
    private String thread_ts;
    private String channel_type;

    private String inviter_id;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackEvent() {
    }

    /**
     *
     */
    public void setInviter(String inviter_id) {
        this.inviter_id = inviter_id;
    }

    /**
     *
     */
    public String getInviter() {
        return inviter_id;
    }

    /**
     * @param bot_id Bot Id, e.g. "B018B5MRHFW"
     */
    public void setBot_id(String bot_id) {
        this.bot_id = bot_id;
    }

    /**
     *
     */
    public String getBot_id() {
        return bot_id;
    }

    /**
     *
     */
    public void setClient_msg_id(String client_msg_id) {
        this.client_msg_id = client_msg_id;
    }

    /**
     *
     */
    public String getClient_msg_id() {
        return client_msg_id;
    }

    /**
     * @param channel Channel Id, e.g. "C018TT68E72"
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return channel Id, e.g. "C018TT68E72"
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param text Message text, e.g. "What are the names of the sons of Michael?"
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return message text, e.g. "What are the names of the sons of Michael?"
     */
    public String getText() {
        return text;
    }

    /**
     *
     */
    public void setUser(String userId) {
        this.userId = userId;
    }

    /**
     * Get user Id, e.g. "U018F80DU1C"
     */
    public String getUser() {
        return userId;
    }

    /**
     *
     */
    public void setTeam(String teamId) {
        this.teamId = teamId;
    }

    /**
     * Get team Id, e.g. "T01548J69AU"
     */
    public String getTeam() {
        return teamId;
    }

    /**
     * @param type Event type, e.g. "message"
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     */
    public String getType() {
        return type;
    }

    /**
     *
     */
    public void setSubtype(String subtype) {
        this.subtype = subtype;
    }

    /**
     *
     */
    public String getSubtype() {
        return subtype;
    }

    /**
     * @param ts Message timestamp, e.g. "1642975755.001100"
     */
    public void setTs(String ts) {
        this.ts = ts;
    }

    /**
     * @return message timestamp, e.g. "1642975755.001100"
     */
    public String getTs() {
        return ts;
    }

    /**
     * @param thread_ts Thread timestamp, e.g. "1642975607.000500"
     */
    public void setThread_ts(String thread_ts) {
        this.thread_ts = thread_ts;
    }

    /**
     * @return thread timestamp, e.g. "1642975607.000500"
     */
    public String getThread_ts() {
        return thread_ts;
    }

    /**
     *
     */
    public void setChannel_type(String channel_type) {
        this.channel_type = channel_type;
    }

    /**
     * @return channel type, e.g. "channel" (regular channel) or "im" (direct message between user and Katie)
     */
    public String getChannel_type() {
        return channel_type;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Type: " + type + ", Subtype: " + subtype + ", User: " + userId + ", Team: " + teamId + ", Channel: " + channel + ", Text: " + text + ", Channel type: " + channel_type;
    }
}
