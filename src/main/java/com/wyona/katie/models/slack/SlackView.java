package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackView {

    private String callback_id;
    private String privateMetadata;
    private String type;
    private SlackViewState state;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackView() {
    }

    /**
     *
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get view type, e.g. "modal"
     */
    public String getType() {
        return type;
    }

    /**
     *
     */
    public void setCallback_id(String callback_id) {
        this.callback_id = callback_id;
    }

    /**
     * Get callback Id, e.g. "connect_domain"
     */
    public String getCallback_id() {
        return callback_id;
    }

    /**
     *
     */
    public void setPrivate_metadata(String privateMetadata) {
        this.privateMetadata = privateMetadata;
    }

    /**
     *
     */
    public String getPrivate_metadata() {
        return privateMetadata;
    }

    /**
     *
     */
    public void setState(SlackViewState state) {
        this.state = state;
    }

    /**
     *
     */
    public SlackViewState getState() {
        return state;
    }
}