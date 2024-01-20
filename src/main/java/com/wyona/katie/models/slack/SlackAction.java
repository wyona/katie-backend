package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackAction {

    private String action_id;
    private String value;
    private String type;
    private SlackText text;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackAction() {
    }

    /**
     *
     */
    public void setText(SlackText text) {
        this.text = text;
    }

    /**
     *
     */
    public SlackText getText() {
        return text;
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
    public void setAction_id(String action_id) {
        this.action_id = action_id;
    }

    /**
     *
     */
    public String getAction_id() {
        return action_id;
    }

    /**
     *
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *
     */
    public String getValue() {
        return value;
    }
}
