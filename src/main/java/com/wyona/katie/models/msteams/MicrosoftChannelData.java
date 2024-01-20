package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftChannelData {

    private String channelId;
    private String teamId;
    private MicrosoftTeam team;
    private MicrosoftChannel channel;
    private MicrosoftTenant tenant;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftChannelData() {
    }

    /**
     *
     */
    public void setTeam(MicrosoftTeam team) {
        this.team = team;
    }

    /**
     *
     */
    public MicrosoftTeam getTeam() {
        return team;
    }

    /**
     *
     */
    public void setChannel(MicrosoftChannel channel) {
        this.channel = channel;
    }

    /**
     *
     */
    public MicrosoftChannel getChannel() {
        return channel;
    }

    /**
     *
     */
    public void setTenant(MicrosoftTenant tenant) {
        this.tenant = tenant;
    }

    /**
     *
     */
    public MicrosoftTenant getTenant() {
        return tenant;
    }

    /**
     *
     */
    public void setTeamsChannelId(String channelId) {
        this.channelId = channelId;
    }

    /**
     *
     */
    public String getTeamsChannelId() {
        return channelId;
    }

    /**
     *
     */
    public void setTeamsTeamId(String teamId) {
        this.teamId = teamId;
    }

    /**
     *
     */
    public String getTeamsTeamId() {
        return teamId;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Team Id: " + teamId + ", Channel Id: " + channelId);
        return s.toString();
    }
}
