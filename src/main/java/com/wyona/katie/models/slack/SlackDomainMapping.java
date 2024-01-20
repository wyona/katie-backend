package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;
import java.util.Date;

/**
 * Mapping between Slack team/channel and Katie domain
 */
@Slf4j
public class SlackDomainMapping {

    private String teamId;
    private String channelId;
    private String domainId;
    private Date created;
    private ConnectStatus status;
    private String token;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackDomainMapping() {
    }

    /**
     * @param teamId Slack team Id
     * @param status Approval status
     * @param token Approval token
     */
    public SlackDomainMapping(String teamId, String channelId, String domainId, Date created, ConnectStatus status, String token) {
        this.teamId = teamId;
        this.channelId = channelId;
        this.domainId = domainId;
        this.created = created;
        this.status = status;
        this.token = token;
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
    public String getTeamId() {
        return teamId;
    }

    /**
     *
     */
    public String getChannelId() {
        return channelId;
    }

    /**
     * Get date when mapping was created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * Get status of whether connection between team/channel and domain is approved
     */
    public ConnectStatus getStatus() {
        return status;
    }

    /**
     * Get JWT token to approve connection between team/channel and domain
     */
    public String getApprovalToken() {
        return token;
    }
}
