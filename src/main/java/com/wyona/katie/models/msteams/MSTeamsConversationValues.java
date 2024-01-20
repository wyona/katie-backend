package com.wyona.katie.models.msteams;

import java.util.Date;

public class MSTeamsConversationValues {

    private String uuid;
    private String domainId;

    private String serviceUrl;
    private String conversationId;
    private String messageId;
    private String katieBotId;
    private String msTeamsUserId;

    private String teamId;
    private String channelId;

    /**
     * @param uuid Channel request UUID
     * @param serviceUrl MS Teams service URL, e.g. "https://smba.trafficmanager.net/ch/"
     * @param conversationId Conversation Id, e.g. a:1D7hLMz0ISuiD0MNVAX0InPTp8vvCHcyiKUZdTlNxjjizqvytsr6NawjEjcoBzsCTbq0oXuKapAQTyirjJ9zzXG05p0Hm0unp0m8FC0dAS_LLVrvuZlNNZZ2lSRVRAQ6h
     * @param messageId Message Id, e.g. f:7ffe82f6-9862-730e-af94-c92611339712
     * @param katieBotId Katie bot Id, e.g. 28:a888d256-6f0e-4358-b23e-9b644fe0fd64
     * @param msTeamsUserId MS Teams user Id, e.g. 29:1EJDwgL0u2JE_2RUJIb7BciubbOTOmD-IxXHEim0k_fqkcijFbagVLa1a8ymq5FbdJp2JaznK_bHrZ7gpyGg5Ug
     */
    public MSTeamsConversationValues(String uuid, String teamId, String channelId, String serviceUrl, String conversationId, String messageId, String katieBotId, String msTeamsUserId) {
        this.uuid = uuid;
        this.teamId = teamId;
        this.channelId = channelId;

        this.serviceUrl = serviceUrl;
        this.conversationId = conversationId;
        this.messageId = messageId;
        this.katieBotId = katieBotId;
        this.msTeamsUserId = msTeamsUserId;
    }

    /**
     * @return UUID of channel request
     */
    public String getUuid() {
        return uuid;
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
     *
     */
    public String getDomainId() {
        return domainId;
    }

    /**
     * @param domainId Domain Id resubmitted question is associated with
     */
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    /**
     *
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     *
     */
    public String getConversationId() {
        return conversationId;
    }

    /**
     *
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     *
     */
    public String getKatieBotId() {
        return katieBotId;
    }

    /**
     *
     */
    public String getMsTeamsUserId() {
        return msTeamsUserId;
    }
}
