package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.Date;

import java.io.Serializable;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Question which was asked by user, for example via Slack channel or FAQ web page search form
 */
@Slf4j
public class AskedQuestion implements Serializable {

    private String uuid;
    private String domainId;
    private String question;
    private List<String> classifications;
    private String remoteAddress;
    private Date timestamp;

    private String username;
    private String qnaUUID;
    private double score;
    private Double scoreThreshold;
    private String permissionStatus;
    private String moderationStatus;

    private ChannelType channelType;
    private String channelRequestId;

    private String clientMessageId;

    private String slackTeamId;
    private String slackChannelId;
    private String slackMsgTimestamp;
    private String subdomain;

    private String discordGuildId;
    private String discordChannelId;

    private String msTeamsId;
    private String msTeamsChannelId;

    private int offset;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public AskedQuestion() {
        this.classifications = new ArrayList<String>();
    }

    /**
     * @param uuid UUID of search /ask request (Database Id of entry)
     * @param domainId Doamin Id for which question was asked, e.g. "jmeter"
     * @param question Asked question, e.g. "When to get funding?"
     * @param classifications Submitted classifications, e.g. "heating"
     * @param qnaUUID UUID of QnA / answer which was suggested by Katie
     * @param score Score of answer
     * @param scoreThreshold Score threshold applied at the time question was asked
     * @param channelRequestId Channel request Id, e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af"
     * @param offset Results offset when question was asked
     */
    public AskedQuestion(String uuid, String domainId, String question, List<String> classifications, String remoteAddress, Date timestamp, String username, String qnaUUID, double score, Double scoreThreshold, String permissionStatus, String moderationStatus, ChannelType channelType, String channelRequestId, int offset) {
        this.uuid = uuid;
        this.domainId = domainId;
        this.question = question;
        this.classifications = classifications;
        this.remoteAddress = remoteAddress;
        this.timestamp = timestamp;
        this.username = username;
        this.qnaUUID = qnaUUID;
        this.score = score;
        this.scoreThreshold = scoreThreshold;
        this.permissionStatus = permissionStatus;
        this.moderationStatus = moderationStatus;

        this.channelType = channelType;
        this.channelRequestId = channelRequestId;

        this.offset = offset;
    }

    /**
     * Get results offset when question was asked. If offset is greater than 0, then it means that best ranked answer was probably not helpful and user was looking for next best answer
     */
    public int getOffset() {
        return offset;
    }

    /**
     *
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     *
     */
    public void setTimestamp(Date timestamp) {
        log.info("TODO: Implement setter!");
    }

    /**
     * Get UUID of search / ask request
     */
    public String getUUID() {
        return uuid;
    }

    /**
     *
     */
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     *
     */
    public String getDomainId() {
        return domainId;
    }

    /**
     *
     */
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    /**
     *
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     *
     */
    public void setRemoteAddress(String remoteAddress) {
        log.info("TODO: Implement setter!");
    }

    /**
     * Get question which was submitted by user
     */
    public String getQuestion() {
        return question;
    }

    /**
     *
     */
    public void setQuestion(String question) {
        log.info("TODO: Implement setter!");
    }

    /**
     *
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     * Get username of user which submitted question
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get UUID of QnA / answer which was suggested by Katie
     */
    public String getAnswerUUID() {
        return qnaUUID;
    }

    /**
     * Get score of answer
     */
    public double getScore() {
        return score;
    }

    /**
     * Get score threshold which was applied at the time when question was asked
     */
    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    /**
     * Get permission status
     */
    public String getPermissionStatus() {
        return permissionStatus;
    }

    /**
     * Get moderation status
     */
    public String getModerationStatus() {
        return moderationStatus;
    }

    /**
     * Get channel type
     */
    public ChannelType getChannelType() {
        return channelType;
    }

    /**
     * Get channel request Id, e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af"
     */
    public String getChannelRequestId() {
        return channelRequestId;
    }

    /**
     *
     */
    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    /**
     *
     */
    public String getClientMessageId() {
        return clientMessageId;
    }

    /**
     *
     */
    public void setSlackSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    /**
     *
     */
    public String getSlackSubdomain() {
        return subdomain;
    }

    /**
     *
     */
    public void setSlackTeamId(String teamId) {
        this.slackTeamId = teamId;
    }

    /**
     *
     */
    public String getSlackTeamId() {
        return slackTeamId;
    }

    /**
     *
     */
    public void setSlackChannelId(String channelId) {
        this.slackChannelId = channelId;
    }

    /**
     *
     */
    public String getSlackChannelId() {
        return slackChannelId;
    }

    /**
     *
     */
    public void setSlackMsgTimestamp(String msgTimestamp) {
        this.slackMsgTimestamp = msgTimestamp;
    }

    /**
     *
     */
    public String getSlackMsgTimestamp() {
        return slackMsgTimestamp;
    }

    /**
     *
     */
    public void setDiscordGuildId(String guildId) {
        this.discordGuildId = guildId;
    }

    /**
     *
     */
    public String getDiscordGuildId() {
        return discordGuildId;
    }

    /**
     *
     */
    public void setDiscordChannelId(String channelId) {
        this.discordChannelId = channelId;
    }

    /**
     *
     */
    public String getDiscordChannelId() {
        return discordChannelId;
    }

    /**
     *
     */
    public void setMsTeamsId(String msTeamsId) {
        this.msTeamsId = msTeamsId;
    }

    /**
     *
     */
    public String getMsTeamsId() {
        return msTeamsId;
    }

    /**
     *
     */
    public void setMsTeamsChannelId(String msTeamsChannelId) {
        this.msTeamsChannelId = msTeamsChannelId;
    }

    /**
     *
     */
    public String getMsTeamsChannelId() {
        return msTeamsChannelId;
    }
}
