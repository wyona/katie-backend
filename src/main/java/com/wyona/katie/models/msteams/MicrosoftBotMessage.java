package com.wyona.katie.models.msteams;

import com.wyona.katie.models.MessageValue;
import lombok.extern.slf4j.Slf4j;

/**
 * See https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-api-reference?view=azure-bot-service-4.0
 * Use sudo ngrep -d lo "" "port 6060"
 *
 * Example message:
 *
 * {
 *   "type":"message",
 *   "id":"5fXxxuMPf5gnSy2OnSl67-h|0000001",
 *   "timestamp":"2020-08-30T07:09:43.3133668Z",
 *   "serviceUrl":"https://webchat.botframework.com/",
 *   "channelId":"webchat",
 *   "from":{
 *      "id":"97b40934-6f23-4705-83dd-a60b8451f784",
 *      "name":"You"
 *   },
 *   "conversation":{
 *      "id":"5fXxxuMPf5gnSy2OnSl67-h"
 *   },
 *   "recipient":{
 *      "id":"askkatie@BwUXs4nRsbg",
 *      "name":"Katie"
 *   },
 *   "textFormat":"plain",
 *   "locale":"de-CH",
 *   "text":"Test 2",
 *   "channelData":{
 *      "clientActivityID":"15987713832991mh8krcwc1xj",
 *      "clientTimestamp":"2020-08-30T07:09:43.299Z"
 *   }
 * }
 *
 * Example messaga of type 'conversationUpdate':
 *
 * {
 *   "type":"conversationUpdate",
 *   "id":"779gjw7aGug",
 *   "timestamp":"2020-08-30T10:54:39.3449218Z",
 *   "serviceUrl":"https://webchat.botframework.com/",
 *   "channelId":"webchat",
 *   "from":{
 *      "id":"97b40934-6f23-4705-83dd-a60b8451f784"
 *   },
 *   "conversation":{
 *      "id":"2gayAWelAAp30hpI5gw3Hd-h"
 *   },
 *   "recipient":{
 *      "id":"askkatie@BwUXs4nRsbg",
 *      "name":"Katie"
 *   },
 *   "membersAdded":[
 *      {
 *         "id":"askkatie@BwUXs4nRsbg",
 *         "name":"Katie"
 *      },
 *      {
 *         "id":"97b40934-6f23-4705-83dd-a60b841f784"
 *      }
 *   ]
 * }
 *
 *
 * Example message of channel id 'msteams':
 *
 * {
 *    "text":"Who is Vanya?",
 *    "textFormat":"plain",
 *    "type":"message",
 *    "timestamp":"2020-12-13T21:39:16.1633304Z",
 *    "localTimestamp":"2020-12-13T22:39:16.1633304+01:00",
 *    "id":"1607895556148",
 *    "channelId":"msteams",
 *    "serviceUrl":"https://smba.trafficmanager.net/emea/",
 *    "from":{
 *       "id":"29:1jv2ha38tgKNdva2oayD6AoELwZsUHwsloI2eMc86eJkKW1Nj2aux60NlzPRayHJH8bms6hNatKriCGyLK8xDwA",
 *       "name":"Michael Wechner",
 *       "aadObjectId":"97b40934-6f23-4705-83dd-a60b8451f784"
 *    },
 *    "conversation":{
 *       "conversationType":"personal",
 *       "tenantId":"8704bed1-95d8-4bfc-9438-047bb64b2c83",
 *       "id":"a:1MEv2rVobUaLRnVU5Z5LhvkU6VZ1PvkZUEkk3BXmvVNO1dy3PozquRtkEgF2OEP39Py4KxxDi9cE5_zQHu_Ri0gFrzmXqbsKjhcqiGFpmPg1sVoks55N_Xr6H584afXZR"
 *    },
 *    "recipient":{
 *       "id":"28:aaa8c4a1-d204-468f-ac6e-540b26b3a122",
 *       "name":"askkatie3"
 *    },
 *    "entities":[
 *       {
 *          "locale":"de-DE",
 *          "country":"DE",
 *          "platform":"Mac",
 *          "timezone":"Europe/Zurich",
 *          "type":"clientInfo"
 *       }
 *    ],
 *    "channelData":{
 *       "tenant":{
 *          "id":"8704bed1-95d8-4bfc-9438-047bb64b2c83"
 *       }
 *    },
 *    "locale":"de-DE"
 * }
 *
 * Example message containing channel data (channel Id, team Id, tenant Id)
 * {"text":"<at>katie</at> Who is Ezra?\n","textFormat":"plain","attachments":[{"contentType":"text/html","content":"<div><div><span itemscope=\"\" itemtype=\"http://schema.skype.com/Mention\" itemid=\"0\">katie</span> Who is Ezra?</div>\n</div>"}],"type":"message","timestamp":"2021-01-09T20:13:13.591474Z","localTimestamp":"2021-01-09T21:13:13.591474+01:00","id":"1610223193571","channelId":"msteams","serviceUrl":"https://smba.trafficmanager.net/emea/","from":{"id":"29:1Ia_BSwgfyf67OsIqLNm3ypT7auekd7dsVXazY1IlzKO5kipbpV0XaGcvLWwGMafWoj0Nq6IOyOB1snycSnwQjQ","name":"Michael Wechner","aadObjectId":"97b40934-6f23-4705-83dd-a60b8451f784"},"conversation":{"isGroup":true,"conversationType":"channel","tenantId":"8704bed1-95d8-4bfc-9438-047bb64b2c83","id":"19:6b1b66b1617c4946959c334b4d428fc0@thread.tacv2;messageid=1610223193571"},"recipient":{"id":"28:a888d256-6f0e-4358-b23e-9b644fe0fd64","name":"Katie2"},"entities":[{"mentioned":{"id":"28:a888d256-6f0e-4358-b23e-9b644fe0fd64","name":"katie"},"text":"<at>katie</at>","type":"mention"},{"locale":"de-DE","country":"DE","platform":"Mac","timezone":"Europe/Zurich","type":"clientInfo"}],"channelData":{"teamsChannelId":"19:6b1b66b1617c4946959c334b4d428fc0@thread.tacv2","teamsTeamId":"19:6b1b66b1617c4946959c334b4d428fc0@thread.tacv2","channel":{"id":"19:6b1b66b1617c4946959c334b4d428fc0@thread.tacv2"},"team":{"id":"19:6b1b66b1617c4946959c334b4d428fc0@thread.tacv2"},"tenant":{"id":"8704bed1-95d8-4bfc-9438-047bb64b2c83"}},"locale":"de-DE","localTimezone":"Europe/Zurich"}
 *
 * Example message when clicking on Action.Submit
 *
 * {
 *    "type":"message",
 *    "timestamp":"2023-10-05T12:14:46.605Z",
 *    "localTimestamp":"2023-10-05T14:14:46.605+02:00",
 *    "id":"f:3115a5f8-aeda-f7c0-bc77-1c8c6ce99b04",
 *    "channelId":"msteams",
 *    "serviceUrl":"https://smba.trafficmanager.net/ch/",
 *    "from":{
 *       "id":"29:1lGs5Cd7zlT-i8q09aAHzYrCRi4sjE_YDRN-QoEflAfFXpTF3LSxY-m6xSlKB08FY69tjOr-uEDUxJC5sAHsyzQ",
 *       "name":"Michael Wechner",
 *       "aadObjectId":"47aca620-b9b4-49df-ab4c-0f093ef0ca7c"
 *    },
 *    "conversation":{
 *       "isGroup":true,
 *       "conversationType":"channel",
 *       "tenantId":"c5dce9b8-8095-444d-9730-5ccb69b43413",
 *       "id":"19:d03152c19e8c4d628a8e5ad338387cec@thread.tacv2;messageid=1696507855771",
 *       "name":"Katie Test"
 *    },
 *    "recipient":{
 *       "id":"28:e9d6ff18-084d-4891-97ed-8a7667db3d7a",
 *       "name":"katie7"
 *    },
 *    "entities":[
 *       {
 *          "locale":"de-DE",
 *          "country":"DE",
 *          "platform":"Mac",
 *          "timezone":"Europe/Zurich",
 *          "type":"clientInfo"
 *       }
 *    ],
 *    "channelData":{
 *       "channel":{
 *          "id":"19:d03152c19e8c4d628a8e5ad338387cec@thread.tacv2"
 *       },
 *       "team":{
 *          "id":"19:Jq32E4iv6KWCG1CZyVPBUoQgcgcESZ3G9FRCf60xE781@thread.tacv2"
 *       },
 *       "tenant":{
 *          "id":"c5dce9b8-8095-444d-9730-5ccb69b43413"
 *       },
 *       "source":{
 *          "name":"message"
 *       },
 *       "legacy":{
 *          "replyToId":"1:1q8I-qGzi8lvXuVYP2D_ejRP-A0gIo28_TWd8xpNYUHE"
 *       }
 *    },
 *    "replyToId":"1696507898030",
 *    "value":{
 *       "message":"SEND_BETTER_ANSWER::8c46a0d5-6c18-4c5d-8eb8-5fc91a41f5ed"
 *    },
 *    "locale":"de-DE",
 *    "localTimezone":"Europe/Zurich"
 * }
 *
 */
