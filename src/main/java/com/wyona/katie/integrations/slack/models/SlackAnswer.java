package com.wyona.katie.integrations.slack.models;

import java.util.List;
import java.util.ArrayList;

/**
 *
 */
public class SlackAnswer {

    private String answer;
    private String format;
    private List<SlackActionElement> elements;

    /**
     * @oaram answer Answer text
     * @param format Answer format, e.g. "mrkdwn" or "plain_text"
     */
    public SlackAnswer(String answer, String format) {
        this.answer = answer;
        this.format = format;
        elements = new ArrayList<SlackActionElement>();
    }

    /**
     *
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * @return answer formar, e.g. "mrkdwn" or "plain_text"
     */
    public String getFormat() {
        return format;
    }

    /**
     *
     */
    public void addElement(SlackActionElement element) {
        elements.add(element);
    }

    /**
     *
     */
    public SlackActionElement[] getElements() {
        return elements.toArray(new SlackActionElement[0]);
    }
}
