package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * See https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-api-reference?view=azure-bot-service-4.0
 *
 * Example response:
 *
 * {
 *    "type": "message",
 *    "from": {
 *        "id": "12345678",
 *        "name": "bot's name"
 *    },
 *    "conversation": {
 *        "id": "abcd1234",
 *        "name": "conversation's name"
 *    },
 *   "recipient": {
 *        "id": "1234abcd",
 *        "name": "user's name"
 *    },
 *    "text": "I have several times available on Saturday!",
 *    "replyToId": "bf3cc9a2f5de..."
 * }
 *
 */
@Slf4j
public class MicrosoftResponse {

    private String type;
    private String text;
    private String textFormat;
    private String replyToId;

    private MicrosoftFrom from;
    private MicrosoftConversation conversation;
    private MicrosoftRecipient recipient;

    private List<MicrosoftAttachment> attachments;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftResponse() {
        this.type = "message";
        this.textFormat = "plain";
        this.attachments = new ArrayList<MicrosoftAttachment>();
    }

    /**
     *
     */
    public void addAttachment(MicrosoftAttachment attachment) {
        attachments.add(attachment);
    }

    /**
     *
     */
    public MicrosoftAttachment[] getAttachments() {
        return attachments.toArray(new MicrosoftAttachment[0]);
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
    public void setReplyToId(String replyToId) {
        this.replyToId = replyToId;
    }

    /**
     *
     */
    public String getReplyToId() {
        return replyToId;
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
     * @param textFormat Text format, e.g. "plain" or "xml" or "markdown"
     */
    public void setTextFormat(String textFormat) {
        this.textFormat = textFormat;
    }

    /**
     *
     */
    public String getTextFormat() {
        return textFormat;
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
        StringBuilder s = new StringBuilder("Type: " + type + ", Text: " + text);
        s.append(", Reply to Id: " + replyToId);
        s.append(", From: " + from);
        s.append(", Conversation: " + conversation);
        s.append(", Recipient: " + recipient);
        //s.append(", Suggested Actions: " + actions);
        return s.toString();
    }
}