@Slf4j
public class MicrosoftBotMessage {

    private String id;
    private String type;
    private String text;
    private String serviceUrl;

    private ChannelId channelId;

    private MicrosoftFrom from;
    private MicrosoftConversation conversation;
    private MicrosoftRecipient recipient;
    private MicrosoftEntity[] entities;
    private MicrosoftChannelData channelData;

    private String locale;
    private String localTimezone;

    private MessageValue value;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftBotMessage() {
    }

    /**
     *
     */
    public void setValue(MessageValue value) {
        this.value = value;
    }

    /**
     *
     */
    public MessageValue getValue() {
        return value;
    }

    /**
     *
     */
    public void setLocalTimezone(String localTimezone) {
        this.localTimezone = localTimezone;
    }

    /**
     *
     */
    public String getLocalTimezone() {
        return localTimezone;
    }

    /**
     *
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    /**
     * Get language, e.g. "en-US" or "de"
     */
    public String getLocale() {
        return locale;
    }

    /**
     *
     */
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    /**
     *
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * @param channelId Channel Id, e.g. "msteams" or "webchat"
     */
    public void setChannelId(String channelId) {
        this.channelId = ChannelId.valueOf(channelId);
    }

    /**
     * @return channel Id, e.g. "msteams" or "webchat"
     */
    public ChannelId getChannelId() {
        if (channelId == null) {
            return ChannelId.UNDEFINED;
        }
        return channelId;
    }

