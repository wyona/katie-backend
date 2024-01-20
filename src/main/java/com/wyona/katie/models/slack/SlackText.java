package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackText {

    private String type;
    private String text;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackText() {
    }

    /**
     * @param text Text, e.g. "Send question to expert ..."
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
     * @param type Text type, e.g. "plain_text"
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
}
