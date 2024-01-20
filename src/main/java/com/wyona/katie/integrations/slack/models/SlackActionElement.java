package com.wyona.katie.integrations.slack.models;

import com.wyona.katie.models.ChannelAction;

/**
 *
 */
public class SlackActionElement {

    private String label;
    private String value;
    private ChannelAction action;

    /**
     * @param label Action button label, e.g. "Send question to expert ..." or "Get protected answer ..."
     * @param value Action value, e.g. question asked or JWT
     */
    public SlackActionElement(String label, String value, ChannelAction action) {
        this.label = label;
        this.value = value;
        this.action = action;
    }

    /**
     *
     */
    public String getLabel() {
        return label;
    }

    /**
     *
     */
    public String getValue() {
        return value;
    }

    /**
     *
     */
    public ChannelAction getAction() {
        return action;
    }
}
