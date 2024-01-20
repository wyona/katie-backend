package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackNodeInputValue {

    private String value;

    /**
     *
     */
    public SlackNodeInputValue() {
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