    /**
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     *
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
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     */
    public String getText() {
        return text;
    }

    /**
     *
     */
    public void setFrom(MicrosoftFrom from) {
        this.from = from;
    }

    /**
     *
     */
    public MicrosoftFrom getFrom() {
        return from;
    }

    /**
     *
     */
    public void setChannelData(MicrosoftChannelData channelData) {
        this.channelData = channelData;
    }

    /**
     *
     */
    public MicrosoftChannelData getChannelData() {
        return channelData;
    }

    /**
     *
     */
    public void setConversation(MicrosoftConversation conversation) {
        this.conversation = conversation;
    }

    /**
     *
     */
    public MicrosoftConversation getConversation() {
        return conversation;
    }

    /**
     *
     */
    public void setEntities(MicrosoftEntity[] entities) {
        this.entities = entities;
    }

    /**
     *
     */
    public MicrosoftEntity[] getEntities() {
        return entities;
    }

    /**
     *
     */
    public void setRecipient(MicrosoftRecipient recipient) {
        this.recipient = recipient;
    }

    /**
     *
     */
    public MicrosoftRecipient getRecipient() {
        return recipient;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("Type: " + type);
        s.append(", Text: " + text);
        s.append(", Id: " + id);
        s.append(", Service URL: " + serviceUrl);

        s.append(", From: " + from);
        s.append(", Conversation: " + conversation);
        s.append(", Recipient: " + recipient);
        s.append(", Entities: " + entities);
        s.append(", Channel " + channelData);

        s.append(", Locale: " + locale);
        s.append(", Local timezone: " + localTimezone);

        s.append(", Message: " + value);

        return s.toString();
    }
}
