package com.wyona.katie.models.slack;

public class SlackAuthorizations {

    private boolean isBot;
    private String teamId;
    private String userId;

    /**
     *
     */
    public void setTeam_id(String teamId) {
        this.teamId = teamId;
    }

    /**
     *
     */
    public String getTeam_id() {
        return teamId;
    }

    /**
     *
     */
    public void setUser_id(String userId) {
        this.userId = userId;
    }

    /**
     *
     */
    public String getUser_id() {
        return userId;
    }

    /**
     *
     */
    public void setIs_bot(boolean isBot) {
        this.isBot = isBot;
    }

    /**
     *
     */
    public boolean getIs_bot() {
        return isBot;
    }
}
